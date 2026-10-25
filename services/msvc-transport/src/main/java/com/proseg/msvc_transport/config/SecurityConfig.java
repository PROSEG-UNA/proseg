package com.proseg.msvc_transport.config;

import com.proseg.msvc_transport.security.Privileges;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final String AUTH_COOKIE_NAME = "auth_token";

    @Value("${routes.transport:/api/v1/transport}")
    private String transportRoute;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtConverter());

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, transportRoute + "/status").permitAll()
                        .requestMatchers(HttpMethod.GET, transportRoute + "/drivers", transportRoute + "/drivers/**")
                        .hasAuthority(Privileges.Drivers.LEER)
                        .requestMatchers(HttpMethod.POST, transportRoute + "/drivers")
                        .hasAuthority(Privileges.Drivers.GESTIONAR)
                        .requestMatchers(HttpMethod.PUT, transportRoute + "/drivers/**")
                        .hasAuthority(Privileges.Drivers.GESTIONAR)
                        .requestMatchers(HttpMethod.DELETE, transportRoute + "/drivers/**")
                        .hasAuthority(Privileges.Drivers.ELIMINAR)
                        .requestMatchers(HttpMethod.PATCH, transportRoute + "/drivers/**")
                        .hasAuthority(Privileges.Drivers.GESTIONAR)

                        .requestMatchers(HttpMethod.GET, transportRoute + "/vehicles", transportRoute + "/vehicles/**")
                        .hasAuthority(Privileges.Vehicles.LEER)
                        .requestMatchers(HttpMethod.POST, transportRoute + "/vehicles")
                        .hasAuthority(Privileges.Vehicles.GESTIONAR)
                        .requestMatchers(HttpMethod.PUT, transportRoute + "/vehicles/**")
                        .hasAuthority(Privileges.Vehicles.GESTIONAR)
                        .requestMatchers(HttpMethod.DELETE, transportRoute + "/vehicles/**")
                        .hasAuthority(Privileges.Vehicles.ELIMINAR)

                        .requestMatchers(HttpMethod.GET, transportRoute + "/borrowed-vehicles", transportRoute + "/borrowed-vehicles/**")
                        .hasAuthority(Privileges.Vehicles.LEER)
                        .requestMatchers(HttpMethod.POST, transportRoute + "/borrowed-vehicles")
                        .hasAuthority(Privileges.Vehicles.GESTIONAR)
                        .requestMatchers(HttpMethod.PUT, transportRoute + "/borrowed-vehicles/**")
                        .hasAuthority(Privileges.Vehicles.GESTIONAR)
                        .requestMatchers(HttpMethod.DELETE, transportRoute + "/borrowed-vehicles/**")
                        .hasAuthority(Privileges.Vehicles.ELIMINAR)

                        .requestMatchers(
                                HttpMethod.GET,
                                transportRoute + "/cleaning/history",
                                transportRoute + "/cleaning/history/**",
                                transportRoute + "/cleaning/imports/schema",
                                transportRoute + "/cleaning/drafts",
                                transportRoute + "/cleaning/drafts/**"
                        )
                        .hasAnyAuthority(Privileges.Tours.LEER, Privileges.Tours.GESTIONAR, Privileges.Tours.ELIMINAR)
                        .requestMatchers(HttpMethod.POST, transportRoute + "/cleaning/**")
                        .hasAuthority(Privileges.Tours.GESTIONAR)
                        .requestMatchers(HttpMethod.PUT, transportRoute + "/cleaning/drafts/**")
                        .hasAuthority(Privileges.Tours.GESTIONAR)
                        .requestMatchers(HttpMethod.DELETE, transportRoute + "/cleaning/drafts/**")
                        .hasAuthority(Privileges.Tours.GESTIONAR)
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