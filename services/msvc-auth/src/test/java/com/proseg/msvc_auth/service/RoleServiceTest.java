package com.proseg.msvc_auth.service;

import com.proseg.common.api.response.PagedResponse;
import com.proseg.msvc_auth.dto.KeycloakUserResponseDto;
import com.proseg.msvc_auth.dto.RoleResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.mockito.Mockito.*;

class RoleServiceTest {
    @Test
    void delegatesRoleQueriesAndMutationsToKeycloakAdmin() {
        KeycloakAdminService admin = mock(KeycloakAdminService.class);
        RoleService service = new RoleService(admin);
        when(admin.getBaseRoles()).thenReturn(List.of());
        when(admin.getCompositeRoles(PageRequest.of(0, 5))).thenReturn(PagedResponse.<RoleResponseDto>builder().build());
        when(admin.getRoleComposites("MANAGER")).thenReturn(List.of());
        when(admin.getUsersByRole("MANAGER")).thenReturn(List.of(KeycloakUserResponseDto.builder().build()));

        service.getBaseRoles();
        service.getCompositeRoles(PageRequest.of(0, 5));
        service.getRoleComposites("MANAGER");
        service.getUsersByRole("MANAGER");
        service.createCompositeRole("MANAGER", "Managers", List.of("READ"));
        service.updateRole("MANAGER", "MANAGER_V2", "Managers v2", List.of("READ"));
        service.deleteRole("MANAGER");

        verify(admin).getBaseRoles();
        verify(admin).getCompositeRoles(PageRequest.of(0, 5));
        verify(admin).getRoleComposites("MANAGER");
        verify(admin).getUsersByRole("MANAGER");
        verify(admin).createCompositeRole("MANAGER", "Managers", List.of("READ"));
        verify(admin).updateRole("MANAGER", "MANAGER_V2", "Managers v2", List.of("READ"));
        verify(admin).deleteRole("MANAGER");
    }
}
