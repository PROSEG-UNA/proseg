package com.sssi.msvc_auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sssi.msvc_auth.exception.AuthenticationException;
import com.sssi.msvc_auth.exception.TokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class KeycloakAuthService {

    @Value("${keycloak.server-url:http://localhost:8080}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm:sssi-realm}")
    private String realm;

    @Value("${keycloak.client.id:sssi-app}")
    private String clientId;

    @Value("${keycloak.client.secret:}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public KeycloakAuthService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String getToken(String username, String password) {
        try {
            String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", clientId);
            if (clientSecret != null && !clientSecret.isEmpty()) {
                body.add("client_secret", clientSecret);
            }
            body.add("username", username);
            body.add("password", password);
            body.add("scope", "openid profile email");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(tokenUrl, request, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            if (jsonNode.has("access_token")) {
                log.info("Token obtenido exitosamente para usuario: {}", username);
                return jsonNode.get("access_token").asText();
            }

            if (jsonNode.has("error")) {
                String error = jsonNode.get("error").asText();
                log.warn("Error de Keycloak para usuario {}: {}", username, error);
                throw AuthenticationException.invalidCredentials();
            }

            throw TokenException.malformed();

        } catch (AuthenticationException | TokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado obteniendo token para usuario {}: {}", username, e.getMessage());
            throw TokenException.malformed();
        }
    }
}