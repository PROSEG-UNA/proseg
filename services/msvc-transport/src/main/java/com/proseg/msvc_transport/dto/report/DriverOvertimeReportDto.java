package com.proseg.msvc_transport.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverOvertimeReportDto {
    private UUID driverId;
    private String driverName;
    private double ordinaryHours;
    private double surplusHours;
    private double overtimeHours;
    private int totalAssignedTours;
}
