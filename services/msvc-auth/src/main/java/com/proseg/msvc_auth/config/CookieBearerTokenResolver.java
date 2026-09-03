package com.proseg.msvc_auth.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

import java.util.Set;

import static org.apache.kafka.common.requests.DeleteAclsResponse.log;

public class CookieBearerTokenResolver implements BearerTokenResolver {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/logout",
            "/api/auth/refresh"
    );

    private final String cookieName;

    public CookieBearerTokenResolver(String cookieName) {
        this.cookieName = cookieName;
    }

    @Override
    public String resolve(HttpServletRequest request) {

        String path = request.getRequestURI();

        if (PUBLIC_PATHS.contains(path)) {
            return null;
        }

        log.debug("Resolving token for request path: {}", request);

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}