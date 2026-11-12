package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.assigned;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.driver;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.sameDayTour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.tour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.withPassengers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Timeout(60)
class DriverAutoAssignerBusinessDaysTest {

    private static final int DEFAULT_BUSINESS_DAYS = 28;
    private static final double DEFAULT_CAP = 37.5;

    private DriverAutoAssigner assigner(DriverBusinessDays businessDays, List<Driver> catalog, List<TourProcessingDraftAssignment> rows) {
        return new DriverAutoAssigner(2026, 5, businessDays, catalog, List.of(), rows, ManualDays.NONE);
    }

    private List<TourProcessingDraftAssignment> surplusOnWeekdays(Driver owner, int firstNumber, double... hoursPerDay) {
        List<TourProcessingDraftAssignment> rows = new ArrayList<>();
        LocalDate day = LocalDate.of(2026, 5, 4);
        for (int index = 0; index < hoursPerDay.length; index++) {
            LocalTime end = LocalTime.of(16, 30).plusMinutes((long) (hoursPerDay[index] * 60));
            rows.add(assigned(sameDayTour(Integer.toString(firstNumber + index), day.toString(), "16:30", end.toString()), owner));
            day = day.plusDays(1);
            if (day.getDayOfWeek().getValue() >= 6) {
                day = day.plusDays(8 - day.getDayOfWeek().getValue());
            }
        }
        return rows;
    }

    @Test
    void givesTheWeekendTourToTheDriverWhoReachedNinetyFivePercentOfTheirOwnMax() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>();
        rows.addAll(surplusOnWeekdays(ana, 1, 7, 7, 5));
        rows.addAll(surplusOnWeekdays(beto, 4, 7, 7, 5));
        TourProcessingDraftAssignment weekend = sameDayTour("9", "2026-05-09", "08:00", "12:00");
        rows.add(weekend);
        DriverBusinessDays businessDays = new DriverBusinessDays(DEFAULT_BUSINESS_DAYS, Map.of(ana.getId(), 14));

        DriverAutoAssigner assigner = assigner(businessDays, List.of(ana, beto), rows);
        assertThat(assigner.rejectionFor(ana, weekend)).isEmpty();
        assertThat(assigner.rejectionFor(beto, weekend)).isPresent();
        assertThat(assigner.rejectionFor(beto, weekend).get().message()).contains("95%");

        Map<TourProcessingDraftAssignment, Driver> result = assigner.assign(List.of(weekend));

        assertThat(businessDays.surplusCapOf(ana)).isEqualTo(19.0);
        assertThat(result).containsOnlyKeys(weekend).containsEntry(weekend, ana);
    }

    @Test
    void sameWeekendTourWithoutIndividualDaysHasNoEligibleDriver() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>();
        rows.addAll(surplusOnWeekdays(ana, 1, 7, 7, 5));
        rows.addAll(surplusOnWeekdays(beto, 4, 7, 7, 5));
        TourProcessingDraftAssignment weekend = withPassengers(sameDayTour("9", "2026-05-09", "08:00", "12:00"), 5);
        rows.add(weekend);

        Map<TourProcessingDraftAssignment, Driver> result =
                assigner(DriverBusinessDays.uniform(DEFAULT_BUSINESS_DAYS), List.of(ana, beto), rows).assign(List.of(weekend));

        assertThat(result).isEmpty();
    }

    @Test
    void aHigherIndividualMaxRaisesTheNinetyFivePercentBarForThatDriverOnly() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>();
        rows.addAll(surplusOnWeekdays(ana, 1, 6, 6, 6, 6, 6, 6));
        rows.addAll(surplusOnWeekdays(beto, 7, 6, 6, 6, 6, 6, 6));
        TourProcessingDraftAssignment weekend = sameDayTour("20", "2026-05-16", "08:00", "12:00");
        rows.add(weekend);
        DriverBusinessDays businessDays = new DriverBusinessDays(DEFAULT_BUSINESS_DAYS, Map.of(ana.getId(), 31));

        Map<TourProcessingDraftAssignment, Driver> individual = assigner(businessDays, List.of(ana, beto), rows).assign(List.of(weekend));
        Map<TourProcessingDraftAssignment, Driver> uniform =
                assigner(DriverBusinessDays.uniform(DEFAULT_BUSINESS_DAYS), List.of(ana, beto), rows).assign(List.of(weekend));

        assertThat(businessDays.surplusCapOf(ana)).isEqualTo(41.5);
        assertThat(individual).containsOnlyKeys(weekend).containsEntry(weekend, beto);
        assertThat(uniform).containsOnlyKeys(weekend).containsEntry(weekend, ana);
    }

    @Test
    void reportsSurplusDeficitsAndTargetsAgainstEachDriversOwnMax() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        DriverBusinessDays businessDays = new DriverBusinessDays(DEFAULT_BUSINESS_DAYS, Map.of(ana.getId(), 14));

        DriverAutoAssigner assigner = assigner(businessDays, List.of(ana, beto), List.of());

        assertThat(assigner.surplusDeficits().get("Ana")).isCloseTo(18.05, within(1e-9));
        assertThat(assigner.surplusDeficits().get("Beto")).isCloseTo(35.625, within(1e-9));
        assertThat(assigner.surplusTargets().get("Ana")).isCloseTo(18.05, within(1e-9));
        assertThat(assigner.surplusTargets().get("Beto")).isCloseTo(35.625, within(1e-9));
        assertThat(assigner.surplusTargetHours(ana)).isCloseTo(18.05, within(1e-9));
        assertThat(assigner.defaultSurplusTargetHours()).isCloseTo(35.625, within(1e-9));
    }

    @Test
    void keepsEveryDriverWithinTheirOwnMaxAndOvertimeRuleOnAMixedMonth() {
        List<Driver> drivers = new ArrayList<>();
        Map<java.util.UUID, Integer> customized = new HashMap<>();
        for (int index = 0; index < 18; index++) {
            Driver chofer = driver("Chofer " + index);
            drivers.add(chofer);
            if (index < 6) {
                customized.put(chofer.getId(), 14);
            } else if (index < 12) {
                customized.put(chofer.getId(), 31);
            }
        }
        DriverBusinessDays businessDays = new DriverBusinessDays(DEFAULT_BUSINESS_DAYS, customized);
        List<TourProcessingDraftAssignment> rows = monthOfMixedTours();

        Map<TourProcessingDraftAssignment, Driver> result = assigner(businessDays, drivers, rows).assign(rows);
        List<TourProcessingDraftAssignment> reversed = new ArrayList<>(rows);
        Collections.reverse(reversed);

        assertThat(result).isNotEmpty().isEqualTo(assigner(businessDays, drivers, reversed).assign(reversed));
        boolean someoneReachedOvertime = false;
        for (Driver chofer : drivers) {
            DriverWorkload workload = new DriverWorkload(chofer);
            result.forEach((row, assignedDriver) -> {
                if (chofer.equals(assignedDriver)) {
                    workload.add(row);
                }
            });
            double cap = businessDays.surplusCapOf(chofer);
            DriverMonthTotals totals = workload.totals(cap, DriverHoursRules.monthWeekends(2026, 5));
            assertThat(totals.surplusHours()).isLessThanOrEqualTo(cap);
            assertThat(totals.overtimeHours()).isLessThanOrEqualTo(DriverHoursRules.MONTHLY_OVERTIME_CAP);
            if (totals.overtimeHours() > 0) {
                someoneReachedOvertime = true;
                assertThat(totals.surplusHours()).isGreaterThanOrEqualTo(cap * DriverHoursRules.SURPLUS_CUTOFF_RATIO);
            }
        }
        assertThat(someoneReachedOvertime).isTrue();
    }

    @Test
    void everyDriverWithTheDefaultValueGivesTheSameResultAsTheUniformConstructor() {
        List<Driver> drivers = new ArrayList<>();
        Map<java.util.UUID, Integer> explicit = new HashMap<>();
        for (int index = 0; index < 18; index++) {
            Driver chofer = driver("Chofer " + index);
            drivers.add(chofer);
            explicit.put(chofer.getId(), DEFAULT_BUSINESS_DAYS);
        }
        List<TourProcessingDraftAssignment> rows = monthOfMixedTours();

        Map<TourProcessingDraftAssignment, Driver> viaInteger = new DriverAutoAssigner(
                2026, 5, DEFAULT_BUSINESS_DAYS, drivers, List.of(), rows, ManualDays.NONE).assign(rows);
        Map<TourProcessingDraftAssignment, Driver> viaExplicit =
                assigner(new DriverBusinessDays(DEFAULT_BUSINESS_DAYS, explicit), drivers, rows).assign(rows);

        assertThat(viaInteger).isNotEmpty().isEqualTo(viaExplicit);
    }

    @Test
    void customizedDaysOfDriversOutsideTheCatalogDoNotChangeTheCatalogDrivers() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment pending = sameDayTour("2", "2026-05-04", "08:00", "12:00");
        DriverBusinessDays businessDays = new DriverBusinessDays(DEFAULT_BUSINESS_DAYS, Map.of(java.util.UUID.randomUUID(), 3));

        Map<TourProcessingDraftAssignment, Driver> result = assigner(businessDays, List.of(ana), List.of(pending)).assign(List.of(pending));

        assertThat(result).containsOnlyKeys(pending).containsEntry(pending, ana);
        assertThat(businessDays.surplusCapOf(ana)).isEqualTo(DEFAULT_CAP);
    }

    private List<TourProcessingDraftAssignment> monthOfMixedTours() {
        List<TourProcessingDraftAssignment> rows = new ArrayList<>();
        Random random = new Random(202605);
        for (int index = 0; index < 428; index++) {
            LocalDate departure = LocalDate.of(2026, 5, 1).plusDays(random.nextInt(29));
            LocalDate arrival = departure.plusDays(random.nextInt(3));
            LocalTime start = LocalTime.of(6 + random.nextInt(9), 30);
            LocalTime end = LocalTime.of(15 + random.nextInt(7), 30);
            TourProcessingDraftAssignment row = tour(Integer.toString(index + 1), departure.toString(), start.toString(), arrival.toString(), end.toString());
            row.getTour().setPriority(1 + random.nextInt(7));
            int passengers = 1 + random.nextInt(15);
            row.setPassengers(passengers);
            row.getTour().setPassengers(passengers);
            if (index % 9 == 0) {
                row.getTour().setExecutingUnit("Programa De Servicios Generales");
            }
            rows.add(row);
        }
        return rows;
    }
}
