package com.proseg.msvc_auth.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class JwtErrorHandlersTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void authenticationEntryPointExplainsExpiredInvalidAndUnknownTokens() throws Exception {
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();

        assertUnauthorized(entryPoint, new BadCredentialsException("Token expired"));
        assertUnauthorized(entryPoint, new BadCredentialsException("Invalid signature"));
        assertUnauthorized(entryPoint, new BadCredentialsException("Authentication required"));
    }

    @Test
    void accessDeniedHandlerReturnsJsonForbiddenResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new JwtAccessDeniedHandler().handle(
                new MockHttpServletRequest(), response, new AccessDeniedException("missing authority"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(objectMapper.readTree(response.getContentAsString()).path("message").asText())
                .isEqualTo("Acceso denegado");
    }

    private void assertUnauthorized(
            JwtAuthenticationEntryPoint entryPoint,
            org.springframework.security.core.AuthenticationException exception) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        entryPoint.commence(new MockHttpServletRequest(), response, exception);

        assertThat(response.getStatus()).isEqualTo(401);
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.path("status").asInt()).isEqualTo(401);
        assertThat(body.path("message").asText()).isNotBlank();
    }
}
