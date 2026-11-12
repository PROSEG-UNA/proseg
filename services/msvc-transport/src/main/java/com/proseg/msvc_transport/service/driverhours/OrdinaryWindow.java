package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.Driver;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record OrdinaryWindow(double start, double end) {

    public static final OrdinaryWindow STANDARD = new OrdinaryWindow(DriverHoursRules.ORDINARY_START, DriverHoursRules.ORDINARY_END);
    public static final OrdinaryWindow AFTERNOON = new OrdinaryWindow(DriverHoursRules.AFTERNOON_ORDINARY_START, DriverHoursRules.AFTERNOON_ORDINARY_END);
    public static final List<OrdinaryWindow> ALLOWED = List.of(STANDARD, AFTERNOON);

    public static OrdinaryWindow of(Driver driver) {
        if (driver == null || driver.getOrdinaryStart() == null || driver.getOrdinaryEnd() == null) {
            return STANDARD;
        }
        return new OrdinaryWindow(DriverHoursRules.hourOf(driver.getOrdinaryStart()), DriverHoursRules.hourOf(driver.getOrdinaryEnd()));
    }

    public static Optional<OrdinaryWindow> allowed(LocalTime start, LocalTime end) {
        return ALLOWED.stream()
                .filter(window -> window.startTime().equals(start) && window.endTime().equals(end))
                .findFirst();
    }

    public static String allowedDescription() {
        return ALLOWED.stream().map(OrdinaryWindow::label).collect(Collectors.joining(" o "));
    }

    public LocalTime startTime() {
        return DriverHoursRules.timeOf(start);
    }

    public LocalTime endTime() {
        return DriverHoursRules.timeOf(end);
    }

    public String label() {
        return startTime() + " a " + endTime();
    }

    public double hours() {
        return end - start;
    }
}
