package com.proseg.msvc_gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class JwtHandlersTest {

    @Test
    void authenticationEntryPointReturnsSafeJsonAndUnauthorizedStatus() {
        var exchange = MockServerWebExchange.from(
                org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/protected"));

        new JwtAuthenticationEntryPoint().commence(exchange, new BadCredentialsException("Invalid signature"))
                .block();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
        assertThat(exchange.getResponse().getHeaders().getContentType().toString()).contains("application/json");
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("Token inválido")
                .doesNotContain("private-key-material");
    }

    @Test
    void deniedHandlerReturnsForbiddenJsonWithoutLeakingSensitiveTokenValues() {
        var exchange = MockServerWebExchange.from(
                org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/protected"));

        new JwtAccessDeniedHandler().handle(exchange, new AccessDeniedException("missing privilege"))
                .block();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(403);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("Acceso denegado", "missing privilege")
                .doesNotContain("private-key-material");
    }
}
