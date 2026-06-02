package com.sssi.msvc_maintenance.dto.response;

import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private String description;
    private String email;
    private MaintenanceStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private UUID campusId;
    private UUID buildingId;
    private List<UserCompanyResponseDto> assignedTechnicians;
    private UserCompanyResponseDto leaderUserCompany;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}