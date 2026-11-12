package com.proseg.msvc_transport.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
        private UUID id;
        private String name;
        private String organizationalUnit;
        private String status;
        private int businessDays;
        private double surplusCap;
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
        private UUID id;
        private String number;
        private Integer priority;
        private String executingUnit;
        private String responsible;
        private LocalDate departureDate;
        private LocalDate returnDate;
        private String destination;
        private String vehicle;
        private Integer passengers;
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
        private double ordinaryHours;
        private double surplusHours;
        private double overtimeHours;
    }
}
