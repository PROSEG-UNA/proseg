package com.proseg.msvc_email.notificacion.template.impl;

import com.proseg.common.utils.DateUtils;
import com.proseg.msvc_email.notificacion.template.EmailTemplateDefinition;
import com.proseg.msvc_email.notificacion.util.TemplateValidator;
import com.proseg.msvc_email.utils.UserStatusTranslator;
import lombok.Builder;
import org.thymeleaf.context.Context;

import java.util.List;

@Builder
public class UserStatusChangedEmailTemplate implements EmailTemplateDefinition {
    private final String firstName;
    private final String lastName;
    private final String username;
    private final String email;
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
        return "user-status-changed-email";
    }

    @Override
    public Context buildContext() {
        validate();

        Context ctx = new Context();

        ctx.setVariable("firstName", firstName);
        ctx.setVariable("lastName", lastName);
        ctx.setVariable("username", username);
        ctx.setVariable("email", email);
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
        String template = "UserStatusChangedEmailTemplate";

        TemplateValidator.requireNotBlank(firstName,          "firstName",          template);
        TemplateValidator.requireNotBlank(lastName,           "lastName",           template);
        TemplateValidator.requireNotBlank(username,           "username",           template);
        TemplateValidator.requireNotBlank(email,              "email",              template);
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
