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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class KeycloakAuthService {

    @Value("${keycloak.server-url:https://auth.devbychris.com}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm:sssi-realm}")
    private String realm;

    @Value("${keycloak.client.id:sssi-app}")
    private String clientId;

    @Value("${keycloak.client.secret:Dl8QcQxHHmu6qERkUMmUJ0qWSulRRx6y}")
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
            if (clientSecret != null && !clientSecret.isEmpty()) {
                log.info("Client secret presente: {}", clientSecret != null && !clientSecret.isBlank());
                body.add("client_secret", clientSecret);
            }
            body.add("client_id", clientId);
            body.add("username", username);
            body.add("password", password);
            body.add("scope", "openid profile email");

            log.info("Token URL: {}", tokenUrl);
            log.info("Realm: {}", realm);
            log.info("Client ID: {}", clientId);
            log.info("Client secret presente: {}", clientSecret != null && !clientSecret.isBlank());
            log.info("Username: {}", username);
            log.info("Password presente: {}", password != null && !password.isBlank());

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

        } catch (HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Keycloak devolvio {} para usuario {}. Body: {}",
                    e.getStatusCode(), username, responseBody);

            if (!responseBody.isBlank()) {
                try {
                    JsonNode errorNode = objectMapper.readTree(responseBody);
                    String error = errorNode.path("error").asText("");
                    String description = errorNode.path("error_description").asText("");

                    if ("invalid_grant".equals(error)) {
                        if (description.toLowerCase().contains("disabled")) {
                            throw AuthenticationException.accountDisabled();
                        }
                        throw AuthenticationException.invalidCredentials();
                    }

                    if ("invalid_client".equals(error)) {
                        throw new IllegalStateException("Cliente de Keycloak invalido o secreto incorrecto");
                    }

                    if ("unauthorized_client".equals(error)) {
                        throw new IllegalStateException("El cliente no tiene habilitado Direct Access Grants");
                    }

                    throw new IllegalStateException("Error de Keycloak: " + error + " - " + description);

                } catch (AuthenticationException e1) {
                    throw e1;
                } catch (IllegalStateException e1) {
                    throw e1;
                } catch (Exception parseEx) {
                    log.warn("No se pudo parsear el body de error de Keycloak: {}", parseEx.getMessage());
                }
            }

            throw new IllegalStateException("Keycloak respondio con error HTTP sin detalle parseable");
        } catch (AuthenticationException | TokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado obteniendo token para usuario {}: {}", username, e.getMessage(), e);
            throw new IllegalStateException("Error inesperado al obtener token desde Keycloak", e);
        }
    }
}