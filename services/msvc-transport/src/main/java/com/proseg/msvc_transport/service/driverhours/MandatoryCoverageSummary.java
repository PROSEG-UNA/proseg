package com.proseg.msvc_transport.service.driverhours;

import java.util.List;
import java.util.UUID;

public record MandatoryCoverageSummary(
        int total,
        int fulfilled,
        int withWarning,
        int unattended,
        List<TourNote> warnings,
        List<TourNote> unattendedTours,
        List<CoveringDriver> coveringDrivers,
        int driversBelowTarget,
        int driversBelowTargetByConcentration
) {

    public static final MandatoryCoverageSummary EMPTY = new MandatoryCoverageSummary(0, 0, 0, 0, List.of(), List.of(), List.of(), 0, 0);

    public record TourNote(UUID tourId, String number, String message) {}

    public record CoveringDriver(String name, double surplusHours, double targetHours, double surplusPercent, List<String> tourNumbers) {}
}
