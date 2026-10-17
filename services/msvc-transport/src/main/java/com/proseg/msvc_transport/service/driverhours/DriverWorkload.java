package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.CleaningDraftRow;
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

    private static final Comparator<CleaningDraftRow> CHRONOLOGICAL = Comparator
            .comparing(CleaningDraftRow::getDepartureDate)
            .thenComparing(CleaningDraftRow::getDepartureTime)
            .thenComparing(CleaningDraftRow::getNumber);

    private final List<CleaningDraftRow> tours = new ArrayList<>();

    public static Map<UUID, DriverWorkload> fromDraftRows(List<CleaningDraftRow> rows, Map<String, Driver> catalogByKey) {
        Map<UUID, DriverWorkload> workloads = new LinkedHashMap<>();
        for (CleaningDraftRow row : rows) {
            Driver driver = catalogByKey.get(DriverHoursRules.driverKey(row.getDriver()));
            if (driver != null && DriverHoursRules.isPrimary(row)) {
                workloads.computeIfAbsent(driver.getId(), id -> new DriverWorkload()).add(row);
            }
        }
        return workloads;
    }

    public void add(CleaningDraftRow tour) {
        tours.add(tour);
    }

    public List<CleaningDraftRow> tours() {
        return List.copyOf(tours);
    }

    public boolean overlaps(CleaningDraftRow tour) {
        return findOverlap(tour).isPresent();
    }

    public Optional<CleaningDraftRow> findOverlap(CleaningDraftRow tour) {
        return tours.stream().filter(existing -> DriverHoursRules.toursOverlap(existing, tour)).findFirst();
    }

    public Map<LocalDate, Double> hoursByDate() {
        Map<LocalDate, Double> hoursByDate = new HashMap<>();
        for (CleaningDraftRow tour : tours) {
            for (TourDaySpan span : DriverHoursRules.daySpans(tour)) {
                hoursByDate.merge(span.date(), span.hours(), Double::sum);
            }
        }
        return hoursByDate;
    }

    public DriverMonthTotals totals(double surplusCap, Set<LocalDate> monthWeekends) {
        return totalsOf(tours, surplusCap, monthWeekends);
    }

    public DriverMonthTotals totalsWith(CleaningDraftRow tour, double surplusCap, Set<LocalDate> monthWeekends) {
        List<CleaningDraftRow> withTour = new ArrayList<>(tours);
        withTour.add(tour);
        return totalsOf(withTour, surplusCap, monthWeekends);
    }

    public List<TourAllocation> allocations(double surplusCap) {
        return allocationsOf(tours, surplusCap);
    }

    private static List<TourAllocation> allocationsOf(List<CleaningDraftRow> tours, double surplusCap) {
        List<TourAllocation> allocations = new ArrayList<>();
        double accumulatedSurplus = 0;
        for (CleaningDraftRow tour : tours.stream().sorted(CHRONOLOGICAL).toList()) {
            List<TourDaySpan> spans = DriverHoursRules.daySpans(tour);
            double offScheduleHours = spans.stream().mapToDouble(TourDaySpan::offScheduleWeekdayHours).sum();
            double tourSurplus = surplusFor(accumulatedSurplus, surplusCap, offScheduleHours);
            accumulatedSurplus += tourSurplus;

            double surplusLeft = tourSurplus;
            List<DayAllocation> days = new ArrayList<>();
            for (TourDaySpan span : spans) {
                double daySurplus = Math.min(span.offScheduleWeekdayHours(), surplusLeft);
                surplusLeft -= daySurplus;
                double dayOvertime = span.offScheduleWeekdayHours() - daySurplus + span.weekendHours();
                days.add(new DayAllocation(span, daySurplus, dayOvertime));
            }
            allocations.add(new TourAllocation(tour, days));
        }
        return allocations;
    }

    private static double surplusFor(double accumulatedSurplus, double surplusCap, double offScheduleHours) {
        double remainingSurplus = surplusCap - accumulatedSurplus;
        boolean surplusExhausted = remainingSurplus <= DriverHoursRules.HOURS_TOLERANCE;
        boolean pastCutoffWithoutRoom = accumulatedSurplus >= surplusCap * DriverHoursRules.SURPLUS_CUTOFF_RATIO
                && remainingSurplus < offScheduleHours;
        if (surplusExhausted || pastCutoffWithoutRoom) {
            return 0;
        }
        return Math.min(offScheduleHours, remainingSurplus);
    }

    private static DriverMonthTotals totalsOf(List<CleaningDraftRow> tours, double surplusCap, Set<LocalDate> monthWeekends) {
        double ordinary = 0;
        double surplus = 0;
        double overtime = 0;
        Set<LocalDate> weekendsWorked = new HashSet<>();

        for (TourAllocation allocation : allocationsOf(tours, surplusCap)) {
            for (DayAllocation day : allocation.days()) {
                TourDaySpan span = day.span();
                ordinary += span.ordinaryHours();
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
