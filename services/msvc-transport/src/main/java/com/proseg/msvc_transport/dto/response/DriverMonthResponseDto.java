package com.proseg.msvc_transport.dto.response;

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
public class DriverMonthResponseDto {
    private UUID driverId;
    private String driverName;
    private Integer year;
    private Integer month;
    private Double ordinaryHoursAccrued;
    private Double surplusHoursAccrued;
    private Double overtimeHoursAccrued;
    private Integer weekendsWorked;
    private boolean hasRecord;
}
