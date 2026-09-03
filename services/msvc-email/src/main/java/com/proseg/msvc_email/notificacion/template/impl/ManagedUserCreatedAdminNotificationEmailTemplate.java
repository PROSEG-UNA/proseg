package com.proseg.msvc_email.notificacion.template.impl;

import com.proseg.common.utils.DateUtils;
import com.proseg.msvc_email.notificacion.template.EmailTemplateDefinition;
import com.proseg.msvc_email.notificacion.util.TemplateValidator;
import lombok.Builder;
import org.thymeleaf.context.Context;

import java.util.List;

@Builder
public class ManagedUserCreatedAdminNotificationEmailTemplate implements EmailTemplateDefinition {

    private final String newUserFirstName;
    private final String newUserLastName;
    private final String newUsername;
    private final String newUserEmail;
    private final String adminFirstName;
    private final String adminLastName;
    private final String adminUsername;
    private final String adminEmail;
    private final long timestamp;

    @Override
    public String getTemplateName() {
        return "managed-user-created-admin-notification-email";
    }

    @Override
    public Context buildContext() {
        validate();

        Context ctx = new Context();

        ctx.setVariable("newUserFirstName", newUserFirstName);
        ctx.setVariable("newUserLastName", newUserLastName);
        ctx.setVariable("newUsername", newUsername);
        ctx.setVariable("newUserEmail", newUserEmail);
        ctx.setVariable("adminFirstName", adminFirstName);
        ctx.setVariable("adminLastName", adminLastName);
        ctx.setVariable("adminUsername", adminUsername);
        ctx.setVariable("adminEmail", adminEmail);
        ctx.setVariable("timestamp", DateUtils.formatReadable(timestamp));

        return ctx;
    }

    @Override
    public List<String> getInlineImages() {
        return List.of("flower.png");
    }

    private void validate() {
        String template = "ManagedUserCreatedAdminNotificationEmailTemplate";

        TemplateValidator.requireNotBlank(
                newUserFirstName,
                "newUserFirstName",
                template
        );

        TemplateValidator.requireNotBlank(
                newUserLastName,
                "newUserLastName",
                template
        );

        TemplateValidator.requireNotBlank(
                newUsername,
                "newUsername",
                template
        );

        TemplateValidator.requireNotBlank(
                newUserEmail,
                "newUserEmail",
                template
        );

        TemplateValidator.requireNotBlank(
                adminFirstName,
                "adminFirstName",
                template
        );

        TemplateValidator.requireNotBlank(
                adminLastName,
                "adminLastName",
                template
        );

        TemplateValidator.requireNotBlank(
                adminUsername,
                "adminUsername",
                template
        );

        TemplateValidator.requireNotBlank(
                adminEmail,
                "adminEmail",
                template
        );

        if (timestamp <= 0) {
            throw new IllegalArgumentException(
                    template + ": 'timestamp' es requerido y debe ser valido"
            );
        }
    }
}