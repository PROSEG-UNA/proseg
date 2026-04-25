package com.sssi.msvc_gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(-200)
@RequiredArgsConstructor
@Slf4j
public class TokenRefreshFilter implements WebFilter {

    private static final String AUTH_COOKIE = "auth_token";
    private static final String REFRESH_COOKIE = "refresh_token";

    private static final Set<String> SKIP_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh"
    );

    private final WebClient authServiceClient;
    private final ObjectMapper objectMapper;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (SKIP_PATHS.contains(path)) {
            return chain.filter(exchange);
        }

        HttpCookie authCookie = exchange.getRequest().getCookies().getFirst(AUTH_COOKIE);
        if (authCookie == null || authCookie.getValue().isBlank()) {
            return chain.filter(exchange);
        }

        if (!isTokenExpired(authCookie.getValue())) {
            return chain.filter(exchange);
        }

        HttpCookie refreshCookie = exchange.getRequest().getCookies().getFirst(REFRESH_COOKIE);
        if (refreshCookie == null || refreshCookie.getValue().isBlank()) {
            log.debug("Access token vencido pero no hay refresh token, continuando");
            return chain.filter(exchange);
        }

        log.debug("Access token vencido, intentando renovar");
        return authServiceClient.post()
                .uri("/api/auth/refresh")
                .cookie(REFRESH_COOKIE, refreshCookie.getValue())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(body -> {
                    String newAccessToken = body.path("data").path("accessToken").asText();
                    String newRefreshToken = body.path("data").path("refreshToken").asText();

                    if (newAccessToken.isBlank()) {
                        log.warn("Respuesta de refresh sin accessToken, continuando sin renovar");
                        return chain.filter(exchange);
                    }

                    exchange.getResponse().addCookie(buildCookie(AUTH_COOKIE, newAccessToken));
                    exchange.getResponse().addCookie(buildCookie(REFRESH_COOKIE, newRefreshToken));

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(r -> r.headers(headers ->
                                    headers.set(HttpHeaders.COOKIE, buildUpdatedCookieHeader(exchange, newAccessToken))
                            ))
                            .build();

                    log.debug("Token renovado exitosamente");
                    return chain.filter(mutatedExchange);
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.warn("Error renovando token ({}), continuando sin renovar", ex.getStatusCode());
                    if (ex.getStatusCode().value() == 400 || ex.getStatusCode().value() == 401) {
                        exchange.getResponse().addCookie(expireCookie(AUTH_COOKIE));
                        exchange.getResponse().addCookie(expireCookie(REFRESH_COOKIE));
                    }
                    return chain.filter(exchange);
                })
                .onErrorResume(ex -> {
                    log.warn("Error inesperado renovando token: {}, continuando sin renovar", ex.getMessage());
                    return chain.filter(exchange);
                });
    }

    private boolean isTokenExpired(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return true;

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode claims = objectMapper.readTree(payload);

            long exp = claims.path("exp").asLong(0);
            return exp == 0 || Instant.now().getEpochSecond() >= (exp - 30);
        } catch (Exception e) {
            log.warn("No se pudo verificar expiración del token: {}", e.getMessage());
            return true;
        }
    }

    private String buildUpdatedCookieHeader(ServerWebExchange exchange, String newAuthToken) {
        return exchange.getRequest().getCookies().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(cookie -> {
                            String value = AUTH_COOKIE.equals(entry.getKey())
                                    ? newAuthToken
                                    : cookie.getValue();
                            return entry.getKey() + "=" + value;
                        }))
                .collect(Collectors.joining("; "));
    }

    private ResponseCookie expireCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }

    private ResponseCookie buildCookie(String name, String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite("Strict")
                .build();
    }
}