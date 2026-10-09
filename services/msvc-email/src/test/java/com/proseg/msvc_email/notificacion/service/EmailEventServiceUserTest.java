package com.proseg.msvc_email.notificacion.service;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.kafka.events.ManagedUserCreatedEvent;
import com.proseg.common.kafka.events.UserInvitedEvent;
import com.proseg.msvc_email.notificacion.client.AuthClient;
import com.proseg.msvc_email.notificacion.client.MaintenanceClient;
import com.proseg.msvc_email.notificacion.dto.KeycloakUserResponseDto;
import com.proseg.msvc_email.notificacion.model.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailEventServiceUserTest {

    private static final String SET_PASSWORD_URL = "https://app.test/set-password";
    private static final String BRAND = "MARCA_TEST";
    private static final long TS = 1_700_000_000_000L;

    @Mock private EmailService emailService;
    @Mock private AuthClient authClient;
    @Mock private MaintenanceClient maintenanceClient;

    private EmailEventService emailEventService;

    @BeforeEach
    void setUp() {
        emailEventService = new EmailEventService(emailService, authClient, maintenanceClient);
        ReflectionTestUtils.setField(emailEventService, "loginUrl", "https://app.test/login");
        ReflectionTestUtils.setField(emailEventService, "approvalsBaseUrl", "https://app.test/approvals");
        ReflectionTestUtils.setField(emailEventService, "setPasswordBaseUrl", SET_PASSWORD_URL);
        ReflectionTestUtils.setField(emailEventService, "resetPasswordBaseUrl", "https://app.test/reset-password");
        ReflectionTestUtils.setField(emailEventService, "brandName", BRAND);
    }

    @Test
    @DisplayName("sendInvitationEmail: URL de contraseña incluye token en query")
    void sendInvitationEmail_cuandoInvitacionValida_urlConToken() {
        UserInvitedEvent event = UserInvitedEvent.builder()
                .firstName("Lu").lastName("Ma").username("luma").email("lu@test.com")
                .invitationToken("tok-abc").timestamp(TS).build();

        emailEventService.sendInvitationEmail(event);

        Email sent = captureSingleEmail();
        assertThat(sent.getTo()).containsExactly("lu@test.com");
        assertThat(sent.getSubject()).contains(BRAND);
        Context ctx = sent.getTemplateDefinition().buildContext();
        assertThat(ctx.getVariable("setPasswordUrl")).isEqualTo(SET_PASSWORD_URL + "?token=tok-abc");
    }

    @Test
    @DisplayName("sendManagedUserCreatedNotificationEmails: sin admins no envía")
    void sendManagedUserCreated_cuandoSinAdmins_noEnvia() {
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR"))
                .thenReturn(new ApiResponse<>("ok", null, 200));

        emailEventService.sendManagedUserCreatedNotificationEmails(managedUserEvent());

        verify(emailService, never()).sendEmail(any());
    }

    @Test
    @DisplayName("sendManagedUserCreatedNotificationEmails: notifica admins válidos")
    void sendManagedUserCreated_cuandoAdminsValidos_enviaPlantillaAdmin() {
        KeycloakUserResponseDto admin = new KeycloakUserResponseDto();
        admin.setUsername("adm");
        admin.setEmail("super@test.com");
        admin.setFirstName("Super");
        admin.setLastName("Admin");
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR"))
                .thenReturn(new ApiResponse<>("ok", List.of(admin), 200));

        emailEventService.sendManagedUserCreatedNotificationEmails(managedUserEvent());

        Email sent = captureSingleEmail();
        assertThat(sent.getTo()).containsExactly("super@test.com");
        assertThat(sent.getSubject()).isEqualTo("Nuevo usuario creado en " + BRAND);
        assertThat(sent.getTemplateDefinition().getTemplateName())
                .isEqualTo("managed-user-created-admin-notification-email");
        Context ctx = sent.getTemplateDefinition().buildContext();
        assertThat(ctx.getVariable("newUsername")).isEqualTo("nuevo.user");
        assertThat(ctx.getVariable("adminEmail")).isEqualTo("creator@test.com");
    }

    private Email captureSingleEmail() {
        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailService).sendEmail(captor.capture());
        return captor.getValue();
    }

    private static ManagedUserCreatedEvent managedUserEvent() {
        return ManagedUserCreatedEvent.builder()
                .username("nuevo.user").email("nuevo@test.com")
                .firstName("Nuevo").lastName("User")
                .createdByUsername("creator").createdByEmail("creator@test.com")
                .createdByFirstName("Cre").createdByLastName("Ator")
                .timestamp(TS).build();
    }
}
