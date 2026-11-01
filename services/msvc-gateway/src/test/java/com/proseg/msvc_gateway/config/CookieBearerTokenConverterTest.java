package com.proseg.msvc_gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;

class CookieBearerTokenConverterTest {

    private final CookieBearerTokenConverter converter = new CookieBearerTokenConverter("auth_token");

    @Test
    void prefersBearerHeaderToCookie() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer header-token")
                .cookie(new org.springframework.http.HttpCookie("auth_token", "cookie-token"))
                .build());

        assertThat(converter.convert(exchange).block())
                .isInstanceOf(BearerTokenAuthenticationToken.class)
                .extracting(authentication -> ((BearerTokenAuthenticationToken) authentication).getToken())
                .isEqualTo("header-token");
    }

    @Test
    void usesNonblankCookieWhenBearerHeaderIsAbsent() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/user/me")
                .cookie(new org.springframework.http.HttpCookie("auth_token", "cookie-token"))
                .build());

        assertThat(converter.convert(exchange).block())
                .isInstanceOf(BearerTokenAuthenticationToken.class)
                .extracting(authentication -> ((BearerTokenAuthenticationToken) authentication).getToken())
                .isEqualTo("cookie-token");
    }

    @Test
    void returnsEmptyForMissingAndBlankCredentials() {
        var missing = MockServerWebExchange.from(MockServerHttpRequest.get("/api/user/me").build());
        var blank = MockServerWebExchange.from(MockServerHttpRequest.get("/api/user/me")
                .cookie(new org.springframework.http.HttpCookie("auth_token", " "))
                .build());

        assertThat(converter.convert(missing).block()).isNull();
        assertThat(converter.convert(blank).block()).isNull();
    }

    @Test
    void onlySkipsTheExactPublicPathsConfiguredByTheConverter() {
        var login = MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/login")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ignored")
                .build());
        var inventoryRoute = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/inventory/assets")
                .header(HttpHeaders.AUTHORIZATION, "Bearer still-resolved")
                .build());

        assertThat(converter.convert(login).block()).isNull();
        assertThat(converter.convert(inventoryRoute).block())
                .isInstanceOf(BearerTokenAuthenticationToken.class);
    }
}
