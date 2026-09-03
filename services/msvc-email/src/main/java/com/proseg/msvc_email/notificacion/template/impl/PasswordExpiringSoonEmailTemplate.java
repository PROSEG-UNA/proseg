package com.proseg.msvc_email.notificacion.template.impl;

import com.proseg.common.utils.DateUtils;
import com.proseg.msvc_email.notificacion.template.EmailTemplateDefinition;
import com.proseg.msvc_email.notificacion.util.TemplateValidator;
import lombok.Builder;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.util.List;

@Builder
public class PasswordExpiringSoonEmailTemplate implements EmailTemplateDefinition {

    private final String firstName;
    private final long daysRemaining;
    private final Instant expiresAt;
    private final String loginUrl;
    private final long timestamp;

    @Override
    public String getTemplateName() {
        return "password-expiring-soon-email";
    }

    @Override
    public Context buildContext() {

        validate();

        Context ctx = new Context();
        ctx.setVariable("firstName", firstName);
        ctx.setVariable("daysRemaining", daysRemaining);
        ctx.setVariable("expiresAt", DateUtils.formatReadable(expiresAt.toEpochMilli()));
        ctx.setVariable("loginUrl", loginUrl);
        ctx.setVariable("timestamp", DateUtils.formatReadable(timestamp));

        return ctx;
    }

    @Override
    public List<String> getInlineImages() {
        return List.of("flower.png");
    }

    private void validate() {

        TemplateValidator.requireNotBlank(
                firstName,
                "firstName",
                getTemplateName()
        );

        TemplateValidator.requireNotBlank(
                loginUrl,
                "loginUrl",
                getTemplateName()
        );

        if (daysRemaining < 0) {
            throw new IllegalArgumentException("daysRemaining inválido");
        }

        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt es requerido");
        }

        if (timestamp <= 0) {
            throw new IllegalArgumentException("timestamp inválido");
        }
    }
}