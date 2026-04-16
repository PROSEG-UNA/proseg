package com.sssi.msvc_email.notificacion.template.impl;

import com.sssi.msvc_email.notificacion.exception.EmailTemplateException;
import com.sssi.msvc_email.notificacion.template.EmailTemplateDefinition;
import com.sssi.msvc_email.notificacion.util.TemplateValidator;
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
        String template = "GenericEmailTemplate";

        TemplateValidator.requireNotBlank(userName, "userName", template);
        TemplateValidator.requireNotBlank(emailTitle, "emailTitle", template);
        TemplateValidator.requireNotBlank(emailContent, "emailContent", template);
    }
}