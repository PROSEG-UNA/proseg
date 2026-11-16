package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.assigned;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.driver;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.fullSurplusTour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.sameDayTour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.withPassengers;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.withPriority;
import static org.assertj.core.api.Assertions.assertThat;

class PriorityOneSurplusCompletionTest {

    private static final int MAY_BUSINESS_DAYS = 28;
    private static final String SATURDAY = "2026-05-09";
    private static final double NINETY_FIVE_PERCENT = 35.625;

    private DriverAutoAssigner assigner(List<Driver> catalog, List<TourProcessingDraftAssignment> rows) {
        return new DriverAutoAssigner(2026, 5, MAY_BUSINESS_DAYS, catalog, List.of(), rows, ManualDays.NONE);
    }

    private TourProcessingDraftAssignment weekendTour(String number, int priority, int passengers) {
        return withPriority(withPassengers(sameDayTour(number, SATURDAY, "06:00", "18:00"), passengers), priority);
    }

    private TourProcessingDraftAssignment weekdaySurplusTour(String number, String date) {
        return withPriority(fullSurplusTour(number, date), 3);
    }

    @Test
    void completesTheSurplusByTakingWeekdayToursFromAnotherDriverAndAttendsTheTour() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        List<TourProcessingDraftAssignment> weekdays = List.of(
                weekdaySurplusTour("10", "2026-05-04"), weekdaySurplusTour("11", "2026-05-05"),
                weekdaySurplusTour("12", "2026-05-06"), weekdaySurplusTour("13", "2026-05-07"));
        TourProcessingDraftAssignment weekend = weekendTour("1", 1, 6);
        List<TourProcessingDraftAssignment> rows = new java.util.ArrayList<>(weekdays);
        rows.add(weekend);
        DriverAutoAssigner assigner = assigner(List.of(ana, beto), rows);

        Map<TourProcessingDraftAssignment, Driver> result = assigner.assign(rows);

        assertThat(result).containsKey(weekend);
        assertThat(result.keySet()).containsAll(weekdays);
        assertThat(assigner.surplusHoursOf(result.get(weekend))).isGreaterThanOrEqualTo(NINETY_FIVE_PERCENT);
    }

    @Test
    void leavesTheTourUnattendedWhenTheOnlyDriverCannotCompleteTheSurplus() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment weekend = weekendTour("1", 1, 6);
        DriverAutoAssigner assigner = assigner(List.of(ana), List.of(weekend));

        Map<TourProcessingDraftAssignment, Driver> result = assigner.assign(List.of(weekend));

        assertThat(result).doesNotContainKey(weekend);
    }

    @Test
    void leavesTheTourUnattendedWhenTheOtherDriversWeekdayToursAreFixed() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        TourProcessingDraftAssignment anaOne = assigned(weekdaySurplusTour("10", "2026-05-04"), ana);
        TourProcessingDraftAssignment anaTwo = assigned(weekdaySurplusTour("11", "2026-05-05"), ana);
        TourProcessingDraftAssignment betoOne = assigned(weekdaySurplusTour("12", "2026-05-06"), beto);
        TourProcessingDraftAssignment betoTwo = assigned(weekdaySurplusTour("13", "2026-05-07"), beto);
        TourProcessingDraftAssignment weekend = weekendTour("1", 1, 6);
        DriverAutoAssigner assigner = assigner(List.of(ana, beto), List.of(anaOne, anaTwo, betoOne, betoTwo, weekend));

        Map<TourProcessingDraftAssignment, Driver> result = assigner.assign(List.of(weekend));

        assertThat(result).doesNotContainKey(weekend);
    }

    private Map<TourProcessingDraftAssignment, Driver> weekdaysAssignedFirst(DriverAutoAssigner assigner, List<TourProcessingDraftAssignment> weekdays) {
        return new java.util.LinkedHashMap<>(assigner.assign(weekdays));
    }

    @Test
    void theCompletionStageMovesWeekdayToursToTheDriverUntilReachingNinetyFivePercent() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        List<TourProcessingDraftAssignment> weekdays = List.of(
                weekdaySurplusTour("10", "2026-05-04"), weekdaySurplusTour("11", "2026-05-05"),
                weekdaySurplusTour("12", "2026-05-06"), weekdaySurplusTour("13", "2026-05-07"));
        TourProcessingDraftAssignment weekend = weekendTour("1", 1, 6);
        List<TourProcessingDraftAssignment> rows = new java.util.ArrayList<>(weekdays);
        rows.add(weekend);
        DriverAutoAssigner assigner = assigner(List.of(ana, beto), rows);
        Map<TourProcessingDraftAssignment, Driver> assignments = weekdaysAssignedFirst(assigner, weekdays);
        assertThat(assignments).hasSize(4);
        assertThat(assigner.surplusHoursOf(ana)).isLessThan(NINETY_FIVE_PERCENT);
        assertThat(assigner.surplusHoursOf(beto)).isLessThan(NINETY_FIVE_PERCENT);

        assigner.completeSurplusForPriorityOne(new java.util.LinkedHashSet<>(rows), assignments);

        Driver chosen = assignments.get(weekend);
        assertThat(chosen).isNotNull();
        assertThat(assigner.surplusHoursOf(chosen)).isGreaterThanOrEqualTo(NINETY_FIVE_PERCENT);
        assertThat(assignments.keySet()).containsAll(weekdays);
        assertThat(assigner.conditionalTours()).doesNotContain(weekend);
    }

    @Test
    void theCompletionStageRemovesTheTourWhenTheOtherDriversToursAreFixed() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        TourProcessingDraftAssignment anaOne = assigned(weekdaySurplusTour("10", "2026-05-04"), ana);
        TourProcessingDraftAssignment anaTwo = assigned(weekdaySurplusTour("11", "2026-05-05"), ana);
        TourProcessingDraftAssignment betoOne = assigned(weekdaySurplusTour("12", "2026-05-06"), beto);
        TourProcessingDraftAssignment betoTwo = assigned(weekdaySurplusTour("13", "2026-05-07"), beto);
        TourProcessingDraftAssignment weekend = weekendTour("1", 1, 6);
        List<TourProcessingDraftAssignment> rows = List.of(anaOne, anaTwo, betoOne, betoTwo, weekend);
        DriverAutoAssigner assigner = assigner(List.of(ana, beto), rows);
        Map<TourProcessingDraftAssignment, Driver> assignments = new java.util.LinkedHashMap<>();
        rows.stream().filter(row -> row.getDriverId() != null)
                .forEach(row -> assignments.put(row, row.getDriverId().equals(ana.getId()) ? ana : beto));

        assigner.completeSurplusForPriorityOne(new java.util.LinkedHashSet<>(rows), assignments);

        assertThat(assignments).doesNotContainKey(weekend);
        assertThat(assigner.conditionalTours()).doesNotContain(weekend);
        assertThat(assigner.surplusHoursOf(ana)).isEqualTo(29.0);
    }

    @Test
    void theCompletionStageIgnoresToursOfOtherPriorities() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        List<TourProcessingDraftAssignment> weekdays = List.of(
                weekdaySurplusTour("10", "2026-05-04"), weekdaySurplusTour("11", "2026-05-05"),
                weekdaySurplusTour("12", "2026-05-06"), weekdaySurplusTour("13", "2026-05-07"));
        TourProcessingDraftAssignment weekend = weekendTour("1", 2, 6);
        List<TourProcessingDraftAssignment> rows = new java.util.ArrayList<>(weekdays);
        rows.add(weekend);
        DriverAutoAssigner assigner = assigner(List.of(ana, beto), rows);
        Map<TourProcessingDraftAssignment, Driver> assignments = weekdaysAssignedFirst(assigner, weekdays);

        assigner.completeSurplusForPriorityOne(new java.util.LinkedHashSet<>(rows), assignments);

        assertThat(assignments).doesNotContainKey(weekend);
    }

    @Test
    void stillRespectsTheLimitOfWeekendsPerDriver() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment first = assigned(sameDayTour("10", "2026-05-02", "08:00", "10:00"), ana);
        TourProcessingDraftAssignment second = assigned(sameDayTour("11", "2026-05-16", "08:00", "10:00"), ana);
        TourProcessingDraftAssignment third = assigned(sameDayTour("12", "2026-05-23", "08:00", "10:00"), ana);
        TourProcessingDraftAssignment tour = weekendTour("1", 1, 6);
        DriverAutoAssigner assigner = assigner(List.of(ana), List.of(first, second, third, tour));

        Map<TourProcessingDraftAssignment, Driver> result = assigner.assign(List.of(tour));

        assertThat(result).doesNotContainKey(tour);
    }
}
