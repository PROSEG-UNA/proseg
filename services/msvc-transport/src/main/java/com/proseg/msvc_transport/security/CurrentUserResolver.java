package com.proseg.msvc_transport.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUserResolver {

    private static final String UNKNOWN_USER = "usuario_desconocido";

    public String resolve() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return UNKNOWN_USER;
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Jwt jwt = jwtAuthenticationToken.getToken();
            String preferredUsername = safe(jwt.getClaimAsString("preferred_username"));
            if (!preferredUsername.isBlank()) return preferredUsername;

            String displayName = safe(jwt.getClaimAsString("name"));
            if (!displayName.isBlank()) return displayName;

            String givenName = safe(jwt.getClaimAsString("given_name"));
            String familyName = safe(jwt.getClaimAsString("family_name"));
            String fullName = (givenName + " " + familyName).trim();
            if (!fullName.isBlank()) return fullName;

            String email = safe(jwt.getClaimAsString("email"));
            if (!email.isBlank()) return email;
        }

        String fallback = safe(authentication.getName());
        if (!fallback.isBlank() && !looksLikeIdentifier(fallback)) {
            return fallback;
        }
        return UNKNOWN_USER;
    }

    private boolean looksLikeIdentifier(String value) {
        String trimmed = value.trim();
        try {
            UUID.fromString(trimmed);
            return true;
        } catch (IllegalArgumentException ignored) {
        }
        return trimmed.matches("^[0-9a-fA-F]{32}$")
                || trimmed.matches("^\\d{8,}$");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
