package com.sssi.msvc_email.notificacion.template.impl;

import com.sssi.common.utils.DateUtils;
import com.sssi.msvc_email.notificacion.template.EmailTemplateDefinition;
import com.sssi.msvc_email.notificacion.util.TemplateValidator;
import lombok.Builder;
import org.thymeleaf.context.Context;

import java.util.List;

@Builder
public class UserPasswordChangedEmailTemplate implements EmailTemplateDefinition {

    private final String firstName;
    private final String lastName;
    private final String email;
    private final String loginUrl;
    private final long timestamp;

    @Override
    public String getTemplateName() {
        return "user-password-changed-email";
    }

    @Override
    public Context buildContext() {

        validate();

        Context ctx = new Context();
        ctx.setVariable("firstName", firstName);
        ctx.setVariable("lastName", lastName);
        ctx.setVariable("email", email);
        ctx.setVariable("loginUrl", loginUrl);
        ctx.setVariable("timestamp", DateUtils.formatReadable(timestamp));

        return ctx;
    }

    @Override
    public List<String> getInlineImages() {
        return List.of("flower.png");
    }

    private void validate() {

        TemplateValidator.requireNotBlank(firstName, "firstName", getTemplateName());
        TemplateValidator.requireNotBlank(lastName, "lastName", getTemplateName());
        TemplateValidator.requireNotBlank(email, "email", getTemplateName());
        TemplateValidator.requireNotBlank(loginUrl, "loginUrl", getTemplateName());

        if (timestamp <= 0) {
            throw new IllegalArgumentException("timestamp inválido");
        }
    }
}