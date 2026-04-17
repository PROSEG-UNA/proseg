package com.sssi.msvc_auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sssi.msvc_auth.dto.KeycloakUserResponseDto;
import com.sssi.msvc_auth.exception.UserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class KeycloakAdminService {

    @Value("${keycloak.server-url:https://auth.devbychris.com}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm:sssi-realm}")
    private String realm;

    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:sssi_user}")
    private String adminPassword;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public KeycloakAdminService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    private String getAdminToken() {
        try {
            String tokenUrl = keycloakServerUrl + "/realms/master/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", "admin-cli");
            body.add("username", adminUsername);
            body.add("password", adminPassword);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(tokenUrl, request, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            if (jsonNode.has("access_token")) {
                return jsonNode.get("access_token").asText();
            }

            throw new IllegalStateException("Keycloak no retorno access_token de administrador");
        } catch (Exception e) {
            log.error("Error obteniendo token de administrador: {}", e.getMessage(), e);
            throw new IllegalStateException("No se pudo autenticar con Keycloak admin", e);
        }
    }

    public KeycloakUserResponseDto getUserById(String userId) {
        String adminToken = getAdminToken();
        String url = keycloakServerUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders headers = buildJsonHeaders(adminToken);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            JsonNode node = objectMapper.readTree(response.getBody());

            return KeycloakUserResponseDto.builder()
                    .id(node.path("id").asText())
                    .username(node.path("username").asText())
                    .email(node.path("email").asText())
                    .firstName(node.path("firstName").asText())
                    .lastName(node.path("lastName").asText())
                    .build();

        } catch (HttpStatusCodeException e) {

            if (e.getStatusCode().value() == 404) {
                throw UserException.notFound(userId);
            }

            log.error("Error HTTP obteniendo usuario {}: {} - {}",
                    userId, e.getStatusCode(), e.getResponseBodyAsString());

            throw new IllegalStateException("Error consultando usuario en Keycloak", e);

        } catch (Exception e) {
            log.error("Error inesperado obteniendo usuario {}: {}", userId, e.getMessage(), e);
            throw new IllegalStateException("Error inesperado consultando Keycloak", e);
        }
    }

    public String registerUser(String username, String email, String password, String firstName, String lastName) {
        String adminToken = getAdminToken();
        String createUserUrl = keycloakServerUrl + "/admin/realms/" + realm + "/users";

        HttpHeaders headers = buildJsonHeaders(adminToken);

        try {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("username", username);
            userMap.put("email", email);
            userMap.put("firstName", firstName);
            userMap.put("lastName", lastName);
            userMap.put("enabled", false);
            userMap.put("emailVerified", false);

            ResponseEntity<String> response = restTemplate.exchange(
                    createUserUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(userMap), headers),
                    String.class
            );

            String userId = extractUserIdFromLocation(response);
            setPassword(userId, password, headers);

            log.info("Usuario creado en Keycloak y bloqueado hasta aprobacion: {}", username);
            return userId;
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 409) {
                throw UserException.userAlreadyExists(username);
            }
            log.error("Error creando usuario en Keycloak: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new IllegalStateException("Error al crear el usuario en Keycloak", e);
        } catch (Exception e) {
            log.error("Error creando usuario en Keycloak: {}", e.getMessage(), e);
            throw new IllegalStateException("Error al crear el usuario en Keycloak", e);
        }
    }

    public void enableUser(String userId) {
        updateUserEnabled(userId, true);
    }

    public void disableUser(String userId) {
        updateUserEnabled(userId, false);
    }

    private void updateUserEnabled(String userId, boolean enabled) {
        String adminToken = getAdminToken();
        String userUrl = keycloakServerUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders headers = buildJsonHeaders(adminToken);

        try {
            String currentUserResponse = restTemplate.exchange(
                    userUrl,
                    HttpMethod.GET,
                    new HttpEntity<>("", headers),
                    String.class
            ).getBody();

            JsonNode userNode = objectMapper.readTree(currentUserResponse);
            Map<String, Object> userMap = objectMapper.convertValue(userNode, Map.class);
            userMap.put("enabled", enabled);

            restTemplate.exchange(
                    userUrl,
                    HttpMethod.PUT,
                    new HttpEntity<>(objectMapper.writeValueAsString(userMap), headers),
                    String.class
            );

            log.info("Usuario {} en Keycloak con enabled={}", userId, enabled);
        } catch (Exception e) {
            log.error("Error actualizando enabled del usuario {} en Keycloak: {}", userId, e.getMessage(), e);
            throw new IllegalStateException("No se pudo actualizar el estado del usuario en Keycloak", e);
        }
    }

    private void setPassword(String userId, String password, HttpHeaders headers) {
        try {
            String passwordUrl = keycloakServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password";

            Map<String, Object> credentialMap = new HashMap<>();
            credentialMap.put("type", "password");
            credentialMap.put("value", password);
            credentialMap.put("temporary", false);

            restTemplate.exchange(
                    passwordUrl,
                    HttpMethod.PUT,
                    new HttpEntity<>(objectMapper.writeValueAsString(credentialMap), headers),
                    String.class
            );
        } catch (Exception e) {
            log.error("Error estableciendo contraseña para userId {}: {}", userId, e.getMessage(), e);
            throw new IllegalStateException("Error al establecer la contraseña del usuario", e);
        }
    }

    private HttpHeaders buildJsonHeaders(String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);
        return headers;
    }

    private String extractUserIdFromLocation(ResponseEntity<String> response) {
        URI location = response.getHeaders().getLocation();

        if (location == null) {
            throw new IllegalStateException("Keycloak no retorno Location header al crear el usuario");
        }

        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}