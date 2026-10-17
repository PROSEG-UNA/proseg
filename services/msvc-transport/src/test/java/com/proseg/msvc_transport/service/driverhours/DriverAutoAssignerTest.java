package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.CleaningDraftRow;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.entity.VehicleType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.assigned;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.driver;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.fullSurplusTour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.sameDayTour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.tour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.vehicle;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.withVehicle;
import static org.assertj.core.api.Assertions.assertThat;

class DriverAutoAssignerTest {

    private static final int MAY_BUSINESS_DAYS = 28;

    private Map<CleaningDraftRow, Driver> run(List<Driver> catalog, List<CleaningDraftRow> rows) {
        return run(catalog, List.of(), rows);
    }

    private Map<CleaningDraftRow, Driver> run(List<Driver> catalog, List<Vehicle> vehicles, List<CleaningDraftRow> rows) {
        List<CleaningDraftRow> pending = rows.stream().filter(row -> row.getDriver() == null).toList();
        return new DriverAutoAssigner(2026, 5, MAY_BUSINESS_DAYS, catalog, vehicles, rows).assign(pending);
    }

    @Test
    void ordersLevelsByPriorityPassengersAndGeneralServices() {
        CleaningDraftRow smallPriorityOne = sameDayTour("1", "2026-05-05", "08:00", "10:00");
        CleaningDraftRow generalServices = sameDayTour("2", "2026-05-05", "08:00", "10:00");
        generalServices.setPriority(7);
        generalServices.setExecutingUnit("  Programa De  Servicios Generales ");
        CleaningDraftRow largePriorityOne = sameDayTour("3", "2026-05-05", "08:00", "10:00");
        largePriorityOne.setPassengers(5);
        CleaningDraftRow smallPriorityTwo = sameDayTour("4", "2026-05-05", "08:00", "10:00");
        smallPriorityTwo.setPriority(2);
        CleaningDraftRow largePriorityThree = sameDayTour("5", "2026-05-05", "08:00", "10:00");
        largePriorityThree.setPriority(3);
        largePriorityThree.setPassengers(12);
        CleaningDraftRow prioritySixOutsideGeneralServices = sameDayTour("6", "2026-05-05", "08:00", "10:00");
        prioritySixOutsideGeneralServices.setPriority(6);

        assertThat(List.of(smallPriorityOne, generalServices, largePriorityOne, smallPriorityTwo, largePriorityThree, prioritySixOutsideGeneralServices))
                .extracting(DriverAutoAssigner::assignmentLevel)
                .containsExactly(0, 1, 2, 3, 6, 11);
    }

    @Test
    void processesHigherLevelsFirstThenEarlierDatesThenLowerNumbers() {
        Driver ana = driver("Ana");
        CleaningDraftRow priorityTwo = sameDayTour("1", "2026-05-06", "08:00", "10:00");
        priorityTwo.setPriority(2);
        CleaningDraftRow laterNumber = sameDayTour("20", "2026-05-06", "09:00", "11:00");
        CleaningDraftRow earlierNumber = sameDayTour("3", "2026-05-06", "09:00", "11:00");
        List<CleaningDraftRow> rows = new ArrayList<>(List.of(priorityTwo, laterNumber, earlierNumber));

        Map<CleaningDraftRow, Driver> result = run(List.of(ana), rows);

        assertThat(result).containsOnlyKeys(earlierNumber);
    }

    @Test
    void picksDriverWithFewestSurplusAndOvertimeHours() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        List<CleaningDraftRow> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-04", "16:30", "19:30"), ana),
                sameDayTour("2", "2026-05-06", "08:00", "10:00")
        ));

        Map<CleaningDraftRow, Driver> result = run(List.of(ana, beto), rows);

        assertThat(result).containsEntry(rows.get(1), beto);
    }

    @Test
    void prefersDriverFreeThatDayOverLessLoadedDriverAlreadyWorkingIt() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        CleaningDraftRow morning = sameDayTour("10", "2026-05-06", "08:00", "10:00");
        CleaningDraftRow afternoon = sameDayTour("11", "2026-05-06", "13:00", "15:00");
        List<CleaningDraftRow> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-04", "16:30", "19:30"), beto),
                morning,
                afternoon
        ));

        Map<CleaningDraftRow, Driver> result = run(List.of(ana, beto), rows);

        assertThat(result).containsEntry(morning, ana).containsEntry(afternoon, beto);
    }

    @Test
    void neverStacksToursBeyondTwelveHoursInADayButAcceptsALongSingleTour() {
        Driver ana = driver("Ana");
        CleaningDraftRow stacked = sameDayTour("2", "2026-05-06", "17:00", "21:00");
        CleaningDraftRow longAlone = sameDayTour("3", "2026-05-07", "06:00", "21:00");
        List<CleaningDraftRow> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-06", "07:30", "16:30"), ana),
                stacked,
                longAlone
        ));

        Map<CleaningDraftRow, Driver> result = run(List.of(ana), rows);

        assertThat(result).doesNotContainKey(stacked).containsEntry(longAlone, ana);
    }

    @Test
    void skipsDriversWithOverlappingTours() {
        Driver ana = driver("Ana");
        CleaningDraftRow overlapping = sameDayTour("2", "2026-05-06", "11:00", "14:00");
        List<CleaningDraftRow> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-06", "08:00", "12:00"), ana),
                overlapping
        ));

        assertThat(run(List.of(ana), rows)).isEmpty();
    }

    @Test
    void protectsLastFreeWeekendWhenAnotherQualifiedDriverCanCoverIt() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        CleaningDraftRow lastWeekend = sameDayTour("9", "2026-05-30", "08:00", "10:00");
        List<CleaningDraftRow> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-02", "08:00", "10:00"), ana),
                assigned(sameDayTour("2", "2026-05-09", "08:00", "10:00"), ana),
                assigned(sameDayTour("3", "2026-05-16", "08:00", "10:00"), ana),
                assigned(sameDayTour("4", "2026-05-23", "08:00", "10:00"), ana),
                assigned(fullSurplusTour("5", "2026-05-11"), ana),
                assigned(fullSurplusTour("6", "2026-05-12"), ana),
                assigned(fullSurplusTour("7", "2026-05-13"), ana),
                assigned(fullSurplusTour("8", "2026-05-04"), beto),
                assigned(fullSurplusTour("10", "2026-05-05"), beto),
                assigned(fullSurplusTour("11", "2026-05-06"), beto),
                lastWeekend
        ));

        assertThat(run(List.of(ana, beto), rows)).containsEntry(lastWeekend, beto);
    }

    @Test
    void usesLastFreeWeekendWhenNoOtherDriverIsAvailable() {
        Driver ana = driver("Ana");
        CleaningDraftRow lastWeekend = sameDayTour("9", "2026-05-30", "08:00", "10:00");
        List<CleaningDraftRow> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-02", "08:00", "10:00"), ana),
                assigned(sameDayTour("2", "2026-05-09", "08:00", "10:00"), ana),
                assigned(sameDayTour("3", "2026-05-16", "08:00", "10:00"), ana),
                assigned(sameDayTour("4", "2026-05-23", "08:00", "10:00"), ana),
                assigned(fullSurplusTour("5", "2026-05-11"), ana),
                assigned(fullSurplusTour("6", "2026-05-12"), ana),
                assigned(fullSurplusTour("7", "2026-05-13"), ana),
                lastWeekend
        ));

        assertThat(run(List.of(ana), rows)).containsEntry(lastWeekend, ana);
    }

    @Test
    void firstPassAssignsWeekendToursByEquityEvenBelowTheSurplusThreshold() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        CleaningDraftRow saturday = sameDayTour("9", "2026-05-02", "08:00", "10:00");
        List<CleaningDraftRow> rows = new ArrayList<>(List.of(
                assigned(fullSurplusTour("1", "2026-05-04"), beto),
                assigned(fullSurplusTour("2", "2026-05-05"), beto),
                assigned(fullSurplusTour("3", "2026-05-06"), beto),
                saturday
        ));

        assertThat(run(List.of(ana, beto), rows)).containsEntry(saturday, ana);
    }

    @Test
    void secondPassSwapsALowPriorityWeekendTourWithAWeekdayTourFromADriverAlreadyAtTheThreshold() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        CleaningDraftRow lowPriorityWeekend = sameDayTour("9", "2026-05-02", "08:00", "10:00");
        lowPriorityWeekend.setPriority(5);
        CleaningDraftRow betoWeekday = assigned(fullSurplusTour("1", "2026-05-04"), beto);
        List<CleaningDraftRow> rows = new ArrayList<>(List.of(
                betoWeekday,
                assigned(fullSurplusTour("2", "2026-05-05"), beto),
                assigned(fullSurplusTour("3", "2026-05-06"), beto),
                assigned(fullSurplusTour("4", "2026-05-11"), beto),
                lowPriorityWeekend
        ));

        Map<CleaningDraftRow, Driver> result = run(List.of(ana, beto), rows);

        assertThat(result).containsEntry(lowPriorityWeekend, beto).containsEntry(betoWeekday, ana);
    }

    @Test
    void secondPassLeavesTheWeekendTourAsAssignedWhenNoDriverHasSpareWeekdayCapacity() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        CleaningDraftRow saturday = sameDayTour("9", "2026-05-02", "08:00", "10:00");
        List<CleaningDraftRow> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-04", "16:30", "17:30"), beto),
                saturday
        ));

        assertThat(run(List.of(ana, beto), rows)).containsEntry(saturday, ana);
    }

    @Test
    void rejectsToursThatWouldExceedMonthlyOvertimeCap() {
        Driver ana = driver("Ana");
        List<CleaningDraftRow> rows = new ArrayList<>(List.of(
                assigned(fullSurplusTour("1", "2026-05-04"), ana),
                assigned(fullSurplusTour("2", "2026-05-05"), ana),
                assigned(fullSurplusTour("3", "2026-05-06"), ana),
                assigned(sameDayTour("4", "2026-05-02", "07:30", "19:30"), ana),
                assigned(sameDayTour("5", "2026-05-03", "07:30", "19:30"), ana),
                assigned(sameDayTour("6", "2026-05-09", "07:30", "19:30"), ana),
                assigned(sameDayTour("7", "2026-05-10", "07:30", "19:30"), ana)
        ));
        rows.add(sameDayTour("8", "2026-05-17", "07:30", "19:30"));

        assertThat(run(List.of(ana), rows)).isEmpty();
    }

    @Test
    void onlyPicksDriversAllowedToDriveTheLoadedVehicleType() {
        Driver ana = driver("Ana", VehicleType.SEDAN);
        Driver beto = driver("Beto", VehicleType.MICROBUS, VehicleType.SEDAN);
        List<Vehicle> vehicles = List.of(vehicle("301-295", VehicleType.MICROBUS, 15));
        CleaningDraftRow microbusTour = withVehicle(sameDayTour("1", "2026-05-06", "08:00", "10:00"), " 301 - 295 ");
        List<CleaningDraftRow> rows = new ArrayList<>(List.of(microbusTour));

        assertThat(run(List.of(ana, beto), vehicles, rows)).containsEntry(microbusTour, beto);
        assertThat(run(List.of(ana), vehicles, rows)).isEmpty();
    }

    @Test
    void ignoresVehicleTypeWhenLoadedVehicleIsUnknownOrHasNoType() {
        Driver ana = driver("Ana", VehicleType.SEDAN);
        List<Vehicle> vehicles = List.of(vehicle("400-100", null, 5));
        CleaningDraftRow untypedVehicle = withVehicle(sameDayTour("1", "2026-05-06", "08:00", "10:00"), "400-100");
        CleaningDraftRow unknownPlate = withVehicle(sameDayTour("2", "2026-05-07", "08:00", "10:00"), "999-999");
        CleaningDraftRow vehicleTypeText = withVehicle(sameDayTour("3", "2026-05-08", "08:00", "10:00"), "Microbus");
        List<CleaningDraftRow> rows = new ArrayList<>(List.of(untypedVehicle, unknownPlate, vehicleTypeText));

        assertThat(run(List.of(ana), vehicles, rows)).containsOnlyKeys(untypedVehicle, unknownPlate, vehicleTypeText);
    }

    @Test
    void ignoresInactiveDrivers() {
        Driver ana = driver("Ana");
        ana.setStatus("INACTIVE");
        List<CleaningDraftRow> rows = new ArrayList<>(List.of(sameDayTour("1", "2026-05-06", "08:00", "10:00")));

        assertThat(run(List.of(ana), rows)).isEmpty();
    }

    @Test
    void explainsWhyADriverCannotTakeATourThatOverlapsAnotherOfTheirTours() {
        Driver ana = driver("Ana");
        CleaningDraftRow nextDayTour = assigned(sameDayTour("200", "2026-05-06", "08:00", "17:00"), ana);
        CleaningDraftRow extendedTour = tour("100", "2026-05-05", "08:00", "2026-05-06", "12:00");
        DriverAutoAssigner assigner = new DriverAutoAssigner(2026, 5, MAY_BUSINESS_DAYS, List.of(ana), List.of(nextDayTour));

        assertThat(assigner.rejectionFor(ana, extendedTour))
                .hasValueSatisfying(rejection -> {
                    assertThat(rejection.reason()).isEqualTo(DriverRejection.Reason.OVERLAP);
                    assertThat(rejection.message()).contains("Ana", "200", "06/05/2026 08:00");
                });
    }

    @Test
    void explainsWhyADriverCannotStackMoreThanTwelveHoursInADay() {
        Driver ana = driver("Ana");
        CleaningDraftRow morning = assigned(sameDayTour("1", "2026-05-06", "07:30", "16:30"), ana);
        CleaningDraftRow evening = sameDayTour("2", "2026-05-06", "17:00", "21:00");
        DriverAutoAssigner assigner = new DriverAutoAssigner(2026, 5, MAY_BUSINESS_DAYS, List.of(ana), List.of(morning));

        assertThat(assigner.rejectionFor(ana, evening))
                .hasValueSatisfying(rejection -> {
                    assertThat(rejection.reason()).isEqualTo(DriverRejection.Reason.DAY_HOURS_EXCEEDED);
                    assertThat(rejection.message()).contains("9 horas", "06/05/2026", "4", "12 horas");
                });
    }

    @Test
    void explainsWhyADriverBelowTheSurplusThresholdCannotTakeAWeekendTour() {
        Driver ana = driver("Ana");
        CleaningDraftRow saturday = sameDayTour("1", "2026-05-09", "08:00", "12:00");
        DriverAutoAssigner assigner = new DriverAutoAssigner(2026, 5, MAY_BUSINESS_DAYS, List.of(ana), List.of());

        assertThat(assigner.rejectionFor(ana, saturday))
                .hasValueSatisfying(rejection ->
                        assertThat(rejection.reason()).isEqualTo(DriverRejection.Reason.WEEKEND_WITHOUT_SURPLUS));
    }

    @Test
    void acceptsATourWhenTheDriverIsFree() {
        Driver ana = driver("Ana");
        CleaningDraftRow otherDay = assigned(sameDayTour("200", "2026-05-12", "08:00", "17:00"), ana);
        CleaningDraftRow extendedTour = tour("100", "2026-05-05", "08:00", "2026-05-06", "12:00");
        DriverAutoAssigner assigner = new DriverAutoAssigner(2026, 5, MAY_BUSINESS_DAYS, List.of(ana), List.of(otherDay));

        assertThat(assigner.rejectionFor(ana, extendedTour)).isEmpty();
    }
}
