package com.sssi.msvc_document_processor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class KeycloakJwtConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final Logger log = LoggerFactory.getLogger(KeycloakJwtConverter.class);

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {

        Collection<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null) {
            log.warn("realm_access is NULL");
            return authorities;
        }

        Object rolesObj = realmAccess.get("roles");

        if (rolesObj == null) {
            log.warn("roles inside realm_access is NULL");
            return authorities;
        }

        List<String> roles = (List<String>) rolesObj;

        roles.stream()
                .filter(role -> !role.startsWith("default-roles"))
                .filter(role -> !role.equals("offline_access"))
                .filter(role -> !role.equals("uma_authorization"))
                .forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));

        return authorities;
    }
}
