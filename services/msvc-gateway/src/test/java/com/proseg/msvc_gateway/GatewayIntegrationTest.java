package com.proseg.msvc_gateway;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "management.endpoints.web.exposure.include=health,info",
                "cors.allowed-origins=http://localhost:5173,http://localhost:5174"
        }
)
@Import(GatewayIntegrationTest.JwtTestConfiguration.class)
class GatewayIntegrationTest {

    private static final AtomicReference<CapturedRequest> CAPTURED = new AtomicReference<>();
    private static final DisposableServer BACKEND = startBackend();

    @LocalServerPort
    private int gatewayPort;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @AfterAll
    static void stopBackend() {
        BACKEND.disposeNow();
    }

    @Test
    void declaresAllConfiguredRoutesAndEmailPrefixRewrite() {
        Map<String, RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectMap(RouteDefinition::getId)
                .block();

        assertThat(routes).containsKeys(
                "msvc-auth", "msvc-auth-user", "msvc-auth-role", "gateway-actuator",
                "msvc-email", "msvc-inventory", "msvc-archive", "msvc-transport",
                "msvc-document-processor", "msvc-maintenance");
        assertThat(routes.get("msvc-email").getFilters())
                .anySatisfy(filter -> {
                    assertThat(filter.getName()).isEqualTo("StripPrefix");
                    assertThat(filter.getArgs()).containsValue("2");
                });
    }

    @DynamicPropertySource
    static void configureBackend(DynamicPropertyRegistry registry) {
        String baseUrl = "http://127.0.0.1:" + BACKEND.port();
        registry.add("AUTH_SERVICE_URL", () -> baseUrl);
        registry.add("services.auth.url", () -> baseUrl);
        registry.add("EMAIL_SERVICE_URL", () -> baseUrl);
        registry.add("INVENTORY_SERVICE_URL", () -> baseUrl);
        registry.add("ARCHIVE_SERVICE_URL", () -> baseUrl);
        registry.add("TRANSPORT_SERVICE_URL", () -> baseUrl);
        registry.add("DOCUMENT_PROCESSOR_SERVICE_URL", () -> baseUrl);
        registry.add("MAINTENANCE_SERVICE_URL", () -> baseUrl);
    }

    @Test
    void forwardsMethodsQueriesHeadersAndBodiesAcrossConfiguredRoutes() {
        WebTestClient client = client();
        String body = "{\"message\":\"gateway-test\"}";

        client.method(HttpMethod.POST)
                .uri("/api/v1/archive/files/123?download=true&version=2")
                .header("X-Request-Id", "request-123")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Backend", "local-test")
                .expectBody(String.class).isEqualTo(body);

        CapturedRequest captured = CAPTURED.get();
        assertThat(captured.method()).isEqualTo("POST");
        assertThat(captured.path()).isEqualTo("/api/v1/archive/files/123");
        assertThat(captured.query()).isEqualTo("download=true&version=2");
        assertThat(captured.body()).isEqualTo(body);
        assertThat(captured.requestId()).isEqualTo("request-123");
    }

    @Test
    void appliesEmailStripPrefixAndPreservesOtherServicePrefixes() {
        WebTestClient client = client();

        client.get().uri("/api/email/notifications/send?channel=mail")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(Instant.now().plusSeconds(120), List.of()))
                .exchange()
                .expectStatus().isForbidden();

        client.get().uri("/api/v1/maintenance/requests/42")
                .exchange()
                .expectStatus().isOk();
        assertThat(CAPTURED.get().path()).isEqualTo("/api/v1/maintenance/requests/42");
    }

    @Test
    void routesEveryConfiguredServiceToTheLocalBackend() {
        WebTestClient client = client();
        Map<String, String> routes = Map.of(
                "/api/auth/login", "/api/auth/login",
                "/api/user/statuses", "/api/user/statuses",
                "/api/role/base", "/api/role/base",
                "/api/v1/inventory/assets", "/api/v1/inventory/assets",
                "/api/v1/archive/files/x", "/api/v1/archive/files/x",
                "/api/v1/transport/vehicles", "/api/v1/transport/vehicles",
                "/api/v1/document-processor/exports/assets", "/api/v1/document-processor/exports/assets",
                "/api/v1/maintenance/requests", "/api/v1/maintenance/requests"
        );

        routes.forEach((requestPath, expectedPath) -> {
            client.get().uri(requestPath).exchange().expectStatus().isOk();
            assertThat(CAPTURED.get().path()).isEqualTo(expectedPath);
        });
    }

    @Test
    void propagatesBackendErrorStatusAndResponseBody() {
        client().get().uri("/api/v1/archive/files/missing")
                .header("X-Test-Status", "404")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class).isEqualTo("backend-error");

        client().get().uri("/api/v1/archive/files/unavailable")
                .header("X-Test-Status", "503")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody(String.class).isEqualTo("backend-error");
    }

    @Test
    void rejectsMalformedAndCryptographicallyInvalidBearerTokens() {
        client().get().uri("/api/v1/archive/files/private")
                .header(HttpHeaders.AUTHORIZATION, "Bearer malformed")
                .exchange()
                .expectStatus().isUnauthorized();

        String valid = token(Instant.now().plusSeconds(120), List.of("LEER_ARCHIVO"));
        String altered = valid.substring(0, valid.length() - 2) + "xx";
        client().get().uri("/api/v1/archive/files/private")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + altered)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rejectsExpiredSignedJwt() {
        String token = token(Instant.now().minusSeconds(120), List.of());

        client().get().uri("/api/v1/archive/files/private")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void acceptsLocallySignedJwtAndForwardsTheBearerHeader() {
        String token = token(Instant.now().plusSeconds(120), List.of("LEER_ARCHIVO"));

        client().get().uri("/api/v1/archive/files/private")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        assertThat(CAPTURED.get().authorization()).isEqualTo("Bearer " + token);
    }

    @Test
    void acceptsAnonymousRequestsOnConfiguredApiRoutesAndUnknownPaths() {
        client().get().uri("/api/v1/archive/files/public")
                .exchange()
                .expectStatus().isOk();

        client().get().uri("/not-configured")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void appliesCorsToAllowedOriginsAndDoesNotGrantAnUnlistedOrigin() {
        client().options().uri("/api/v1/archive/files/x")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,x-request-id")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173")
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                .expectHeader().value(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        value -> assertThat(value).contains("POST"));

        client().options().uri("/api/v1/archive/files/x")
                .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }

    @Test
    void exposesHealthWithoutAuthentication() {
        client().get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void refreshesExpiredCookieAndForwardsNewSignedAccessToken() {
        var result = client().get().uri("/api/v1/archive/files/after-refresh")
                .cookie("auth_token", expiredJwt())
                .cookie("refresh_token", "refresh-test")
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        assertThat(CAPTURED.get().path()).isEqualTo("/api/v1/archive/files/after-refresh");
        assertThat(CAPTURED.get().authorization()).startsWith("Bearer ")
                .doesNotContain(expiredJwt());
        assertThat(result.getResponseHeaders().get(HttpHeaders.SET_COOKIE))
                .anySatisfy(value -> assertThat(value).contains("auth_token="))
                .anySatisfy(value -> assertThat(value).contains("refresh_token="));
    }

    @Test
    void expiresCookiesWhenAuthServiceRejectsRefreshAndContinuesRequest() {
        var result = client().get().uri("/api/v1/archive/files/after-refresh-failure")
                .cookie("auth_token", expiredJwt())
                .cookie("refresh_token", "invalid-refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(Instant.now().plusSeconds(120), List.of()))
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        assertThat(CAPTURED.get().path()).isEqualTo("/api/v1/archive/files/after-refresh-failure");
        assertThat(result.getResponseHeaders().get(HttpHeaders.SET_COOKIE))
                .anySatisfy(value -> assertThat(value).contains("auth_token=;", "Max-Age=0"))
                .anySatisfy(value -> assertThat(value).contains("refresh_token=;", "Max-Age=0"));
    }

    private String token(Instant expiresAt, List<String> roles) {
        Instant issuedAt = expiresAt.isBefore(Instant.now())
                ? expiresAt.minusSeconds(60)
                : Instant.now().minusSeconds(1);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("gateway-test-user")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("realm_access", Map.of("roles", roles))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(SignatureAlgorithm.RS256).build(),
                claims)).getTokenValue();
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://127.0.0.1:" + gatewayPort).build();
    }

    private static String expiredJwt() {
        String payload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"exp\":" + (Instant.now().getEpochSecond() - 120) + "}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "eyJhbGciOiJub25lIn0." + payload + ".";
    }

    private static DisposableServer startBackend() {
        return HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .handle((request, response) -> request.receive()
                        .aggregate()
                        .asString()
                        .defaultIfEmpty("")
                        .flatMap(body -> {
                            CAPTURED.set(new CapturedRequest(
                                    request.method().name(),
                                    request.uri().split("\\?", 2)[0],
                                    request.uri().contains("?") ? request.uri().split("\\?", 2)[1] : "",
                                    request.requestHeaders().get("X-Request-Id"),
                                    request.requestHeaders().get(HttpHeaders.AUTHORIZATION),
                                    body));
                            if (request.uri().startsWith("/api/auth/refresh")) {
                                if (request.requestHeaders().get("Cookie") != null
                                        && request.requestHeaders().get("Cookie").contains("refresh_token=invalid-refresh")) {
                                    return response.status(HttpStatus.UNAUTHORIZED.value())
                                            .sendString(Mono.just("{\"error\":\"invalid_grant\"}"))
                                            .then();
                                }
                                String accessToken = testRefreshToken();
                                return response.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .sendString(Mono.just("{\"data\":{\"accessToken\":\"" + accessToken
                                                + "\",\"refreshToken\":\"new-refresh-test\"}}"))
                                        .then();
                            }
                            String status = request.requestHeaders().get("X-Test-Status");
                            if (status != null) {
                                return response.status(Integer.parseInt(status))
                                        .header("X-Backend", "local-test")
                                        .sendString(Mono.just("backend-error"))
                                        .then();
                            }
                            return response.header("X-Backend", "local-test")
                                    .sendString(Mono.just(body.isEmpty() ? "backend:" + request.uri() : body))
                                    .then();
                        }))
                .bindNow();
    }

    private static String testRefreshToken() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("refreshed-test-user")
                .issuedAt(now.minusSeconds(1))
                .expiresAt(now.plusSeconds(180))
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(JwtTestConfiguration.KEY)));
        return encoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(SignatureAlgorithm.RS256).build(),
                claims)).getTokenValue();
    }
    private record CapturedRequest(String method, String path, String query,
                                   String requestId, String authorization, String body) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class JwtTestConfiguration {
        private static final RSAKey KEY = generateKey();

        @Bean
        JwtEncoder jwtEncoder() {
            return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(KEY)));
        }

        @Bean
        ReactiveJwtDecoder reactiveJwtDecoder() throws Exception {
            NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withPublicKey(KEY.toRSAPublicKey()).build();
            decoder.setJwtValidator(org.springframework.security.oauth2.jwt.JwtValidators.createDefault());
            return decoder;
        }

        private static RSAKey generateKey() {
            try {
                return new com.nimbusds.jose.jwk.gen.RSAKeyGenerator(2048)
                        .keyID("gateway-test-key")
                        .generate();
            } catch (com.nimbusds.jose.JOSEException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
