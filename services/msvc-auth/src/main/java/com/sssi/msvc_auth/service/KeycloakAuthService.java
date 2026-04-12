package com.sssi.msvc_auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Value("${keycloak.realm:sssi}")
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

    /**
     * Obtiene un token JWT de Keycloak usando Direct Grant Flow (Resource Owner Password Credentials)
     */
    public String getToken(String username, String password) throws Exception {
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
            } else if (jsonNode.has("error")) {
                log.error("Error de Keycloak: {} - {}", jsonNode.get("error").asText(),
                        jsonNode.get("error_description").asText());
                throw new Exception("Invalid credentials or user does not exist");
            } else {
                log.error("Respuesta inesperada de Keycloak: {}", response);
                throw new Exception("Unexpected response from Keycloak");
            }
        } catch (Exception e) {
            log.error("Error en getToken: ", e);
            throw e;
        }
    }

}

