package com.proseg.msvc_auth.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PagedResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.msvc_auth.dto.CreateRoleRequestDto;
import com.proseg.msvc_auth.dto.KeycloakUserResponseDto;
import com.proseg.msvc_auth.dto.RoleResponseDto;
import com.proseg.msvc_auth.dto.UpdateRoleRequestDto;
import com.proseg.msvc_auth.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${routes.role}")
@RequiredArgsConstructor
@Slf4j
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/base")
    public ResponseEntity<ApiResponse<List<RoleResponseDto>>> getBaseRoles() {
        log.info("Obteniendo roles base de Keycloak");
        List<RoleResponseDto> roles = roleService.getBaseRoles();
        return ApiResponseBuilder.ok(roles, "Roles base obtenidos exitosamente");
    }

    @GetMapping("/composite")
    public ResponseEntity<ApiResponse<PagedResponse<RoleResponseDto>>> getCompositeRoles(
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        log.info("Obteniendo roles compuestos de Keycloak");
        PagedResponse<RoleResponseDto> roles = roleService.getCompositeRoles(pageable);
        return ApiResponseBuilder.ok(roles, "Roles compuestos obtenidos exitosamente");
    }

    @GetMapping("/{roleName}/composites")
    public ResponseEntity<ApiResponse<List<RoleResponseDto>>> getRoleComposites(@PathVariable String roleName) {
        log.info("Obteniendo composites del rol: {}", roleName);
        List<RoleResponseDto> roles = roleService.getRoleComposites(roleName);
        return ApiResponseBuilder.ok(roles, "Composites del rol obtenidos exitosamente");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createRole(@Valid @RequestBody CreateRoleRequestDto request) {
        log.info("Creando rol: {}", request.getRoleName());
        roleService.createCompositeRole(request.getRoleName(), request.getDescription(), request.getPrivileges());
        return ApiResponseBuilder.created(null, "Rol " + request.getRoleName() + " creado exitosamente");
    }

    @PutMapping("/{roleName}")
    public ResponseEntity<ApiResponse<Void>> updateRole(
            @PathVariable String roleName,
            @RequestBody UpdateRoleRequestDto request
    ) {
        log.info("Actualizando rol: {}", roleName);
        roleService.updateRole(roleName, request.getRoleName(), request.getDescription(), request.getPrivileges());
        return ApiResponseBuilder.noContent("Rol " + roleName + " actualizado exitosamente");
    }

    @DeleteMapping("/{roleName}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable String roleName) {
        log.info("Eliminando rol: {}", roleName);
        roleService.deleteRole(roleName);
        return ApiResponseBuilder.noContent("Rol " + roleName + " eliminado exitosamente");
    }

    @GetMapping("/{roleName}/users")
    public ResponseEntity<ApiResponse<List<KeycloakUserResponseDto>>> getUsersByRole(@PathVariable String roleName) {
        log.info("Obteniendo usuarios con rol: {}", roleName);
        List<KeycloakUserResponseDto> users = roleService.getUsersByRole(roleName);
        return ApiResponseBuilder.ok(users, "Usuarios obtenidos correctamente");
    }
}

