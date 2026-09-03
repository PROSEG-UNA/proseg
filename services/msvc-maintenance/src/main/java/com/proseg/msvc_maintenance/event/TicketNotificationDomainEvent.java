package com.proseg.msvc_maintenance.event;

import java.util.List;
import java.util.UUID;

public record TicketNotificationDomainEvent(
        UUID ticketId,
        String actionType,
        String actionLabel,
        String actorId,
        String actorName,
        String ticketTitle,
        String ticketDescription,
        String status,
        String priority,
        String campusName,
        String buildingName,
        String detail,
        List<String> changedFields,
        List<String> recipientEmails,
        List<String> extraEmails,
        Long timestamp
) {}
