package com.proseg.msvc_email.notificacion.template.impl;

import com.proseg.common.utils.DateUtils;
import com.proseg.msvc_email.notificacion.template.EmailTemplateDefinition;
import com.proseg.msvc_email.notificacion.util.TemplateValidator;
import com.proseg.msvc_email.utils.UserStatusTranslator;
import lombok.Builder;
import org.thymeleaf.context.Context;

import java.util.List;

@Builder
public class UserStatusChangedAdminNotificationEmailTemplate implements EmailTemplateDefinition {
    private final String userFirstName;
    private final String userLastName;
    private final String username;
    private final String userEmail;
    private final String oldStatus;
    private final String newStatus;
    private final long timestamp;

    private final String changedByFirstName;
    private final String changedByLastName;
    private final String changedByUsername;
    private final String changedByEmail;
    private final String reason;

    @Override
    public String getTemplateName() {
        return "user-status-changed-admin-notification-email";
    }

    @Override
    public Context buildContext() {
        validate();

        Context ctx = new Context();

        ctx.setVariable("userFirstName", userFirstName);
        ctx.setVariable("userLastName", userLastName);
        ctx.setVariable("username", username);
        ctx.setVariable("userEmail", userEmail);
        ctx.setVariable("oldStatus", UserStatusTranslator.translateStatus(oldStatus));
        ctx.setVariable("newStatus", UserStatusTranslator.translateStatus(newStatus));
        ctx.setVariable("timestamp", DateUtils.formatReadable(timestamp));
        ctx.setVariable("changedByFirstName", changedByFirstName);
        ctx.setVariable("changedByLastName", changedByLastName);
        ctx.setVariable("changedByUsername", changedByUsername);
        ctx.setVariable("changedByEmail", changedByEmail);
        ctx.setVariable("reason", reason != null ? reason : "Sin especificar");

        return ctx;
    }

    @Override
    public List<String> getInlineImages() {
        return List.of("flower.png");
    }

    private void validate() {
        String template = "UserStatusChangedAdminNotificationEmailTemplate";

        TemplateValidator.requireNotBlank(userFirstName,      "userFirstName",      template);
        TemplateValidator.requireNotBlank(userLastName,       "userLastName",       template);
        TemplateValidator.requireNotBlank(username,           "username",           template);
        TemplateValidator.requireNotBlank(userEmail,          "userEmail",          template);
        TemplateValidator.requireNotBlank(oldStatus,          "oldStatus",          template);
        TemplateValidator.requireNotBlank(newStatus,          "newStatus",          template);
        TemplateValidator.requireNotBlank(changedByFirstName, "changedByFirstName", template);
        TemplateValidator.requireNotBlank(changedByLastName,  "changedByLastName",  template);
        TemplateValidator.requireNotBlank(changedByUsername,  "changedByUsername",  template);
        TemplateValidator.requireNotBlank(changedByEmail,     "changedByEmail",     template);

        if (timestamp <= 0) {
            throw new IllegalArgumentException(
                    template + ": 'timestamp' es requerido y debe ser válido"
            );
        }
    }
}
