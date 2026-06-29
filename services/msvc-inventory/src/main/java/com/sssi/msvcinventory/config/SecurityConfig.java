package com.sssi.msvcinventory.config;

import com.sssi.msvcinventory.security.Privileges;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final String AUTH_COOKIE_NAME = "auth_token";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtConverter());

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // --- Importación de Activos ---
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/inventory/assets/import/**"
                        ).hasAuthority(Privileges.Activos.IMPORTAR)

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/inventory/assets/schema"
                        ).hasAuthority(Privileges.Activos.IMPORTAR)

                        // --- Activos ---
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/inventory/assets",
                                "/api/v1/inventory/assets/**",
                                "/api/v1/inventory/asset-archives/**",
                                "/api/v1/inventory/network-interfaces/**"
                        ).hasAuthority(Privileges.Activos.LEER)

                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/inventory/assets",
                                "/api/v1/inventory/asset-archives",
                                "/api/v1/inventory/network-interfaces",
                                "/api/v1/inventory/assets/alarm-sensors"
                        ).hasAuthority(Privileges.Activos.GESTIONAR)

                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/inventory/assets/**",
                                "/api/v1/inventory/asset-archives/**",
                                "/api/v1/inventory/network-interfaces/**",
                                "/api/v1/inventory/assets/alarm-sensors/**"
                        ).hasAuthority(Privileges.Activos.GESTIONAR)

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/inventory/assets/**",
                                "/api/v1/inventory/asset-archives/**",
                                "/api/v1/inventory/network-interfaces/**"
                        ).hasAuthority(Privileges.Activos.ELIMINAR)

                        // --- Ubicaciones ---
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/inventory/campuses",
                                "/api/v1/inventory/campuses/**",
                                "/api/v1/inventory/buildings",
                                "/api/v1/inventory/buildings/**",
                                "/api/v1/inventory/locations",
                                "/api/v1/inventory/locations/**"
                        ).hasAuthority(Privileges.Ubicaciones.LEER)

                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/inventory/campuses",
                                "/api/v1/inventory/buildings",
                                "/api/v1/inventory/locations"
                        ).hasAuthority(Privileges.Ubicaciones.GESTIONAR)

                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/inventory/campuses/**",
                                "/api/v1/inventory/buildings/**",
                                "/api/v1/inventory/locations/**"
                        ).hasAuthority(Privileges.Ubicaciones.GESTIONAR)

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/inventory/campuses/**",
                                "/api/v1/inventory/buildings/**",
                                "/api/v1/inventory/locations/**"
                        ).hasAuthority(Privileges.Ubicaciones.ELIMINAR)

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/inventory/buildings/*/emails",
                                "/api/v1/inventory/campuses/*/emails"
                        ).hasAuthority(Privileges.Ubicaciones.LEER)

                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/inventory/buildings/*/emails"
                        ).hasAuthority(Privileges.Ubicaciones.GESTIONAR)

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/inventory/emails/**"
                        ).hasAuthority(Privileges.Ubicaciones.ELIMINAR)

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/inventory/brands",
                                "/api/v1/inventory/brands/**",
                                "/api/v1/inventory/asset-models",
                                "/api/v1/inventory/asset-models/**",
                                "/api/v1/inventory/asset-types",
                                "/api/v1/inventory/asset-types/**"
                        ).hasAuthority(Privileges.Activos.LEER)

                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/inventory/brands",
                                "/api/v1/inventory/asset-models",
                                "/api/v1/inventory/asset-types"
                        ).hasAuthority(Privileges.Activos.GESTIONAR)

                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/inventory/brands/**",
                                "/api/v1/inventory/asset-models/**",
                                "/api/v1/inventory/asset-types/**"
                        ).hasAuthority(Privileges.Activos.GESTIONAR)

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/inventory/brands/**",
                                "/api/v1/inventory/asset-models/**",
                                "/api/v1/inventory/asset-types/**"
                        ).hasAuthority(Privileges.Activos.ELIMINAR)

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                        .accessDeniedHandler(new JwtAccessDeniedHandler())
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(new CookieBearerTokenResolver(AUTH_COOKIE_NAME))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                );

        return http.build();
    }
}
