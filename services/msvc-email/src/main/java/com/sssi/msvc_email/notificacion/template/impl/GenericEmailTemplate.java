package com.sssi.msvc_email.notificacion.template.impl;

import com.sssi.msvc_email.notificacion.exception.EmailTemplateException;
import com.sssi.msvc_email.notificacion.template.EmailTemplateDefinition;
import lombok.Builder;
import org.thymeleaf.context.Context;

import java.util.List;

@Builder
public class GenericEmailTemplate implements EmailTemplateDefinition {

    private final String userName;

    private final String emailTitle;

    private final String emailContent;

    @Override
    public String getTemplateName() {
        return "generic-email";
    }

    @Override
    public Context buildContext() {
        validate();

        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        ctx.setVariable("emailTitle", emailTitle);
        ctx.setVariable("emailContent", emailContent);
        return ctx;
    }

    @Override
    public List<String> getInlineImages() {
        return List.of("logo-4.png");
    }

    private void validate() {
        if (userName == null || userName.isBlank()) {
            throw new EmailTemplateException(
                    "GenericEmailTemplate: 'userName' es requerido");
        }
        if (emailTitle == null || emailTitle.isBlank()) {
            throw new EmailTemplateException(
                    "GenericEmailTemplate: 'emailTitle' es requerido");
        }
        if (emailContent == null || emailContent.isBlank()) {
            throw new EmailTemplateException(
                    "GenericEmailTemplate: 'emailContent' es requerido");
        }
    }
}