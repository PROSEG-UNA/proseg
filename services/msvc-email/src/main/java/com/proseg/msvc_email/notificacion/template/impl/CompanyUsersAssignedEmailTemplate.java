package com.proseg.msvc_email.notificacion.template.impl;

import com.proseg.common.utils.DateUtils;
import com.proseg.msvc_email.notificacion.template.EmailTemplateDefinition;
import com.proseg.msvc_email.notificacion.util.TemplateValidator;
import lombok.Builder;
import org.thymeleaf.context.Context;

import java.util.List;

@Builder
public class CompanyUsersAssignedEmailTemplate implements EmailTemplateDefinition {

    private final String firstName;
    private final String email;
    private final String companyName;
    private final String legalId;
    private final String contactEmail;
    private final String contactPhone;
    private final String address;
    private final String loginUrl;
    private final long timestamp;

    @Override
    public String getTemplateName() {
        return "company-users-assigned-email";
    }

    @Override
    public Context buildContext() {
        validate();

        Context ctx = new Context();
        ctx.setVariable("firstName",    firstName);
        ctx.setVariable("email",        email);
        ctx.setVariable("companyName",  companyName);
        ctx.setVariable("legalId",      legalId);
        ctx.setVariable("contactEmail", contactEmail);
        ctx.setVariable("contactPhone", contactPhone);
        ctx.setVariable("address",      address);
        ctx.setVariable("loginUrl",     loginUrl);
        ctx.setVariable("timestamp",    DateUtils.formatReadable(timestamp));
        return ctx;
    }

    @Override
    public List<String> getInlineImages() {
        return List.of("flower.png");
    }

    private void validate() {
        TemplateValidator.requireNotBlank(firstName,   "firstName",   getTemplateName());
        TemplateValidator.requireNotBlank(email,       "email",       getTemplateName());
        TemplateValidator.requireNotBlank(companyName, "companyName", getTemplateName());
        TemplateValidator.requireNotBlank(loginUrl,    "loginUrl",    getTemplateName());
        if (timestamp <= 0) {
            throw new IllegalArgumentException("timestamp inválido en " + getTemplateName());
        }
    }
}