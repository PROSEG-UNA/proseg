package com.sssi.common.kafka.events;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRequestCancelledEvent {

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
    private String leaderName;

    private List<String> emails;

    private Long timestamp;
}