package com.sssi.msvc_email.notificacion.template.impl;

import com.sssi.common.utils.DateUtils;
import com.sssi.msvc_email.notificacion.template.EmailTemplateDefinition;
import com.sssi.msvc_email.notificacion.util.TemplateValidator;
import lombok.Builder;
import org.thymeleaf.context.Context;

import java.util.List;

@Builder
public class UserRoleAssignedEmailTemplate implements EmailTemplateDefinition {
    private final String firstName;
    private final String lastName;
    private final String username;
    private final String email;
    private final String roleName;
    private final long timestamp;

    private final String assignedByFirstName;
    private final String assignedByLastName;
    private final String assignedByUsername;
    private final String assignedByEmail;

    @Override
    public String getTemplateName() {
        return "user-role-assigned-email";
    }

    @Override
    public Context buildContext() {
        validate();

        Context ctx = new Context();

        ctx.setVariable("firstName", firstName);
        ctx.setVariable("lastName", lastName);
        ctx.setVariable("username", username);
        ctx.setVariable("email", email);
        ctx.setVariable("roleName", roleName);
        ctx.setVariable("timestamp", DateUtils.formatReadable(timestamp));
        ctx.setVariable("assignedByFirstName", assignedByFirstName);
        ctx.setVariable("assignedByLastName", assignedByLastName);
        ctx.setVariable("assignedByUsername", assignedByUsername);
        ctx.setVariable("assignedByEmail", assignedByEmail);

        return ctx;
    }

    @Override
    public List<String> getInlineImages() {
        return List.of("flower.png");
    }

    private void validate() {
        String template = "UserRoleAssignedEmailTemplate";

        TemplateValidator.requireNotBlank(firstName,          "firstName",          template);
        TemplateValidator.requireNotBlank(lastName,           "lastName",           template);
        TemplateValidator.requireNotBlank(username,           "username",           template);
        TemplateValidator.requireNotBlank(email,              "email",              template);
        TemplateValidator.requireNotBlank(roleName,           "roleName",           template);
        TemplateValidator.requireNotBlank(assignedByFirstName, "assignedByFirstName", template);
        TemplateValidator.requireNotBlank(assignedByLastName,  "assignedByLastName",  template);
        TemplateValidator.requireNotBlank(assignedByUsername,  "assignedByUsername",  template);
        TemplateValidator.requireNotBlank(assignedByEmail,     "assignedByEmail",     template);

        if (timestamp <= 0) {
            throw new IllegalArgumentException(
                    template + ": 'timestamp' es requerido y debe ser válido"
            );
        }
    }
}
