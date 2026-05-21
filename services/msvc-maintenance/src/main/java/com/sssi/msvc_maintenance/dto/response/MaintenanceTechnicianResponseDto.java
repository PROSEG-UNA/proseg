package com.sssi.msvc_maintenance.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceTechnicianResponseDto {

    private UUID id;
    private String fullName;
    private String position;
    private String email;
    private String phone;
    private boolean leader;
}