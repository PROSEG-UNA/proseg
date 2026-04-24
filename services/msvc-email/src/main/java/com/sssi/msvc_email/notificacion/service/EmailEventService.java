package com.sssi.msvc_email.notificacion.service;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.kafka.events.UserLoginEvent;
import com.sssi.common.kafka.events.UserRegisteredEvent;
import com.sssi.common.utils.DateUtils;
import com.sssi.msvc_email.notificacion.client.AuthClient;
import com.sssi.msvc_email.notificacion.dto.KeycloakUserDto;
import com.sssi.msvc_email.notificacion.model.Email;
import com.sssi.msvc_email.notificacion.template.impl.GenericEmailTemplate;
import com.sssi.msvc_email.notificacion.template.impl.UserApprovalEmailTemplate;
import com.sssi.msvc_email.notificacion.template.impl.UserRegisteredEmailTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailEventService {

    private final EmailService emailService;
    private final AuthClient authClient;

    @Value("${app.urls.login:https://sssi.com/login}")
    private String loginUrl;

    @Value("${app.urls.approvals:http://localhost:5173/login}")
    private String approvalsBaseUrl;

    public void sendLoginEmail(UserLoginEvent event) {
        ApiResponse<KeycloakUserDto> apiResponse = authClient.getUserById(event.getUserId());
        KeycloakUserDto user = apiResponse.getData();

        String formattedDate = DateUtils.formatReadable(event.getTimestamp());

        GenericEmailTemplate template = GenericEmailTemplate.builder()
                .userName(user.getFirstName())
                .emailTitle("Inicio de sesión detectado")
                .emailContent(
                        "Se ha detectado un inicio de sesión en su cuenta el " +
                                formattedDate +
                                ". Si no reconoce esta actividad, por favor cambie su contraseña inmediatamente."
                )
                .build();

        emailService.sendEmail(
                Email.builder()
                        .to(List.of(user.getEmail()))
                        .subject("Alerta de seguridad - SSSI")
                        .templateDefinition(template)
                        .build()
        );

        log.info("Email de login enviado a: {}", user.getEmail());
    }

    public void sendRegisteredEmail(UserRegisteredEvent event) {
        UserRegisteredEmailTemplate template = UserRegisteredEmailTemplate.builder()
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .username(event.getUsername())
                .email(event.getEmail())
                .loginUrl(loginUrl)
                .timestamp(event.getTimestamp())
                .build();

        emailService.sendEmail(
                Email.builder()
                        .to(List.of(event.getEmail()))
                        .subject("Bienvenido a SSSI - Registro exitoso")
                        .templateDefinition(template)
                        .build()
        );

        log.info("Email de bienvenida enviado a: {}", event.getEmail());
    }

    public void sendApprovalEmails(UserRegisteredEvent event) {

        ApiResponse<List<KeycloakUserDto>> response = authClient.getUsersByRole("SUPER_ADMINISTRADOR");
        List<KeycloakUserDto> superAdmins = response.getData();

        if (superAdmins == null || superAdmins.isEmpty()) {
            log.warn("No se encontraron administradores para notificar registro de: {}", event.getUsername());
            return;
        }

        String approvalUrl = approvalsBaseUrl;

        superAdmins.stream()
                .filter(admin -> admin.getUsername() != null && !admin.getUsername().startsWith("service-account"))
                .filter(admin -> admin.getEmail() != null && !admin.getEmail().isBlank())
                .forEach(admin -> {
                    try {

                        UserApprovalEmailTemplate template = UserApprovalEmailTemplate.builder()
                                .firstName(event.getFirstName())
                                .lastName(event.getLastName())
                                .username(event.getUsername())
                                .email(event.getEmail())
                                .approvalUrl(approvalUrl)
                                .timestamp(event.getTimestamp())
                                .adminFirstName(admin.getFirstName())
                                .adminLastName(admin.getLastName())
                                .build();

                        emailService.sendEmail(
                                Email.builder()
                                        .to(List.of(admin.getEmail()))
                                        .subject("Nuevo usuario requiere aprobación - SSSI")
                                        .templateDefinition(template)
                                        .build()
                        );

                        log.info("Email de aprobación enviado a admin [{}] por registro de [{}]",
                                admin.getEmail(), event.getUsername());

                    } catch (Exception e) {
                        log.error("Error enviando email a [{}]", admin.getEmail(), e);
                    }
                });
    }
}