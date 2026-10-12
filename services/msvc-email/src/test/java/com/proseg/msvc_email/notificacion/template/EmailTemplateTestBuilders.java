package com.proseg.msvc_email.notificacion.template;

import com.proseg.msvc_email.notificacion.template.impl.*;

import java.time.Instant;
import java.util.UUID;

public final class EmailTemplateTestBuilders {

    public static final long TS = 1_704_000_000_000L;

    private EmailTemplateTestBuilders() {
    }

    public static CompanyUsersAssignedEmailTemplate companyUsersAssigned() {
        return CompanyUsersAssignedEmailTemplate.builder()
                .firstName("Ana").email("a@b.com").companyName("Acme")
                .loginUrl("https://app/login").timestamp(TS).build();
    }

    public static EmailTemplateDefinition companyUsersAssignedField(String field, String value) {
        var b = CompanyUsersAssignedEmailTemplate.builder()
                .firstName("Ana").email("a@b.com").companyName("Acme")
                .loginUrl("https://app/login").timestamp(TS);
        switch (field) {
            case "firstName" -> b.firstName(value);
            case "email" -> b.email(value);
            case "companyName" -> b.companyName(value);
            case "loginUrl" -> b.loginUrl(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

    public static GenericEmailTemplate generic() {
        return GenericEmailTemplate.builder().userName("Ana").emailTitle("Título").emailContent("Cuerpo").build();
    }

    public static EmailTemplateDefinition genericField(String field, String value) {
        var b = GenericEmailTemplate.builder().userName("Ana").emailTitle("Título").emailContent("Cuerpo");
        switch (field) {
            case "userName" -> b.userName(value);
            case "emailTitle" -> b.emailTitle(value);
            case "emailContent" -> b.emailContent(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

    public static MaintenanceRequestCreatedEmailTemplate maintenanceCreated() {
        return MaintenanceRequestCreatedEmailTemplate.builder()
                .companyName("Acme").status("OPEN").timestamp(TS).build();
    }

    public static EmailTemplateDefinition maintenanceCreatedField(String field, String value) {
        var b = MaintenanceRequestCreatedEmailTemplate.builder()
                .companyName("Acme").status("OPEN").timestamp(TS);
        switch (field) {
            case "companyName" -> b.companyName(value);
            case "status" -> b.status(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

    public static MaintenanceRequestNotificationEmailTemplate maintenanceNotification() {
        return MaintenanceRequestNotificationEmailTemplate.builder()
                .requestId(UUID.randomUUID()).actionLabel("Actualización")
                .companyName("Acme").status("OPEN").timestamp(TS).build();
    }

    public static EmailTemplateDefinition maintenanceNotificationField(String field, String value) {
        var b = MaintenanceRequestNotificationEmailTemplate.builder()
                .requestId(UUID.randomUUID()).actionLabel("Actualización")
                .companyName("Acme").status("OPEN").timestamp(TS);
        switch (field) {
            case "actionLabel" -> b.actionLabel(value);
            case "companyName" -> b.companyName(value);
            case "status" -> b.status(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

    public static ManagedUserCreatedAdminNotificationEmailTemplate managedUserCreated() {
        return ManagedUserCreatedAdminNotificationEmailTemplate.builder()
                .newUserFirstName("Nuevo").newUserLastName("Usuario").newUsername("nuevo")
                .newUserEmail("n@b.com").adminFirstName("Admin").adminLastName("Uno")
                .adminUsername("admin").adminEmail("a@b.com").timestamp(TS).build();
    }

    public static EmailTemplateDefinition managedUserCreatedField(String field, String value) {
        var b = ManagedUserCreatedAdminNotificationEmailTemplate.builder()
                .newUserFirstName("Nuevo").newUserLastName("Usuario").newUsername("nuevo")
                .newUserEmail("n@b.com").adminFirstName("Admin").adminLastName("Uno")
                .adminUsername("admin").adminEmail("a@b.com").timestamp(TS);
        switch (field) {
            case "newUserFirstName" -> b.newUserFirstName(value);
            case "newUserLastName" -> b.newUserLastName(value);
            case "newUsername" -> b.newUsername(value);
            case "newUserEmail" -> b.newUserEmail(value);
            case "adminFirstName" -> b.adminFirstName(value);
            case "adminLastName" -> b.adminLastName(value);
            case "adminUsername" -> b.adminUsername(value);
            case "adminEmail" -> b.adminEmail(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

    public static PasswordExpiredResetEmailTemplate passwordExpiredReset() {
        return PasswordExpiredResetEmailTemplate.builder()
                .firstName("Ana").email("a@b.com").resetPasswordUrl("https://reset").timestamp(TS).build();
    }

    public static EmailTemplateDefinition passwordExpiredResetField(String field, String value) {
        var b = PasswordExpiredResetEmailTemplate.builder()
                .firstName("Ana").email("a@b.com").resetPasswordUrl("https://reset").timestamp(TS);
        switch (field) {
            case "firstName" -> b.firstName(value);
            case "email" -> b.email(value);
            case "resetPasswordUrl" -> b.resetPasswordUrl(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

    public static PasswordExpiringSoonEmailTemplate passwordExpiringSoon() {
        return PasswordExpiringSoonEmailTemplate.builder()
                .firstName("Ana").daysRemaining(3).expiresAt(Instant.ofEpochMilli(TS))
                .loginUrl("https://login").timestamp(TS).build();
    }

    public static EmailTemplateDefinition passwordExpiringSoonField(String field, String value) {
        var b = PasswordExpiringSoonEmailTemplate.builder()
                .firstName("Ana").daysRemaining(3).expiresAt(Instant.ofEpochMilli(TS))
                .loginUrl("https://login").timestamp(TS);
        switch (field) {
            case "firstName" -> b.firstName(value);
            case "loginUrl" -> b.loginUrl(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

    public static PasswordResetEmailTemplate passwordReset() {
        return PasswordResetEmailTemplate.builder()
                .firstName("Ana").email("a@b.com").resetPasswordUrl("https://reset").timestamp(TS).build();
    }

    public static EmailTemplateDefinition passwordResetField(String field, String value) {
        var b = PasswordResetEmailTemplate.builder()
                .firstName("Ana").email("a@b.com").resetPasswordUrl("https://reset").timestamp(TS);
        switch (field) {
            case "firstName" -> b.firstName(value);
            case "email" -> b.email(value);
            case "resetPasswordUrl" -> b.resetPasswordUrl(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

    public static TicketNotificationEmailTemplate ticketNotification() {
        return TicketNotificationEmailTemplate.builder()
                .ticketId(UUID.randomUUID()).actionLabel("Comentario")
                .ticketTitle("Falla eléctrica").timestamp(TS).build();
    }

    public static EmailTemplateDefinition ticketNotificationField(String field, String value) {
        var b = TicketNotificationEmailTemplate.builder()
                .ticketId(UUID.randomUUID()).actionLabel("Comentario")
                .ticketTitle("Falla eléctrica").timestamp(TS);
        switch (field) {
            case "actionLabel" -> b.actionLabel(value);
            case "ticketTitle" -> b.ticketTitle(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

    public static UserApprovalEmailTemplate userApproval() {
        return UserApprovalEmailTemplate.builder().firstName("Ana").lastName("Lopez").username("ana")
                .email("a@b.com").approvalUrl("https://approve").adminFirstName("Admin").adminLastName("Uno")
                .timestamp(TS).build();
    }

    public static EmailTemplateDefinition userApprovalField(String field, String value) {
        var b = UserApprovalEmailTemplate.builder().firstName("Ana").lastName("Lopez").username("ana")
                .email("a@b.com").approvalUrl("https://approve").adminFirstName("Admin").adminLastName("Uno")
                .timestamp(TS);
        switch (field) {
            case "firstName" -> b.firstName(value);
            case "lastName" -> b.lastName(value);
            case "username" -> b.username(value);
            case "email" -> b.email(value);
            case "approvalUrl" -> b.approvalUrl(value);
            case "adminFirstName" -> b.adminFirstName(value);
            case "adminLastName" -> b.adminLastName(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

    public static UserInvitedEmailTemplate userInvited() {
        return UserInvitedEmailTemplate.builder().firstName("Ana").lastName("Lopez").username("ana")
                .email("a@b.com").setPasswordUrl("https://set-pwd").timestamp(TS).build();
    }

    public static EmailTemplateDefinition userInvitedField(String field, String value) {
        var b = UserInvitedEmailTemplate.builder().firstName("Ana").lastName("Lopez").username("ana")
                .email("a@b.com").setPasswordUrl("https://set-pwd").timestamp(TS);
        switch (field) {
            case "firstName" -> b.firstName(value);
            case "lastName" -> b.lastName(value);
            case "username" -> b.username(value);
            case "email" -> b.email(value);
            case "setPasswordUrl" -> b.setPasswordUrl(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

    public static UserPasswordChangedEmailTemplate userPasswordChanged() {
        return UserPasswordChangedEmailTemplate.builder().firstName("Ana").lastName("Lopez").username("ana")
                .email("a@b.com").loginUrl("https://login").timestamp(TS).build();
    }

    public static EmailTemplateDefinition userPasswordChangedField(String field, String value) {
        var b = UserPasswordChangedEmailTemplate.builder().firstName("Ana").lastName("Lopez").username("ana")
                .email("a@b.com").loginUrl("https://login").timestamp(TS);
        switch (field) {
            case "firstName" -> b.firstName(value);
            case "lastName" -> b.lastName(value);
            case "username" -> b.username(value);
            case "email" -> b.email(value);
            case "loginUrl" -> b.loginUrl(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

    public static UserPasswordConfiguredEmailTemplate userPasswordConfigured() {
        return UserPasswordConfiguredEmailTemplate.builder().firstName("Ana").lastName("Lopez").username("ana")
                .email("a@b.com").loginUrl("https://login").timestamp(TS).build();
    }

    public static EmailTemplateDefinition userPasswordConfiguredField(String field, String value) {
        var b = UserPasswordConfiguredEmailTemplate.builder().firstName("Ana").lastName("Lopez").username("ana")
                .email("a@b.com").loginUrl("https://login").timestamp(TS);
        switch (field) {
            case "firstName" -> b.firstName(value);
            case "lastName" -> b.lastName(value);
            case "username" -> b.username(value);
            case "email" -> b.email(value);
            case "loginUrl" -> b.loginUrl(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

    public static UserRegisteredEmailTemplate userRegistered() {
        return UserRegisteredEmailTemplate.builder().firstName("Ana").lastName("Lopez").username("ana")
                .email("a@b.com").loginUrl("https://login").timestamp(TS).build();
    }

    public static EmailTemplateDefinition userRegisteredField(String field, String value) {
        var b = UserRegisteredEmailTemplate.builder().firstName("Ana").lastName("Lopez").username("ana")
                .email("a@b.com").loginUrl("https://login").timestamp(TS);
        switch (field) {
            case "firstName" -> b.firstName(value);
            case "lastName" -> b.lastName(value);
            case "username" -> b.username(value);
            case "email" -> b.email(value);
            case "loginUrl" -> b.loginUrl(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

    public static UserRoleAssignedEmailTemplate userRoleAssigned() {
        return UserRoleAssignedEmailTemplate.builder().firstName("Ana").lastName("Lopez").username("ana")
                .email("a@b.com").roleName("Operador").assignedByFirstName("Admin").assignedByLastName("Uno")
                .assignedByUsername("admin").assignedByEmail("admin@b.com").timestamp(TS).build();
    }

    public static EmailTemplateDefinition userRoleAssignedField(String field, String value) {
        var b = UserRoleAssignedEmailTemplate.builder().firstName("Ana").lastName("Lopez").username("ana")
                .email("a@b.com").roleName("Operador").assignedByFirstName("Admin").assignedByLastName("Uno")
                .assignedByUsername("admin").assignedByEmail("admin@b.com").timestamp(TS);
        switch (field) {
            case "firstName" -> b.firstName(value);
            case "lastName" -> b.lastName(value);
            case "username" -> b.username(value);
            case "email" -> b.email(value);
            case "roleName" -> b.roleName(value);
            case "assignedByFirstName" -> b.assignedByFirstName(value);
            case "assignedByLastName" -> b.assignedByLastName(value);
            case "assignedByUsername" -> b.assignedByUsername(value);
            case "assignedByEmail" -> b.assignedByEmail(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

    public static UserRoleAssignedAdminNotificationEmailTemplate userRoleAssignedAdmin() {
        return UserRoleAssignedAdminNotificationEmailTemplate.builder().userFirstName("Ana").userLastName("Lopez")
                .username("ana").userEmail("a@b.com").roleName("Operador").assignedByFirstName("Admin")
                .assignedByLastName("Uno").assignedByUsername("admin").assignedByEmail("admin@b.com").timestamp(TS)
                .build();
    }

    public static EmailTemplateDefinition userRoleAssignedAdminField(String field, String value) {
        var b = UserRoleAssignedAdminNotificationEmailTemplate.builder().userFirstName("Ana").userLastName("Lopez")
                .username("ana").userEmail("a@b.com").roleName("Operador").assignedByFirstName("Admin")
                .assignedByLastName("Uno").assignedByUsername("admin").assignedByEmail("admin@b.com").timestamp(TS);
        switch (field) {
            case "userFirstName" -> b.userFirstName(value);
            case "userLastName" -> b.userLastName(value);
            case "username" -> b.username(value);
            case "userEmail" -> b.userEmail(value);
            case "roleName" -> b.roleName(value);
            case "assignedByFirstName" -> b.assignedByFirstName(value);
            case "assignedByLastName" -> b.assignedByLastName(value);
            case "assignedByUsername" -> b.assignedByUsername(value);
            case "assignedByEmail" -> b.assignedByEmail(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

    public static UserStatusChangedEmailTemplate userStatusChanged() {
        return UserStatusChangedEmailTemplate.builder().firstName("Ana").lastName("Lopez").username("ana")
                .email("a@b.com").oldStatus("APPROVED").newStatus("PENDING").reason(null)
                .changedByFirstName("Admin").changedByLastName("Uno").changedByUsername("admin")
                .changedByEmail("admin@b.com").timestamp(TS).build();
    }

    public static EmailTemplateDefinition userStatusChangedField(String field, String value) {
        var b = UserStatusChangedEmailTemplate.builder().firstName("Ana").lastName("Lopez").username("ana")
                .email("a@b.com").oldStatus("APPROVED").newStatus("PENDING").reason("motivo")
                .changedByFirstName("Admin").changedByLastName("Uno").changedByUsername("admin")
                .changedByEmail("admin@b.com").timestamp(TS);
        switch (field) {
            case "firstName" -> b.firstName(value);
            case "lastName" -> b.lastName(value);
            case "username" -> b.username(value);
            case "email" -> b.email(value);
            case "oldStatus" -> b.oldStatus(value);
            case "newStatus" -> b.newStatus(value);
            case "changedByFirstName" -> b.changedByFirstName(value);
            case "changedByLastName" -> b.changedByLastName(value);
            case "changedByUsername" -> b.changedByUsername(value);
            case "changedByEmail" -> b.changedByEmail(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

    public static UserStatusChangedAdminNotificationEmailTemplate userStatusChangedAdmin() {
        return UserStatusChangedAdminNotificationEmailTemplate.builder().userFirstName("Ana").userLastName("Lopez")
                .username("ana").userEmail("a@b.com").oldStatus("APPROVED").newStatus("PENDING").reason(null)
                .changedByFirstName("Admin").changedByLastName("Uno").changedByUsername("admin")
                .changedByEmail("admin@b.com").timestamp(TS).build();
    }

    public static EmailTemplateDefinition userStatusChangedAdminField(String field, String value) {
        var b = UserStatusChangedAdminNotificationEmailTemplate.builder().userFirstName("Ana").userLastName("Lopez")
                .username("ana").userEmail("a@b.com").oldStatus("APPROVED").newStatus("PENDING").reason("motivo")
                .changedByFirstName("Admin").changedByLastName("Uno").changedByUsername("admin")
                .changedByEmail("admin@b.com").timestamp(TS);
        switch (field) {
            case "userFirstName" -> b.userFirstName(value);
            case "userLastName" -> b.userLastName(value);
            case "username" -> b.username(value);
            case "userEmail" -> b.userEmail(value);
            case "oldStatus" -> b.oldStatus(value);
            case "newStatus" -> b.newStatus(value);
            case "changedByFirstName" -> b.changedByFirstName(value);
            case "changedByLastName" -> b.changedByLastName(value);
            case "changedByUsername" -> b.changedByUsername(value);
            case "changedByEmail" -> b.changedByEmail(value);
            default -> throw new IllegalArgumentException(field);
        }
        return b.build();
    }

}
