package com.proseg.msvc_auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtConverterTest {
    private final KeycloakJwtConverter converter = new KeycloakJwtConverter();

    @Test
    void combinesRealmAndApplicationRolesAndFiltersTechnicalRoles() {
        Jwt jwt = Jwt.withTokenValue("fixture")
                .header("alg", "RS256")
                .claim("realm_access", Map.of("roles", List.of(
                        "AUTH_READ", "default-roles-proseg", "offline_access", "uma_authorization", "AUTH_READ", 5)))
                .claim("resource_access", Map.of("proseg-app", Map.of("roles", List.of("USER_READ"))))
                .build();

        Collection<GrantedAuthority> result = converter.convert(jwt);

        assertThat(result).extracting(GrantedAuthority::getAuthority)
                .containsExactly("AUTH_READ", "USER_READ");
    }

    @Test
    void toleratesMissingOrWronglyTypedClaimsAndIgnoresOtherClients() {
        Jwt noClaims = Jwt.withTokenValue("fixture").header("alg", "RS256").subject("user-1").build();
        Jwt malformedClaims = Jwt.withTokenValue("fixture").header("alg", "RS256")
                .claim("realm_access", Map.of("roles", "not-a-list"))
                .claim("resource_access", Map.of("another-client", Map.of("roles", List.of("IGNORED"))))
                .build();

        assertThat(converter.convert(noClaims)).isEmpty();
        assertThat(converter.convert(malformedClaims)).isEmpty();
    }
}
