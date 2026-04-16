package com.sssi.msvc_auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sssi.msvc_auth.exception.UserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
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

            throw new IllegalStateException("Keycloak no retornó access_token de administrador");

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error obteniendo token de administrador: {}", e.getMessage());
            throw new IllegalStateException("No se pudo autenticar con Keycloak admin", e);
        }
    }

    public String registerUser(String username, String email, String password,
                               String firstName, String lastName) {
        String adminToken = getAdminToken();
        String createUserUrl = keycloakServerUrl + "/admin/realms/" + realm + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + adminToken);

        try {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("username", username);
            userMap.put("email", email);
            userMap.put("firstName", firstName);
            userMap.put("lastName", lastName);
            userMap.put("enabled", true);

            String userJson = objectMapper.writeValueAsString(userMap);
            restTemplate.exchange(createUserUrl, HttpMethod.POST,
                    new HttpEntity<>(userJson, headers), String.class);

            log.info("Usuario creado en Keycloak: {}", username);

        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("409")) {
                throw UserException.userAlreadyExists(username);
            }
            log.error("Error creando usuario en Keycloak: {}", e.getMessage(), e);
            throw new IllegalStateException("Error al crear el usuario en Keycloak", e);
        }

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Iniciando resolveUserId para {}", username);
        String userId = resolveUserId(username, createUserUrl, headers);
        log.info("resolveUserId exitoso para {} con id {}", username, userId);

        log.info("Iniciando setPassword para {}", username);
        setPassword(userId, username, password, createUserUrl, headers);
        log.info("setPassword exitoso para {}", username);

        return userId;
    }

    private String resolveUserId(String username, String baseUrl, HttpHeaders headers) {
        for (int intento = 0; intento < 5; intento++) {
            try {
                String searchUrl = baseUrl + "?username=" + username;
                String response = restTemplate.exchange(
                        searchUrl, HttpMethod.GET,
                        new HttpEntity<>("", headers), String.class).getBody();

                JsonNode users = objectMapper.readTree(response);
                if (users.isArray() && users.size() > 0) {
                    String userId = users.get(0).get("id").asText();
                    log.info("Usuario ID obtenido: {}", userId);
                    return userId;
                }
            } catch (Exception e) {
                log.warn("Intento {} de obtener usuario ID falló: {}", intento + 1, e.getMessage());
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        throw new IllegalStateException("Usuario creado pero no fue posible obtener su ID en Keycloak");
    }

    private void setPassword(String userId, String username, String password,
                             String baseUrl, HttpHeaders headers) {
        try {
            String passwordUrl = baseUrl + "/" + userId + "/reset-password";

            Map<String, Object> credentialMap = new HashMap<>();
            credentialMap.put("type", "password");
            credentialMap.put("value", password);
            credentialMap.put("temporary", false);

            String credentialJson = objectMapper.writeValueAsString(credentialMap);
            restTemplate.exchange(passwordUrl, HttpMethod.PUT,
                    new HttpEntity<>(credentialJson, headers), String.class);

            log.info("Contraseña establecida para usuario: {}", username);

        } catch (Exception e) {
            log.error("Error estableciendo contraseña para {}: {}", username, e.getMessage());
            throw new IllegalStateException("Error al establecer la contraseña del usuario", e);
        }
    }

    public boolean userExists(String username) {
        try {
            String adminToken = getAdminToken();
            String searchUrl = keycloakServerUrl + "/admin/realms/" + realm + "/users?username=" + username;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + adminToken);

            String response = restTemplate.exchange(
                    searchUrl, HttpMethod.GET,
                    new HttpEntity<>("", headers), String.class).getBody();

            JsonNode users = objectMapper.readTree(response);
            return users.isArray() && users.size() > 0;

        } catch (Exception e) {
            log.error("Error verificando existencia de usuario {}: {}", username, e.getMessage());
            return false;
        }
    }

    public String findUserIdByIdentifier(String identifier) {
        try {
            String adminToken = getAdminToken();
            String encodedIdentifier = UriUtils.encodeQueryParam(identifier, StandardCharsets.UTF_8);
            String usersUrl = keycloakServerUrl + "/admin/realms/" + realm + "/users";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + adminToken);

            String userId = extractFirstUserId(usersUrl + "?username=" + encodedIdentifier + "&exact=true", headers);
            if (userId != null) {
                return userId;
            }

            userId = extractFirstUserId(usersUrl + "?email=" + encodedIdentifier + "&exact=true", headers);
            if (userId != null) {
                return userId;
            }

            throw UserException.notFound(identifier);
        } catch (UserException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error buscando userId en Keycloak para {}: {}", identifier, e.getMessage(), e);
            throw new IllegalStateException("No se pudo resolver el usuario en Keycloak", e);
        }
    }

    private String extractFirstUserId(String searchUrl, HttpHeaders headers) throws Exception {
        String response = restTemplate.exchange(
                searchUrl,
                HttpMethod.GET,
                new HttpEntity<>("", headers),
                String.class
        ).getBody();

        JsonNode users = objectMapper.readTree(response);
        if (users.isArray() && users.size() > 0 && users.get(0).has("id")) {
            return users.get(0).get("id").asText();
        }

        return null;
    }
}