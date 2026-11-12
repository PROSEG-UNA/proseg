package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.service.DraftCatalogLinker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.assigned;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.driver;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.sameDayTour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.tour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.withPassengers;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.withPriority;
import static org.assertj.core.api.Assertions.assertThat;

@Timeout(60)
class MandatoryCoverageTest {

    private static final int MAY_BUSINESS_DAYS = 28;
    private static final double MAY_CAP = 37.5;
    private static final double MAY_TARGET = MAY_CAP * DriverHoursRules.SURPLUS_CUTOFF_RATIO;
    private static final Set<LocalDate> MAY_WEEKENDS = DriverHoursRules.monthWeekends(2026, 5);
    private static final String[] TEN_WEEKDAYS = {
            "2026-05-11", "2026-05-12", "2026-05-13", "2026-05-14", "2026-05-15",
            "2026-05-18", "2026-05-19", "2026-05-20", "2026-05-21", "2026-05-22"
    };

    private record Outcome(DriverAutoAssigner assigner, Map<TourProcessingDraftAssignment, Driver> changes, List<Driver> catalog,
                            List<TourProcessingDraftAssignment> rows) {

        Driver finalDriverOf(TourProcessingDraftAssignment tour) {
            if (changes.containsKey(tour)) {
                return changes.get(tour);
            }
            return catalog.stream().filter(driver -> driver.getId().equals(tour.getDriverId())).findFirst().orElse(null);
        }

        List<TourProcessingDraftAssignment> toursOf(Driver driver) {
            return rows.stream().filter(row -> driver.equals(finalDriverOf(row))).toList();
        }

        DriverMonthTotals totalsOf(Driver driver) {
            DriverWorkload workload = new DriverWorkload(driver);
            toursOf(driver).forEach(workload::add);
            return workload.totals(MAY_CAP, MAY_WEEKENDS);
        }

        long attended(List<TourProcessingDraftAssignment> tours) {
            return tours.stream().filter(tour -> finalDriverOf(tour) != null).count();
        }
    }

    private Outcome run(List<Driver> catalog, List<TourProcessingDraftAssignment> rows) {
        return run(catalog, rows, DriverBusinessDays.uniform(MAY_BUSINESS_DAYS));
    }

    private Outcome run(List<Driver> catalog, List<TourProcessingDraftAssignment> rows, DriverBusinessDays businessDays) {
        DraftCatalogLinker linker = DraftCatalogLinker.of(catalog, List.of());
        rows.forEach(linker::link);
        List<TourProcessingDraftAssignment> pending = rows.stream().filter(row -> row.getDriver() == null).toList();
        DriverAutoAssigner assigner = new DriverAutoAssigner(2026, 5, businessDays, catalog, List.of(), rows, ManualDays.NONE);
        return new Outcome(assigner, assigner.assign(pending), catalog, rows);
    }

    private List<Driver> drivers(int count) {
        List<Driver> drivers = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            drivers.add(driver(String.format("Chofer %02d", index)));
        }
        return drivers;
    }

    private List<TourProcessingDraftAssignment> surplusSupply(String prefix, String[] days, int toursPerDay) {
        List<TourProcessingDraftAssignment> rows = new ArrayList<>();
        for (String day : days) {
            for (int index = 0; index < toursPerDay; index++) {
                rows.add(withPriority(sameDayTour(prefix + day + "-" + index, day, "16:30", "20:30"), 4));
            }
        }
        return rows;
    }

    private List<TourProcessingDraftAssignment> protectedSurplus(Driver owner, String prefix, String... days) {
        List<TourProcessingDraftAssignment> rows = new ArrayList<>();
        for (String day : days) {
            rows.add(assigned(withPriority(sameDayTour(prefix + day, day, "16:30", "22:30"), 4), owner));
        }
        return rows;
    }

    private TourProcessingDraftAssignment saturday(String number, String date) {
        return sameDayTour(number, date, "08:00", "12:00");
    }

    @Test
    void definesObligatoryToursAsPriorityOneWithFewerThanFivePassengersAndExtrasAsAnyWeekendDay() {
        TourProcessingDraftAssignment weekendSmall = saturday("1", "2026-05-09");
        TourProcessingDraftAssignment weekdaySmall = sameDayTour("2", "2026-05-12", "08:00", "12:00");
        TourProcessingDraftAssignment fridayToSaturday = tour("3", "2026-05-08", "18:00", "2026-05-09", "09:00");
        TourProcessingDraftAssignment largeGroup = withPassengers(saturday("4", "2026-05-09"), 5);
        TourProcessingDraftAssignment priorityTwo = withPriority(saturday("5", "2026-05-09"), 2);

        assertThat(List.of(weekendSmall, weekdaySmall, fridayToSaturday)).allMatch(MandatoryCoverage::isMandatory);
        assertThat(List.of(largeGroup, priorityTwo)).noneMatch(MandatoryCoverage::isMandatory);
        assertThat(MandatoryCoverage.requiresExtras(weekendSmall)).isTrue();
        assertThat(MandatoryCoverage.requiresExtras(fridayToSaturday)).isTrue();
        assertThat(MandatoryCoverage.requiresExtras(weekdaySmall)).isFalse();
    }

    @Test
    void lowSupplyConcentratesTheSurplusOnTheDriversWhoCoverTheObligatoryWeekendTours() {
        List<Driver> drivers = drivers(10);
        List<TourProcessingDraftAssignment> weekend = List.of(saturday("W0", "2026-05-09"), saturday("W1", "2026-05-09"), saturday("W2", "2026-05-09"));
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(weekend);
        rows.addAll(surplusSupply("S", TEN_WEEKDAYS, 3));

        Outcome outcome = run(drivers, rows);

        List<Driver> covering = weekend.stream().map(outcome::finalDriverOf).toList();
        assertThat(covering).doesNotContainNull().doesNotHaveDuplicates();
        assertThat(covering).allSatisfy(driver -> assertThat(outcome.totalsOf(driver).surplusHours()).isGreaterThanOrEqualTo(MAY_TARGET));
        List<Driver> others = drivers.stream().filter(driver -> !covering.contains(driver)).toList();
        assertThat(others).hasSize(7).allSatisfy(driver -> assertThat(outcome.totalsOf(driver).surplusHours()).isLessThan(MAY_TARGET));

        MandatoryCoverageSummary summary = outcome.assigner().coverageSummary();
        assertThat(summary.total()).isEqualTo(3);
        assertThat(summary.fulfilled()).isEqualTo(3);
        assertThat(summary.withWarning()).isZero();
        assertThat(summary.unattended()).isZero();
        assertThat(summary.coveringDrivers()).extracting(MandatoryCoverageSummary.CoveringDriver::name)
                .containsExactlyInAnyOrderElementsOf(covering.stream().map(Driver::getName).toList());
        assertThat(summary.coveringDrivers()).allSatisfy(driver -> assertThat(driver.surplusHours()).isGreaterThanOrEqualTo(MAY_TARGET));
        assertThat(summary.driversBelowTarget()).isEqualTo(7);
        assertThat(summary.driversBelowTargetByConcentration()).isEqualTo(7);
    }

    @Test
    void sufficientSupplyLeavesEveryDriverAtNinetyFivePercentWithoutWarnings() {
        List<Driver> drivers = drivers(10);
        List<TourProcessingDraftAssignment> weekend = List.of(saturday("W0", "2026-05-09"), saturday("W1", "2026-05-16"), saturday("W2", "2026-05-23"));
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(weekend);
        rows.addAll(surplusSupply("S", TEN_WEEKDAYS, 10));

        Outcome outcome = run(drivers, rows);

        assertThat(outcome.attended(weekend)).isEqualTo(3);
        assertThat(drivers).allSatisfy(driver -> assertThat(outcome.totalsOf(driver).surplusHours()).isGreaterThanOrEqualTo(MAY_TARGET));
        double highest = drivers.stream().mapToDouble(driver -> outcome.totalsOf(driver).surplusHours()).max().orElseThrow();
        double lowest = drivers.stream().mapToDouble(driver -> outcome.totalsOf(driver).surplusHours()).min().orElseThrow();
        assertThat(highest - lowest).isLessThanOrEqualTo(MAY_CAP - MAY_TARGET + 4);
        MandatoryCoverageSummary summary = outcome.assigner().coverageSummary();
        assertThat(summary.withWarning()).isZero();
        assertThat(summary.unattended()).isZero();
        assertThat(summary.driversBelowTarget()).isZero();
        assertThat(summary.driversBelowTargetByConcentration()).isZero();
    }

    @Test
    void fallbackAssignsTheObligatoryTourWithAWarningWhenNobodyCanReachNinetyFivePercent() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        TourProcessingDraftAssignment weekend = saturday("W", "2026-05-09");
        List<TourProcessingDraftAssignment> supply = surplusSupply("S", new String[]{"2026-05-11", "2026-05-12"}, 1);
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(weekend));
        rows.addAll(supply);

        Outcome outcome = run(List.of(ana, beto), rows);

        assertThat(outcome.finalDriverOf(weekend)).isEqualTo(ana);
        assertThat(outcome.toursOf(ana)).containsAll(supply);
        MandatoryCoverageSummary summary = outcome.assigner().coverageSummary();
        assertThat(summary.total()).isEqualTo(1);
        assertThat(summary.fulfilled()).isZero();
        assertThat(summary.withWarning()).isEqualTo(1);
        assertThat(summary.unattended()).isZero();
        assertThat(summary.warnings().get(0).number()).isEqualTo("W");
        assertThat(summary.warnings().get(0).message()).contains("Asignada a Ana con sobresueldo al 21.3%").contains("8 de 37.5 horas");
        assertThat(summary.coveringDrivers()).hasSize(1);
        assertThat(summary.coveringDrivers().get(0).surplusPercent()).isLessThan(95);
    }

    @Test
    void failedRelocationLeavesTheStateExactlyAsThePlanBeforeTheAttempt() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment weekend = saturday("W", "2026-05-09");
        List<TourProcessingDraftAssignment> supply = surplusSupply("S", new String[]{"2026-05-11", "2026-05-12", "2026-05-13"}, 1);
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(weekend));
        rows.addAll(supply);

        Outcome outcome = run(List.of(ana), rows);

        assertThat(outcome.changes()).hasSize(4);
        assertThat(outcome.toursOf(ana)).containsExactlyInAnyOrder(rows.toArray(new TourProcessingDraftAssignment[0]));
        assertValidState(outcome);
    }

    @Test
    void relocationDoesNotChangeTheOutcomeWhenAnotherDriverFacesTheSameShortage() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        TourProcessingDraftAssignment weekend = saturday("W", "2026-05-09");
        List<TourProcessingDraftAssignment> supply = surplusSupply("S", new String[]{"2026-05-11", "2026-05-12", "2026-05-13"}, 1);
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(weekend));
        rows.addAll(supply);

        Outcome outcome = run(List.of(ana, beto), rows);

        assertThat(outcome.finalDriverOf(weekend)).isEqualTo(ana);
        assertThat(outcome.toursOf(ana)).hasSize(4);
        assertThat(outcome.toursOf(beto)).isEmpty();
        assertThat(outcome.assigner().coverageSummary().withWarning()).isEqualTo(1);
        assertValidState(outcome);
    }

    @Test
    void relocationMovesTheObligatoryTourToADriverWhoDoesReachNinetyFivePercent() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        TourProcessingDraftAssignment weekend = saturday("W", "2026-05-16");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>();
        rows.addAll(protectedSurplus(ana, "A", "2026-05-04", "2026-05-05", "2026-05-06", "2026-05-07", "2026-05-08", "2026-05-11"));
        rows.addAll(protectedSurplus(beto, "B", "2026-05-04", "2026-05-05", "2026-05-06", "2026-05-07", "2026-05-08", "2026-05-11"));
        rows.add(weekend);
        DriverBusinessDays businessDays = new DriverBusinessDays(MAY_BUSINESS_DAYS, Map.of(ana.getId(), 31));

        Outcome outcome = run(List.of(ana, beto), rows, businessDays);

        assertThat(outcome.finalDriverOf(weekend)).isEqualTo(beto);
        MandatoryCoverageSummary summary = outcome.assigner().coverageSummary();
        assertThat(summary.withWarning()).isZero();
        assertThat(summary.fulfilled()).isEqualTo(1);
        assertThat(summary.coveringDrivers()).extracting(MandatoryCoverageSummary.CoveringDriver::name).containsExactly("Beto");
    }

    @Test
    void spreadsTheObligatoryWeekendToursAcrossSeveralDriversWhenTheWeekendCapIsReached() {
        List<Driver> drivers = drivers(4);
        List<TourProcessingDraftAssignment> weekend = List.of(saturday("W0", "2026-05-02"), saturday("W1", "2026-05-09"),
                saturday("W2", "2026-05-16"), saturday("W3", "2026-05-23"));
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(weekend);
        rows.addAll(surplusSupply("S", TEN_WEEKDAYS, 3));

        Outcome outcome = run(drivers, rows);

        assertThat(outcome.attended(weekend)).isEqualTo(4);
        List<Driver> covering = weekend.stream().map(outcome::finalDriverOf).distinct().toList();
        assertThat(covering).hasSize(2);
        assertThat(covering).allSatisfy(driver -> {
            assertThat(outcome.totalsOf(driver).weekendsWorked()).isLessThanOrEqualTo(3);
            assertThat(outcome.totalsOf(driver).surplusHours()).isGreaterThanOrEqualTo(MAY_TARGET);
        });
        assertThat(outcome.assigner().coverageSummary().coveringDrivers()).hasSize(2);
    }

    @Test
    void spreadsTheObligatoryWeekendToursAcrossSeveralDriversWhenTheOvertimeCapIsReached() {
        List<Driver> drivers = drivers(4);
        List<TourProcessingDraftAssignment> weekend = List.of(tour("W0", "2026-05-02", "08:00", "2026-05-03", "20:00"),
                tour("W1", "2026-05-09", "08:00", "2026-05-10", "20:00"), tour("W2", "2026-05-16", "08:00", "2026-05-17", "20:00"));
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(weekend);
        rows.addAll(surplusSupply("S", TEN_WEEKDAYS, 3));

        Outcome outcome = run(drivers, rows);

        assertThat(outcome.attended(weekend)).isEqualTo(3);
        assertThat(weekend.stream().map(outcome::finalDriverOf).distinct().count()).isEqualTo(2);
        assertThat(drivers).allSatisfy(driver -> assertThat(outcome.totalsOf(driver).overtimeHours()).isLessThanOrEqualTo(DriverHoursRules.MONTHLY_OVERTIME_CAP));
    }

    @Test
    void aMultiDayObligatoryTourGoesWholeToOneDriverWhoIsFreeEveryDay() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        TourProcessingDraftAssignment sundayBusy = assigned(sameDayTour("B", "2026-05-17", "12:00", "14:00"), ana);
        TourProcessingDraftAssignment multiDay = tour("M", "2026-05-15", "08:00", "2026-05-18", "12:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(sundayBusy, multiDay));
        rows.addAll(surplusSupply("S", TEN_WEEKDAYS, 2));

        Outcome outcome = run(List.of(ana, beto), rows);

        assertThat(outcome.finalDriverOf(multiDay)).isEqualTo(beto);
        assertThat(outcome.changes()).containsKey(multiDay);
        assertThat(outcome.toursOf(ana)).doesNotContain(multiDay);
        assertThat(outcome.toursOf(beto).stream().filter(row -> DriverHoursRules.toursOverlap(row, multiDay) && row != multiDay)).isEmpty();
    }

    @Test
    void neverReassignsProtectedToursWhileCoveringTheObligatoryOnes() {
        List<Driver> drivers = drivers(4);
        TourProcessingDraftAssignment protectedWeekend = assigned(saturday("P", "2026-05-09"), drivers.get(0));
        TourProcessingDraftAssignment obligatory = saturday("W", "2026-05-09");
        List<TourProcessingDraftAssignment> protectedSurplus = protectedSurplus(drivers.get(1), "Q", "2026-05-11", "2026-05-12", "2026-05-13");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(protectedWeekend, obligatory));
        rows.addAll(protectedSurplus);
        rows.addAll(surplusSupply("S", TEN_WEEKDAYS, 2));

        Outcome outcome = run(drivers, rows);

        assertThat(outcome.changes()).doesNotContainKeys(protectedWeekend);
        assertThat(outcome.changes()).doesNotContainKeys(protectedSurplus.toArray(new TourProcessingDraftAssignment[0]));
        assertThat(outcome.finalDriverOf(protectedWeekend)).isEqualTo(drivers.get(0));
        assertThat(outcome.finalDriverOf(obligatory)).isNotNull().isNotEqualTo(drivers.get(0));
        assertValidState(outcome);
    }

    @Test
    void reportsAnObligatoryTourNobodyCanPhysicallyTakeWithItsReason() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment busy = assigned(saturday("B", "2026-05-09"), ana);
        TourProcessingDraftAssignment obligatory = tour("W", "2026-05-08", "18:00", "2026-05-10", "10:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(busy, obligatory));

        Outcome outcome = run(List.of(ana), rows);

        assertThat(outcome.changes()).isEmpty();
        MandatoryCoverageSummary summary = outcome.assigner().coverageSummary();
        assertThat(summary.total()).isEqualTo(2);
        assertThat(summary.unattended()).isEqualTo(1);
        assertThat(summary.unattendedTours().get(0).number()).isEqualTo("W");
        assertThat(summary.unattendedTours().get(0).message()).contains("Ningún chofer puede tomarla").contains("1 por cruce de horario");
    }

    @Test
    void theObligatoryExceptionNeverLeaksToNonObligatoryWeekendTours() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment obligatory = saturday("M", "2026-05-09");
        TourProcessingDraftAssignment other = withPassengers(saturday("N", "2026-05-16"), 5);
        TourProcessingDraftAssignment lowerPriority = withPriority(saturday("L", "2026-05-23"), 2);
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(obligatory, other, lowerPriority));

        Outcome outcome = run(List.of(ana), rows);

        assertThat(outcome.finalDriverOf(obligatory)).isEqualTo(ana);
        assertThat(outcome.finalDriverOf(other)).isNull();
        assertThat(outcome.finalDriverOf(lowerPriority)).isNull();
        assertThat(outcome.assigner().coverageSummary().withWarning()).isEqualTo(1);
    }

    @Test
    void anObligatoryTourWinsTheOnlyFreeSlotOverALowerPriorityTour() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment lowerPriority = withPriority(saturday("A", "2026-05-09"), 2);
        TourProcessingDraftAssignment obligatory = saturday("Z", "2026-05-09");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(lowerPriority, obligatory));

        Outcome outcome = run(List.of(ana), rows);

        assertThat(outcome.finalDriverOf(obligatory)).isEqualTo(ana);
        assertThat(outcome.finalDriverOf(lowerPriority)).isNull();
    }

    @Test
    void obligatoryToursWithoutWeekendHoursKeepTheNormalFlowAndNeedNoWarning() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment weekday = sameDayTour("1", "2026-05-12", "08:00", "12:00");

        Outcome outcome = run(List.of(ana), new ArrayList<>(List.of(weekday)));

        assertThat(outcome.finalDriverOf(weekday)).isEqualTo(ana);
        MandatoryCoverageSummary summary = outcome.assigner().coverageSummary();
        assertThat(summary.total()).isEqualTo(1);
        assertThat(summary.fulfilled()).isEqualTo(1);
        assertThat(summary.coveringDrivers()).isEmpty();
    }

    @Test
    void producesTheSameResultRegardlessOfRowOrderAndAcrossRepeatedRuns() {
        List<Driver> drivers = drivers(12);
        List<TourProcessingDraftAssignment> rows = new ArrayList<>();
        Random random = new Random(20261);
        for (int index = 0; index < 160; index++) {
            LocalDate departure = LocalDate.of(2026, 5, 1).plusDays(random.nextInt(29));
            LocalDate arrival = departure.plusDays(random.nextInt(3));
            TourProcessingDraftAssignment row = tour(Integer.toString(index + 1), departure.toString(), LocalTime.of(6 + random.nextInt(9), 30).toString(),
                    arrival.toString(), LocalTime.of(15 + random.nextInt(7), 30).toString());
            withPriority(row, 1 + random.nextInt(4));
            withPassengers(row, 1 + random.nextInt(8));
            rows.add(row);
        }
        List<TourProcessingDraftAssignment> reversed = new ArrayList<>(rows);
        Collections.reverse(reversed);

        Outcome first = run(drivers, rows);
        Outcome second = run(drivers, rows);
        Outcome reordered = run(drivers, reversed);

        assertThat(first.changes()).isNotEmpty().isEqualTo(second.changes()).isEqualTo(reordered.changes());
        assertThat(first.assigner().coverageSummary()).isEqualTo(second.assigner().coverageSummary());
        assertValidState(first);
    }

    private void assertValidState(Outcome outcome) {
        int weekendCap = WeekendCap.forWeekendsInMonth(MAY_WEEKENDS.size());
        for (Driver driver : outcome.catalog()) {
            DriverMonthTotals totals = outcome.totalsOf(driver);
            assertThat(totals.weekendsWorked()).isLessThanOrEqualTo(weekendCap);
            assertThat(totals.overtimeHours()).isLessThanOrEqualTo(DriverHoursRules.MONTHLY_OVERTIME_CAP);
            assertThat(totals.surplusHours()).isLessThanOrEqualTo(MAY_CAP);
            List<TourProcessingDraftAssignment> tours = outcome.toursOf(driver);
            for (TourProcessingDraftAssignment tour : tours) {
                assertThat(tours.stream().filter(other -> other != tour).anyMatch(other -> DriverHoursRules.toursOverlap(tour, other))).isFalse();
            }
            Map<LocalDate, Double> hoursPerDay = tours.stream().flatMap(tour -> DriverHoursRules.daySpans(tour).stream())
                    .filter(span -> span.hours() > 0)
                    .collect(Collectors.groupingBy(TourDaySpan::date, Collectors.summingDouble(TourDaySpan::hours)));
            Map<LocalDate, Long> toursPerDay = tours.stream().flatMap(tour -> DriverHoursRules.daySpans(tour).stream())
                    .filter(span -> span.hours() > 0).collect(Collectors.groupingBy(TourDaySpan::date, Collectors.counting()));
            hoursPerDay.forEach((day, hours) -> {
                if (toursPerDay.get(day) > 1) {
                    assertThat(hours).isLessThanOrEqualTo(DriverHoursRules.MAX_STACKED_DAY_HOURS);
                }
            });
        }
        Set<UUID> assignedIds = outcome.changes().values().stream().filter(driver -> driver != null).map(Driver::getId).collect(Collectors.toSet());
        assertThat(outcome.catalog().stream().map(Driver::getId).collect(Collectors.toSet())).containsAll(assignedIds);
    }
}
