package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ManualDays {

    public static final ManualDays NONE = new ManualDays(Map.of());

    private final Map<UUID, Map<LocalDate, DayAllocation>> byAssignmentId;

    private ManualDays(Map<UUID, Map<LocalDate, DayAllocation>> byAssignmentId) {
        this.byAssignmentId = byAssignmentId;
    }

    public static ManualDays of(Collection<TourProcessingDraftDay> days) {
        Map<UUID, Map<LocalDate, DayAllocation>> byAssignmentId = new HashMap<>();
        days.stream()
                .filter(TourProcessingDraftDay::isManuallyEdited)
                .forEach(day -> byAssignmentId
                        .computeIfAbsent(day.getAssignment().getId(), id -> new HashMap<>())
                        .put(day.getDate(), new DayAllocation(
                                new TourDaySpan(day.getDate(), DriverHoursRules.hourOf(day.getStartTime()), DriverHoursRules.hourOf(day.getEndTime())),
                                day.getOrdinaryHours(),
                                day.getSurplusHours(),
                                day.getOvertimeHours())));
        return byAssignmentId.isEmpty() ? NONE : new ManualDays(byAssignmentId);
    }

    public Map<LocalDate, DayAllocation> of(TourProcessingDraftAssignment assignment) {
        return assignment.getId() == null ? Map.of() : byAssignmentId.getOrDefault(assignment.getId(), Map.of());
    }
}
