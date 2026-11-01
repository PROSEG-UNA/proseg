package com.proseg.msvc_document_processor.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.msvc_document_processor.MsvcDocumentProcessorApplication;
import com.proseg.msvc_document_processor.security.Privileges;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import feign.RequestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SecuritySupportTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bearer_resolver_prefers_authorization_header_and_falls_back_to_nonblank_cookie() {
        var resolver = new CookieBearerTokenResolver("auth_token");
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer header-token");
        request.setCookies(new Cookie("auth_token", "cookie-token"));
        assertThat(resolver.resolve(request)).isEqualTo("header-token");

        request.removeHeader("Authorization");
        assertThat(resolver.resolve(request)).isEqualTo("cookie-token");
        request.setCookies(new Cookie("other", "x"), new Cookie("auth_token", " "));
        assertThat(resolver.resolve(request)).isNull();
        request.setCookies((Cookie[]) null);
        assertThat(resolver.resolve(request)).isNull();
        request.addHeader("Authorization", "Basic ignored");
        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void keycloak_converter_handles_missing_claims_and_filters_system_roles() {
        var converter = new KeycloakJwtConverter();
        assertThat(converter.convert(jwt(Map.of()))).isEmpty();
        assertThat(converter.convert(jwt(Map.of("realm_access", Map.of())))).isEmpty();
        var converted = converter.convert(jwt(Map.of("realm_access", Map.of("roles", List.of(
                "IMPORTAR_ACTIVOS", "default-roles-proseg", "offline_access", "uma_authorization")))));
        assertThat(converted).extracting("authority").containsExactly("IMPORTAR_ACTIVOS");
    }

    @Test
    void privilege_groups_expose_expected_authorities() {
        assertThat(new Privileges.Activos().IMPORTAR).isEqualTo("IMPORTAR_ACTIVOS");
        assertThat(new Privileges.Activos().EXPORTAR).isEqualTo("EXPORTAR_ACTIVOS");
        assertThat(new Privileges.TicketsMantenimiento().LEER).isEqualTo("LEER_TICKET_MANTENIMIENTO");
        assertThat(new Privileges.SolicitudesMantenimiento().LEER).isEqualTo("LEER_SOLICITUDES_MANTENIMIENTO");
    }

    @Test
    void application_main_delegates_to_spring_boot() {
        String[] args = {"--spring.main.web-application-type=none"};
        try (var springApplication = mockStatic(SpringApplication.class)) {
            MsvcDocumentProcessorApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(MsvcDocumentProcessorApplication.class, args));
        }
    }

    @Test
    void feign_interceptor_forwards_only_jwt_authentication() {
        var interceptor = new FeignConfig().requestInterceptor();
        var request = new RequestTemplate();
        interceptor.apply(request);
        assertThat(request.headers()).doesNotContainKey("Authorization");

        Jwt jwt = Jwt.withTokenValue("jwt-value").header("alg", "none").subject("subject").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        interceptor.apply(request);
        assertThat(request.headers().get("Authorization")).containsExactly("Bearer jwt-value");

        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("user", "n/a"));
        var otherRequest = new RequestTemplate();
        interceptor.apply(otherRequest);
        assertThat(otherRequest.headers()).doesNotContainKey("Authorization");
    }

    @Test
    void authentication_entry_point_returns_status_json_and_reason_specific_messages() throws Exception {
        var entryPoint = new JwtAuthenticationEntryPoint();
        assertEntryPoint(entryPoint, "expired token", "Token expirado");
        assertEntryPoint(entryPoint, "invalid token", "Token inválido");
        assertEntryPoint(entryPoint, "malformed token", "Token inválido");
        assertEntryPoint(entryPoint, "signature mismatch", "Firma inválida");
        assertEntryPoint(entryPoint, "credentials missing", "No autenticado");

        var response = new MockHttpServletResponse();
        entryPoint.commence(new MockHttpServletRequest(), response, new BadCredentialsException(null));
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("message").asText()).isEqualTo("No autenticado");
        assertThat(body.get("errors").get(0).asText()).isEqualTo("Unauthorized");
    }

    @Test
    void denied_handler_returns_forbidden_and_fallback_message() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        new JwtAccessDeniedHandler().handle(request, response, new AccessDeniedException("forbidden"));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(body.get("errors").get(0).asText()).isEqualTo("forbidden");

        var nullMessageResponse = new MockHttpServletResponse();
        new JwtAccessDeniedHandler().handle(request, nullMessageResponse, new AccessDeniedException(null, null));
        assertThat(objectMapper.readTree(nullMessageResponse.getContentAsString()).get("errors").get(0).asText())
                .isEqualTo("No tiene permisos");
    }

    private void assertEntryPoint(JwtAuthenticationEntryPoint entryPoint, String message, String expected) throws Exception {
        var response = new MockHttpServletResponse();
        entryPoint.commence(new MockHttpServletRequest(), response, new BadCredentialsException(message));
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(body.get("message").asText()).isEqualTo(expected);
        assertThat(body.get("errors").get(0).asText()).isEqualTo(message);
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token").header("alg", "none").subject("subject").claims(values -> values.putAll(claims)).build();
    }
}
