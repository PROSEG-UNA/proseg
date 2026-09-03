package com.proseg.msvc_auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

public class KeycloakJwtConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final Logger log = LoggerFactory.getLogger(KeycloakJwtConverter.class);
    private static final String CLIENT_ID = "proseg-app";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {

        Collection<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        extractRoles(realmAccess == null ? null : realmAccess.get("roles"))
                .forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            Object clientAccess = resourceAccess.get(CLIENT_ID);
            if (clientAccess instanceof Map<?, ?> clientAccessMap) {
                Object clientRoles = clientAccessMap.get("roles");
                extractRoles(clientRoles).forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
            }
        }

        log.info("FINAL authorities: {}", authorities);

        return authorities;
    }

    private List<String> extractRoles(Object rolesObj) {
        if (rolesObj == null) {
            return List.of();
        }

        if (!(rolesObj instanceof List<?> rawRoles)) {
            log.warn("Unexpected roles claim type: {}", rolesObj.getClass().getName());
            return List.of();
        }

        return rawRoles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(role -> !role.startsWith("default-roles"))
                .filter(role -> !role.equals("offline_access"))
                .filter(role -> !role.equals("uma_authorization"))
                .distinct()
                .toList();
    }
}