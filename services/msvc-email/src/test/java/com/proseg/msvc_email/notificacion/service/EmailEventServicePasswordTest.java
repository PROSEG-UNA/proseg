package com.proseg.msvc_email.notificacion.service;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.kafka.events.*;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailEventServicePasswordTest {

    private static final String LOGIN_URL = "https://app.test/login";
    private static final String RESET_PASSWORD_URL = "https://app.test/reset-password";
    private static final String BRAND = "MARCA_TEST";
    private static final long TS = 1_700_000_000_000L;

    @Mock private EmailService emailService;
    @Mock private AuthClient authClient;
    @Mock private MaintenanceClient maintenanceClient;

    private EmailEventService emailEventService;

    @BeforeEach
    void setUp() {
        emailEventService = new EmailEventService(emailService, authClient, maintenanceClient);
        ReflectionTestUtils.setField(emailEventService, "loginUrl", LOGIN_URL);
        ReflectionTestUtils.setField(emailEventService, "approvalsBaseUrl", "https://app.test/approvals");
        ReflectionTestUtils.setField(emailEventService, "setPasswordBaseUrl", "https://app.test/set-password");
        ReflectionTestUtils.setField(emailEventService, "resetPasswordBaseUrl", RESET_PASSWORD_URL);
        ReflectionTestUtils.setField(emailEventService, "brandName", BRAND);
    }

    @Test
    @DisplayName("sendPasswordConfiguredEmail: usuario null omite envío")
    void sendPasswordConfigured_cuandoUsuarioNull_noEnvia() {
        when(authClient.getUserById("kc-x")).thenReturn(new ApiResponse<>("ok", null, 200));

        emailEventService.sendPasswordConfiguredEmail(
                UserPasswordConfiguredEvent.builder().keycloakUserId("kc-x").timestamp(TS).build()
        );

        verify(emailService, never()).sendEmail(any());
    }

    @Test
    @DisplayName("sendPasswordConfiguredEmail: envía activación con loginUrl")
    void sendPasswordConfigured_cuandoUsuarioExiste_enviaActivacion() {
        KeycloakUserResponseDto user = user("kc-1", "act@test.com", "Act", "Ivo", "activo");
        when(authClient.getUserById("kc-1")).thenReturn(apiResponse(user));

        emailEventService.sendPasswordConfiguredEmail(
                UserPasswordConfiguredEvent.builder().keycloakUserId("kc-1").timestamp(TS).build()
        );

        Email sent = captureSingleEmail();
        assertThat(sent.getTo()).containsExactly("act@test.com");
        assertThat(sent.getSubject()).contains(BRAND);
        Context ctx = sent.getTemplateDefinition().buildContext();
        assertThat(ctx.getVariable("loginUrl")).isEqualTo(LOGIN_URL);
    }

    @Test
    @DisplayName("sendPasswordResetEmail: URL incluye token en query")
    void sendPasswordReset_cuandoSolicitudValida_urlConToken() {
        PasswordResetRequestedEvent event = PasswordResetRequestedEvent.builder()
                .email("reset@test.com").firstName("Re").resetToken("rst-99").timestamp(TS).build();

        emailEventService.sendPasswordResetEmail(event);

        Email sent = captureSingleEmail();
        assertThat(sent.getTo()).containsExactly("reset@test.com");
        assertThat(sent.getSubject()).isEqualTo("Restablecé tu contraseña - " + BRAND);
        Context ctx = sent.getTemplateDefinition().buildContext();
        assertThat(ctx.getVariable("resetPasswordUrl"))
                .isEqualTo(RESET_PASSWORD_URL + "?token=rst-99");
    }

    @Test
    @DisplayName("sendPasswordChangedEmail: usuario null omite envío")
    void sendPasswordChanged_cuandoUsuarioNull_noEnvia() {
        when(authClient.getUserById("kc-y")).thenReturn(new ApiResponse<>("ok", null, 200));

        emailEventService.sendPasswordChangedEmail(
                PasswordChangedEvent.builder().keycloakUserId("kc-y").timestamp(TS).build()
        );

        verify(emailService, never()).sendEmail(any());
    }

    @Test
    @DisplayName("sendPasswordChangedEmail: error en authClient se traga sin enviar")
    void sendPasswordChanged_cuandoAuthClientFalla_noEnvia() {
        when(authClient.getUserById("kc-z")).thenThrow(new RuntimeException("auth caído"));

        assertThatCode(() -> emailEventService.sendPasswordChangedEmail(
                PasswordChangedEvent.builder().keycloakUserId("kc-z").timestamp(TS).build()
        )).doesNotThrowAnyException();

        verify(emailService, never()).sendEmail(any());
    }

    @Test
    @DisplayName("sendPasswordChangedEmail: envía confirmación al email del usuario")
    void sendPasswordChanged_cuandoUsuarioExiste_enviaConfirmacion() {
        when(authClient.getUserById("kc-2")).thenReturn(apiResponse(
                user("kc-2", "chg@test.com", "Cam", "Bio", "cambio")));

        emailEventService.sendPasswordChangedEmail(
                PasswordChangedEvent.builder().keycloakUserId("kc-2").timestamp(TS).build()
        );

        Email sent = captureSingleEmail();
        assertThat(sent.getTo()).containsExactly("chg@test.com");
        assertThat(sent.getTemplateDefinition().getTemplateName()).isEqualTo("user-password-changed-email");
    }

    @Test
    @DisplayName("sendPasswordExpiringSoonEmail: envía aviso con días restantes")
    void sendPasswordExpiringSoon_cuandoDatosValidos_enviaAviso() {
        PasswordExpiringSoonEvent event = PasswordExpiringSoonEvent.builder()
                .email("exp@test.com").firstName("Exp")
                .daysRemaining(3).expiresAt(Instant.parse("2026-01-15T00:00:00Z"))
                .timestamp(TS).build();

        emailEventService.sendPasswordExpiringSoonEmail(event);

        Email sent = captureSingleEmail();
        assertThat(sent.getTo()).containsExactly("exp@test.com");
        Context ctx = sent.getTemplateDefinition().buildContext();
        assertThat(ctx.getVariable("daysRemaining")).isEqualTo(3L);
        assertThat(ctx.getVariable("loginUrl")).isEqualTo(LOGIN_URL);
    }

    @Test
    @DisplayName("sendPasswordExpiringSoonEmail: fallo al enviar no propaga excepción")
    void sendPasswordExpiringSoon_cuandoSendEmailFalla_noPropaga() {
        doThrow(new RuntimeException("mail error")).when(emailService).sendEmail(any());
        PasswordExpiringSoonEvent event = PasswordExpiringSoonEvent.builder()
                .email("exp2@test.com").firstName("Exp")
                .daysRemaining(1).expiresAt(Instant.now().plusSeconds(86400))
                .timestamp(TS).build();

        assertThatCode(() -> emailEventService.sendPasswordExpiringSoonEmail(event))
                .doesNotThrowAnyException();

        verify(emailService).sendEmail(any());
    }

    @Test
    @DisplayName("sendPasswordExpiredResetEmail: URL incluye token en query")
    void sendPasswordExpiredReset_cuandoExpiracion_urlConToken() {
        PasswordExpiredResetRequiredEvent event = PasswordExpiredResetRequiredEvent.builder()
                .email("old@test.com").firstName("Old").resetToken("exp-tok").timestamp(TS).build();

        emailEventService.sendPasswordExpiredResetEmail(event);

        Email sent = captureSingleEmail();
        assertThat(sent.getTo()).containsExactly("old@test.com");
        Context ctx = sent.getTemplateDefinition().buildContext();
        assertThat(ctx.getVariable("resetPasswordUrl"))
                .isEqualTo(RESET_PASSWORD_URL + "?token=exp-tok");
    }

    @Test
    @DisplayName("sendPasswordExpiredResetEmail: fallo al enviar no propaga excepción")
    void sendPasswordExpiredReset_cuandoSendEmailFalla_noPropaga() {
        doThrow(new RuntimeException("mail error")).when(emailService).sendEmail(any());

        assertThatCode(() -> emailEventService.sendPasswordExpiredResetEmail(
                PasswordExpiredResetRequiredEvent.builder()
                        .email("x@test.com").firstName("X").resetToken("t").timestamp(TS).build()
        )).doesNotThrowAnyException();

        verify(emailService).sendEmail(any());
    }

    private Email captureSingleEmail() {
        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailService).sendEmail(captor.capture());
        return captor.getValue();
    }

    private static KeycloakUserResponseDto user(
            String id, String email, String first, String last, String username) {
        KeycloakUserResponseDto dto = new KeycloakUserResponseDto();
        dto.setId(id);
        dto.setEmail(email);
        dto.setFirstName(first);
        dto.setLastName(last);
        dto.setUsername(username);
        return dto;
    }

    private static <T> ApiResponse<T> apiResponse(T data) {
        return new ApiResponse<>("ok", data, 200);
    }
}
