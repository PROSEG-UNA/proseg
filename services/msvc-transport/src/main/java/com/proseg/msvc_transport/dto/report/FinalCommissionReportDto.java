package com.proseg.msvc_transport.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalCommissionReportDto {
    private int totalTours;
    private int assignedTours;
    private int rejectedTours;
    private double jornadas;
    private double totalSurplusHours;
    private double totalOvertimeHours;
    private int contractingCount;
    private int loansCount;
    private List<FinalCommissionRejectedTourDto> rejectedToursList;
}
