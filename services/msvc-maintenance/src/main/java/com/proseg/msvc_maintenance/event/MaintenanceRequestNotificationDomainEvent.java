package com.proseg.msvc_maintenance.event;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record MaintenanceRequestNotificationDomainEvent(
        UUID requestId,
        String actionType,
        String actionLabel,
        String detail,
        String companyName,
        String legalId,
        String description,
        String cancellationReason,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        UUID campusId,
        UUID buildingId,
        List<String> changedFields,
        List<String> recipientEmails,
        Long timestamp
) {}
