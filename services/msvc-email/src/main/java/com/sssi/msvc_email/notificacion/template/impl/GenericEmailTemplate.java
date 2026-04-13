package com.sssi.msvc_email.notificacion.template.impl;

import com.sssi.msvc_email.notificacion.exception.EmailTemplateException;
import com.sssi.msvc_email.notificacion.template.EmailTemplateDefinition;
import lombok.Builder;
import org.thymeleaf.context.Context;

import java.util.List;

/**
 * Generic-purpose email template.
 * Renders: generic-email.html
 * Required fields : userName, emailTitle, emailContent
 * Inline images   : logo-4.png
 * Use this template when the content doesn't justify a dedicated
 * template class. For recurring or structured scenarios (welcome,
 * password reset, etc.) prefer a purpose-built implementation.
 */
@Builder
public class GenericEmailTemplate implements EmailTemplateDefinition {

    /**
     * Recipient's display name — shown in the greeting line.
     */
    private final String userName;

    /**
     * Title rendered in the email header bar.
     */
    private final String emailTitle;

    /**
     * Main body content.
     * Supports safe HTML (rendered via th:utext) so callers can pass
     * simple inline tags like {@code <br>}, {@code <b>}, {@code <a>}.
     * Never pass raw user input here without prior sanitization.
     */
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