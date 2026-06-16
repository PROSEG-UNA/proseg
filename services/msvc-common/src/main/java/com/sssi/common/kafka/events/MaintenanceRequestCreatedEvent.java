package com.sssi.common.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRequestCreatedEvent {

    private List<String> emails;
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
