package com.proseg.msvc_email.notificacion.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CompanyResponseDto {

    private UUID id;
    private String name;
    private String legalId;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private List<String> keycloakUserIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}