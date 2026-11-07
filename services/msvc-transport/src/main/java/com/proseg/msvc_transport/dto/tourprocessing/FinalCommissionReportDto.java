package com.proseg.msvc_transport.dto.tourprocessing;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalCommissionReportDto {
    private int year;
    private int month;
    private int businessDays;
    private double surplusCapPerDriver;
    private int activeDrivers;
    private int activeDriversBusinessDays;
    private double activeDriversSurplusCap;
    private List<Tour> tours;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Tour {
        private String number;
        private Integer passengers;
        private String executingUnit;
        private String responsible;
        private String destination;
        private Integer durationDays;
        private Integer priority;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDate departureDate;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalTime departureTime;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDate returnDate;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalTime returnTime;
        private String category;
        private int vehicles;
        private double ordinaryStart;
        private double ordinaryEnd;
        private List<Day> days;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Day {
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDate date;
        private double start;
        private double end;
    }
}
