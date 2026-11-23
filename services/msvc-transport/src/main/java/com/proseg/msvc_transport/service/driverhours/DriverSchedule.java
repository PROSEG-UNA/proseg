package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.Driver;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

public record DriverSchedule(OrdinaryWindow base, Map<LocalDate, OrdinaryWindow> overrides) {

    public DriverSchedule {
        overrides = overrides == null ? Map.of() : Map.copyOf(overrides);
    }

    public static DriverSchedule of(Driver driver) {
        return new DriverSchedule(OrdinaryWindow.of(driver), Map.of());
    }

    public static DriverSchedule uniform(OrdinaryWindow window) {
        return new DriverSchedule(window, Map.of());
    }

    public OrdinaryWindow windowOn(LocalDate date) {
        return date == null ? base : overrides.getOrDefault(date, base);
    }

    public OrdinaryWindow windowOn(LocalDate departure, LocalDate date) {
        if (departure == null || date == null || YearMonth.from(date).equals(YearMonth.from(departure))) {
            return windowOn(date);
        }
        return windowOn(departure);
    }
}
