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
                        .hasAnyAuthority(Privileges.Drivers.LEER, Privileges.Drivers.GESTIONAR, Privileges.Drivers.ELIMINAR)
                        .requestMatchers(HttpMethod.POST, transportRoute + "/drivers")
                        .hasAuthority(Privileges.Drivers.GESTIONAR)
                        .requestMatchers(HttpMethod.PUT, transportRoute + "/drivers/**")
                        .hasAuthority(Privileges.Drivers.GESTIONAR)
                        .requestMatchers(HttpMethod.DELETE, transportRoute + "/drivers/**")
                        .hasAuthority(Privileges.Drivers.ELIMINAR)
                        .requestMatchers(HttpMethod.PATCH, transportRoute + "/drivers/**")
                        .hasAuthority(Privileges.Drivers.GESTIONAR)

                        .requestMatchers(HttpMethod.GET, transportRoute + "/vehicles", transportRoute + "/vehicles/**")
                        .hasAnyAuthority(Privileges.Vehicles.LEER, Privileges.Vehicles.GESTIONAR, Privileges.Vehicles.ELIMINAR)
                        .requestMatchers(HttpMethod.POST, transportRoute + "/vehicles")
                        .hasAuthority(Privileges.Vehicles.GESTIONAR)
                        .requestMatchers(HttpMethod.PUT, transportRoute + "/vehicles/**")
                        .hasAuthority(Privileges.Vehicles.GESTIONAR)
                        .requestMatchers(HttpMethod.DELETE, transportRoute + "/vehicles/**")
                        .hasAuthority(Privileges.Vehicles.ELIMINAR)

                        .requestMatchers(HttpMethod.GET, transportRoute + "/borrowed-vehicles", transportRoute + "/borrowed-vehicles/**")
                        .hasAnyAuthority(Privileges.Vehicles.LEER, Privileges.Vehicles.GESTIONAR, Privileges.Vehicles.ELIMINAR)
                        .requestMatchers(HttpMethod.POST, transportRoute + "/borrowed-vehicles")
                        .hasAuthority(Privileges.Vehicles.GESTIONAR)
                        .requestMatchers(HttpMethod.PUT, transportRoute + "/borrowed-vehicles/**")
                        .hasAuthority(Privileges.Vehicles.GESTIONAR)
                        .requestMatchers(HttpMethod.DELETE, transportRoute + "/borrowed-vehicles/**")
                        .hasAuthority(Privileges.Vehicles.ELIMINAR)

                        .requestMatchers(HttpMethod.GET, transportRoute + "/tour-processing/imports/schema")
                        .hasAuthority(Privileges.Tours.IMPORTAR)
                        .requestMatchers(
                                HttpMethod.GET,
                                transportRoute + "/tour-processing/drafts",
                                transportRoute + "/tour-processing/drafts/**"
                        )
                        .hasAnyAuthority(
                                Privileges.Tours.LEER,
                                Privileges.Tours.IMPORTAR,
                                Privileges.Tours.CREAR,
                                Privileges.Tours.EDITAR,
                                Privileges.Tours.ELIMINAR,
                                Privileges.Tours.EXPORTAR
                        )
                        .requestMatchers(HttpMethod.POST, transportRoute + "/tour-processing/drafts")
                        .hasAuthority(Privileges.Tours.IMPORTAR)
                        .requestMatchers(HttpMethod.POST, transportRoute + "/tour-processing/drafts/*/rows")
                        .hasAuthority(Privileges.Tours.CREAR)
                        .requestMatchers(HttpMethod.POST, transportRoute + "/tour-processing/**")
                        .hasAuthority(Privileges.Tours.EDITAR)
                        .requestMatchers(HttpMethod.PUT, transportRoute + "/tour-processing/drafts/**")
                        .hasAuthority(Privileges.Tours.EDITAR)
                        .requestMatchers(HttpMethod.PATCH, transportRoute + "/tour-processing/drafts/**")
                        .hasAuthority(Privileges.Tours.EDITAR)
                        .requestMatchers(HttpMethod.DELETE, transportRoute + "/tour-processing/drafts/**")
                        .hasAuthority(Privileges.Tours.ELIMINAR)
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