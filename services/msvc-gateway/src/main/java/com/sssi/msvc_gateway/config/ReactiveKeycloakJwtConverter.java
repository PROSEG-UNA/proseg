package com.sssi.msvc_gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;

@Component
public class ReactiveKeycloakJwtConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    private static final Logger log = LoggerFactory.getLogger(ReactiveKeycloakJwtConverter.class);

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {

        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess != null && realmAccess.containsKey("roles")) {
            List<String> roles = (List<String>) realmAccess.get("roles");

            roles.stream()
                    .filter(role -> !role.startsWith("default-roles"))
                    .filter(role -> !role.equals("offline_access"))
                    .filter(role -> !role.equals("uma_authorization"))

                    .forEach(role -> {
                        authorities.add(new SimpleGrantedAuthority(role));
                    });

        } else {
            log.warn("No realm roles found");
        }

        log.info("JWT authorities extracted: {}", authorities);

        return Mono.just(new JwtAuthenticationToken(jwt, authorities));
    }
}