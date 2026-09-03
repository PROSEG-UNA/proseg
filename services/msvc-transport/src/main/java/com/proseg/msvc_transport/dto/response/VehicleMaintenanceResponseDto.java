package com.proseg.msvc_transport.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleMaintenanceResponseDto {
    private UUID id;
    private UUID vehicleId;
    private String vehiclePlate;
    private String title;
    private String type;
    private LocalDate scheduledDate;
    private BigDecimal cost;
    private String status;
    private String notes;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
