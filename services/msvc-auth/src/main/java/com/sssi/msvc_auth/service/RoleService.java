package com.sssi.msvc_auth.service;

import com.sssi.msvc_auth.dto.KeycloakUserResponseDto;
import com.sssi.msvc_auth.dto.RoleResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final KeycloakAdminService keycloakAdminService;

    @Transactional(readOnly = true)
    public List<RoleResponseDto> getBaseRoles() {
        return keycloakAdminService.getBaseRoles();
    }

    @Transactional(readOnly = true)
    public List<RoleResponseDto> getCompositeRoles() {
        return keycloakAdminService.getCompositeRoles();
    }

    @Transactional(readOnly = true)
    public List<RoleResponseDto> getRoleComposites(String roleName) {
        return keycloakAdminService.getRoleComposites(roleName);
    }

    public void createCompositeRole(String roleName, List<String> privileges) {
        keycloakAdminService.createCompositeRole(roleName, privileges);
    }

    public void updateRole(String roleName, List<String> privileges) {
        keycloakAdminService.updateRole(roleName, privileges);
    }

    public void deleteRole(String roleName) {
        keycloakAdminService.deleteRole(roleName);
    }

    @Transactional(readOnly = true)
    public List<KeycloakUserResponseDto> getUsersByRole(String roleName) {
        return keycloakAdminService.getUsersByRole(roleName);
    }
}

