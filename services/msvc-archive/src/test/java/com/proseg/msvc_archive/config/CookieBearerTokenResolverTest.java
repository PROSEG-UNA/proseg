package com.proseg.msvc_archive.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CookieBearerTokenResolverTest {

    private final CookieBearerTokenResolver resolver = new CookieBearerTokenResolver("access_token");

    @Test
    @DisplayName("resolve: Authorization Bearer tiene prioridad")
    void resolve_headerBearer_devuelveToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer header-token");
        request.setCookies(new Cookie("access_token", "cookie-token"));

        assertThat(resolver.resolve(request)).isEqualTo("header-token");
    }

    @Test
    @DisplayName("resolve: lee token desde cookie si no hay header")
    void resolve_soloCookie_devuelveToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("access_token", "cookie-token"));

        assertThat(resolver.resolve(request)).isEqualTo("cookie-token");
    }

    @Test
    @DisplayName("resolve: sin header ni cookie válida devuelve null")
    void resolve_sinCredenciales_null() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(resolver.resolve(request)).isNull();
    }
}
