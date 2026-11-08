package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;

import java.util.List;

public record TourAllocation(TourProcessingDraftAssignment tour, List<DayAllocation> days) {

    public double surplusHours() {
        return days.stream().mapToDouble(DayAllocation::surplusHours).sum();
    }

    public double overtimeHours() {
        return days.stream().mapToDouble(DayAllocation::overtimeHours).sum();
    }
}
