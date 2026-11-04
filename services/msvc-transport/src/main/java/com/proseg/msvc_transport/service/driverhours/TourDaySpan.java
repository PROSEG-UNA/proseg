package com.proseg.msvc_transport.service.driverhours;

import java.time.LocalDate;

public record TourDaySpan(LocalDate date, double start, double end) {

    public double hours() {
        return end - start;
    }

    public boolean weekend() {
        return DriverHoursRules.isWeekend(date);
    }

    public double ordinaryHours() {
        return ordinaryHours(OrdinaryWindow.STANDARD);
    }

    public double ordinaryHours(OrdinaryWindow window) {
        if (weekend()) {
            return 0;
        }
        double overlapStart = Math.max(start, window.start());
        double overlapEnd = Math.min(end, window.end());
        return Math.max(0, overlapEnd - overlapStart);
    }

    public double offScheduleWeekdayHours() {
        return offScheduleWeekdayHours(OrdinaryWindow.STANDARD);
    }

    public double offScheduleWeekdayHours(OrdinaryWindow window) {
        return weekend() ? 0 : hours() - ordinaryHours(window);
    }

    public double weekendHours() {
        return weekend() ? hours() : 0;
    }
}
