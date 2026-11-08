package com.proseg.msvc_transport.service.driverhours;

public record DayAllocation(TourDaySpan span, double ordinaryHours, double surplusHours, double overtimeHours) {
}
