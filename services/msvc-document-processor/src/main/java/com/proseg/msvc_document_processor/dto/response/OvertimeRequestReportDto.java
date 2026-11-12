package com.proseg.msvc_document_processor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimeRequestReportDto {
    private int year;
    private int month;
    private int businessDays;
    private double surplusCapPerDriver;
    private List<DriverBlock> drivers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DriverBlock {
        private String name;
        private Integer businessDays;
        private Double surplusCap;
        private double surplusHours;
        private double overtimeHours;
        private List<TourBlock> tours;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TourBlock {
        private String number;
        private Integer priority;
        private String executingUnit;
        private String responsible;
        private List<TourDay> days;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TourDay {
        private LocalDate date;
        private double start;
        private double end;
        private double surplusHours;
        private double overtimeHours;
    }
}
