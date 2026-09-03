package com.proseg.common.kafka.events;

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
public class MaintenanceRequestCreatedEvent {

    private UUID requestId;
    private List<String> emails;
    private List<String> extraEmails;
    private String companyName;
    private String legalId;
    private String description;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String campusName;
    private String buildingName;
    private List<String> technicianNames;
    private String responsibleName;
    private Long timestamp;
}
