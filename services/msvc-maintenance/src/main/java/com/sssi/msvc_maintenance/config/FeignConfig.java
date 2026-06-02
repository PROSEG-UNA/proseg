package com.sssi.msvc_maintenance.config;

import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
@RequiredArgsConstructor
public class FeignConfig {

    private final OAuth2AuthorizedClientManager clientManager;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // If there is an authenticated JWT from the incoming request, propagate it
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                try {
                    String token = jwtAuth.getToken().getTokenValue();
                    requestTemplate.header("Authorization", "Bearer " + token);
                    return;
                } catch (Exception ignored) {
                }
            }

            // fallback to client credentials when no incoming JWT
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                    .withClientRegistrationId("keycloak")
                    .principal("msvc-maintenance")
                    .build();

            OAuth2AuthorizedClient client = clientManager.authorize(request);

            if (client != null) {
                String token = client.getAccessToken().getTokenValue();
                requestTemplate.header("Authorization", "Bearer " + token);
            }
        };
    }
}

