package com.sssi.msvc_maintenance.dto.response;

import com.sssi.msvc_maintenance.entity.enums.MaintenancePriority;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRequestResponseDto {

    private UUID id;
    private CompanyResponseDto company;
    private UUID assetId;
    private String title;
    private String description;
    private MaintenanceStatus status;
    private MaintenancePriority priority;
    private LocalDate scheduledDate;
    private String observations;
    private List<MaintenanceTechnicianResponseDto> technicians;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}