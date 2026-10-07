package com.proseg.msvc_email.notificacion.service;

import com.proseg.common.kafka.events.*;
import com.proseg.msvc_email.notificacion.client.AuthClient;
import com.proseg.msvc_email.notificacion.client.MaintenanceClient;
import com.proseg.msvc_email.notificacion.dto.CompanyResponseDto;
import com.proseg.msvc_email.notificacion.model.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.context.Context;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.proseg.msvc_email.notificacion.service.EmailEventServiceTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailEventServiceMaintenanceTest {

    @Mock private EmailService emailService;
    @Mock private AuthClient authClient;
    @Mock private MaintenanceClient maintenanceClient;

    private EmailEventService emailEventService;
    private final UUID companyId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        emailEventService = newService(emailService, authClient, maintenanceClient);
    }

    @Test
    @DisplayName("sendCompanyUsersAssignedEmails: asignación feliz con datos de empresa")
    void sendCompanyUsersAssigned_cuandoEmpresaYUsuarioValidos_enviaAccesoEmpresarial() {
        CompanyResponseDto company = new CompanyResponseDto();
        company.setName("Acme SA");
        company.setLegalId("3101");
        when(maintenanceClient.getCompanyById(companyId)).thenReturn(apiResponse(company));
        when(authClient.getUserById("kc-1")).thenReturn(apiResponse(
                keycloakUser("user1", "user@test.com", "Us", "Er")));

        emailEventService.sendCompanyUsersAssignedEmails(CompanyUsersAssignedEvent.builder()
                .companyId(companyId).keycloakUserIds(List.of("kc-1")).timestamp(TS).build());

        Email sent = captureEmail(emailService);
        assertThat(sent.getTo()).containsExactly("user@test.com");
        assertThat(sent.getSubject()).isEqualTo("Acceso empresarial habilitado - " + BRAND);
        assertThat(sent.getTemplateDefinition().getTemplateName()).isEqualTo("company-users-assigned-email");
        Context ctx = sent.getTemplateDefinition().buildContext();
        assertThat(ctx.getVariable("companyName")).isEqualTo("Acme SA");
        assertThat(ctx.getVariable("loginUrl")).isEqualTo(LOGIN_URL);
    }

    @Test
    @DisplayName("sendCompanyUsersAssignedEmails: sin usuarios no consulta mantenimiento")
    void sendCompanyUsersAssigned_cuandoIdsVacios_noEnvia() {
        emailEventService.sendCompanyUsersAssignedEmails(CompanyUsersAssignedEvent.builder()
                .companyId(companyId).keycloakUserIds(List.of()).timestamp(TS).build());

        verify(maintenanceClient, never()).getCompanyById(any());
        verify(emailService, never()).sendEmail(any());
    }

    @Test
    @DisplayName("sendCompanyUsersAssignedEmails: fallo de maintenanceClient aborta sin envío")
    void sendCompanyUsersAssigned_cuandoMaintenanceClientFalla_noEnvia() {
        when(maintenanceClient.getCompanyById(companyId)).thenThrow(new RuntimeException("timeout"));

        emailEventService.sendCompanyUsersAssignedEmails(CompanyUsersAssignedEvent.builder()
                .companyId(companyId).keycloakUserIds(List.of("kc-1")).timestamp(TS).build());

        verify(emailService, never()).sendEmail(any());
    }

    @Test
    @DisplayName("sendCompanyUsersAssignedEmails: empresa null en data captura error por usuario (sin envío)")
    void sendCompanyUsersAssigned_cuandoEmpresaDataNull_noEnviaPorUsuario() {
        when(maintenanceClient.getCompanyById(companyId)).thenReturn(apiResponse(null));
        when(authClient.getUserById("kc-1")).thenReturn(apiResponse(
                keycloakUser("user1", "user@test.com", "Us", "Er")));

        emailEventService.sendCompanyUsersAssignedEmails(CompanyUsersAssignedEvent.builder()
                .companyId(companyId).keycloakUserIds(List.of("kc-1")).timestamp(TS).build());

        verify(emailService, never()).sendEmail(any());
    }

    @Test
    @DisplayName("sendMaintenanceRequestCreatedEmail: limpia, deduplica to/cc/bcc")
    void sendMaintenanceRequestCreated_cuandoCorreosMixtos_normalizaDestinatarios() {
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR"))
                .thenReturn(apiResponse(superAdminsIncludingNoise()));

        emailEventService.sendMaintenanceRequestCreatedEmail(MaintenanceRequestCreatedEvent.builder()
                .companyName("Acme").status("OPEN").timestamp(TS)
                .emails(Arrays.asList("  to@test.com ", "to@test.com", null, ""))
                .extraEmails(List.of("to@test.com", " cc@test.com "))
                .build());

        Email sent = captureEmail(emailService);
        assertThat(sent.getTo()).containsExactly("to@test.com");
        assertThat(sent.getCc()).containsExactly("cc@test.com");
        assertThat(sent.getBcc()).containsExactly("bcc-admin@test.com");
        assertThat(sent.getSubject()).isEqualTo("Nueva solicitud de mantenimiento registrada - " + BRAND);
        assertThat(sent.getTemplateDefinition().getTemplateName()).isEqualTo("maintenance-request-created-email");
    }

    @Test
    @DisplayName("sendMaintenanceRequestCreatedEmail: sin destinatarios válidos no envía")
    void sendMaintenanceRequestCreated_cuandoSinDestinatarios_noEnvia() {
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR")).thenReturn(apiResponse(List.of()));

        emailEventService.sendMaintenanceRequestCreatedEmail(MaintenanceRequestCreatedEvent.builder()
                .companyName("Acme").status("OPEN").timestamp(TS)
                .emails(null).extraEmails(Arrays.asList("  ", null)).build());

        verify(emailService, never()).sendEmail(any());
    }

    @Test
    @DisplayName("resolveSuperAdminEmails: authClient lanza excepción pero el to sigue enviándose")
    void sendMaintenanceRequestCreated_cuandoSuperAdminFalla_enviaSoloTo() {
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR"))
                .thenThrow(new RuntimeException("auth down"));

        emailEventService.sendMaintenanceRequestCreatedEmail(MaintenanceRequestCreatedEvent.builder()
                .companyName("Acme").status("OPEN").timestamp(TS)
                .emails(List.of("solo-to@test.com")).build());

        Email sent = captureEmail(emailService);
        assertThat(sent.getTo()).containsExactly("solo-to@test.com");
        assertThat(sent.getBcc()).isEmpty();
    }

    @Test
    @DisplayName("resolveSuperAdminEmails: data null o vacía deja bcc vacío")
    void sendMaintenanceRequestCreated_cuandoSuperAdminDataVacia_sinBcc() {
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR"))
                .thenReturn(apiResponse(null));

        emailEventService.sendMaintenanceRequestCreatedEmail(MaintenanceRequestCreatedEvent.builder()
                .companyName("Acme").status("OPEN").timestamp(TS)
                .emails(List.of("to-only@test.com")).build());

        Email sent = captureEmail(emailService);
        assertThat(sent.getBcc()).isEmpty();
    }

    @ParameterizedTest(name = "actionLabel={0}")
    @MethodSource("maintenanceSubjectCases")
    @DisplayName("sendMaintenanceRequestNotificationEmail: asunto según actionLabel")
    void sendMaintenanceRequestNotification_asuntoSegunActionLabel(String actionLabel, String expectedSubject) {
        stubRecipientsOnlyTo("notify@test.com");

        emailEventService.sendMaintenanceRequestNotificationEmail(
                maintenanceNotificationEvent(actionLabel));

        assertThat(captureEmail(emailService).getSubject()).isEqualTo(expectedSubject);
    }

    @Test
    @DisplayName("sendMaintenanceRequestNotificationEmail: camino feliz con plantilla")
    void sendMaintenanceRequestNotification_cuandoDatosValidos_enviaPlantilla() {
        stubRecipientsOnlyTo("notify@test.com");

        emailEventService.sendMaintenanceRequestNotificationEmail(
                maintenanceNotificationEvent("Aprobada"));

        Email sent = captureEmail(emailService);
        assertThat(sent.getTemplateDefinition().getTemplateName())
                .isEqualTo("maintenance-request-notification-email");
        Context ctx = sent.getTemplateDefinition().buildContext();
        assertThat(ctx.getVariable("actionLabel")).isEqualTo("Aprobada");
    }

    @Test
    @DisplayName("sendMaintenanceRequestNotificationEmail: sin destinatarios no envía")
    void sendMaintenanceRequestNotification_cuandoSinDestinatarios_noEnvia() {
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR")).thenReturn(apiResponse(List.of()));

        emailEventService.sendMaintenanceRequestNotificationEmail(
                MaintenanceRequestNotificationEvent.builder()
                        .requestId(UUID.randomUUID())
                        .actionLabel("X")
                        .companyName("Acme")
                        .status("OPEN")
                        .recipientEmails(null)
                        .timestamp(TS)
                        .build());

        verify(emailService, never()).sendEmail(any());
    }

    @ParameterizedTest(name = "actionLabel={0}")
    @MethodSource("ticketSubjectCases")
    @DisplayName("sendTicketNotificationEmail: asunto según actionLabel")
    void sendTicketNotification_asuntoSegunActionLabel(String actionLabel, String expectedSubject) {
        stubRecipientsOnlyTo("ticket@test.com");

        emailEventService.sendTicketNotificationEmail(ticketEvent(actionLabel));

        assertThat(captureEmail(emailService).getSubject()).isEqualTo(expectedSubject);
    }

    @Test
    @DisplayName("sendTicketNotificationEmail: camino feliz con plantilla")
    void sendTicketNotification_cuandoDatosValidos_enviaPlantilla() {
        stubRecipientsOnlyTo("ticket@test.com");

        emailEventService.sendTicketNotificationEmail(ticketEvent("Asignado"));

        Email sent = captureEmail(emailService);
        assertThat(sent.getTo()).containsExactly("ticket@test.com");
        assertThat(sent.getTemplateDefinition().getTemplateName()).isEqualTo("ticket-notification-email");
    }

    @Test
    @DisplayName("sendTicketNotificationEmail: sin destinatarios no envía")
    void sendTicketNotification_cuandoSinDestinatarios_noEnvia() {
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR")).thenReturn(apiResponse(List.of()));

        emailEventService.sendTicketNotificationEmail(TicketNotificationEvent.builder()
                .ticketId(UUID.randomUUID())
                .actionLabel("X")
                .ticketTitle("T")
                .recipientEmails(List.of("  "))
                .timestamp(TS)
                .build());

        verify(emailService, never()).sendEmail(any());
    }

    private void stubRecipientsOnlyTo(String to) {
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR")).thenReturn(apiResponse(List.of()));
    }

    private static Stream<Arguments> maintenanceSubjectCases() {
        return Stream.of(
                Arguments.of("Aprobada", "Aprobada - Solicitud de mantenimiento - " + BRAND),
                Arguments.of(null, "Actualizacion de solicitud - Solicitud de mantenimiento - " + BRAND),
                Arguments.of("   ", "Actualizacion de solicitud - Solicitud de mantenimiento - " + BRAND)
        );
    }

    private static Stream<Arguments> ticketSubjectCases() {
        return Stream.of(
                Arguments.of("Cerrado", "Cerrado - Ticket - " + BRAND),
                Arguments.of(null, "Actualizacion de ticket - Ticket - " + BRAND),
                Arguments.of("  ", "Actualizacion de ticket - Ticket - " + BRAND)
        );
    }

    private static MaintenanceRequestNotificationEvent maintenanceNotificationEvent(String actionLabel) {
        return MaintenanceRequestNotificationEvent.builder()
                .requestId(UUID.randomUUID())
                .actionLabel(actionLabel)
                .companyName("Acme")
                .status("OPEN")
                .recipientEmails(List.of("notify@test.com"))
                .timestamp(TS)
                .build();
    }

    private static TicketNotificationEvent ticketEvent(String actionLabel) {
        return TicketNotificationEvent.builder()
                .ticketId(UUID.randomUUID())
                .actionLabel(actionLabel)
                .ticketTitle("Falla eléctrica")
                .recipientEmails(List.of("ticket@test.com"))
                .timestamp(TS)
                .build();
    }
}
