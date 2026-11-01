package com.proseg.msvc_gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReactiveKeycloakJwtConverterTest {

    private final ReactiveKeycloakJwtConverter converter = new ReactiveKeycloakJwtConverter();

    @Test
    void convertsRealmRolesAndFiltersKeycloakInternalRoles() {
        Jwt jwt = jwt(Map.of("realm_access", Map.of("roles", List.of(
                "ADMIN",
                "default-roles-proseg",
                "offline_access",
                "uma_authorization",
                "ADMIN"
        ))));

        AbstractAuthenticationToken authentication = converter.convert(jwt).block();

        assertThat(authentication.getAuthorities())
                .containsExactly(new SimpleGrantedAuthority("ADMIN"), new SimpleGrantedAuthority("ADMIN"));
        assertThat(authentication.getName()).isEqualTo("user-1");
    }

    @Test
    void returnsNoAuthoritiesWhenRealmRolesAreAbsent() {
        AbstractAuthenticationToken authentication = converter.convert(jwt(Map.of())).block();

        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    void returnsNoAuthoritiesWhenRealmAccessHasNoRolesKey() {
        AbstractAuthenticationToken authentication = converter.convert(jwt(Map.of(
                "realm_access", Map.of("other", List.of("ADMIN"))
        ))).block();

        assertThat(authentication.getAuthorities()).isEmpty();
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("signed-test-fixture")
                .header("alg", "RS256")
                .subject("user-1")
                .issuedAt(Instant.now().minusSeconds(1))
                .expiresAt(Instant.now().plusSeconds(60))
                .claims(values -> values.putAll(claims))
                .build();
    }
}
