package com.proseg.msvc_transport.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverMonthAccrualRequestDto {

    @Builder.Default
    private Double ordinaryHoursDelta = 0D;

    @Builder.Default
    private Double surplusHoursDelta = 0D;

    @Builder.Default
    private Double overtimeHoursDelta = 0D;

    @Builder.Default
    private Integer weekendsWorkedDelta = 0;
}
