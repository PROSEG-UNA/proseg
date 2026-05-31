package com.sssi.msvc_maintenance.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCompanyResponseDto {

    private UUID id;
    private String keycloakUserId;
    private String userEmail;
    private CompanyResponseDto company;
}