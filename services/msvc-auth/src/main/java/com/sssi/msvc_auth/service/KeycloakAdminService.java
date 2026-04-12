package com.sssi.msvc_auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class KeycloakAdminService {

    @Value("${keycloak.server-url:http://localhost:8080}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm:sssi-realm}")
    private String realm;

    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:admin}")
    private String adminPassword;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public KeycloakAdminService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    private String getAdminToken() throws Exception {
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
            } else {
                throw new Exception("Failed to get admin token");
            }
        } catch (Exception e) {
            log.error("Error getting admin token: ", e);
            throw e;
        }
    }

    /**
     * Registra un nuevo usuario en Keycloak
     */
    public void registerUser(String username, String email, String password, String firstName, String lastName) throws Exception {
        try {
            String adminToken = getAdminToken();

            // 1. Crear usuario
            String createUserUrl = keycloakServerUrl + "/admin/realms/" + realm + "/users";

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("username", username);
            userMap.put("email", email);
            userMap.put("firstName", firstName);
            userMap.put("lastName", lastName);
            userMap.put("enabled", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + adminToken);

            String userJson = objectMapper.writeValueAsString(userMap);
            HttpEntity<String> createUserRequest = new HttpEntity<>(userJson, headers);

            try {
                restTemplate.exchange(createUserUrl, HttpMethod.POST, createUserRequest, String.class);
            } catch (Exception e) {
                if (e.getMessage().contains("409")) {
                    throw new Exception("El usuario ya existe");
                }
                throw e;
            }
            log.info("Usuario creado en Keycloak: {}", username);

            // 2. Obtener ID del usuario creado (con reintentos)
            String userId = null;
            for (int i = 0; i < 3; i++) {
                try {
                    String searchUrl = createUserUrl + "?username=" + username;
                    HttpEntity<String> searchRequest = new HttpEntity<>("", headers);
                    String searchResponse = restTemplate.exchange(searchUrl, HttpMethod.GET, searchRequest, String.class).getBody();

                    JsonNode users = objectMapper.readTree(searchResponse);
                    if (users.isArray() && users.size() > 0) {
                        userId = users.get(0).get("id").asText();
                        log.info("Usuario ID obtenido: {}", userId);
                        break;
                    }
                } catch (Exception e) {
                    log.warn("Intento {} de obtener usuario ID falló, reintentando...", i + 1);
                    if (i < 2) {
                        Thread.sleep(500); // Esperar 500ms antes de reintentar
                    }
                }
            }

            if (userId == null) {
                throw new Exception("No se pudo obtener el ID del usuario después de creación");
            }

            // 3. Establecer contraseña usando el endpoint correcto
            String passwordUrl = createUserUrl + "/" + userId + "/reset-password";

            Map<String, Object> credentialMap = new HashMap<>();
            credentialMap.put("type", "password");
            credentialMap.put("value", password);
            credentialMap.put("temporary", false);

            String credentialJson = objectMapper.writeValueAsString(credentialMap);
            HttpEntity<String> passwordRequest = new HttpEntity<>(credentialJson, headers);

            try {
                restTemplate.exchange(passwordUrl, HttpMethod.PUT, passwordRequest, String.class);
                log.info("Contraseña establecida para usuario: {}", username);
            } catch (Exception e) {
                log.error("Error estableciendo contraseña: ", e);
                throw new Exception("Error al establecer la contraseña del usuario");
            }

            // Pequeño delay para asegurar que el usuario esté completamente propagado
            Thread.sleep(1000);

        } catch (Exception e) {
            log.error("Error en registerUser: ", e);
            throw e;
        }
    }

    public boolean userExists(String username) {
        try {
            String adminToken = getAdminToken();
            String searchUrl = keycloakServerUrl + "/admin/realms/" + realm + "/users?username=" + username;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + adminToken);

            HttpEntity<String> request = new HttpEntity<>("", headers);
            String response = restTemplate.exchange(searchUrl, HttpMethod.GET, request, String.class).getBody();

            JsonNode users = objectMapper.readTree(response);
            return users.isArray() && users.size() > 0;
        } catch (Exception e) {
            log.error("Error checking if user exists: ", e);
            return false;
        }
    }
}
