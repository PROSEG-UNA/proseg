package com.sssi.msvc_maintenance.event;

import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record MaintenanceRequestCreatedDomainEvent(
        List<String> emails,
        String companyName,
        String legalId,
        String description,
        MaintenanceStatus status,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        UUID campusId,
        UUID buildingId,
        List<String> technicianKeycloakIds,
        String responsibleKeycloakId
) {}
