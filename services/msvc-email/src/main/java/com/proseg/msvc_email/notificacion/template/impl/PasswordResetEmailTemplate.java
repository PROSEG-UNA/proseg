package com.proseg.msvc_email.notificacion.template.impl;

import com.proseg.common.utils.DateUtils;
import com.proseg.msvc_email.notificacion.template.EmailTemplateDefinition;
import com.proseg.msvc_email.notificacion.util.TemplateValidator;
import lombok.Builder;
import org.thymeleaf.context.Context;

import java.util.List;

@Builder
public class PasswordResetEmailTemplate implements EmailTemplateDefinition {

    private final String firstName;
    private final String email;
    private final String resetPasswordUrl;
    private final long timestamp;

    @Override
    public String getTemplateName() {
        return "password-reset-email";
    }

    @Override
    public Context buildContext() {
        validate();

        Context ctx = new Context();
        ctx.setVariable("firstName", firstName);
        ctx.setVariable("email", email);
        ctx.setVariable("resetPasswordUrl", resetPasswordUrl);
        ctx.setVariable("timestamp", DateUtils.formatReadable(timestamp));
        return ctx;
    }

    @Override
    public List<String> getInlineImages() {
        return List.of("flower.png");
    }

    private void validate() {
        String template = "PasswordResetEmailTemplate";
        TemplateValidator.requireNotBlank(firstName, "firstName", template);
        TemplateValidator.requireNotBlank(email, "email", template);
        TemplateValidator.requireNotBlank(resetPasswordUrl, "resetPasswordUrl", template);

        if (timestamp <= 0) {
            throw new IllegalArgumentException(
                    template + ": 'timestamp' es requerido y debe ser valido"
            );
        }
    }
}