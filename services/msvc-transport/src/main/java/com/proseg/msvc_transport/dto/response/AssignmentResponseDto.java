package com.proseg.msvc_transport.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentResponseDto {
    private UUID id;
    private UUID driverId;
    private String driverName;
    private UUID vehicleId;
    private String vehiclePlate;
    private UUID tourId;
    private String tourName;
    private String status;
    private String notes;
    private boolean isContracted;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
