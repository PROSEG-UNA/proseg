package com.proseg.msvc_email.config;

import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.*;

@Configuration
@RequiredArgsConstructor
public class FeignConfig {

    private final OAuth2AuthorizedClientManager clientManager;

   @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {

            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                    .withClientRegistrationId("keycloak")
                    .principal("msvc-email")
                    .build();

            OAuth2AuthorizedClient client = clientManager.authorize(request);

            if (client != null) {
                String token = client.getAccessToken().getTokenValue();
                requestTemplate.header("Authorization", "Bearer " + token);
            }
        };
    }
}