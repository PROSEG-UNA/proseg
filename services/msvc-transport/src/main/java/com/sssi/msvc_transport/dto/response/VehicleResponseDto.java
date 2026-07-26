package com.sssi.msvc_transport.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponseDto {
    private UUID id;
    private String plate;
    private String brand;
    private String model;
    private Integer year;
    private Integer capacity;
    private String status;
    private String type;
    private boolean available;
    private boolean underMaintenance;
    private LocalDate lastMaintenanceDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
