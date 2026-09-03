package com.proseg.msvc_auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.msvc_auth.exception.AuthenticationException;
import com.proseg.msvc_auth.exception.TokenException;
import com.proseg.msvc_auth.dto.KeycloakTokenDto;
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


import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Slf4j
public class KeycloakAuthService {

    @Value("${keycloak.server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm:proseg-realm}")
    private String realm;

    @Value("${keycloak.client.id}")
    private String clientId;

    @Value("${keycloak.client.secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public KeycloakAuthService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public KeycloakTokenDto getToken(String username, String password) {
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
                return KeycloakTokenDto.builder()
                        .accessToken(jsonNode.get("access_token").asText())
                        .refreshToken(jsonNode.path("refresh_token").asText(""))
                        .build();
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
                        throw new IllegalStateException(
                                "Keycloak rechazo al cliente. Revise el client secret y que el cliente"
                                        + " tenga habilitado Direct Access Grants. Detalle: " + description);
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

    public KeycloakTokenDto refreshToken(String refreshToken) {
        try {
            String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("refresh_token", refreshToken);
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(tokenUrl, request, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            if (jsonNode.has("access_token")) {
                log.info("Token renovado exitosamente");
                return KeycloakTokenDto.builder()
                        .accessToken(jsonNode.get("access_token").asText())
                        .refreshToken(jsonNode.path("refresh_token").asText(""))
                        .build();
            }

            throw TokenException.refreshTokenExpired();

        } catch (HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            log.warn("Error renovando token. Status: {}. Body: {}", e.getStatusCode(), responseBody);

            if (!responseBody.isBlank()) {
                try {
                    JsonNode errorNode = objectMapper.readTree(responseBody);
                    String error = errorNode.path("error").asText("");

                    if ("invalid_grant".equals(error)) {
                        throw TokenException.refreshTokenExpired();
                    }
                } catch (TokenException te) {
                    throw te;
                } catch (Exception parseEx) {
                    log.warn("No se pudo parsear el error de renovacion: {}", parseEx.getMessage());
                }
            }

            throw TokenException.refreshTokenExpired();
        } catch (TokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado renovando token: {}", e.getMessage(), e);
            throw new IllegalStateException("Error inesperado al renovar token desde Keycloak", e);
        }
    }

    public void logout(String refreshToken) {
        try {
            String logoutUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(logoutUrl, request, String.class);

            log.info("Sesion revocada en Keycloak exitosamente");
        } catch (Exception e) {
            log.warn("No se pudo revocar la sesion en Keycloak: {}", e.getMessage());
        }
    }

    public String extractUserIdFromToken(String token) {
        try {
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length < 2) {
                throw TokenException.malformed();
            }

            String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);
            JsonNode payloadNode = objectMapper.readTree(payload);
            String userId = payloadNode.path("sub").asText("");

            if (userId.isBlank()) {
                throw TokenException.malformed();
            }

            return userId;
        } catch (TokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("No se pudo extraer el userId desde el token: {}", e.getMessage(), e);
            throw TokenException.malformed();
        }
    }
}