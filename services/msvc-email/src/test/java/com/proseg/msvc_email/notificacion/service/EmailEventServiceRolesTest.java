package com.proseg.msvc_email.notificacion.service;

import com.proseg.common.kafka.events.UserRoleAssignedEvent;
import com.proseg.common.kafka.events.UserStatusChangedEvent;
import com.proseg.msvc_email.notificacion.client.AuthClient;
import com.proseg.msvc_email.notificacion.client.MaintenanceClient;
import com.proseg.msvc_email.notificacion.model.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.context.Context;

import java.util.List;

import static com.proseg.msvc_email.notificacion.service.EmailEventServiceTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailEventServiceRolesTest {

    @Mock private EmailService emailService;
    @Mock private AuthClient authClient;
    @Mock private MaintenanceClient maintenanceClient;

    private EmailEventService emailEventService;

    @BeforeEach
    void setUp() {
        emailEventService = newService(emailService, authClient, maintenanceClient);
    }

    @Test
    @DisplayName("sendUserRoleAssignedEmail: envía al usuario y notifica admins válidos")
    void sendUserRoleAssigned_cuandoAdminsValidos_enviaUsuarioYAdmin() {
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR")).thenReturn(apiResponse(List.of(
                keycloakUser("service-account-x", "sa@test.com", "SA", "Bot"),
                keycloakUser("super", "admin@test.com", "Super", "Admin")
        )));

        emailEventService.sendUserRoleAssignedEmail(roleAssignedEvent());

        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailService, times(2)).sendEmail(captor.capture());
        Email userMail = captor.getAllValues().get(0);
        Email adminMail = captor.getAllValues().get(1);
        assertThat(userMail.getTo()).containsExactly("user@test.com");
        assertThat(userMail.getSubject()).isEqualTo("Se te ha asignado un nuevo rol en " + BRAND);
        assertThat(userMail.getTemplateDefinition().getTemplateName()).isEqualTo("user-role-assigned-email");
        Context userCtx = userMail.getTemplateDefinition().buildContext();
        assertThat(userCtx.getVariable("roleName")).isEqualTo("TECNICO");

        assertThat(adminMail.getTo()).containsExactly("admin@test.com");
        assertThat(adminMail.getSubject()).isEqualTo("Alerta: Cambio de rol realizado - " + BRAND);
        assertThat(adminMail.getTemplateDefinition().getTemplateName())
                .isEqualTo("user-role-assigned-admin-notification-email");
    }

    @Test
    @DisplayName("sendUserRoleAssignedEmail: sin admins solo envía al usuario")
    void sendUserRoleAssigned_cuandoSinAdmins_soloUsuario() {
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR")).thenReturn(apiResponse(null));

        emailEventService.sendUserRoleAssignedEmail(roleAssignedEvent());

        verify(emailService, times(1)).sendEmail(any());
        assertThat(captureEmail(emailService).getTo()).containsExactly("user@test.com");
    }

    @Test
    @DisplayName("sendUserRoleAssignedEmail: fallo resolviendo admins no impide correo al usuario")
    void sendUserRoleAssigned_cuandoAuthClientFallaEnAdmins_usuarioRecibeCorreo() {
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR"))
                .thenThrow(new RuntimeException("auth caído"));

        emailEventService.sendUserRoleAssignedEmail(roleAssignedEvent());

        verify(emailService, times(1)).sendEmail(any());
        assertThat(captureEmail(emailService).getTo()).containsExactly("user@test.com");
    }

    @Test
    @DisplayName("sendUserStatusChangedEmail: envía al usuario y alerta a admins")
    void sendUserStatusChanged_cuandoAdminsValidos_enviaUsuarioYAdmin() {
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR")).thenReturn(apiResponse(List.of(
                keycloakUser("super", "status-admin@test.com", "Super", "Admin")
        )));

        emailEventService.sendUserStatusChangedEmail(statusChangedEvent());

        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailService, times(2)).sendEmail(captor.capture());
        Email userMail = captor.getAllValues().get(0);
        Email adminMail = captor.getAllValues().get(1);
        assertThat(userMail.getTo()).containsExactly("user@test.com");
        assertThat(userMail.getSubject()).isEqualTo("Tu estado ha sido actualizado en " + BRAND);
        Context ctx = userMail.getTemplateDefinition().buildContext();
        assertThat(ctx.getVariable("newStatus")).isEqualTo("ACTIVO");

        assertThat(adminMail.getTo()).containsExactly("status-admin@test.com");
        assertThat(adminMail.getSubject()).isEqualTo("Alerta: Cambio de estado de usuario - " + BRAND);
        assertThat(adminMail.getTemplateDefinition().getTemplateName())
                .isEqualTo("user-status-changed-admin-notification-email");
    }

    @Test
    @DisplayName("sendUserStatusChangedEmail: sin admins solo envía al usuario")
    void sendUserStatusChanged_cuandoSinAdmins_soloUsuario() {
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR")).thenReturn(apiResponse(List.of()));

        emailEventService.sendUserStatusChangedEmail(statusChangedEvent());

        verify(emailService, times(1)).sendEmail(any());
    }

    @Test
    @DisplayName("sendUserStatusChangedEmail: fallo resolviendo admins no impide correo al usuario")
    void sendUserStatusChanged_cuandoAuthClientFallaEnAdmins_usuarioRecibeCorreo() {
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR"))
                .thenThrow(new RuntimeException("auth caído"));

        emailEventService.sendUserStatusChangedEmail(statusChangedEvent());

        verify(emailService, times(1)).sendEmail(any());
        assertThat(captureEmail(emailService).getTo()).containsExactly("user@test.com");
    }

    private static UserRoleAssignedEvent roleAssignedEvent() {
        return UserRoleAssignedEvent.builder()
                .userId("u-1").username("user1").email("user@test.com")
                .firstName("Us").lastName("Er").roleName("TECNICO")
                .assignedByUsername("admin").assignedByEmail("admin@test.com")
                .assignedByFirstName("Ad").assignedByLastName("Min")
                .timestamp(TS).build();
    }

    private static UserStatusChangedEvent statusChangedEvent() {
        return UserStatusChangedEvent.builder()
                .userId("u-1").username("user1").email("user@test.com")
                .firstName("Us").lastName("Er")
                .oldStatus("PENDIENTE").newStatus("ACTIVO")
                .changedByUsername("admin").changedByEmail("admin@test.com")
                .changedByFirstName("Ad").changedByLastName("Min")
                .timestamp(TS).build();
    }
}
