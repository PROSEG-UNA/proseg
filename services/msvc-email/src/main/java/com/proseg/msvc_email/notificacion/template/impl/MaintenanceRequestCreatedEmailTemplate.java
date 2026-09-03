package com.proseg.msvc_email.notificacion.template.impl;

import com.proseg.common.utils.DateUtils;
import com.proseg.msvc_email.notificacion.template.EmailTemplateDefinition;
import com.proseg.msvc_email.notificacion.util.TemplateValidator;
import lombok.Builder;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Builder
public class MaintenanceRequestCreatedEmailTemplate implements EmailTemplateDefinition {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final String companyName;
    private final String legalId;
    private final String description;
    private final String status;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String campusName;
    private final String buildingName;
    private final List<String> technicianNames;
    private final String responsibleName;
    private final long timestamp;

    @Override
    public String getTemplateName() {
        return "maintenance-request-created-email";
    }

    @Override
    public Context buildContext() {
        validate();

        Context ctx = new Context();
        ctx.setVariable("companyName",     companyName);
        ctx.setVariable("legalId",         legalId);
        ctx.setVariable("description",     description);
        ctx.setVariable("status",          status);
        ctx.setVariable("startDate",       formatDate(startDate));
        ctx.setVariable("endDate",         formatDate(endDate));
        ctx.setVariable("startTime",       formatTime(startTime));
        ctx.setVariable("endTime",         formatTime(endTime));
        ctx.setVariable("campusName",      campusName);
        ctx.setVariable("buildingName",    buildingName);
        ctx.setVariable("technicianNames", technicianNames == null ? List.of() : technicianNames);
        ctx.setVariable("responsibleName",    responsibleName);
        ctx.setVariable("timestamp",       DateUtils.formatReadable(timestamp));
        return ctx;
    }

    @Override
    public List<String> getInlineImages() {
        return List.of("flower.png");
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : null;
    }

    private String formatTime(LocalTime time) {
        return time != null ? time.format(TIME_FORMATTER) : null;
    }

    private void validate() {
        TemplateValidator.requireNotBlank(companyName, "companyName", getTemplateName());
        TemplateValidator.requireNotBlank(status,      "status",      getTemplateName());
        if (timestamp <= 0) {
            throw new IllegalArgumentException("timestamp inválido en " + getTemplateName());
        }
    }
}
