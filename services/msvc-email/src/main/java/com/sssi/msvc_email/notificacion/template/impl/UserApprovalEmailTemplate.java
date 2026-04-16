package com.sssi.msvc_email.notificacion.template.impl;

import com.sssi.common.utils.DateUtils;
import com.sssi.msvc_email.notificacion.template.EmailTemplateDefinition;
import com.sssi.msvc_email.notificacion.util.TemplateValidator;
import lombok.Builder;
import org.thymeleaf.context.Context;

import java.util.List;

@Builder
public class UserApprovalEmailTemplate implements EmailTemplateDefinition {

    private final String firstName;
    private final String lastName;
    private final String username;
    private final String email;
    private final String approvalUrl;
    private final long timestamp;

    @Override
    public String getTemplateName() {
        return "user-approval-email";
    }

    @Override
    public Context buildContext() {
        validate();

        Context ctx = new Context();

        ctx.setVariable("firstName", firstName);
        ctx.setVariable("lastName", lastName);
        ctx.setVariable("username", username);
        ctx.setVariable("email", email);
        ctx.setVariable("approvalUrl", approvalUrl);
        String formattedDate = DateUtils.formatReadable(timestamp);
        ctx.setVariable("timestamp", formattedDate);

        return ctx;
    }

    @Override
    public List<String> getInlineImages() {
        return List.of("logo-4.png");
    }

    private void validate() {
        String template = "UserApprovalEmailTemplate";

        TemplateValidator.requireNotBlank(firstName, "firstName", template);
        TemplateValidator.requireNotBlank(lastName, "lastName", template);
        TemplateValidator.requireNotBlank(username, "username", template);
        TemplateValidator.requireNotBlank(email, "email", template);
        TemplateValidator.requireNotBlank(approvalUrl, "approvalUrl", template);
        if (timestamp <= 0) {
            throw new IllegalArgumentException(
                    template + ": 'timestamp' es requerido y debe ser válido"
            );
        }
    }
}