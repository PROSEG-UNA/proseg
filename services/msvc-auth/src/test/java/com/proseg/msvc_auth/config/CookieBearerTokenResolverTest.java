package com.proseg.msvc_auth.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CookieBearerTokenResolverTest {
    private final CookieBearerTokenResolver resolver = new CookieBearerTokenResolver("auth_token");

    @Test
    void bearerHeaderTakesPriorityOverCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer header-token");
        request.setCookies(new Cookie("auth_token", "cookie-token"));

        assertThat(resolver.resolve(request)).isEqualTo("header-token");
    }

    @Test
    void cookieIsUsedWhenHeaderIsAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("auth_token", "cookie-token"));

        assertThat(resolver.resolve(request)).isEqualTo("cookie-token");
    }

    @Test
    void ignoresBlankCookieAndNonBearerAuthorization() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");
        request.setCookies(new Cookie("auth_token", " "));

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void doesNotResolveTokensForPublicAuthenticationPaths() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        request.addHeader("Authorization", "Bearer header-token");
        request.setCookies(new Cookie("auth_token", "cookie-token"));

        assertThat(resolver.resolve(request)).isNull();
    }
}
