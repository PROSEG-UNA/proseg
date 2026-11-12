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
public class TourControlReportDto {
    private int year;
    private int month;
    private int businessDays;
    private int weekdays;
    private List<Driver> drivers;
    private List<Entry> entries;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Driver {
        private String name;
        private Integer businessDays;
        private double ordinaryStart;
        private double ordinaryEnd;
        private Double surplusHours;
        private Double overtimeHours;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Entry {
        private String section;
        private LocalDate date;
        private Integer driverIndex;
        private String driver;
        private String plate;
        private String destination;
        private String responsible;
        private LocalDate departureDate;
        private LocalDate returnDate;
        private Double start;
        private Double end;
        private Integer passengers;
        private String number;
        private Integer priority;
        private Double hours;
    }
}
