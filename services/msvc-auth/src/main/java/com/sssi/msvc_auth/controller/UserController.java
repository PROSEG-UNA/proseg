package com.sssi.msvc_auth.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PagedResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.msvc_auth.dto.*;
import com.sssi.msvc_auth.entity.User;
import com.sssi.msvc_auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${routes.user:/api/user}")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<KeycloakUserResponseDto>>> getAllUsers(
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        log.info("Obteniendo usuarios paginados - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        PagedResponse<KeycloakUserResponseDto> response = userService.getAllUsers(pageable);
        return ApiResponseBuilder.ok(response, "Usuarios obtenidos correctamente");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateManagedUserResponseDto>> createUser(
            @Valid @RequestBody CreateManagedUserRequestDto request,
            Authentication authentication
    ) {
        String currentUserId = authentication.getName();
        log.info(
                "Usuario [{}] creando usuario administrado: {}",
                currentUserId,
                request.getUsername()
        );
        CreateManagedUserResponseDto response = userService.createManagedUser(request, currentUserId);
        return ApiResponseBuilder.created(response, "Usuario creado. Se envio credencial temporal por email");
    }

    @GetMapping("/keycloak/{id}")
    public ResponseEntity<ApiResponse<KeycloakUserResponseDto>> getUserById(@PathVariable String id) {
        KeycloakUserResponseDto user = userService.getKeycloakUserById(id);
        return ApiResponseBuilder.ok(user, "Usuario obtenido correctamente");
    }

    @GetMapping("/{userId}/roles")
    public ResponseEntity<ApiResponse<List<RoleResponseDto>>> getRolesByUserId(@PathVariable String userId) {
        log.info("Obteniendo roles del usuario {}", userId);
        List<RoleResponseDto> roles = userService.getRolesByUserId(userId);
        return ApiResponseBuilder.ok(roles, "Roles del usuario obtenidos correctamente");
    }

    @GetMapping("/statuses")
    public ResponseEntity<ApiResponse<List<String>>> getUserStatuses() {
        List<String> statuses = userService.getAvailableStatuses();
        return ApiResponseBuilder.ok(statuses, "Estados de usuario obtenidos correctamente");
    }

    @PatchMapping("/approval/{id}")
    public ResponseEntity<ApiResponse<String>> updateUserApproval(
            @PathVariable UUID id,
            @Valid @RequestBody UserApprovalRequestDto request
    ) {
        User updatedUser = userService.updateUserApproval(id, request.getStatus());

        return ApiResponseBuilder.ok(
                updatedUser.getStatus().name(),
                "Estado del usuario actualizado"
        );
    }

    @PutMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<ApiResponse<Void>> assignRoleToUser(
            @PathVariable String userId,
            @PathVariable String roleId
    ) {
        log.info("Asignando rol unico roleId {} al usuario {}", roleId, userId);
        userService.assignRoleToUser(userId, roleId);
        return ApiResponseBuilder.ok(null, "Rol unico asignado correctamente");
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<ApiResponse<Void>> removeRoleFromUser(
            @PathVariable String userId,
            @PathVariable String roleId
    ) {
        log.info("Quitando roleId {} del usuario {}", roleId, userId);
        userService.removeRoleFromUser(userId, roleId);
        return ApiResponseBuilder.ok(null, "Rol removido correctamente");
    }

    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<Void>> setPassword(
            @Valid @RequestBody SetPasswordRequestDto request
    ) {
        userService.activateUserWithToken(request.getToken(), request.getPassword());
        return ApiResponseBuilder.ok(null, "Cuenta activada correctamente");
    }

    @PostMapping("/{userId}/resend-invitation")
    public ResponseEntity<ApiResponse<Void>> resendInvitation(@PathVariable String userId) {
        userService.resendInvitation(userId);
        return ApiResponseBuilder.ok(null, "Invitación reenviada");
    }

    @GetMapping("/invitation-info")
    public ResponseEntity<ApiResponse<InvitationInfoResponseDto>> getInvitationInfo(
            @RequestParam String token
    ) {
        InvitationInfoResponseDto response = userService.getInvitationInfo(token);
        return ApiResponseBuilder.ok(
                response,
                "Información de invitación obtenida correctamente"
        );
    }
}

