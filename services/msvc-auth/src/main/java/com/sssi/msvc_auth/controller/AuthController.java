package com.sssi.msvc_auth.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.kafka.events.UserLoginEvent;
import com.sssi.common.kafka.topics.KafkaTopics;
import com.sssi.msvc_auth.dto.CreateRoleRequestDtoDto;
import com.sssi.msvc_auth.dto.LoginRequestDto;
import com.sssi.msvc_auth.dto.LoginResponseDto;
import com.sssi.msvc_auth.dto.RegisterRequestDto;
import com.sssi.msvc_auth.dto.RegisterResponseDto;
import com.sssi.msvc_auth.dto.RoleResponseDto;
import com.sssi.msvc_auth.dto.UserApprovalRequestDto;
import com.sssi.msvc_auth.entity.User;
import com.sssi.msvc_auth.service.KeycloakAdminService;
import com.sssi.msvc_auth.service.KeycloakAuthService;
import com.sssi.msvc_auth.service.UserApprobationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${routes.auth}")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final KeycloakAuthService keycloakAuthService;
    private final KeycloakAdminService keycloakAdminService;
    private final UserApprobationService userApprobationService;
    private final KafkaTemplate<String, UserLoginEvent> kafkaTemplate;

    @GetMapping
    public String health() {
        return "Auth Service is running";
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("Login attempt for user: {}", request.getIdentifier());

        String token = keycloakAuthService.getToken(
                request.getIdentifier(),
                request.getPassword()
        );

        try {
            kafkaTemplate.send(
                    KafkaTopics.USER_LOGIN_TOPIC,
                    UserLoginEvent.builder()
                            .email(request.getIdentifier())
                            .timestamp(Instant.now().toEpochMilli())
                            .build()
            );
        } catch (Exception e) {
            log.warn("No se pudo enviar evento de login a Kafka: {}", e.getMessage());
        }

        log.info("Login exitoso para usuario: {}", request.getIdentifier());

        return ApiResponseBuilder.ok(
                LoginResponseDto.builder()
                        .token(token)
                        .identifier(request.getIdentifier())
                        .build(),
                "Login exitoso"
        );
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponseDto>> register(@Valid @RequestBody RegisterRequestDto request) {
        log.info("Register attempt for user: {}", request.getUsername());

        String keycloakUserId = keycloakAdminService.registerUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName()
        );

        userApprobationService.createPendingUser(keycloakUserId);

        String token = keycloakAuthService.getToken(
                request.getUsername(),
                request.getPassword()
        );

        log.info("Registro exitoso para usuario: {}", request.getUsername());

        return ApiResponseBuilder.created(
                RegisterResponseDto.builder()
                        .token(token)
                        .username(request.getUsername())
                        .email(request.getEmail())
                        .build(),
                "Registro exitoso"
        );
    }

    @GetMapping("/roles/base")
    public ResponseEntity<ApiResponse<List<RoleResponseDto>>> getBaseRoles() {
        log.info("Obteniendo roles base de Keycloak");
        List<RoleResponseDto> roles = keycloakAdminService.getBaseRoles();
        return ApiResponseBuilder.ok(roles, "Roles base obtenidos exitosamente");
    }

    @GetMapping("/roles/composite")
    public ResponseEntity<ApiResponse<List<RoleResponseDto>>> getCompositeRoles() {
        log.info("Obteniendo roles compuestos de Keycloak");
        List<RoleResponseDto> roles = keycloakAdminService.getCompositeRoles();
        return ApiResponseBuilder.ok(roles, "Roles compuestos obtenidos exitosamente");
    }

    @GetMapping("/roles/{roleName}/composites")
    public ResponseEntity<ApiResponse<List<RoleResponseDto>>> getRoleComposites(@PathVariable String roleName) {
        log.info("Obteniendo composites del rol: {}", roleName);
        List<RoleResponseDto> roles = keycloakAdminService.getRoleComposites(roleName);
        return ApiResponseBuilder.ok(roles, "Composites del rol obtenidos exitosamente");
    }

    @PostMapping("/roles")
    public ResponseEntity<ApiResponse<Void>> createRole(@Valid @RequestBody CreateRoleRequestDto request) {
        log.info("Creando rol: {}", request.getRoleName());
        keycloakAdminService.createCompositeRole(request.getRoleName(), request.getPrivileges());
        return ApiResponseBuilder.created(null, "Rol " + request.getRoleName() + " creado exitosamente");
    }

    @PutMapping("/roles/{roleName}")
    public ResponseEntity<ApiResponse<Void>> updateRole(@PathVariable String roleName,
                                                        @Valid @RequestBody CreateRoleRequestDto request) {
        log.info("Actualizando rol: {}", roleName);
        keycloakAdminService.updateRole(roleName, request.getPrivileges());
        return ApiResponseBuilder.noContent("Rol " + roleName + " actualizado exitosamente");
    }

    @DeleteMapping("/roles/{roleName}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable String roleName) {
        log.info("Eliminando rol: {}", roleName);
        keycloakAdminService.deleteRole(roleName);
        return ApiResponseBuilder.noContent("Rol " + roleName + " eliminado exitosamente");
    }

    @PatchMapping("/users/{id}/approval")
    public ResponseEntity<ApiResponse<String>> updateUserApproval(
            @PathVariable UUID id,
            @Valid @RequestBody UserApprovalRequestDto request
    ) {
        User updatedUser = userApprobationService.updateStatus(id, request.getStatus());

        return ApiResponseBuilder.ok(
                updatedUser.getStatus().name(),
                "Estado del usuario actualizado"
        );
    }
}