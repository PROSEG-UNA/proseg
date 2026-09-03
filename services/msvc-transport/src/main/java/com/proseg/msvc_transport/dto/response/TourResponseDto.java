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
public class TourResponseDto {
    private UUID id;
    private String name;
    private String externalNumber;
    private String origin;
    private String destination;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private Integer priority;
    private Integer passengers;
    private String requestedVehicle;
    private String requestedVehicleType;
    private String requestedDriver;
    private String responsible;
    private String executingUnit;
    private String modality;
    private Integer durationDays;
    private String departureTime;
    private String returnTime;
    private String observations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
