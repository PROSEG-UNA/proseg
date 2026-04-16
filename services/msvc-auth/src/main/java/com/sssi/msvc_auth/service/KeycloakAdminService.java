package com.sssi.msvc_auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sssi.msvc_auth.dto.RoleDto;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public void registerUser(String username, String email, String password,
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
            log.error("Error creando usuario en Keycloak: {}", e.getMessage());
            throw new IllegalStateException("Error al crear el usuario en Keycloak", e);
        }

        String userId = resolveUserId(username, createUserUrl, headers);
        setPassword(userId, username, password, createUserUrl, headers);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String resolveUserId(String username, String baseUrl, HttpHeaders headers) {
        for (int i = 0; i < 3; i++) {
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
                log.warn("Intento {} de obtener usuario ID falló: {}", i + 1, e.getMessage());
            }

            if (i < 2) {
                try { Thread.sleep(500); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw UserException.notFound(username);
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

    private boolean isInternalRole(String name) {
        return name.startsWith("default-roles-")
                || name.equals("offline_access")
                || name.equals("uma_authorization");
    }

    public List<RoleDto> getBaseRoles() {
        String adminToken = getAdminToken();
        String rolesUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);

        try {
            String response = restTemplate.exchange(
                    rolesUrl, HttpMethod.GET,
                    new HttpEntity<>("", headers), String.class).getBody();

            JsonNode rolesNode = objectMapper.readTree(response);
            List<RoleDto> roles = new ArrayList<>();

            for (JsonNode node : rolesNode) {
                String name = node.path("name").asText();
                if (!node.path("composite").asBoolean(false) && !isInternalRole(name)) {
                    roles.add(RoleDto.builder()
                            .id(node.path("id").asText())
                            .name(name)
                            .description(node.path("description").asText(null))
                            .composite(false)
                            .build());
                }
            }

            log.info("Roles base obtenidos: {}", roles.size());
            return roles;

        } catch (Exception e) {
            log.error("Error obteniendo roles base: {}", e.getMessage());
            throw new IllegalStateException("Error al obtener los roles base de Keycloak", e);
        }
    }

    public List<RoleDto> getCompositeRoles() {
        String adminToken = getAdminToken();
        String rolesUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);

        try {
            String response = restTemplate.exchange(
                    rolesUrl, HttpMethod.GET,
                    new HttpEntity<>("", headers), String.class).getBody();

            JsonNode rolesNode = objectMapper.readTree(response);
            List<RoleDto> roles = new ArrayList<>();

            for (JsonNode node : rolesNode) {
                String name = node.path("name").asText();
                if (node.path("composite").asBoolean(false) && !isInternalRole(name)) {
                    roles.add(RoleDto.builder()
                            .id(node.path("id").asText())
                            .name(name)
                            .description(node.path("description").asText(null))
                            .composite(true)
                            .build());
                }
            }

            log.info("Roles compuestos obtenidos: {}", roles.size());
            return roles;

        } catch (Exception e) {
            log.error("Error obteniendo roles compuestos: {}", e.getMessage());
            throw new IllegalStateException("Error al obtener los roles compuestos de Keycloak", e);
        }
    }

    public List<RoleDto> getRoleComposites(String roleName) {
        String adminToken = getAdminToken();
        String compositeUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles/" + roleName + "/composites";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);

        try {
            String response = restTemplate.exchange(
                    compositeUrl, HttpMethod.GET,
                    new HttpEntity<>("", headers), String.class).getBody();

            JsonNode rolesNode = objectMapper.readTree(response);
            List<RoleDto> roles = new ArrayList<>();

            for (JsonNode node : rolesNode) {
                String name = node.path("name").asText();
                if (!isInternalRole(name)) {
                    roles.add(RoleDto.builder()
                            .id(node.path("id").asText())
                            .name(name)
                            .description(node.path("description").asText(null))
                            .composite(node.path("composite").asBoolean(false))
                            .build());
                }
            }

            log.info("Composites del rol {} obtenidos: {}", roleName, roles.size());
            return roles;

        } catch (Exception e) {
            log.error("Error obteniendo composites del rol {}: {}", roleName, e.getMessage());
            throw new IllegalStateException("Error al obtener los composites del rol " + roleName, e);
        }
    }

    public void createCompositeRole(String roleName, List<String> privileges) {
        String adminToken = getAdminToken();
        String rolesUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + adminToken);

        try {
            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("name", roleName);
            roleMap.put("composite", true);

            restTemplate.exchange(rolesUrl, HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(roleMap), headers), String.class);

            log.info("Rol {} creado en Keycloak", roleName);

            List<Map<String, Object>> privilegeRoles = new ArrayList<>();
            for (String privilege : privileges) {
                String roleUrl = rolesUrl + "/" + privilege;
                String response = restTemplate.exchange(roleUrl, HttpMethod.GET,
                        new HttpEntity<>("", headers), String.class).getBody();
                JsonNode node = objectMapper.readTree(response);
                Map<String, Object> roleData = new HashMap<>();
                roleData.put("id", node.path("id").asText());
                roleData.put("name", node.path("name").asText());
                privilegeRoles.add(roleData);
            }

            String compositeUrl = rolesUrl + "/" + roleName + "/composites";
            restTemplate.exchange(compositeUrl, HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(privilegeRoles), headers), String.class);

            log.info("Privilegios asignados al rol {}: {}", roleName, privileges);

        } catch (Exception e) {
            log.error("Error creando rol compuesto {}: {}", roleName, e.getMessage());
            throw new IllegalStateException("Error al crear el rol " + roleName, e);
        }
    }

    public void updateRole(String roleName, List<String> newPrivileges) {
        String adminToken = getAdminToken();
        String rolesUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + adminToken);

        try {
            String compositeUrl = rolesUrl + "/" + roleName + "/composites";
            String currentResponse = restTemplate.exchange(compositeUrl, HttpMethod.GET,
                    new HttpEntity<>("", headers), String.class).getBody();

            JsonNode currentNode = objectMapper.readTree(currentResponse);
            List<Map<String, Object>> currentPrivileges = new ArrayList<>();
            for (JsonNode node : currentNode) {
                Map<String, Object> roleData = new HashMap<>();
                roleData.put("id", node.path("id").asText());
                roleData.put("name", node.path("name").asText());
                currentPrivileges.add(roleData);
            }

            if (!currentPrivileges.isEmpty()) {
                restTemplate.exchange(compositeUrl, HttpMethod.DELETE,
                        new HttpEntity<>(objectMapper.writeValueAsString(currentPrivileges), headers), String.class);
            }

            List<Map<String, Object>> newPrivilegeRoles = new ArrayList<>();
            for (String privilege : newPrivileges) {
                String roleUrl = rolesUrl + "/" + privilege;
                String response = restTemplate.exchange(roleUrl, HttpMethod.GET,
                        new HttpEntity<>("", headers), String.class).getBody();
                JsonNode node = objectMapper.readTree(response);
                Map<String, Object> roleData = new HashMap<>();
                roleData.put("id", node.path("id").asText());
                roleData.put("name", node.path("name").asText());
                newPrivilegeRoles.add(roleData);
            }

            restTemplate.exchange(compositeUrl, HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(newPrivilegeRoles), headers), String.class);

            log.info("Rol {} actualizado con privilegios: {}", roleName, newPrivileges);

        } catch (Exception e) {
            log.error("Error actualizando rol {}: {}", roleName, e.getMessage());
            throw new IllegalStateException("Error al actualizar el rol " + roleName, e);
        }
    }

    public void deleteRole(String roleName) {
        String adminToken = getAdminToken();
        String roleUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles/" + roleName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);

        try {
            restTemplate.exchange(roleUrl, HttpMethod.DELETE,
                    new HttpEntity<>("", headers), String.class);
            log.info("Rol {} eliminado de Keycloak", roleName);

        } catch (Exception e) {
            log.error("Error eliminando rol {}: {}", roleName, e.getMessage());
            throw new IllegalStateException("Error al eliminar el rol " + roleName, e);
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
}