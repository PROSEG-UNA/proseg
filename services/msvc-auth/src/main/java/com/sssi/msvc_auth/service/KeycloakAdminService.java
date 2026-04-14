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
import java.util.ArrayList;
import java.util.List;

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

    public List<Map<String, Object>> getRoles() throws Exception {
        String adminToken = getAdminToken();
        String rolesUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);

        HttpEntity<String> request = new HttpEntity<>("", headers);
        String response = restTemplate.exchange(rolesUrl, HttpMethod.GET, request, String.class).getBody();

        return objectMapper.readValue(response, List.class);
    }

    public List<Map<String, Object>> getBaseRoles() throws Exception {
        String adminToken = getAdminToken();
        String rolesUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);

        HttpEntity<String> request = new HttpEntity<>("", headers);
        String response = restTemplate.exchange(rolesUrl, HttpMethod.GET, request, String.class).getBody();

        List<Map<String, Object>> allRoles = objectMapper.readValue(response, List.class);
        return allRoles.stream()
                .filter(role -> !(Boolean) role.getOrDefault("composite", false))
                .collect(java.util.stream.Collectors.toList());
    }

    public void createCompositeRole(String roleName, List<String> privileges) throws Exception {
        String adminToken = getAdminToken();
        String rolesUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + adminToken);

        Map<String, Object> roleMap = new HashMap<>();
        roleMap.put("name", roleName);
        roleMap.put("composite", true);

        HttpEntity<String> createRequest = new HttpEntity<>(objectMapper.writeValueAsString(roleMap), headers);
        restTemplate.exchange(rolesUrl, HttpMethod.POST, createRequest, String.class);

        List<Map<String, Object>> privilegeRoles = new ArrayList<>();
        for (String privilege : privileges) {
            String roleUrl = rolesUrl + "/" + privilege;
            HttpEntity<String> request = new HttpEntity<>("", headers);
            String response = restTemplate.exchange(roleUrl, HttpMethod.GET, request, String.class).getBody();
            privilegeRoles.add(objectMapper.readValue(response, Map.class));
        }

        String compositeUrl = rolesUrl + "/" + roleName + "/composites";
        HttpEntity<String> compositeRequest = new HttpEntity<>(objectMapper.writeValueAsString(privilegeRoles), headers);
        restTemplate.exchange(compositeUrl, HttpMethod.POST, compositeRequest, String.class);
    }

    public void updateRole(String roleName, List<String> newPrivileges) throws Exception {
        String adminToken = getAdminToken();
        String rolesUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + adminToken);

        String compositeUrl = rolesUrl + "/" + roleName + "/composites";
        HttpEntity<String> request = new HttpEntity<>("", headers);
        String currentResponse = restTemplate.exchange(compositeUrl, HttpMethod.GET, request, String.class).getBody();
        List<Map<String, Object>> currentPrivileges = objectMapper.readValue(currentResponse, List.class);

        if (!currentPrivileges.isEmpty()) {
            HttpEntity<String> deleteRequest = new HttpEntity<>(objectMapper.writeValueAsString(currentPrivileges), headers);
            restTemplate.exchange(compositeUrl, HttpMethod.DELETE, deleteRequest, String.class);
        }

        List<Map<String, Object>> newPrivilegeRoles = new ArrayList<>();
        for (String privilege : newPrivileges) {
            String roleUrl = rolesUrl + "/" + privilege;
            String response = restTemplate.exchange(roleUrl, HttpMethod.GET, request, String.class).getBody();
            newPrivilegeRoles.add(objectMapper.readValue(response, Map.class));
        }

        HttpEntity<String> compositeRequest = new HttpEntity<>(objectMapper.writeValueAsString(newPrivilegeRoles), headers);
        restTemplate.exchange(compositeUrl, HttpMethod.POST, compositeRequest, String.class);
    }

    public void deleteRole(String roleName) throws Exception {
        String adminToken = getAdminToken();
        String roleUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles/" + roleName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);

        HttpEntity<String> request = new HttpEntity<>("", headers);
        restTemplate.exchange(roleUrl, HttpMethod.DELETE, request, String.class);
    }
}
