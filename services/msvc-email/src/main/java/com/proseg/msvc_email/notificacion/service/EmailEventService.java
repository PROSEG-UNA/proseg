package com.proseg.msvc_email.notificacion.service;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.kafka.events.*;
import com.proseg.common.utils.DateUtils;
import com.proseg.msvc_email.notificacion.client.AuthClient;
import com.proseg.msvc_email.notificacion.client.MaintenanceClient;
import com.proseg.msvc_email.notificacion.dto.CompanyResponseDto;
import com.proseg.msvc_email.notificacion.dto.KeycloakUserResponseDto;
import com.proseg.msvc_email.notificacion.model.Email;
import com.proseg.msvc_email.notificacion.template.impl.*;
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
    private final MaintenanceClient maintenanceClient;

    @Value("${app.urls.login:http://localhost:5173/login}")
    private String loginUrl;

    @Value("${app.urls.approvals:http://localhost:5173/login}")
    private String approvalsBaseUrl;

    @Value("${app.urls.set-password:http://localhost:5173/set-password}")
    private String setPasswordBaseUrl;

    @Value("${app.urls.reset-password:http://localhost:5173/reset-password}")
    private String resetPasswordBaseUrl;

    @Value("${app.brand.name:PROSEG}")
    private String brandName;

    public void sendLoginEmail(UserLoginEvent event) {
        ApiResponse<KeycloakUserResponseDto> apiResponse = authClient.getUserById(event.getKeycloakUserId());
        KeycloakUserResponseDto user = apiResponse.getData();
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
                        .subject("Alerta de seguridad - " + brandName)
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
                        .subject("Bienvenido a " + brandName + " - Registro exitoso")
                        .templateDefinition(template)
                        .build()
        );
        log.info("Email de bienvenida enviado a: {}", event.getEmail());
    }

    public void sendApprovalEmails(UserRegisteredEvent event) {
        ApiResponse<List<KeycloakUserResponseDto>> response = authClient.getUsersByRole("SUPER_ADMINISTRADOR");
        List<KeycloakUserResponseDto> superAdmins = response.getData();
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
                                        .subject("Nuevo usuario requiere aprobación - " + brandName)
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

    public void sendInvitationEmail(UserInvitedEvent event) {
        String setPasswordUrl = setPasswordBaseUrl + "?token=" + event.getInvitationToken();
        UserInvitedEmailTemplate template = UserInvitedEmailTemplate.builder()
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .username(event.getUsername())
                .email(event.getEmail())
                .setPasswordUrl(setPasswordUrl)
                .timestamp(event.getTimestamp())
                .build();
        emailService.sendEmail(
                Email.builder()
                        .to(List.of(event.getEmail()))
                        .subject("Fuiste invitado a " + brandName + " - Configurá tu contraseña")
                        .templateDefinition(template)
                        .build()
        );
        log.info("Email de invitación enviado a: {}", event.getEmail());
    }

    public void sendManagedUserCreatedNotificationEmails(ManagedUserCreatedEvent event) {
        ApiResponse<List<KeycloakUserResponseDto>> response =
                authClient.getUsersByRole("SUPER_ADMINISTRADOR");
        List<KeycloakUserResponseDto> superAdmins = response.getData();
        if (superAdmins == null || superAdmins.isEmpty()) {
            log.warn(
                    "No se encontraron administradores para notificar creación del usuario [{}]",
                    event.getUsername()
            );
            return;
        }
        superAdmins.stream()
                .filter(admin ->
                        admin.getUsername() != null &&
                                !admin.getUsername().startsWith("service-account")
                )
                .filter(admin ->
                        admin.getEmail() != null &&
                                !admin.getEmail().isBlank()
                )
                .forEach(admin -> {
                    try {
                        ManagedUserCreatedAdminNotificationEmailTemplate template =
                                ManagedUserCreatedAdminNotificationEmailTemplate.builder()
                                        .newUserFirstName(event.getFirstName())
                                        .newUserLastName(event.getLastName())
                                        .newUsername(event.getUsername())
                                        .newUserEmail(event.getEmail())
                                        .adminFirstName(event.getCreatedByFirstName())
                                        .adminLastName(event.getCreatedByLastName())
                                        .adminUsername(event.getCreatedByUsername())
                                        .adminEmail(event.getCreatedByEmail())
                                        .timestamp(event.getTimestamp())
                                        .build();
                        emailService.sendEmail(
                                Email.builder()
                                        .to(List.of(admin.getEmail()))
                                        .subject("Nuevo usuario creado en " + brandName)
                                        .templateDefinition(template)
                                        .build()
                        );
                        log.info(
                                "Notificación de creación enviada a admin [{}] por usuario [{}]",
                                admin.getEmail(),
                                event.getUsername()
                        );
                    } catch (Exception e) {
                        log.error(
                                "Error enviando notificación de creación a [{}]",
                                admin.getEmail(),
                                e
                        );
                    }
                });
    }

    public void sendPasswordConfiguredEmail(UserPasswordConfiguredEvent event) {
        ApiResponse<KeycloakUserResponseDto> apiResponse =
                authClient.getUserById(event.getKeycloakUserId());
        KeycloakUserResponseDto user = apiResponse.getData();
        if (user == null) {
            log.warn(
                    "No se encontró usuario para evento USER_PASSWORD_CONFIGURED [{}]",
                    event.getKeycloakUserId()
            );
            return;
        }
        UserPasswordConfiguredEmailTemplate template =
                UserPasswordConfiguredEmailTemplate.builder()
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .loginUrl(loginUrl)
                        .timestamp(event.getTimestamp())
                        .build();
        emailService.sendEmail(
                Email.builder()
                        .to(List.of(user.getEmail()))
                        .subject("Tu cuenta en " + brandName + " fue activada correctamente")
                        .templateDefinition(template)
                        .build()
        );
        log.info(
                "Email de cuenta activada enviado a: {}",
                user.getEmail()
        );
    }

    public void sendPasswordResetEmail(PasswordResetRequestedEvent event) {
        String resetPasswordUrl = resetPasswordBaseUrl + "?token=" + event.getResetToken();
        PasswordResetEmailTemplate template = PasswordResetEmailTemplate.builder()
                .firstName(event.getFirstName())
                .email(event.getEmail())
                .resetPasswordUrl(resetPasswordUrl)
                .timestamp(event.getTimestamp())
                .build();
        emailService.sendEmail(
                Email.builder()
                        .to(List.of(event.getEmail()))
                        .subject("Restablecé tu contraseña - " + brandName)
                        .templateDefinition(template)
                        .build()
        );
        log.info("Email de restablecimiento de contraseña enviado a: {}", event.getEmail());
    }

    public void sendPasswordChangedEmail(PasswordChangedEvent event) {
        try {
            ApiResponse<KeycloakUserResponseDto> response =
                    authClient.getUserById(event.getKeycloakUserId());
            KeycloakUserResponseDto user = response.getData();
            if (user == null) {
                log.warn("Usuario no encontrado para password change event: {}",
                        event.getKeycloakUserId());
                return;
            }
            UserPasswordChangedEmailTemplate template =
                    UserPasswordChangedEmailTemplate.builder()
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .loginUrl(loginUrl)
                            .timestamp(event.getTimestamp())
                            .build();
            emailService.sendEmail(
                    Email.builder()
                            .to(List.of(user.getEmail()))
                            .subject("Tu contraseña fue actualizada en " + brandName)
                            .templateDefinition(template)
                            .build()
            );
            log.info("Email password changed enviado a: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error procesando PasswordChangedEvent userId={}: {}",
                    event.getKeycloakUserId(), e.getMessage(), e);
        }
    }

    public void sendPasswordExpiringSoonEmail(PasswordExpiringSoonEvent event) {

        try {
            PasswordExpiringSoonEmailTemplate template =
                    PasswordExpiringSoonEmailTemplate.builder()
                            .firstName(event.getFirstName())
                            .daysRemaining(event.getDaysRemaining())
                            .expiresAt(event.getExpiresAt())
                            .loginUrl(loginUrl)
                            .timestamp(event.getTimestamp())
                            .build();
            emailService.sendEmail(
                    Email.builder()
                            .to(List.of(event.getEmail()))
                            .subject("Tu contraseña expirará pronto en " + brandName)
                            .templateDefinition(template)
                            .build()
            );
            log.info(
                    "Email password expiring soon enviado a: {}",
                    event.getEmail()
            );
        } catch (Exception e) {
            log.error(
                    "Error enviando password expiring soon email userId={}: {}",
                    event.getKeycloakUserId(),
                    e.getMessage(),
                    e
            );
        }
    }

    public void sendPasswordExpiredResetEmail(PasswordExpiredResetRequiredEvent event) {
        try {
            String resetPasswordUrl =
                    resetPasswordBaseUrl + "?token=" + event.getResetToken();
            PasswordExpiredResetEmailTemplate template =
                    PasswordExpiredResetEmailTemplate.builder()
                            .firstName(event.getFirstName())
                            .email(event.getEmail())
                            .resetPasswordUrl(resetPasswordUrl)
                            .timestamp(event.getTimestamp())
                            .build();
            emailService.sendEmail(
                    Email.builder()
                            .to(List.of(event.getEmail()))
                            .subject("Tu contraseña expiró - Acción requerida en " + brandName)
                            .templateDefinition(template)
                            .build()
            );
            log.info(
                    "Email de expiración de contraseña enviado a: {}",
                    event.getEmail()
            );
        } catch (Exception e) {
            log.error(
                    "Error enviando password expired reset email userId={}: {}",
                    event.getKeycloakUserId(),
                    e.getMessage(),
                    e
            );
        }
    }

    public void sendCompanyUsersAssignedEmails(CompanyUsersAssignedEvent event) {
        if (event.getKeycloakUserIds() == null || event.getKeycloakUserIds().isEmpty()) {
            log.warn("CompanyUsersAssignedEvent sin usuarios para companyId={}", event.getCompanyId());
            return;
        }

        // Resolver datos de la empresa una sola vez
        ApiResponse<CompanyResponseDto> apiResponse;
        try {
            apiResponse = maintenanceClient.getCompanyById(event.getCompanyId());
        } catch (Exception e) {
            log.error("No se pudo resolver la empresa companyId={}: {}", event.getCompanyId(), e.getMessage());
            return;
        }

        CompanyResponseDto company = apiResponse.getData();

        event.getKeycloakUserIds().forEach(keycloakUserId -> {
            try {
                ApiResponse<KeycloakUserResponseDto> response = authClient.getUserById(keycloakUserId);
                KeycloakUserResponseDto user = response.getData();

                if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                    log.warn("Usuario no encontrado o sin email para keycloakUserId={}", keycloakUserId);
                    return;
                }

                CompanyUsersAssignedEmailTemplate template = CompanyUsersAssignedEmailTemplate.builder()
                        .firstName(user.getFirstName())
                        .email(user.getEmail())
                        .companyName(company.getName())
                        .legalId(company.getLegalId())
                        .contactEmail(company.getContactEmail())
                        .contactPhone(company.getContactPhone())
                        .address(company.getAddress())
                        .loginUrl(loginUrl)
                        .timestamp(event.getTimestamp())
                        .build();

                emailService.sendEmail(
                        Email.builder()
                                .to(List.of(user.getEmail()))
                                .subject("Acceso empresarial habilitado - " + brandName)
                                .templateDefinition(template)
                                .build()
                );

                log.info("Email de asignación empresarial enviado a: {} (companyId={})",
                        user.getEmail(), event.getCompanyId());

            } catch (Exception e) {
                log.error("Error enviando email de asignación para keycloakUserId={}: {}",
                        keycloakUserId, e.getMessage(), e);
            }
        });
    }

    public void sendMaintenanceRequestCreatedEmail(MaintenanceRequestCreatedEvent event) {
        List<String> toRecipients = cleanEmails(event.getEmails());

        List<String> ccRecipients = cleanEmails(event.getExtraEmails()).stream()
                .filter(email -> !toRecipients.contains(email))
                .toList();

        List<String> bccRecipients = cleanEmails(resolveSuperAdminEmails()).stream()
                .filter(email -> !toRecipients.contains(email) && !ccRecipients.contains(email))
                .toList();

        if (toRecipients.isEmpty() && ccRecipients.isEmpty() && bccRecipients.isEmpty()) {
            log.warn("MaintenanceRequestCreatedEvent sin email destinatario, se omite envío");
            return;
        }

        MaintenanceRequestCreatedEmailTemplate template = MaintenanceRequestCreatedEmailTemplate.builder()
                .companyName(event.getCompanyName())
                .legalId(event.getLegalId())
                .description(event.getDescription())
                .status(event.getStatus())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .campusName(event.getCampusName())
                .buildingName(event.getBuildingName())
                .technicianNames(event.getTechnicianNames())
                .responsibleName(event.getResponsibleName())
                .timestamp(event.getTimestamp() != null ? event.getTimestamp() : System.currentTimeMillis())
                .build();

        emailService.sendEmail(
                Email.builder()
                        .to(toRecipients)
                        .cc(ccRecipients)
                        .bcc(bccRecipients)
                        .subject("Nueva solicitud de mantenimiento registrada - " + brandName)
                        .templateDefinition(template)
                        .build()
        );

        log.info("Email de solicitud de mantenimiento enviado a to={} cc={} bcc={}", toRecipients, ccRecipients, bccRecipients);
    }

    public void sendMaintenanceRequestNotificationEmail(MaintenanceRequestNotificationEvent event) {
        List<String> toRecipients = cleanEmails(event.getRecipientEmails());

        List<String> ccRecipients = cleanEmails(event.getExtraEmails()).stream()
                .filter(email -> !toRecipients.contains(email))
                .toList();

        List<String> bccRecipients = cleanEmails(resolveSuperAdminEmails()).stream()
                .filter(email -> !toRecipients.contains(email) && !ccRecipients.contains(email))
                .toList();

        if (toRecipients.isEmpty() && ccRecipients.isEmpty() && bccRecipients.isEmpty()) {
            log.warn("MaintenanceRequestNotificationEvent sin email destinatario, se omite envio");
            return;
        }

        MaintenanceRequestNotificationEmailTemplate template = MaintenanceRequestNotificationEmailTemplate.builder()
                .requestId(event.getRequestId())
                .actionType(event.getActionType())
                .actionLabel(event.getActionLabel())
                .detail(event.getDetail())
                .companyName(event.getCompanyName())
                .legalId(event.getLegalId())
                .description(event.getDescription())
                .cancellationReason(event.getCancellationReason())
                .status(event.getStatus())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .campusName(event.getCampusName())
                .buildingName(event.getBuildingName())
                .changedFields(event.getChangedFields())
                .timestamp(event.getTimestamp() != null ? event.getTimestamp() : System.currentTimeMillis())
                .build();

        emailService.sendEmail(
                Email.builder()
                        .to(toRecipients)
                        .cc(ccRecipients)
                        .bcc(bccRecipients)
                        .subject(buildMaintenanceNotificationSubject(event))
                        .templateDefinition(template)
                        .build()
        );

        log.info("Email de notificacion de mantenimiento enviado a to={} cc={} bcc={}", toRecipients, ccRecipients, bccRecipients);
    }

    public void sendTicketNotificationEmail(TicketNotificationEvent event) {
        List<String> toRecipients = cleanEmails(event.getRecipientEmails());

        List<String> ccRecipients = cleanEmails(event.getExtraEmails()).stream()
                .filter(email -> !toRecipients.contains(email))
                .toList();

        List<String> bccRecipients = cleanEmails(resolveSuperAdminEmails()).stream()
                .filter(email -> !toRecipients.contains(email) && !ccRecipients.contains(email))
                .toList();

        if (toRecipients.isEmpty() && ccRecipients.isEmpty() && bccRecipients.isEmpty()) {
            log.warn("TicketNotificationEvent sin email destinatario, se omite envio");
            return;
        }

        TicketNotificationEmailTemplate template = TicketNotificationEmailTemplate.builder()
                .ticketId(event.getTicketId())
                .actionType(event.getActionType())
                .actionLabel(event.getActionLabel())
                .actorName(event.getActorName())
                .ticketTitle(event.getTicketTitle())
                .ticketDescription(event.getTicketDescription())
                .status(event.getStatus())
                .priority(event.getPriority())
                .campusName(event.getCampusName())
                .buildingName(event.getBuildingName())
                .detail(event.getDetail())
                .changedFields(event.getChangedFields())
                .timestamp(event.getTimestamp() != null ? event.getTimestamp() : System.currentTimeMillis())
                .build();

        emailService.sendEmail(
                Email.builder()
                        .to(toRecipients)
                        .cc(ccRecipients)
                        .bcc(bccRecipients)
                        .subject(buildTicketNotificationSubject(event))
                        .templateDefinition(template)
                        .build()
        );

        log.info("Email de ticket enviado a to={} cc={} bcc={}", toRecipients, ccRecipients, bccRecipients);
    }

    private String buildTicketNotificationSubject(TicketNotificationEvent event) {
        String actionLabel = event.getActionLabel() != null && !event.getActionLabel().isBlank()
                ? event.getActionLabel()
                : "Actualizacion de ticket";
        return actionLabel + " - Ticket - " + brandName;
    }

    private String buildMaintenanceNotificationSubject(MaintenanceRequestNotificationEvent event) {
        String actionLabel = event.getActionLabel() != null && !event.getActionLabel().isBlank()
                ? event.getActionLabel()
                : "Actualizacion de solicitud";
        return actionLabel + " - Solicitud de mantenimiento - " + brandName;
    }

    public void sendUserRoleAssignedEmail(UserRoleAssignedEvent event) {
        try {
            // Send email to the user who was assigned the role
            UserRoleAssignedEmailTemplate template = UserRoleAssignedEmailTemplate.builder()
                    .firstName(event.getFirstName())
                    .lastName(event.getLastName())
                    .username(event.getUsername())
                    .email(event.getEmail())
                    .roleName(event.getRoleName())
                    .timestamp(event.getTimestamp())
                    .assignedByFirstName(event.getAssignedByFirstName())
                    .assignedByLastName(event.getAssignedByLastName())
                    .assignedByUsername(event.getAssignedByUsername())
                    .assignedByEmail(event.getAssignedByEmail())
                    .build();

            emailService.sendEmail(
                    Email.builder()
                            .to(List.of(event.getEmail()))
                            .subject("Se te ha asignado un nuevo rol en " + brandName)
                            .templateDefinition(template)
                            .build()
            );

            log.info("Email de asignación de rol enviado al usuario: {} (rol: {})",
                    event.getEmail(), event.getRoleName());

            // Send notification email to super admins
            sendRoleAssignmentNotificationToAdmins(event);

        } catch (Exception e) {
            log.error("Error enviando email de asignación de rol a userId={}: {}",
                    event.getUserId(), e.getMessage(), e);
        }
    }

    private void sendRoleAssignmentNotificationToAdmins(UserRoleAssignedEvent event) {
        try {
            ApiResponse<List<KeycloakUserResponseDto>> response = authClient.getUsersByRole("SUPER_ADMINISTRADOR");
            List<KeycloakUserResponseDto> superAdmins = response.getData();
            if (superAdmins == null || superAdmins.isEmpty()) {
                log.warn("No se encontraron administradores para notificar asignación de rol a usuario: {}",
                        event.getUsername());
                return;
            }

            superAdmins.stream()
                    .filter(admin -> admin.getUsername() != null && !admin.getUsername().startsWith("service-account"))
                    .filter(admin -> admin.getEmail() != null && !admin.getEmail().isBlank())
                    .forEach(admin -> {
                        try {
                            UserRoleAssignedAdminNotificationEmailTemplate template = UserRoleAssignedAdminNotificationEmailTemplate.builder()
                                    .userFirstName(event.getFirstName())
                                    .userLastName(event.getLastName())
                                    .username(event.getUsername())
                                    .userEmail(event.getEmail())
                                    .roleName(event.getRoleName())
                                    .timestamp(event.getTimestamp())
                                    .assignedByFirstName(event.getAssignedByFirstName())
                                    .assignedByLastName(event.getAssignedByLastName())
                                    .assignedByUsername(event.getAssignedByUsername())
                                    .assignedByEmail(event.getAssignedByEmail())
                                    .build();

                            emailService.sendEmail(
                                    Email.builder()
                                            .to(List.of(admin.getEmail()))
                                            .subject("Alerta: Cambio de rol realizado - " + brandName)
                                            .templateDefinition(template)
                                            .build()
                            );

                            log.info("Notificación de cambio de rol enviada a admin [{}] para usuario [{}]",
                                    admin.getEmail(), event.getUsername());
                        } catch (Exception e) {
                            log.error("Error enviando notificación de cambio de rol a [{}]",
                                    admin.getEmail(), e);
                        }
                    });

        } catch (Exception e) {
            log.error("Error resolviendo administradores para notificación de cambio de rol: {}",
                    e.getMessage(), e);
        }
    }

    private List<String> cleanEmails(List<String> emails) {
        if (emails == null) {
            return List.of();
        }
        return emails.stream()
                .filter(email -> email != null && !email.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private List<String> resolveSuperAdminEmails() {
        try {
            ApiResponse<List<KeycloakUserResponseDto>> response = authClient.getUsersByRole("SUPER_ADMINISTRADOR");
            List<KeycloakUserResponseDto> superAdmins = response != null ? response.getData() : null;
            if (superAdmins == null || superAdmins.isEmpty()) {
                return List.of();
            }
            return superAdmins.stream()
                    .filter(admin -> admin.getUsername() != null && !admin.getUsername().startsWith("service-account"))
                    .map(KeycloakUserResponseDto::getEmail)
                    .filter(email -> email != null && !email.isBlank())
                    .toList();
        } catch (Exception e) {
            log.error("No se pudieron resolver los SUPER_ADMINISTRADOR para la solicitud de mantenimiento: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public void sendUserStatusChangedEmail(UserStatusChangedEvent event) {
        try {
            // Send email to the user whose status changed
            UserStatusChangedEmailTemplate template = UserStatusChangedEmailTemplate.builder()
                    .firstName(event.getFirstName())
                    .lastName(event.getLastName())
                    .username(event.getUsername())
                    .email(event.getEmail())
                    .oldStatus(event.getOldStatus())
                    .newStatus(event.getNewStatus())
                    .timestamp(event.getTimestamp())
                    .changedByFirstName(event.getChangedByFirstName())
                    .changedByLastName(event.getChangedByLastName())
                    .changedByUsername(event.getChangedByUsername())
                    .changedByEmail(event.getChangedByEmail())
                    .reason(event.getReason())
                    .build();

            emailService.sendEmail(
                    Email.builder()
                            .to(List.of(event.getEmail()))
                            .subject("Tu estado ha sido actualizado en " + brandName)
                            .templateDefinition(template)
                            .build()
            );

            log.info("Email de cambio de estado enviado al usuario: {} (estado anterior: {} -> nuevo: {})",
                    event.getEmail(), event.getOldStatus(), event.getNewStatus());

            // Send notification email to super admins
            sendStatusChangeNotificationToAdmins(event);

        } catch (Exception e) {
            log.error("Error enviando email de cambio de estado a userId={}: {}",
                    event.getUserId(), e.getMessage(), e);
        }
    }

    private void sendStatusChangeNotificationToAdmins(UserStatusChangedEvent event) {
        try {
            ApiResponse<List<KeycloakUserResponseDto>> response = authClient.getUsersByRole("SUPER_ADMINISTRADOR");
            List<KeycloakUserResponseDto> superAdmins = response.getData();
            if (superAdmins == null || superAdmins.isEmpty()) {
                log.warn("No se encontraron administradores para notificar cambio de estado de usuario: {}",
                        event.getUsername());
                return;
            }

            superAdmins.stream()
                    .filter(admin -> admin.getUsername() != null && !admin.getUsername().startsWith("service-account"))
                    .filter(admin -> admin.getEmail() != null && !admin.getEmail().isBlank())
                    .forEach(admin -> {
                        try {
                            UserStatusChangedAdminNotificationEmailTemplate template = UserStatusChangedAdminNotificationEmailTemplate.builder()
                                    .userFirstName(event.getFirstName())
                                    .userLastName(event.getLastName())
                                    .username(event.getUsername())
                                    .userEmail(event.getEmail())
                                    .oldStatus(event.getOldStatus())
                                    .newStatus(event.getNewStatus())
                                    .timestamp(event.getTimestamp())
                                    .changedByFirstName(event.getChangedByFirstName())
                                    .changedByLastName(event.getChangedByLastName())
                                    .changedByUsername(event.getChangedByUsername())
                                    .changedByEmail(event.getChangedByEmail())
                                    .reason(event.getReason())
                                    .build();

                            emailService.sendEmail(
                                    Email.builder()
                                            .to(List.of(admin.getEmail()))
                                            .subject("Alerta: Cambio de estado de usuario - " + brandName)
                                            .templateDefinition(template)
                                            .build()
                            );

                            log.info("Notificación de cambio de estado enviada a admin [{}] para usuario [{}]",
                                    admin.getEmail(), event.getUsername());
                        } catch (Exception e) {
                            log.error("Error enviando notificación de cambio de estado a [{}]",
                                    admin.getEmail(), e);
                        }
                    });

        } catch (Exception e) {
            log.error("Error resolviendo administradores para notificación de cambio de estado: {}",
                    e.getMessage(), e);
        }
    }
}