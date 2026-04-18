package com.sssi.msvc_gateway.config;

import org.springframework.http.HttpCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class CookieBearerTokenConverter implements ServerAuthenticationConverter {

    private final String cookieName;

    public CookieBearerTokenConverter(String cookieName) {
        this.cookieName = cookieName;
    }

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(cookieName);
        if (cookie == null || cookie.getValue().isBlank()) {
            return Mono.empty();
        }
        return Mono.just(new BearerTokenAuthenticationToken(cookie.getValue()));
    }
}