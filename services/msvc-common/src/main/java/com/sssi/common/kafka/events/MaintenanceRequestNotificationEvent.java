package com.sssi.common.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRequestNotificationEvent {

    private UUID requestId;
    private String actionType;
    private String actionLabel;
    private String detail;
    private String companyName;
    private String legalId;
    private String description;
    private String cancellationReason;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String campusName;
    private String buildingName;
    private List<String> changedFields;
    private List<String> recipientEmails;
    private List<String> extraEmails;
    private Long timestamp;
}
