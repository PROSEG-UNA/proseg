package com.sssi.msvc_maintenance.config;

import com.sssi.msvc_maintenance.security.Privileges;
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

                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/maintenance/companies/me"
                        ).hasAnyAuthority(
                                Privileges.SolicitudesMantenimiento.SOLICITAR,
                                Privileges.SolicitudesMantenimiento.EDITAR
                        )

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/maintenance/user-companies/*/has-company"
                        ).authenticated()

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/maintenance/companies",
                                "/api/v1/maintenance/companies/**"
                        ).hasAuthority(Privileges.Empresas.LEER)

                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/maintenance/companies",
                                "/api/v1/maintenance/companies/users",
                                "/api/v1/maintenance/companies/*/users"
                        ).hasAuthority(Privileges.Empresas.GESTIONAR)

                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/maintenance/companies/**"
                        ).hasAuthority(Privileges.Empresas.GESTIONAR)

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/maintenance/companies/**"
                        ).hasAuthority(Privileges.Empresas.ELIMINAR)


                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/maintenance/requests",
                                "/api/v1/maintenance/requests/**"
                        ).hasAuthority(Privileges.SolicitudesMantenimiento.LEER)

                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/maintenance/requests"
                        ).hasAuthority(Privileges.SolicitudesMantenimiento.SOLICITAR)

                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/maintenance/requests/**"
                        ).hasAuthority(Privileges.SolicitudesMantenimiento.EDITAR)

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/maintenance/requests/**"
                        ).hasAuthority(Privileges.SolicitudesMantenimiento.ELIMINAR)

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/maintenance/records",
                                "/api/v1/maintenance/records/**"
                        ).hasAuthority(Privileges.RegistrosMantenimiento.HISTORIAL)

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/maintenance/registers",
                                "/api/v1/maintenance/registers/**"
                        ).hasAuthority(Privileges.RegistrosMantenimiento.LEER)

                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/maintenance/registers/**"
                        ).hasAuthority(Privileges.RegistrosMantenimiento.GESTIONAR)

                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/maintenance/registers/**"
                        ).hasAuthority(Privileges.RegistrosMantenimiento.GESTIONAR)

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/maintenance/technicians",
                                "/api/v1/maintenance/technicians/**"
                        ).hasAuthority(Privileges.TecnicosMantenimiento.LEER)

                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/maintenance/technicians"
                        ).hasAuthority(Privileges.TecnicosMantenimiento.GESTIONAR)

                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/maintenance/technicians/**"
                        ).hasAuthority(Privileges.TecnicosMantenimiento.GESTIONAR)

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/maintenance/technicians/**"
                        ).hasAuthority(Privileges.TecnicosMantenimiento.ELIMINAR)

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/maintenance/user-companies",
                                "/api/v1/maintenance/user-companies/**"
                        ).hasAuthority(Privileges.UsuariosEmpresas.LEER)

                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/maintenance/user-companies"
                        ).hasAuthority(Privileges.UsuariosEmpresas.GESTIONAR)

                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/maintenance/user-companies/**"
                        ).hasAuthority(Privileges.UsuariosEmpresas.GESTIONAR)

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/maintenance/user-companies/**"
                        ).hasAuthority(Privileges.UsuariosEmpresas.ELIMINAR)

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/maintenance/tickets/assignees"
                        ).hasAuthority(Privileges.Tickets.ASIGNAR_TICKET)

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/maintenance/tickets",
                                "/api/v1/maintenance/tickets/**"
                        ).hasAuthority(Privileges.Tickets.LEER)

                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/maintenance/tickets"
                        ).hasAuthority(Privileges.Tickets.CREAR)

                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/maintenance/tickets/**"
                        ).hasAuthority(Privileges.Tickets.EDITAR)

                        .requestMatchers(HttpMethod.PATCH,
                                "/api/v1/maintenance/tickets/*/priority"
                        ).hasAnyAuthority(Privileges.Tickets.EDITAR, Privileges.Tickets.ASIGNAR_PRIORIDAD)

                        .requestMatchers(HttpMethod.PATCH,
                                "/api/v1/maintenance/tickets/*/assigned-to"
                        ).hasAuthority(Privileges.Tickets.ASIGNAR_TICKET)

                        .requestMatchers(HttpMethod.PATCH,
                                "/api/v1/maintenance/tickets/*/status",
                                "/api/v1/maintenance/tickets/*/resolve"
                        ).hasAuthority(Privileges.Tickets.EDITAR)

                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/maintenance/tickets/*/comments"
                        ).hasAuthority(Privileges.Tickets.COMENTAR)

                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/maintenance/tickets/*/comments/*"
                        ).hasAuthority(Privileges.Tickets.COMENTAR)

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/maintenance/tickets/*/comments/*"
                        ).hasAuthority(Privileges.Tickets.COMENTAR)
                        .requestMatchers(
                                "/api/v1/maintenance/ws/**"
                        ).authenticated()

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