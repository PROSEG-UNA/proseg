package com.proseg.msvc_email.notificacion.template.impl;

import com.proseg.common.utils.DateUtils;
import com.proseg.msvc_email.notificacion.template.EmailTemplateDefinition;
import com.proseg.msvc_email.notificacion.util.TemplateValidator;
import lombok.Builder;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.UUID;

@Builder
public class TicketNotificationEmailTemplate implements EmailTemplateDefinition {

    private final UUID ticketId;
    private final String actionType;
    private final String actionLabel;
    private final String actorName;
    private final String ticketTitle;
    private final String ticketDescription;
    private final String status;
    private final String priority;
    private final String campusName;
    private final String buildingName;
    private final String detail;
    private final List<String> changedFields;
    private final long timestamp;

    @Override
    public String getTemplateName() {
        return "ticket-notification-email";
    }

    @Override
    public Context buildContext() {
        validate();

        Context ctx = new Context();
        ctx.setVariable("ticketId", ticketId != null ? ticketId.toString() : null);
        ctx.setVariable("actionType", actionType);
        ctx.setVariable("actionLabel", actionLabel);
        ctx.setVariable("actorName", actorName);
        ctx.setVariable("ticketTitle", ticketTitle);
        ctx.setVariable("ticketDescription", ticketDescription);
        ctx.setVariable("status", status);
        ctx.setVariable("priority", priority);
        ctx.setVariable("campusName", campusName);
        ctx.setVariable("buildingName", buildingName);
        ctx.setVariable("detail", detail);
        ctx.setVariable("changedFields", changedFields == null ? List.of() : changedFields);
        ctx.setVariable("timestamp", DateUtils.formatReadable(timestamp));
        return ctx;
    }

    @Override
    public List<String> getInlineImages() {
        return List.of("flower.png");
    }

    private void validate() {
        TemplateValidator.requireNotBlank(actionLabel, "actionLabel", getTemplateName());
        TemplateValidator.requireNotBlank(ticketTitle, "ticketTitle", getTemplateName());
        if (timestamp <= 0) {
            throw new IllegalArgumentException("timestamp invalido en " + getTemplateName());
        }
    }
}
