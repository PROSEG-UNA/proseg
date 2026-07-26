package com.sssi.msvc_auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sssi.common.api.response.PagedResponse;
import com.sssi.msvc_auth.dto.KeycloakUserResponseDto;
import com.sssi.msvc_auth.exception.KeycloakException;
import com.sssi.msvc_auth.dto.RoleResponseDto;
import com.sssi.msvc_auth.entity.User;
import com.sssi.msvc_auth.exception.UserException;
import com.sssi.msvc_auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KeycloakAdminService {
    private static final String PROFILE_IMAGE_OBJECT_NAME_ATTRIBUTE = "profileImageObjectName";
    private static final String ARCHIVE_FILE_ROUTE_PREFIX = "/api/v1/archive/files/";

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
    private final UserRepository userRepository;

    public KeycloakAdminService(RestTemplate restTemplate, ObjectMapper objectMapper, UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
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
            return toUserResponse(node);

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

    public String registerUser(String username,
                               String email,
                               String password,
                               String firstName,
                               String lastName) {
        return registerUser(username, email, password, firstName, lastName, false);
    }

    public String registerUser(String username,
                               String email,
                               String password,
                               String firstName,
                               String lastName,
                               boolean enabled) {
        String userId = registerUser(username, email, firstName, lastName, enabled);
        if (password != null && !password.isBlank()) {
            String adminToken = getAdminToken();
            HttpHeaders headers = buildJsonHeaders(adminToken);
            setPassword(userId, password, headers);
        }
        return userId;
    }

    public String registerUser(String username,
                               String email,
                               String firstName,
                               String lastName,
                               boolean enabled) {
        String adminToken = getAdminToken();
        String createUserUrl = keycloakServerUrl + "/admin/realms/" + realm + "/users";
        HttpHeaders headers = buildJsonHeaders(adminToken);
        try {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("username", username);
            userMap.put("email", email);
            userMap.put("firstName", firstName);
            userMap.put("lastName", lastName);
            userMap.put("enabled", enabled);
            userMap.put("emailVerified", false);
            ResponseEntity<String> response = restTemplate.exchange(
                    createUserUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(userMap), headers),
                    String.class
            );
            String userId = extractUserIdFromLocation(response);
            log.info("Usuario creado en Keycloak: {} (enabled={})", username, enabled);
            return userId;
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 409) {
                throw buildRegisterConflictException(e, username, email);
            }
            log.error("Error creando usuario en Keycloak: {} - {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e);
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
                    new HttpEntity<>(headers),
                    String.class
            ).getBody();

            JsonNode userNode = objectMapper.readTree(currentUserResponse);
            Map<String, Object> userPayload = new HashMap<>();
            String username = userNode.path("username").asText("");

            if (username.isBlank()) {
                throw new IllegalStateException("El usuario de Keycloak no contiene username");
            }

            userPayload.put("id", userId);
            userPayload.put("username", username);
            userPayload.put("enabled", enabled);

            if (userNode.hasNonNull("firstName")) {
                userPayload.put("firstName", userNode.get("firstName").asText());
            }
            if (userNode.hasNonNull("lastName")) {
                userPayload.put("lastName", userNode.get("lastName").asText());
            }
            if (userNode.hasNonNull("email")) {
                userPayload.put("email", userNode.get("email").asText());
            }
            if (userNode.has("emailVerified")) {
                userPayload.put("emailVerified", userNode.path("emailVerified").asBoolean(false));
            }
            if (userNode.has("attributes")) {
                userPayload.put("attributes", objectMapper.convertValue(userNode.get("attributes"), Map.class));
            }
            if (userNode.has("requiredActions")) {
                userPayload.put("requiredActions", objectMapper.convertValue(userNode.get("requiredActions"), List.class));
            }

            restTemplate.exchange(
                    userUrl,
                    HttpMethod.PUT,
                    new HttpEntity<>(objectMapper.writeValueAsString(userPayload), headers),
                    String.class
            );

            log.info("Usuario {} en Keycloak con enabled={}", userId, enabled);
        } catch (HttpStatusCodeException e) {
            log.error(
                    "Error HTTP actualizando enabled del usuario {} en Keycloak: {} - {}",
                    userId,
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );

            if (e.getStatusCode().value() == 404) {
                throw KeycloakException.userNotFound(userId);
            }

            throw new IllegalStateException("No se pudo actualizar el estado del usuario en Keycloak", e);
        } catch (Exception e) {
            log.error("Error actualizando enabled del usuario {} en Keycloak: {}", userId, e.getMessage(), e);
            throw new IllegalStateException("No se pudo actualizar el estado del usuario en Keycloak", e);
        }
    }

    public KeycloakUserResponseDto updateProfileImageObjectName(String userId, String objectName) {
        String adminToken = getAdminToken();
        String userUrl = keycloakServerUrl + "/admin/realms/" + realm + "/users/" + userId;
        HttpHeaders headers = buildJsonHeaders(adminToken);

        try {
            String currentUserResponse = restTemplate.exchange(
                    userUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            ).getBody();

            JsonNode userNode = objectMapper.readTree(currentUserResponse);
            Map<String, Object> userPayload = buildUserPayload(userId, userNode);

            Map<String, List<String>> attributes = extractAttributes(userNode);
            String normalizedObjectName = normalizeObjectName(objectName);
            if (normalizedObjectName == null) {
                attributes.remove(PROFILE_IMAGE_OBJECT_NAME_ATTRIBUTE);
            } else {
                attributes.put(PROFILE_IMAGE_OBJECT_NAME_ATTRIBUTE, List.of(normalizedObjectName));
            }
            userPayload.put("attributes", attributes);

            restTemplate.exchange(
                    userUrl,
                    HttpMethod.PUT,
                    new HttpEntity<>(objectMapper.writeValueAsString(userPayload), headers),
                    String.class
            );

            return getUserById(userId);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 404) {
                throw KeycloakException.userNotFound(userId);
            }
            log.error(
                    "Error HTTP actualizando imagen de perfil del usuario {} en Keycloak: {} - {}",
                    userId,
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );
            throw new IllegalStateException("Error al actualizar la imagen de perfil en Keycloak", e);
        } catch (Exception e) {
            log.error(
                    "Error inesperado actualizando imagen de perfil del usuario {} en Keycloak: {}",
                    userId,
                    e.getMessage(),
                    e
            );
            throw new IllegalStateException("Error al actualizar la imagen de perfil en Keycloak", e);
        }
    }

    public List<RoleResponseDto> getBaseRoles() {
        String adminToken = getAdminToken();
        String rolesUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles?briefRepresentation=false";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);

        try {
            String response = restTemplate.exchange(
                    rolesUrl, HttpMethod.GET,
                    new HttpEntity<>("", headers), String.class).getBody();

            JsonNode rolesNode = objectMapper.readTree(response);
            List<RoleResponseDto> roles = new ArrayList<>();

            for (JsonNode node : rolesNode) {
                String name = node.path("name").asText();
                if (!node.path("composite").asBoolean(false) && !isInternalRole(name)) {
                    JsonNode domainNode = node.path("attributes").path("domain");
                    String domain = domainNode.isArray() && domainNode.size() > 0
                            ? domainNode.get(0).asText(null)
                            : null;
                    roles.add(RoleResponseDto.builder()
                            .id(node.path("id").asText())
                            .name(name)
                            .description(node.path("description").asText(null))
                            .composite(false)
                            .domain(domain)
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

    public PagedResponse<RoleResponseDto> getCompositeRoles(Pageable pageable) {
        String adminToken = getAdminToken();
        String rolesUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);

        try {
            String response = restTemplate.exchange(
                    rolesUrl, HttpMethod.GET,
                    new HttpEntity<>("", headers), String.class).getBody();

            JsonNode rolesNode = objectMapper.readTree(response);
            List<RoleResponseDto> allRoles = new ArrayList<>();

            for (JsonNode node : rolesNode) {
                String name = node.path("name").asText();
                if (node.path("composite").asBoolean(false) && !isInternalRole(name)) {
                    allRoles.add(RoleResponseDto.builder()
                            .id(node.path("id").asText())
                            .name(name)
                            .description(node.path("description").asText(null))
                            .composite(true)
                            .build());
                }
            }

            int total = allRoles.size();
            int page = pageable.getPageNumber();
            int size = pageable.getPageSize();
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, total);
            List<RoleResponseDto> pageContent = fromIndex >= total ? List.of() : allRoles.subList(fromIndex, toIndex);
            int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);

            log.info("Roles compuestos obtenidos: {}, pagina: {}/{}", total, page + 1, totalPages);
            return PagedResponse.<RoleResponseDto>builder()
                    .content(pageContent)
                    .page(page)
                    .size(size)
                    .totalElements(total)
                    .totalPages(totalPages)
                    .last(page >= totalPages - 1)
                    .build();
        } catch (Exception e) {
            log.error("Error obteniendo roles compuestos: {}", e.getMessage());
            throw new IllegalStateException("Error al obtener los roles compuestos de Keycloak", e);
        }
    }

    public List<RoleResponseDto> getRoleComposites(String roleName) {
        String adminToken = getAdminToken();
        String compositeUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles/" + roleName + "/composites";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);

        try {
            String response = restTemplate.exchange(
                    compositeUrl, HttpMethod.GET,
                    new HttpEntity<>("", headers), String.class).getBody();

            JsonNode rolesNode = objectMapper.readTree(response);
            List<RoleResponseDto> roles = new ArrayList<>();

            for (JsonNode node : rolesNode) {
                String name = node.path("name").asText();
                if (!isInternalRole(name)) {
                    roles.add(RoleResponseDto.builder()
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

    public void createCompositeRole(String roleName, String description, List<String> privileges) {
        String adminToken = getAdminToken();
        String rolesUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + adminToken);

        try {
            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("name", roleName);
            roleMap.put("composite", true);
            if (description != null && !description.isBlank()) {
                roleMap.put("description", description);
            }

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

    public void updateRole(String roleName, String newRoleName, String description, List<String> newPrivileges) {
        String adminToken = getAdminToken();
        String rolesUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + adminToken);

        try {
            String currentRoleResponse = restTemplate.exchange(rolesUrl + "/" + roleName, HttpMethod.GET,
                    new HttpEntity<>("", headers), String.class).getBody();
            JsonNode currentRole = objectMapper.readTree(currentRoleResponse);

            String effectiveName = (newRoleName != null && !newRoleName.isBlank()) ? newRoleName : roleName;
            String effectiveDescription = (description != null && !description.isBlank()) ? description : currentRole.path("description").asText("");

            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("name", effectiveName);
            roleMap.put("description", effectiveDescription);
            restTemplate.exchange(rolesUrl + "/" + roleName, HttpMethod.PUT,
                    new HttpEntity<>(objectMapper.writeValueAsString(roleMap), headers), String.class);

            if (newPrivileges != null && !newPrivileges.isEmpty()) {
                String compositeUrl = rolesUrl + "/" + effectiveName + "/composites";
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
            }

            log.info("Rol {} actualizado exitosamente", effectiveName);
        } catch (Exception e) {
            log.error("Error actualizando rol {}: {}", roleName, e.getMessage());
            throw new IllegalStateException("Error al actualizar el rol " + roleName, e);
        }
    }

    public void deleteRole(String roleName) {
        if ("SUPER_ADMINISTRADOR".equals(roleName.toUpperCase())) {
            throw new IllegalArgumentException("El rol SUPER_ADMINISTRADOR no puede ser eliminado");
        }
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

    public PagedResponse<KeycloakUserResponseDto> getAllUsers(Pageable pageable) {
        String adminToken = getAdminToken();

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int first = page * size;

        HttpHeaders headers = buildJsonHeaders(adminToken);

        try {
            String usersUrl = keycloakServerUrl + "/admin/realms/" + realm +
                    "/users?first=" + first + "&max=" + size;

            String usersResponse = restTemplate.exchange(
                    usersUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            ).getBody();

            JsonNode usersNode = objectMapper.readTree(usersResponse);
            List<KeycloakUserResponseDto> users = new ArrayList<>();

            for (JsonNode node : usersNode) {
                users.add(toUserResponse(node));
            }

            List<String> keycloakUserIds = users.stream()
                    .map(KeycloakUserResponseDto::getId)
                    .toList();
            Map<String, User.UserStatus> statusByKeycloakId = userRepository.findAllByKeycloakUserIdIn(keycloakUserIds)
                    .stream()
                    .collect(Collectors.toMap(User::getKeycloakUserId, User::getStatus));
            users.forEach(user -> user.setStatus(
                    statusByKeycloakId.getOrDefault(user.getId(), User.UserStatus.PENDING).name()
            ));
            String countUrl = keycloakServerUrl + "/admin/realms/" + realm + "/users/count";
            String countResponse = restTemplate.exchange(
                    countUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            ).getBody();
            long total = Long.parseLong(countResponse);
            int totalPages = (int) Math.ceil((double) total / size);
            return PagedResponse.<KeycloakUserResponseDto>builder()
                    .content(users)
                    .page(page)
                    .size(size)
                    .totalElements(total)
                    .totalPages(totalPages)
                    .last(page >= totalPages - 1)
                    .build();
        } catch (Exception e) {
            log.error("Error obteniendo usuarios paginados: {}", e.getMessage(), e);
            throw new IllegalStateException("Error al obtener usuarios de Keycloak", e);
        }
    }

    public List<KeycloakUserResponseDto> getUsersByRole(String roleName) {
        String adminToken = getAdminToken();
        String url = keycloakServerUrl + "/admin/realms/" + realm + "/roles/" + roleName + "/users";

        HttpHeaders headers = buildJsonHeaders(adminToken);

        try {
            String response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            ).getBody();

            JsonNode usersNode = objectMapper.readTree(response);
            List<KeycloakUserResponseDto> users = new ArrayList<>();

            for (JsonNode node : usersNode) {
                users.add(toUserResponse(node));
            }

            log.info("Usuarios con rol {} obtenidos: {}", roleName, users.size());
            return users;

        } catch (Exception e) {
            log.error("Error obteniendo usuarios con rol {}: {}", roleName, e.getMessage());
            throw new IllegalStateException("Error al obtener usuarios con rol " + roleName, e);
        }
    }

    public List<RoleResponseDto> getRolesByUserId(String userId) {
        String adminToken = getAdminToken();
        String url = keycloakServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";

        HttpHeaders headers = buildJsonHeaders(adminToken);

        try {
            String response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            ).getBody();

            JsonNode rolesNode = objectMapper.readTree(response);
            List<RoleResponseDto> roles = new ArrayList<>();

            for (JsonNode node : rolesNode) {
                String name = node.path("name").asText();
                if (!isInternalRole(name)) {
                    roles.add(RoleResponseDto.builder()
                            .id(node.path("id").asText())
                            .name(name)
                            .description(node.path("description").asText(null))
                            .composite(node.path("composite").asBoolean(false))
                            .build());
                }
            }

            log.info("Roles del usuario {} obtenidos: {}", userId, roles.size());
            return roles;

        } catch (HttpStatusCodeException e) {
            log.error("Error obteniendo roles del usuario {}: {} - {}",
                    userId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("Error al obtener roles del usuario " + userId, e);
        } catch (Exception e) {
            log.error("Error inesperado obteniendo roles del usuario {}: {}", userId, e.getMessage(), e);
            throw new IllegalStateException("Error al obtener roles del usuario " + userId, e);
        }
    }

    public void assignRoleToUser(String userId, String roleId) {
        List<RoleResponseDto> currentCompositeRoles = getRolesByUserId(userId).stream()
                .filter(RoleResponseDto::isComposite)
                .toList();
        boolean alreadyAssigned = currentCompositeRoles.stream()
                .anyMatch(role -> role.getId().equals(roleId));
        List<RoleResponseDto> rolesToRemove = currentCompositeRoles.stream()
                .filter(role -> !role.getId().equals(roleId))
                .toList();
        for (RoleResponseDto role : rolesToRemove) {
            removeRoleFromUser(userId, role.getId());
        }
        if (alreadyAssigned) {
            log.info("RoleId {} ya estaba asignado al usuario {} y se removieron {} roles compuestos adicionales",
                    roleId, userId, rolesToRemove.size());
            return;
        }
        String adminToken = getAdminToken();
        HttpHeaders headers = buildJsonHeaders(adminToken);
        try {
            List<Map<String, Object>> roles = List.of(resolveRoleMappingById(roleId, headers));
            String assignRoleUrl = keycloakServerUrl + "/admin/realms/" + realm +
                    "/users/" + userId + "/role-mappings/realm";
            restTemplate.exchange(
                    assignRoleUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(roles), headers),
                    String.class
            );
            log.info("RoleId {} asignado al usuario {} con politica de rol unico", roleId, userId);
        } catch (HttpStatusCodeException e) {
            log.error("Error asignando roleId {} al usuario {}: {} - {}",
                    roleId, userId, e.getStatusCode(), e.getResponseBodyAsString());
            handleKeycloakError(e, roleId, userId);
        } catch (Exception e) {
            log.error("Error inesperado asignando roleId {} al usuario {}: {}",
                    roleId, userId, e.getMessage(), e);
            throw KeycloakException.generic("Error procesando respuesta de Keycloak");
        }
    }

    public void removeRoleFromUser(String userId, String roleId) {
        String adminToken = getAdminToken();

        String rolesByIdUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles-by-id/" + roleId;
        String removeRoleUrl = keycloakServerUrl + "/admin/realms/" + realm +
                "/users/" + userId + "/role-mappings/realm";

        HttpHeaders headers = buildJsonHeaders(adminToken);

        try {
            String roleResponse = restTemplate.exchange(
                    rolesByIdUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            ).getBody();

            JsonNode roleNode = objectMapper.readTree(roleResponse);

            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("id", roleNode.path("id").asText());
            roleMap.put("name", roleNode.path("name").asText());

            List<Map<String, Object>> roles = List.of(roleMap);

            restTemplate.exchange(
                    removeRoleUrl,
                    HttpMethod.DELETE,
                    new HttpEntity<>(objectMapper.writeValueAsString(roles), headers),
                    String.class
            );

            log.info("RoleId {} removido del usuario {}", roleId, userId);

        } catch (HttpStatusCodeException e) {
            log.error("Error removiendo roleId {} del usuario {}: {} - {}",
                    roleId, userId, e.getStatusCode(), e.getResponseBodyAsString());

            handleKeycloakError(e, roleId, userId);

        } catch (Exception e) {
            log.error("Error inesperado removiendo roleId {} del usuario {}: {}",
                    roleId, userId, e.getMessage(), e);

            throw KeycloakException.generic("Error procesando respuesta de Keycloak");
        }
    }

    public String getRoleNameById(String roleId) {
        String adminToken = getAdminToken();
        HttpHeaders headers = buildJsonHeaders(adminToken);
        try {
            return (String) resolveRoleMappingById(roleId, headers).get("name");
        } catch (Exception e) {
            log.error("Error obteniendo el nombre del rol {}: {}", roleId, e.getMessage());
            throw KeycloakException.roleNotFound(roleId);
        }
    }

    private Map<String, Object> resolveRoleMappingById(String roleId, HttpHeaders headers) throws Exception {
        String rolesByIdUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles-by-id/" + roleId;

        String roleResponse = restTemplate.exchange(
                rolesByIdUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        ).getBody();

        JsonNode roleNode = objectMapper.readTree(roleResponse);

        Map<String, Object> roleMap = new HashMap<>();
        roleMap.put("id", roleNode.path("id").asText());
        roleMap.put("name", roleNode.path("name").asText());
        return roleMap;
    }

    private void handleKeycloakError(HttpStatusCodeException e, String roleId, String userId) {
        String body = e.getResponseBodyAsString();

        try {
            JsonNode errorNode = objectMapper.readTree(body);
            String error = errorNode.path("error").asText();
            String normalizedError = error == null ? "" : error.toLowerCase();

            if (normalizedError.contains("role")) {
                throw KeycloakException.roleNotFound(roleId);
            }

            if (normalizedError.contains("user")) {
                throw KeycloakException.userNotFound(userId);
            }

            throw KeycloakException.assignmentError(error);

        } catch (KeycloakException ex) {
            throw ex;
        } catch (Exception parseEx) {
            throw KeycloakException.generic(body);
        }
    }

    private boolean isInternalRole(String name) {
        return name.startsWith("default-roles-")
                || name.equals("offline_access")
                || name.equals("uma_authorization");
    }

    private UserException buildRegisterConflictException(HttpStatusCodeException e, String username, String email) {
        String body = e.getResponseBodyAsString();

        try {
            JsonNode errorNode = objectMapper.readTree(body);
            String rawMessage = errorNode.path("errorMessage").asText(
                    errorNode.path("message").asText(errorNode.path("error").asText(""))
            );
            String message = rawMessage.toLowerCase();

            boolean mentionsUsername = message.contains("username") || message.contains(username.toLowerCase());
            boolean mentionsEmail = message.contains("email") || message.contains(email.toLowerCase());

            if (mentionsUsername && !mentionsEmail) {
                return UserException.userAlreadyExists(username);
            }

            if (mentionsEmail && !mentionsUsername) {
                return UserException.emailAlreadyExists(email);
            }

            return UserException.userOrEmailAlreadyExists(username, email);
        } catch (Exception parseEx) {
            log.warn("No se pudo parsear conflicto 409 de Keycloak al crear usuario: {}", body);
            return UserException.userOrEmailAlreadyExists(username, email);
        }
    }

    public void setPasswordAndEnable(String userId, String password) {
        String adminToken = getAdminToken();
        HttpHeaders headers = buildJsonHeaders(adminToken);
        setPassword(userId, password, headers);
        enableUser(userId);
        log.info("Contraseña establecida y usuario {} habilitado en Keycloak", userId);
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

    public String findUserIdByEmail(String email) {
        String adminToken = getAdminToken();
        String searchUrl = keycloakServerUrl + "/admin/realms/" + realm
                + "/users?email=" + email + "&exact=true";

        HttpHeaders headers = buildJsonHeaders(adminToken);
        try {
            String response = restTemplate.exchange(
                    searchUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            ).getBody();

            JsonNode users = objectMapper.readTree(response);
            if (!users.isArray() || users.isEmpty()) {
                throw com.sssi.msvc_auth.exception.PasswordResetException.emailNotFound(email);
            }
            return users.get(0).path("id").asText();
        } catch (com.sssi.msvc_auth.exception.PasswordResetException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error buscando usuario por email {}: {}", email, e.getMessage(), e);
            throw new IllegalStateException("Error buscando usuario en Keycloak", e);
        }
    }

    public void resetPassword(String userId, String newPassword) {
        String adminToken = getAdminToken();
        HttpHeaders headers = buildJsonHeaders(adminToken);
        setPassword(userId, newPassword, headers);
        log.info("Contraseña restablecida en Keycloak para userId={}", userId);
    }

    public List<KeycloakUserResponseDto> getUsersByIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        String adminToken = getAdminToken();
        HttpHeaders headers = buildJsonHeaders(adminToken);
        List<KeycloakUserResponseDto> users = new ArrayList<>();

        for (String userId : userIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList()) {
            String url = keycloakServerUrl + "/admin/realms/" + realm + "/users/" + userId;

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class
                );

                JsonNode node = objectMapper.readTree(response.getBody());

                users.add(toUserResponse(node));

            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode().value() == 404) {
                    continue;
                }

                throw e;
            } catch (Exception e) {
                log.error("Error obteniendo usuario {}: {}", userId, e.getMessage(), e);
                throw new IllegalStateException("Error consultando usuarios en Keycloak", e);
            }
        }

        return users;
    }

    private KeycloakUserResponseDto toUserResponse(JsonNode node) {
        String profileImageObjectName = extractProfileImageObjectName(node);

        return KeycloakUserResponseDto.builder()
                .id(node.path("id").asText())
                .username(node.path("username").asText())
                .email(node.path("email").asText(null))
                .firstName(node.path("firstName").asText(null))
                .lastName(node.path("lastName").asText(null))
                .profileImageObjectName(profileImageObjectName)
                .profileImageUrl(toArchiveImageUrl(profileImageObjectName))
                .build();
    }

    private String extractProfileImageObjectName(JsonNode node) {
        JsonNode attributeNode = node.path("attributes").path(PROFILE_IMAGE_OBJECT_NAME_ATTRIBUTE);

        if (attributeNode.isArray() && attributeNode.size() > 0) {
            return normalizeObjectName(attributeNode.get(0).asText(null));
        }

        if (attributeNode.isTextual()) {
            return normalizeObjectName(attributeNode.asText());
        }

        return null;
    }

    private String toArchiveImageUrl(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return null;
        }
        return ARCHIVE_FILE_ROUTE_PREFIX + objectName;
    }

    private String normalizeObjectName(String objectName) {
        if (objectName == null) {
            return null;
        }
        String trimmed = objectName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> buildUserPayload(String userId, JsonNode userNode) {
        Map<String, Object> userPayload = new HashMap<>();
        String username = userNode.path("username").asText("");

        if (username.isBlank()) {
            throw new IllegalStateException("El usuario de Keycloak no contiene username");
        }

        userPayload.put("id", userId);
        userPayload.put("username", username);

        if (userNode.has("enabled")) {
            userPayload.put("enabled", userNode.path("enabled").asBoolean(false));
        }
        if (userNode.hasNonNull("firstName")) {
            userPayload.put("firstName", userNode.get("firstName").asText());
        }
        if (userNode.hasNonNull("lastName")) {
            userPayload.put("lastName", userNode.get("lastName").asText());
        }
        if (userNode.hasNonNull("email")) {
            userPayload.put("email", userNode.get("email").asText());
        }
        if (userNode.has("emailVerified")) {
            userPayload.put("emailVerified", userNode.path("emailVerified").asBoolean(false));
        }
        if (userNode.has("requiredActions")) {
            userPayload.put("requiredActions", objectMapper.convertValue(userNode.get("requiredActions"), List.class));
        }

        return userPayload;
    }

    private Map<String, List<String>> extractAttributes(JsonNode userNode) {
        if (!userNode.has("attributes")) {
            return new HashMap<>();
        }
        return new HashMap<>(objectMapper.convertValue(userNode.get("attributes"), Map.class));
    }
}