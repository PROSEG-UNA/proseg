package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.service.driverhours.DayAllocation;
import com.proseg.msvc_transport.service.driverhours.DriverBusinessDays;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import com.proseg.msvc_transport.service.driverhours.DriverWorkload;
import com.proseg.msvc_transport.service.driverhours.ManualDays;
import com.proseg.msvc_transport.service.driverhours.TourAllocation;
import com.proseg.msvc_transport.service.driverhours.TourDaySpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DraftHoursService {

    private final DriverRepository driverRepository;
    private final TourProcessingDraftDayRepository dayRepository;
    private final DriverBusinessDaysService businessDaysService;

    public static Map<UUID, Set<WorkloadSlot>> workloadSlots(List<TourProcessingDraftAssignment> assignments) {
        Map<UUID, Set<WorkloadSlot>> slots = new HashMap<>();
        assignments.stream()
                .filter(TourProcessingDraftAssignment::isActive)
                .filter(assignment -> assignment.getDriverId() != null)
                .forEach(assignment -> slots.computeIfAbsent(assignment.getDriverId(), id -> new HashSet<>())
                        .add(new WorkloadSlot(assignment.getId(), assignment.getDepartureDate(), assignment.getDepartureTime(),
                                assignment.getReturnDate(), assignment.getReturnTime())));
        return slots;
    }

    @Transactional(readOnly = true)
    public ManualDays manualDays(UUID draftId) {
        return ManualDays.of(dayRepository.findAllByDraftId(draftId));
    }

    @Transactional
    public void syncAll(TourProcessingDraft draft, List<TourProcessingDraftAssignment> assignments) {
        sync(draft, assignments, null);
    }

    @Transactional
    public void syncChanged(TourProcessingDraft draft, Map<UUID, Set<WorkloadSlot>> slotsBefore, List<TourProcessingDraftAssignment> assignments) {
        Map<UUID, Set<WorkloadSlot>> slotsAfter = workloadSlots(assignments);
        Set<UUID> changedDriverIds = new HashSet<>(slotsBefore.keySet());
        changedDriverIds.addAll(slotsAfter.keySet());
        changedDriverIds.removeIf(driverId -> Objects.equals(slotsBefore.get(driverId), slotsAfter.get(driverId)));
        if (!changedDriverIds.isEmpty()) {
            sync(draft, assignments, changedDriverIds);
        }
    }

    @Transactional
    public void syncDriver(TourProcessingDraft draft, List<TourProcessingDraftAssignment> assignments, UUID driverId) {
        if (driverId != null) {
            sync(draft, assignments, Set.of(driverId));
        }
    }

    private void sync(TourProcessingDraft draft, List<TourProcessingDraftAssignment> assignments, Set<UUID> onlyDriverIds) {
        if (draft.getYear() == null || draft.getMonth() == null || draft.getBusinessDays() == null) {
            return;
        }
        DriverBusinessDays businessDays = businessDaysService.forDraft(draft);
        Map<UUID, Driver> driversById = DriverHoursRules.driversById(driverRepository.findAll());
        List<TourProcessingDraftDay> existing = dayRepository.findAllByDraftId(draft.getId());
        Map<UUID, List<TourProcessingDraftDay>> existingByAssignment = existing.stream()
                .collect(Collectors.groupingBy(day -> day.getAssignment().getId()));
        ManualDays manualDays = ManualDays.of(existing);
        Map<UUID, DriverWorkload> workloads = DriverWorkload.fromAssignments(assignments, driversById, manualDays);
        Map<UUID, DriverWorkload> informational = DriverWorkload.fromUncountedAssignments(assignments, driversById, manualDays);

        Set<UUID> assignmentsWithDays = new HashSet<>();
        workloads.values().forEach(workload -> workload.tours().forEach(tour -> assignmentsWithDays.add(tour.getId())));
        informational.values().forEach(workload -> workload.tours().forEach(tour -> assignmentsWithDays.add(tour.getId())));
        List<UUID> staleDayIds = new ArrayList<>();
        existingByAssignment.forEach((assignmentId, days) -> {
            if (!assignmentsWithDays.contains(assignmentId)) {
                days.forEach(day -> staleDayIds.add(day.getId()));
            }
        });

        List<TourProcessingDraftDay> daysToSave = new ArrayList<>();
        appendDays(workloads, onlyDriverIds, businessDays, existingByAssignment, daysToSave, staleDayIds);
        appendDays(informational, onlyDriverIds, businessDays, existingByAssignment, daysToSave, staleDayIds);

        if (!staleDayIds.isEmpty()) {
            dayRepository.deleteAllByIdInBatch(staleDayIds);
        }
        if (!daysToSave.isEmpty()) {
            dayRepository.saveAll(daysToSave);
        }
    }

    private void appendDays(
            Map<UUID, DriverWorkload> workloads,
            Set<UUID> onlyDriverIds,
            DriverBusinessDays businessDays,
            Map<UUID, List<TourProcessingDraftDay>> existingByAssignment,
            List<TourProcessingDraftDay> daysToSave,
            List<UUID> staleDayIds
    ) {
        workloads.forEach((driverId, workload) -> {
            if (onlyDriverIds != null && !onlyDriverIds.contains(driverId)) {
                return;
            }
            for (TourAllocation allocation : workload.allocations(businessDays.surplusCapOf(driverId))) {
                TourProcessingDraftAssignment assignment = allocation.tour();
                syncAssignmentDays(assignment, allocation.days(), existingByAssignment.getOrDefault(assignment.getId(), List.of()),
                        daysToSave, staleDayIds);
            }
        });
    }

    private void syncAssignmentDays(
            TourProcessingDraftAssignment assignment,
            List<DayAllocation> freshDays,
            List<TourProcessingDraftDay> existing,
            List<TourProcessingDraftDay> daysToSave,
            List<UUID> staleDayIds
    ) {
        Map<LocalDate, TourProcessingDraftDay> existingByDate = existing.stream()
                .collect(Collectors.toMap(TourProcessingDraftDay::getDate, Function.identity(), (left, right) -> left));
        Set<LocalDate> freshDates = new HashSet<>();
        for (DayAllocation allocation : freshDays) {
            TourDaySpan span = allocation.span();
            freshDates.add(span.date());
            TourProcessingDraftDay current = existingByDate.get(span.date());
            if (current != null && current.isManuallyEdited()) {
                continue;
            }
            TourProcessingDraftDay day = current != null ? current : TourProcessingDraftDay.builder().assignment(assignment).date(span.date()).build();
            day.setStartTime(DriverHoursRules.timeOf(span.start()));
            day.setEndTime(DriverHoursRules.timeOf(span.end()));
            day.setOrdinaryHours(round(allocation.ordinaryHours()));
            day.setSurplusHours(round(allocation.surplusHours()));
            day.setOvertimeHours(round(allocation.overtimeHours()));
            day.setManuallyEdited(false);
            daysToSave.add(day);
        }
        existingByDate.values().stream()
                .filter(day -> !freshDates.contains(day.getDate()))
                .forEach(day -> staleDayIds.add(day.getId()));
    }

    private static double round(double hours) {
        return Math.round(hours * 100) / 100.0;
    }

    public record WorkloadSlot(
            UUID assignmentId,
            LocalDate departureDate,
            LocalTime departureTime,
            LocalDate returnDate,
            LocalTime returnTime
    ) {
    }
}
