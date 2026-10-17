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
        if (weekend()) {
            return 0;
        }
        double overlapStart = Math.max(start, DriverHoursRules.ORDINARY_START);
        double overlapEnd = Math.min(end, DriverHoursRules.ORDINARY_END);
        return Math.max(0, overlapEnd - overlapStart);
    }

    public double offScheduleWeekdayHours() {
        return weekend() ? 0 : hours() - ordinaryHours();
    }

    public double weekendHours() {
        return weekend() ? hours() : 0;
    }
}
