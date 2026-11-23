package com.proseg.msvc_forms.client;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_forms.dto.response.KeycloakUserResponseDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Resuelve usuarios de Keycloak/msvc-auth reutilizando el mismo endpoint
 * batch (`/api/user/keycloak/batch`) consumido por msvc-maintenance, pero
 * reenviando el token del usuario autenticado en vez de credenciales de
 * cliente, para evitar registrar un nuevo cliente en Keycloak.
 */
@Component
@RequiredArgsConstructor
public class AuthUserClient {

    private static final Logger log = LoggerFactory.getLogger(AuthUserClient.class);

    private final RestTemplate restTemplate;

    @Value("${gateway.base-url:http://msvc-gateway:8081}")
    private String gatewayBaseUrl;

    public List<KeycloakUserResponseDto> findUsersByIds(List<String> ids, String bearerToken) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (bearerToken != null && !bearerToken.isBlank()) {
                headers.setBearerAuth(bearerToken);
            }

            HttpEntity<List<String>> entity = new HttpEntity<>(ids, headers);

            ResponseEntity<ApiResponse<List<KeycloakUserResponseDto>>> response = restTemplate.exchange(
                    gatewayBaseUrl + "/api/user/keycloak/batch",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            ApiResponse<List<KeycloakUserResponseDto>> body = response.getBody();
            return body != null && body.getData() != null ? body.getData() : List.of();
        } catch (Exception exception) {
            log.warn("No se pudieron resolver {} usuario(s) contra msvc-auth: {}", ids.size(), exception.getMessage());
            return List.of();
        }
    }
}
