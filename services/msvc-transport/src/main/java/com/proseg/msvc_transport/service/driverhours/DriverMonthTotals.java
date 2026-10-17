package com.proseg.msvc_transport.service.driverhours;

public record DriverMonthTotals(double ordinaryHours, double surplusHours, double overtimeHours, int weekendsWorked) {

    public double surplusAndOvertimeHours() {
        return surplusHours + overtimeHours;
    }
}
