package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.Driver;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class DriverWorkload {

    private static final Comparator<TourProcessingDraftAssignment> CHRONOLOGICAL = Comparator
            .comparing(TourProcessingDraftAssignment::getDepartureDate)
            .thenComparing(TourProcessingDraftAssignment::getDepartureTime)
            .thenComparing(TourProcessingDraftAssignment::getNumber);

    private final List<TourProcessingDraftAssignment> tours = new ArrayList<>();
    private final OrdinaryWindow window;
    private final ManualDays manualDays;

    public DriverWorkload() {
        this(OrdinaryWindow.STANDARD, ManualDays.NONE);
    }

    public DriverWorkload(Driver driver) {
        this(driver, ManualDays.NONE);
    }

    public DriverWorkload(Driver driver, ManualDays manualDays) {
        this(OrdinaryWindow.of(driver), manualDays);
    }

    private DriverWorkload(OrdinaryWindow window, ManualDays manualDays) {
        this.window = window;
        this.manualDays = manualDays;
    }

    public static Map<UUID, DriverWorkload> fromAssignments(
            List<TourProcessingDraftAssignment> assignments,
            Map<UUID, Driver> driversById,
            ManualDays manualDays
    ) {
        Map<UUID, DriverWorkload> workloads = new LinkedHashMap<>();
        for (TourProcessingDraftAssignment assignment : assignments) {
            Driver driver = DriverHoursRules.assignedDriver(assignment, driversById);
            if (assignment.isActive() && DriverHoursRules.countsTowardOwnDriver(assignment, driver)) {
                workloads.computeIfAbsent(driver.getId(), id -> new DriverWorkload(driver, manualDays)).add(assignment);
            }
        }
        return workloads;
    }

    public static Map<UUID, DriverWorkload> fromUncountedAssignments(
            List<TourProcessingDraftAssignment> assignments,
            Map<UUID, Driver> driversById,
            ManualDays manualDays
    ) {
        Map<UUID, DriverWorkload> workloads = new LinkedHashMap<>();
        for (TourProcessingDraftAssignment assignment : assignments) {
            Driver driver = DriverHoursRules.assignedDriver(assignment, driversById);
            if (driver != null && assignment.isActive() && !DriverHoursRules.countsTowardOwnDriver(assignment, driver)) {
                workloads.computeIfAbsent(driver.getId(), id -> new DriverWorkload(driver, manualDays)).add(assignment);
            }
        }
        return workloads;
    }

    public void add(TourProcessingDraftAssignment tour) {
        tours.add(tour);
    }

    public DriverWorkload without(TourProcessingDraftAssignment excluded) {
        DriverWorkload copy = new DriverWorkload(window, manualDays);
        tours.stream().filter(tour -> tour != excluded).forEach(copy::add);
        return copy;
    }

    public DriverWorkload copy() {
        DriverWorkload copy = new DriverWorkload(window, manualDays);
        tours.forEach(copy::add);
        return copy;
    }

    public DriverWorkload withoutAnyOf(Set<TourProcessingDraftAssignment> excluded) {
        DriverWorkload copy = new DriverWorkload(window, manualDays);
        tours.stream().filter(tour -> !excluded.contains(tour)).forEach(copy::add);
        return copy;
    }

    public boolean containsAnyOf(Set<TourProcessingDraftAssignment> candidates) {
        return tours.stream().anyMatch(candidates::contains);
    }

    public List<TourProcessingDraftAssignment> tours() {
        return hourCarriers(tours);
    }

    public DriverWorkload withoutSiblingsOf(TourProcessingDraftAssignment assignment) {
        DriverWorkload copy = new DriverWorkload(window, manualDays);
        tours.stream().filter(tour -> !sharedTrip(tour, assignment)).forEach(copy::add);
        return copy;
    }

    public boolean holdsSiblingOf(TourProcessingDraftAssignment assignment) {
        return tours.stream().anyMatch(tour -> sharedTrip(tour, assignment));
    }

    public static boolean sameTour(TourProcessingDraftAssignment left, TourProcessingDraftAssignment right) {
        return tourKey(left).equals(tourKey(right));
    }

    public static boolean sharedTrip(TourProcessingDraftAssignment existing, TourProcessingDraftAssignment candidate) {
        return existing != candidate && sameTour(existing, candidate)
                && candidate.getDriverId() != null && candidate.getDriverId().equals(existing.getDriverId());
    }

    public static boolean sharedVehicleTrip(TourProcessingDraftAssignment existing, TourProcessingDraftAssignment candidate) {
        return existing != candidate && sameTour(existing, candidate)
                && candidate.getVehicleId() != null && candidate.getVehicleId().equals(existing.getVehicleId());
    }

    private static Object tourKey(TourProcessingDraftAssignment assignment) {
        return assignment.getTour().getId() != null ? assignment.getTour().getId() : assignment.getTour();
    }

    private static int partOf(TourProcessingDraftAssignment assignment) {
        return assignment.getPart() == null ? Integer.MAX_VALUE : assignment.getPart();
    }

    private static List<TourProcessingDraftAssignment> hourCarriers(List<TourProcessingDraftAssignment> assignments) {
        Map<Object, TourProcessingDraftAssignment> carriers = new LinkedHashMap<>();
        for (TourProcessingDraftAssignment assignment : assignments) {
            carriers.merge(tourKey(assignment), assignment, (kept, candidate) -> partOf(candidate) < partOf(kept) ? candidate : kept);
        }
        return List.copyOf(carriers.values());
    }

    public boolean overlaps(TourProcessingDraftAssignment tour) {
        return findOverlap(tour).isPresent();
    }

    public Optional<TourProcessingDraftAssignment> findOverlap(TourProcessingDraftAssignment tour) {
        return tours.stream().filter(existing -> !sharedTrip(existing, tour) && DriverHoursRules.toursOverlap(existing, tour)).findFirst();
    }

    public Map<LocalDate, Double> hoursByDate() {
        Map<LocalDate, Double> hoursByDate = new HashMap<>();
        for (TourProcessingDraftAssignment tour : hourCarriers(tours)) {
            Map<LocalDate, DayAllocation> manual = manualDays.of(tour);
            for (TourDaySpan span : DriverHoursRules.daySpans(tour)) {
                DayAllocation edited = manual.get(span.date());
                hoursByDate.merge(span.date(), edited == null ? span.hours() : edited.span().hours(), Double::sum);
            }
        }
        return hoursByDate;
    }

    public DriverMonthTotals totals(double surplusCap, Set<LocalDate> monthWeekends) {
        return totalsOf(tours, surplusCap, monthWeekends);
    }

    public DriverMonthTotals totalsWith(TourProcessingDraftAssignment tour, double surplusCap, Set<LocalDate> monthWeekends) {
        List<TourProcessingDraftAssignment> withTour = new ArrayList<>(tours);
        withTour.add(tour);
        return totalsOf(withTour, surplusCap, monthWeekends);
    }

    public List<TourAllocation> allocations(double surplusCap) {
        return allocationsOf(tours, surplusCap);
    }

    private List<TourAllocation> allocationsOf(List<TourProcessingDraftAssignment> assigned, double surplusCap) {
        List<TourProcessingDraftAssignment> tours = hourCarriers(assigned);
        List<TourAllocation> allocations = new ArrayList<>();
        double accumulatedSurplus = 0;
        double reservedManualSurplus = tours.stream().flatMap(tour -> DriverHoursRules.daySpans(tour).stream()
                        .map(span -> manualDays.of(tour).get(span.date())))
                .filter(java.util.Objects::nonNull).mapToDouble(DayAllocation::surplusHours).sum();
        for (TourProcessingDraftAssignment tour : tours.stream().sorted(CHRONOLOGICAL).toList()) {
            List<TourDaySpan> spans = DriverHoursRules.daySpans(tour);
            Map<LocalDate, DayAllocation> manual = manualDays.of(tour);
            double manualSurplus = 0;
            double offScheduleHours = 0;
            for (TourDaySpan span : spans) {
                DayAllocation edited = manual.get(span.date());
                if (edited == null) {
                    offScheduleHours += span.offScheduleWeekdayHours(window);
                } else {
                    manualSurplus += edited.surplusHours();
                }
            }
            reservedManualSurplus -= manualSurplus;
            double tourSurplus = surplusFor(accumulatedSurplus + manualSurplus, surplusCap, offScheduleHours, reservedManualSurplus);
            accumulatedSurplus += manualSurplus + tourSurplus;

            double surplusLeft = tourSurplus;
            List<DayAllocation> days = new ArrayList<>();
            for (TourDaySpan span : spans) {
                DayAllocation edited = manual.get(span.date());
                if (edited != null) {
                    days.add(edited);
                    continue;
                }
                double dayOffSchedule = span.offScheduleWeekdayHours(window);
                double daySurplus = Math.min(dayOffSchedule, surplusLeft);
                surplusLeft -= daySurplus;
                double dayOvertime = dayOffSchedule - daySurplus + span.weekendHours();
                days.add(new DayAllocation(span, span.ordinaryHours(window), daySurplus, dayOvertime));
            }
            allocations.add(new TourAllocation(tour, days));
        }
        return allocations;
    }

    private static double surplusFor(double accumulatedSurplus, double surplusCap, double offScheduleHours, double reservedManualSurplus) {
        double remainingSurplus = surplusCap - accumulatedSurplus - reservedManualSurplus;
        boolean surplusExhausted = remainingSurplus <= DriverHoursRules.HOURS_TOLERANCE;
        boolean pastCutoffWithoutRoom = accumulatedSurplus >= surplusCap * DriverHoursRules.SURPLUS_CUTOFF_RATIO
                && remainingSurplus < offScheduleHours;
        if (surplusExhausted || pastCutoffWithoutRoom) {
            return 0;
        }
        return Math.min(offScheduleHours, remainingSurplus);
    }

    private DriverMonthTotals totalsOf(List<TourProcessingDraftAssignment> tours, double surplusCap, Set<LocalDate> monthWeekends) {
        double ordinary = 0;
        double surplus = 0;
        double overtime = 0;
        Set<LocalDate> weekendsWorked = new HashSet<>();

        for (TourAllocation allocation : allocationsOf(tours, surplusCap)) {
            for (DayAllocation day : allocation.days()) {
                TourDaySpan span = day.span();
                ordinary += day.ordinaryHours();
                surplus += day.surplusHours();
                overtime += day.overtimeHours();
                LocalDate weekend = DriverHoursRules.weekendKey(span.date());
                if (span.weekend() && span.hours() > 0 && monthWeekends.contains(weekend)) {
                    weekendsWorked.add(weekend);
                }
            }
        }

        return new DriverMonthTotals(round(ordinary), round(surplus), round(overtime), weekendsWorked.size());
    }

    private static double round(double hours) {
        return Math.round(hours * 100) / 100.0;
    }
}
