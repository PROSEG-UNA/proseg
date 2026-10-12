package com.proseg.msvc_archive.config;

import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("convert: mapea roles de realm_access filtrando roles técnicos")
    void convert_conRoles_devuelveAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("realm_access", Map.of("roles", List.of(
                        "ADMIN",
                        "default-roles-proseg",
                        "offline_access",
                        "uma_authorization"
                )))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("convert: realm_access null devuelve lista vacía")
    void convert_sinRealmAccess_listaVacia() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "user-1")
                .build();

        assertThat(converter.convert(jwt)).isEmpty();
    }
}
