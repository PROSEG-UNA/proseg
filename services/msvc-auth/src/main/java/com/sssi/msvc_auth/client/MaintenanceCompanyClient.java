package com.sssi.msvc_auth.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sssi.msvc_auth.exception.KeycloakException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class MaintenanceCompanyClient {

    @Value("${GATEWAY_BASE_URL:http://localhost:8081}")
    private String gatewayBaseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MaintenanceCompanyClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean userHasCompany(String keycloakUserId) {
        String url = gatewayBaseUrl + "/api/v1/maintenance/user-companies/" + keycloakUserId + "/has-company";

        HttpHeaders headers = new HttpHeaders();
        String token = currentToken();
        if (token != null) {
            headers.setBearerAuth(token);
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode node = objectMapper.readTree(response.getBody());
            return node.path("data").asBoolean(false);
        } catch (Exception e) {
            log.error("Error verificando empresa asociada del usuario {}: {}", keycloakUserId, e.getMessage());
            throw KeycloakException.generic("No se pudo verificar la empresa asociada del usuario");
        }
    }

    private String currentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getTokenValue();
        }
        return null;
    }
}
