package com.sssi.msvc_auth.config;

import com.sssi.msvc_auth.security.Privileges;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String AUTH_COOKIE_NAME = "auth_token";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtConverter());

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/user/set-password",
                                "/api/user/invitation-info/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/role/base")
                        .hasAuthority(Privileges.Role.READ_BASE)

                        .requestMatchers(HttpMethod.GET, "/api/role/composite")
                        .hasAuthority(Privileges.Role.READ_COMPOSITE)
                        .requestMatchers(HttpMethod.GET, "/api/role/*/composites")
                        .hasAuthority(Privileges.Role.READ_ROLE_COMPOSITES)

                        .requestMatchers(HttpMethod.POST, "/api/role")
                        .hasAuthority(Privileges.Role.CREATE)

                        .requestMatchers(HttpMethod.PUT, "/api/role/*")
                        .hasAuthority(Privileges.Role.UPDATE)

                        .requestMatchers(HttpMethod.DELETE, "/api/role/*")
                        .hasAuthority(Privileges.Role.DELETE)

                        .requestMatchers(HttpMethod.GET, "/api/role/*/users")
                        .hasAuthority(Privileges.Role.READ_USERS_BY_ROLE)

                        .requestMatchers(HttpMethod.GET, "/api/user")
                        .hasAnyAuthority(Privileges.User.READ_ALL, Privileges.User.READ)

                        .requestMatchers(HttpMethod.POST, "/api/user")
                        .hasAuthority(Privileges.User.CREATE)

                        .requestMatchers(HttpMethod.GET, "/api/user/keycloak/*")
                        .hasAuthority(Privileges.User.READ)
                        .requestMatchers(HttpMethod.POST, "/api/user/keycloak/batch")
                        .hasAuthority(Privileges.User.READ)

                        .requestMatchers(HttpMethod.GET, "/api/user/*/roles")
                        .hasAuthority(Privileges.User.READ_ROLES)

                        .requestMatchers(HttpMethod.PATCH, "/api/user/approval/*")
                        .hasAuthority(Privileges.User.APPROVE)

                        .requestMatchers(HttpMethod.POST, "/api/user/*/roles/*")
                        .hasAuthority(Privileges.User.ASSIGN_ROLE)

                        .requestMatchers(HttpMethod.DELETE, "/api/user/*/roles/*")
                        .hasAuthority(Privileges.User.REMOVE_ROLE)

                        .requestMatchers(HttpMethod.GET, "/api/invitations/**")
                        .hasAuthority(Privileges.Invitation.READ_PENDING)

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                        .accessDeniedHandler(new JwtAccessDeniedHandler())
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2
                                .bearerTokenResolver(new CookieBearerTokenResolver(AUTH_COOKIE_NAME))
                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)
                                )
                );
        return http.build();
    }
}