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
public class DriverResponseDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private String documentId;
    private String licenseNumber;
    private String phone;
    private String email;
    private String status;
    private String restrictions;
    private boolean availability;
    private double accumulatedHours;
    private double freeWeekendsCount;
    private double overtimeHours;
    private double surplusHours;
    private double jornadaHoursPerDay;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
