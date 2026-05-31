package com.sssi.msvc_maintenance.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyResponseDto {

    private UUID id;
    private String name;
    private String legalId;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private List<String> keycloakUserIds;
    private List<UserCompanyResponseDto> userCompanies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}