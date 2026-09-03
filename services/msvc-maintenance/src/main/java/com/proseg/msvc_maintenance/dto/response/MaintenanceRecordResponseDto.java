package com.proseg.msvc_maintenance.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRecordResponseDto {

    private UUID id;
    private UUID registerId;
    private UUID assetId;
    private CompanyResponseDto company;
    private String keycloakUserId;
    private String userEmail;
    private String description;
    private LocalDateTime createdAt;
}
