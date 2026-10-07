package com.proseg.msvc_email.notificacion.service;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.kafka.events.*;
import com.proseg.msvc_email.notificacion.client.AuthClient;
import com.proseg.msvc_email.notificacion.client.MaintenanceClient;
import com.proseg.msvc_email.notificacion.dto.KeycloakUserResponseDto;
import com.proseg.msvc_email.notificacion.model.Email;
import com.proseg.msvc_email.notificacion.template.impl.*;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailEventServiceTest {

    private static final String LOGIN_URL = "https://app.test/login";
    private static final String APPROVALS_URL = "https://app.test/approvals";
    private static final String BRAND = "MARCA_TEST";
    private static final long TS = 1_700_000_000_000L;

    @Mock
    private EmailService emailService;
    @Mock
    private AuthClient authClient;
    @Mock
    private MaintenanceClient maintenanceClient;

    private EmailEventService emailEventService;

    @BeforeEach
    void setUp() {
        emailEventService = new EmailEventService(emailService, authClient, maintenanceClient);
        ReflectionTestUtils.setField(emailEventService, "loginUrl", LOGIN_URL);
        ReflectionTestUtils.setField(emailEventService, "approvalsBaseUrl", APPROVALS_URL);
        ReflectionTestUtils.setField(emailEventService, "setPasswordBaseUrl", "https://app.test/set-password");
        ReflectionTestUtils.setField(emailEventService, "resetPasswordBaseUrl", "https://app.test/reset-password");
        ReflectionTestUtils.setField(emailEventService, "brandName", BRAND);
    }

    @Test
    @DisplayName("sendLoginEmail: usuario resuelto envía alerta con plantilla genérica")
    void sendLoginEmail_cuandoUsuarioExiste_enviaEmailConDestinatarioYPlantilla() {
        KeycloakUserResponseDto user = admin("u1", "ana@test.com", "Ana", "Lopez");
        when(authClient.getUserById("kc-1")).thenReturn(apiResponse(user));
        UserLoginEvent event = UserLoginEvent.builder().keycloakUserId("kc-1").timestamp(TS).build();

        emailEventService.sendLoginEmail(event);

        Email sent = captureSingleEmail();
        assertThat(sent.getTo()).containsExactly("ana@test.com");
        assertThat(sent.getSubject()).isEqualTo("Alerta de seguridad - " + BRAND);
        assertThat(sent.getTemplateDefinition()).isInstanceOf(GenericEmailTemplate.class);
        assertThat(sent.getTemplateDefinition().getTemplateName()).isEqualTo("generic-email");
        Context ctx = sent.getTemplateDefinition().buildContext();
        assertThat(ctx.getVariable("userName")).isEqualTo("Ana");
        assertThat(ctx.getVariable("emailTitle")).isEqualTo("Inicio de sesión detectado");
        assertThat(ctx.getVariable("emailContent")).asString().contains("inicio de sesión");
    }

    @Test
    @DisplayName("sendLoginEmail: getData() null provoca NullPointerException (sin envío)")
    void sendLoginEmail_cuandoGetDataEsNull_lanzaNullPointerException() {
        ApiResponse<KeycloakUserResponseDto> empty = new ApiResponse<>("ok", null, 200);
        when(authClient.getUserById("kc-missing")).thenReturn(empty);
        UserLoginEvent event = UserLoginEvent.builder().keycloakUserId("kc-missing").timestamp(TS).build();

        assertThatThrownBy(() -> emailEventService.sendLoginEmail(event))
                .isInstanceOf(NullPointerException.class);

        verify(emailService, never()).sendEmail(any());
    }

    @Test
    @DisplayName("sendRegisteredEmail: envía bienvenida al email del evento")
    void sendRegisteredEmail_cuandoDatosValidos_enviaBienvenida() {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .firstName("Juan").lastName("Perez").username("jperez")
                .email("juan@test.com").timestamp(TS).build();

        emailEventService.sendRegisteredEmail(event);

        Email sent = captureSingleEmail();
        assertThat(sent.getTo()).containsExactly("juan@test.com");
        assertThat(sent.getSubject()).isEqualTo("Bienvenido a " + BRAND + " - Registro exitoso");
        assertThat(sent.getTemplateDefinition().getTemplateName()).isEqualTo("user-registered-email");
        Context ctx = sent.getTemplateDefinition().buildContext();
        assertThat(ctx.getVariable("loginUrl")).isEqualTo(LOGIN_URL);
        assertThat(ctx.getVariable("email")).isEqualTo("juan@test.com");
    }

    @Test
    @DisplayName("sendApprovalEmails: lista null no envía correos")
    void sendApprovalEmails_cuandoAdminsNull_noEnvia() {
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR"))
                .thenReturn(new ApiResponse<>("ok", null, 200));
        UserRegisteredEvent event = registeredEvent();

        emailEventService.sendApprovalEmails(event);

        verify(emailService, never()).sendEmail(any());
    }

    @Test
    @DisplayName("sendApprovalEmails: lista vacía no envía correos")
    void sendApprovalEmails_cuandoAdminsVacios_noEnvia() {
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR"))
                .thenReturn(apiResponse(List.of()));
        UserRegisteredEvent event = registeredEvent();

        emailEventService.sendApprovalEmails(event);

        verify(emailService, never()).sendEmail(any());
    }

    @Test
    @DisplayName("sendApprovalEmails: filtra service-account y admins sin email")
    void sendApprovalEmails_cuandoAdminsInvalidos_soloEnviaAValidos() {
        List<KeycloakUserResponseDto> admins = List.of(
                admin("service-account-sync", "sa@test.com", "SA", "X"),
                admin("admin1", null, "A", "B"),
                admin("admin2", "   ", "C", "D"),
                admin("admin3", "valid@test.com", "Val", "Ida")
        );
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR")).thenReturn(apiResponse(admins));

        emailEventService.sendApprovalEmails(registeredEvent());

        verify(emailService, times(1)).sendEmail(any());
        Email sent = captureSingleEmail();
        assertThat(sent.getTo()).containsExactly("valid@test.com");
        assertThat(sent.getTemplateDefinition().getTemplateName()).isEqualTo("user-approval-email");
        Context ctx = sent.getTemplateDefinition().buildContext();
        assertThat(ctx.getVariable("approvalUrl")).isEqualTo(APPROVALS_URL);
    }

    @Test
    @DisplayName("sendApprovalEmails: error en un admin no impide enviar al siguiente")
    void sendApprovalEmails_cuandoPrimerAdminFalla_intentaConElSegundo() {
        KeycloakUserResponseDto a1 = admin("a1", "one@test.com", "One", "Admin");
        KeycloakUserResponseDto a2 = admin("a2", "two@test.com", "Two", "Admin");
        when(authClient.getUsersByRole("SUPER_ADMINISTRADOR")).thenReturn(apiResponse(List.of(a1, a2)));
        doThrow(new RuntimeException("smtp down"))
                .doNothing()
                .when(emailService).sendEmail(any());

        emailEventService.sendApprovalEmails(registeredEvent());

        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailService, times(2)).sendEmail(captor.capture());
        assertThat(captor.getAllValues().get(1).getTo()).containsExactly("two@test.com");
    }

    private Email captureSingleEmail() {
        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailService).sendEmail(captor.capture());
        return captor.getValue();
    }

    private static UserRegisteredEvent registeredEvent() {
        return UserRegisteredEvent.builder()
                .username("new.user").email("new@test.com")
                .firstName("New").lastName("User").timestamp(TS).build();
    }

    private static KeycloakUserResponseDto admin(String username, String email, String first, String last) {
        KeycloakUserResponseDto dto = new KeycloakUserResponseDto();
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setFirstName(first);
        dto.setLastName(last);
        return dto;
    }

    private static <T> ApiResponse<T> apiResponse(T data) {
        return new ApiResponse<>("ok", data, 200);
    }
}
