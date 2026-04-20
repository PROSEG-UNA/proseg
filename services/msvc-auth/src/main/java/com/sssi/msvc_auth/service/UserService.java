package com.sssi.msvc_auth.service;

import com.sssi.common.api.response.PagedResponse;
import com.sssi.msvc_auth.dto.KeycloakUserResponseDto;
import com.sssi.msvc_auth.dto.RoleResponseDto;
import com.sssi.msvc_auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final KeycloakAdminService keycloakAdminService;
    private final UserApprobationService userApprobationService;

    @Transactional(readOnly = true)
    public PagedResponse<KeycloakUserResponseDto> getAllUsers(Pageable pageable) {
    return keycloakAdminService.getAllUsers(pageable);
    }

    @Transactional(readOnly = true)
    public KeycloakUserResponseDto getKeycloakUserById(String id) {
        return keycloakAdminService.getUserById(id);
    }

    @Transactional
    public User updateUserApproval(UUID id, User.UserStatus status) {
        User updatedUser = userApprobationService.updateStatus(id, status);

        if (updatedUser.getStatus() == User.UserStatus.APPROVED) {
            keycloakAdminService.enableUser(updatedUser.getKeycloakUserId());
        } else {
            keycloakAdminService.disableUser(updatedUser.getKeycloakUserId());
        }

        log.info("Estado de aprobacion actualizado para userId {} -> {}", id, updatedUser.getStatus());
        return updatedUser;
    }

    public void assignRoleToUser(String userId, String roleId) {
        keycloakAdminService.assignRoleToUser(userId, roleId);
    }

    public void removeRoleFromUser(String userId, String roleId) {
        keycloakAdminService.removeRoleFromUser(userId, roleId);
    }

    @Transactional(readOnly = true)
    public List<RoleResponseDto> getRolesByUserId(String userId) {
        return keycloakAdminService.getRolesByUserId(userId);
    }
}

