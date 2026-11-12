package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.DriverUnit;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.UlloaShift;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.entity.VehicleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.assigned;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.driver;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.driverWithSchedule;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.fullSurplusTour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.notApproved;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.sameDayTour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.tour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.ulloaDriver;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.ulloaTour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.vehicle;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.withPassengers;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.withPriority;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.withVehicle;
import static org.assertj.core.api.Assertions.assertThat;

@Timeout(30)
class DriverAutoAssignerTest {

    private static final int MAY_BUSINESS_DAYS = 28;

    private Map<TourProcessingDraftAssignment, Driver> run(List<Driver> catalog, List<TourProcessingDraftAssignment> rows) {
        return run(catalog, List.of(), rows);
    }

    private Map<TourProcessingDraftAssignment, Driver> run(List<Driver> catalog, List<Vehicle> vehicles, List<TourProcessingDraftAssignment> rows) {
        var linker = com.proseg.msvc_transport.service.DraftCatalogLinker.of(catalog, vehicles);
        rows.forEach(linker::link);
        List<TourProcessingDraftAssignment> pending = rows.stream().filter(row -> row.getDriver() == null).toList();
        return new DriverAutoAssigner(2026, 5, MAY_BUSINESS_DAYS, catalog, vehicles, rows, ManualDays.NONE).assign(pending);
    }

    @Test
    void ignoresNotApprovedToursAsPendingWorkAndAsExistingLoad() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment notApprovedPending = notApproved(sameDayTour("1", "2026-05-05", "08:00", "10:00"));
        TourProcessingDraftAssignment notApprovedAssigned = notApproved(assigned(sameDayTour("2", "2026-05-06", "08:00", "18:00"), ana));
        TourProcessingDraftAssignment approvedOverlapping = sameDayTour("3", "2026-05-06", "09:00", "11:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(notApprovedPending, notApprovedAssigned, approvedOverlapping));

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(ana), rows);

        assertThat(result).containsOnlyKeys(approvedOverlapping);
        assertThat(result).containsEntry(approvedOverlapping, ana);
    }

    @Test
    void ordersLevelsByPriorityPassengersAndGeneralServices() {
        TourProcessingDraftAssignment smallPriorityOne = sameDayTour("1", "2026-05-05", "08:00", "10:00");
        TourProcessingDraftAssignment generalServices = sameDayTour("2", "2026-05-05", "08:00", "10:00");
        generalServices.getTour().setPriority(7);
        generalServices.getTour().setExecutingUnit("  Programa De  Servicios Generales ");
        TourProcessingDraftAssignment largePriorityOne = sameDayTour("3", "2026-05-05", "08:00", "10:00");
        largePriorityOne.setPassengers(5);
        largePriorityOne.getTour().setPassengers(5);
        TourProcessingDraftAssignment smallPriorityTwo = sameDayTour("4", "2026-05-05", "08:00", "10:00");
        smallPriorityTwo.getTour().setPriority(2);
        TourProcessingDraftAssignment largePriorityThree = sameDayTour("5", "2026-05-05", "08:00", "10:00");
        largePriorityThree.getTour().setPriority(3);
        largePriorityThree.setPassengers(12);
        largePriorityThree.getTour().setPassengers(12);
        TourProcessingDraftAssignment prioritySixOutsideGeneralServices = sameDayTour("6", "2026-05-05", "08:00", "10:00");
        prioritySixOutsideGeneralServices.getTour().setPriority(6);

        assertThat(List.of(smallPriorityOne, generalServices, largePriorityOne, smallPriorityTwo, largePriorityThree, prioritySixOutsideGeneralServices))
                .extracting(DriverAutoAssigner::assignmentLevel)
                .containsExactly(0, 1, 2, 3, 6, 11);
    }

    @Test
    void processesHigherLevelsFirstThenEarlierDatesThenLowerNumbers() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment priorityTwo = sameDayTour("1", "2026-05-06", "08:00", "10:00");
        priorityTwo.getTour().setPriority(2);
        TourProcessingDraftAssignment laterNumber = sameDayTour("20", "2026-05-06", "09:00", "11:00");
        TourProcessingDraftAssignment earlierNumber = sameDayTour("3", "2026-05-06", "09:00", "11:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(priorityTwo, laterNumber, earlierNumber));

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(ana), rows);

        assertThat(result).containsOnlyKeys(earlierNumber);
    }

    @Test
    void picksDriverWithFewestSurplusAndOvertimeHours() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-04", "16:30", "19:30"), ana),
                sameDayTour("2", "2026-05-06", "08:00", "10:00")
        ));

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(ana, beto), rows);

        assertThat(result).containsEntry(rows.get(1), beto);
    }

    @Test
    void assignsOnlyGeneralServicesDriversAndKeepsOtherUnitManualAssignments() {
        Driver otherUnit = driver("Ana");
        otherUnit.setOrganizationalUnit(DriverUnit.OTHER_UNIT);
        Driver generalServices = driver("Beto");
        TourProcessingDraftAssignment manual = assigned(sameDayTour("1", "2026-05-04", "08:00", "10:00"), otherUnit);
        TourProcessingDraftAssignment pending = sameDayTour("2", "2026-05-04", "08:00", "10:00");

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(otherUnit, generalServices), List.of(manual, pending));

        assertThat(result).containsOnlyKeys(pending).containsEntry(pending, generalServices);
        assertThat(manual.getDriver()).isEqualTo(otherUnit.getName());
    }

    @Test
    void prefersDriverFreeThatDayOverLessLoadedDriverAlreadyWorkingIt() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        TourProcessingDraftAssignment morning = sameDayTour("10", "2026-05-06", "08:00", "10:00");
        TourProcessingDraftAssignment afternoon = sameDayTour("11", "2026-05-06", "13:00", "15:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-04", "16:30", "19:30"), beto),
                morning,
                afternoon
        ));

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(ana, beto), rows);

        assertThat(result).containsEntry(morning, ana).containsEntry(afternoon, beto);
    }

    @Test
    void neverStacksToursBeyondTwelveHoursInADayButAcceptsALongSingleTour() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment stacked = sameDayTour("2", "2026-05-06", "17:00", "21:00");
        TourProcessingDraftAssignment longAlone = sameDayTour("3", "2026-05-07", "06:00", "21:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-06", "07:30", "16:30"), ana),
                stacked,
                longAlone
        ));

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(ana), rows);

        assertThat(result).doesNotContainKey(stacked).containsEntry(longAlone, ana);
    }

    @Test
    void skipsDriversWithOverlappingTours() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment overlapping = sameDayTour("2", "2026-05-06", "11:00", "14:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-06", "08:00", "12:00"), ana),
                overlapping
        ));

        assertThat(run(List.of(ana), rows)).isEmpty();
    }

    @Test
    void doesNotTreatAManuallyAssignedBorrowedTourAsOccupyingTheDriver() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment borrowed = assigned(sameDayTour("1", "2026-05-06", "08:00", "12:00"), ana);
        borrowed.setResourceType(ResourceType.BORROWED_VEHICLE);
        TourProcessingDraftAssignment pending = sameDayTour("2", "2026-05-06", "09:00", "11:00");

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(ana), List.of(borrowed, pending));

        assertThat(result).containsEntry(pending, ana);
        assertThat(borrowed.getDriver()).isEqualTo("Ana");
    }

    @Test
    void leavesAFreeTextBorrowedDriverUntouched() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment borrowed = sameDayTour("1", "2026-05-06", "08:00", "10:00");
        borrowed.setResourceType(ResourceType.BORROWED_VEHICLE);
        borrowed.setDriver("Pedro Externo");
        TourProcessingDraftAssignment pending = sameDayTour("2", "2026-05-06", "13:00", "15:00");

        run(List.of(ana), List.of(borrowed, pending));

        assertThat(borrowed.getDriver()).isEqualTo("Pedro Externo");
        assertThat(borrowed.getDriverId()).isNull();
    }

    @Test
    void protectsLastFreeWeekendWhenAnotherQualifiedDriverCanCoverIt() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        TourProcessingDraftAssignment lastWeekend = sameDayTour("9", "2026-05-30", "08:00", "10:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
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
    void leavesTheWeekendUnassignedWhenTheOnlyDriverAlreadyReachedTheWeekendCap() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment lastWeekend = sameDayTour("9", "2026-05-30", "08:00", "10:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-02", "08:00", "10:00"), ana),
                assigned(sameDayTour("2", "2026-05-09", "08:00", "10:00"), ana),
                assigned(sameDayTour("3", "2026-05-16", "08:00", "10:00"), ana),
                assigned(sameDayTour("4", "2026-05-23", "08:00", "10:00"), ana),
                assigned(fullSurplusTour("5", "2026-05-11"), ana),
                assigned(fullSurplusTour("6", "2026-05-12"), ana),
                assigned(fullSurplusTour("7", "2026-05-13"), ana),
                lastWeekend
        ));
        DriverAutoAssigner assigner = new DriverAutoAssigner(2026, 5, MAY_BUSINESS_DAYS, List.of(ana), rows);

        assertThat(assigner.assign(List.of(lastWeekend))).isEmpty();
        assertThat(assigner.reasonsForUnassigned(lastWeekend, false)).anyMatch(reason -> reason.contains("máximo de 3"));
    }

    @Test
    void transfersAWeekendTourToADriverWhoMeetsTheSurplusThreshold() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        TourProcessingDraftAssignment saturday = sameDayTour("9", "2026-05-02", "08:00", "10:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(fullSurplusTour("1", "2026-05-04"), beto),
                assigned(fullSurplusTour("2", "2026-05-05"), beto),
                assigned(fullSurplusTour("3", "2026-05-06"), beto),
                saturday
        ));

        assertThat(run(List.of(ana, beto), rows)).containsOnlyKeys(saturday).containsEntry(saturday, beto);
    }

    @Test
    void preservesExistingWeekdaysWhenAssigningAWeekendToAQualifiedDriver() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        TourProcessingDraftAssignment lowPriorityWeekend = sameDayTour("9", "2026-05-02", "08:00", "10:00");
        lowPriorityWeekend.getTour().setPriority(5);
        TourProcessingDraftAssignment betoWeekday = assigned(fullSurplusTour("1", "2026-05-04"), beto);
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                betoWeekday,
                assigned(fullSurplusTour("2", "2026-05-05"), beto),
                assigned(fullSurplusTour("3", "2026-05-06"), beto),
                assigned(fullSurplusTour("4", "2026-05-11"), beto),
                lowPriorityWeekend
        ));

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(ana, beto), rows);

        assertThat(result).containsOnlyKeys(lowPriorityWeekend).containsEntry(lowPriorityWeekend, beto);
        assertThat(betoWeekday.getDriver()).isEqualTo(beto.getName());
    }

    @Test
    void leavesTheWeekendTourUnassignedWhenNoDriverCanMeetTheSurplusThreshold() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        TourProcessingDraftAssignment saturday = withPassengers(sameDayTour("9", "2026-05-02", "08:00", "10:00"), 5);
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-04", "16:30", "17:30"), beto),
                saturday
        ));

        assertThat(run(List.of(ana, beto), rows)).isEmpty();
    }

    @Test
    void replacesAHigherPriorityWeekendTourWithWeekdaySurplusHours() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment weekend = withPassengers(tour("1", "2026-05-09", "08:00", "2026-05-11", "08:00"), 5);
        TourProcessingDraftAssignment weekday = sameDayTour("2", "2026-05-11", "07:30", "20:30");
        weekday.getTour().setPriority(2);
        List<TourProcessingDraftAssignment> rows = List.of(weekend, weekday);

        Map<TourProcessingDraftAssignment, Driver> result = new DriverAutoAssigner(2026, 5, 3, List.of(ana), rows).assign(rows);

        assertThat(result).containsOnlyKeys(weekday).containsEntry(weekday, ana);
        assertValidResult(List.of(ana), rows, result, 3);
    }

    @Test
    void replacesOneWeekendTourWithMultipleWeekdayToursToReachTheThreshold() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment weekend = withPassengers(tour("1", "2026-05-08", "16:30", "2026-05-12", "08:00"), 5);
        TourProcessingDraftAssignment monday = fullSurplusTour("2", "2026-05-11");
        TourProcessingDraftAssignment tuesday = sameDayTour("3", "2026-05-12", "00:00", "22:30");
        monday.getTour().setPriority(2);
        tuesday.getTour().setPriority(2);
        List<TourProcessingDraftAssignment> rows = List.of(weekend, monday, tuesday);

        Map<TourProcessingDraftAssignment, Driver> result = new DriverAutoAssigner(2026, 5, 21, List.of(ana), rows).assign(rows);

        assertThat(result).containsOnlyKeys(monday, tuesday).containsEntry(monday, ana).containsEntry(tuesday, ana);
        assertValidResult(List.of(ana), rows, result, 21);
    }

    @Test
    void preservesAndReportsExistingOvertimeBelowTheThreshold() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment weekday = assigned(sameDayTour("1", "2026-05-04", "16:30", "19:30"), ana);
        weekday.getTour().setPriority(7);
        TourProcessingDraftAssignment weekend = assigned(sameDayTour("2", "2026-05-09", "08:00", "12:00"), ana);
        List<TourProcessingDraftAssignment> rows = List.of(weekday, weekend);

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(ana), rows);

        assertThat(result).isEmpty();
        DriverAutoAssigner assigner = new DriverAutoAssigner(2026, 5, MAY_BUSINESS_DAYS, List.of(ana), rows);
        assertThat(assigner.preservedAssignmentWarnings().get(weekend)).anyMatch(reason -> reason.contains("95%"));
        assertThat(weekend.getDriver()).isEqualTo(ana.getName());
    }

    @Test
    void preservesAValidWeekdayWhenRemovingItWouldNotImproveSurplus() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment morning = assigned(sameDayTour("1", "2026-05-04", "07:30", "16:30"), ana);
        TourProcessingDraftAssignment blocked = sameDayTour("2", "2026-05-04", "17:00", "21:00");
        List<TourProcessingDraftAssignment> rows = List.of(morning, blocked);

        assertThat(run(List.of(ana), rows)).isEmpty();
    }

    @Test
    void preservesAndReportsExistingOverlaps() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment important = assigned(sameDayTour("1", "2026-05-04", "08:00", "12:00"), ana);
        TourProcessingDraftAssignment lessImportant = assigned(sameDayTour("2", "2026-05-04", "09:00", "13:00"), ana);
        lessImportant.getTour().setPriority(2);
        List<TourProcessingDraftAssignment> rows = List.of(important, lessImportant);

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(ana), rows);

        assertThat(result).isEmpty();
        DriverAutoAssigner assigner = new DriverAutoAssigner(2026, 5, MAY_BUSINESS_DAYS, List.of(ana), rows);
        assertThat(assigner.preservedAssignmentWarnings()).containsOnlyKeys(important, lessImportant);
    }

    @Test
    void ignoresExternalToursEvenWhenTheirDriverIsInTheCatalog() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment external = assigned(sameDayTour("1", "2026-05-04", "08:00", "12:00"), ana);
        external.setResourceType(ResourceType.EXTERNAL);
        TourProcessingDraftAssignment internal = sameDayTour("2", "2026-05-04", "08:00", "12:00");

        assertThat(run(List.of(ana), List.of(external, internal)))
                .containsOnlyKeys(internal).containsEntry(internal, ana);
    }

    @Test
    void preservesAndReportsAnExistingTourFromAnInactiveDriver() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        ana.setStatus("INACTIVE");
        TourProcessingDraftAssignment existing = assigned(sameDayTour("1", "2026-05-04", "08:00", "12:00"), ana);
        List<TourProcessingDraftAssignment> rows = List.of(existing);

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(ana, beto), rows);

        assertThat(result).isEmpty();
        DriverAutoAssigner assigner = new DriverAutoAssigner(2026, 5, MAY_BUSINESS_DAYS, List.of(ana, beto), rows);
        assertThat(assigner.preservedAssignmentWarnings().get(existing)).anyMatch(reason -> reason.contains("inactivo"));
    }

    @Test
    void allowsAWeekendTourWhoseOwnWeekdayHoursCompleteTheMonthlyThreshold() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment existing = assigned(fullSurplusTour("1", "2026-05-04"), ana);
        TourProcessingDraftAssignment weekend = tour("2", "2026-05-08", "07:30", "2026-05-10", "12:00");
        List<TourProcessingDraftAssignment> rows = List.of(existing, weekend);

        Map<TourProcessingDraftAssignment, Driver> result = new DriverAutoAssigner(2026, 5, 14, List.of(ana), rows).assign(List.of(weekend));

        assertThat(result).containsOnlyKeys(weekend).containsEntry(weekend, ana);
        assertValidResult(List.of(ana), rows, result, 14);
    }

    @Test
    void doesNotFreeOvertimeCapacityByMovingAnExistingAssignment() {
        Driver ana = driver("Ana", VehicleType.MICROBUS);
        Driver beto = driver("Beto", VehicleType.SEDAN);
        TourProcessingDraftAssignment transferredWeekday = assigned(fullSurplusTour("1", "2026-05-04"), ana);
        TourProcessingDraftAssignment weekend = sameDayTour("9", "2026-05-16", "08:00", "10:00");
        weekend.getTour().setPriority(5);
        TourProcessingDraftAssignment recovered = withVehicle(sameDayTour("10", "2026-05-17", "07:30", "19:30"), "301-295");
        recovered.getTour().setPriority(6);
        List<TourProcessingDraftAssignment> rows = List.of(
                transferredWeekday,
                assigned(fullSurplusTour("2", "2026-05-05"), ana),
                assigned(fullSurplusTour("3", "2026-05-06"), ana),
                assigned(fullSurplusTour("4", "2026-05-11"), ana),
                assigned(sameDayTour("5", "2026-05-02", "07:30", "19:30"), ana),
                assigned(sameDayTour("6", "2026-05-03", "07:30", "19:30"), ana),
                assigned(sameDayTour("7", "2026-05-09", "07:30", "19:30"), ana),
                weekend,
                recovered
        );
        List<Vehicle> vehicles = List.of(vehicle("301-295", VehicleType.MICROBUS, 15));

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(ana, beto), vehicles, rows);

        assertThat(result).containsOnlyKeys(weekend).containsEntry(weekend, ana);
        assertThat(transferredWeekday.getDriver()).isEqualTo(ana.getName());
        assertValidResult(List.of(ana, beto), rows, result, MAY_BUSINESS_DAYS);
    }

    @Test
    void producesAValidAndDeterministicScheduleForAMonthOfMixedTours() {
        List<Driver> drivers = new ArrayList<>();
        for (int index = 0; index < 18; index++) {
            drivers.add(driver("Chofer " + index));
        }
        List<TourProcessingDraftAssignment> rows = new ArrayList<>();
        Random random = new Random(202605);
        for (int index = 0; index < 428; index++) {
            LocalDate departure = LocalDate.of(2026, 5, 1).plusDays(random.nextInt(29));
            LocalDate arrival = departure.plusDays(random.nextInt(3));
            LocalTime start = LocalTime.of(6 + random.nextInt(9), 30);
            LocalTime end = LocalTime.of(15 + random.nextInt(7), 30);
            TourProcessingDraftAssignment row = tour(Integer.toString(index + 1), departure.toString(), start.toString(), arrival.toString(), end.toString());
            row.getTour().setPriority(1 + random.nextInt(7));
            row.setPassengers(1 + random.nextInt(15));
        row.getTour().setPassengers(1 + random.nextInt(15));
            if (index % 9 == 0) {
                row.getTour().setExecutingUnit("Programa De Servicios Generales");
            }
            rows.add(row);
        }

        Map<TourProcessingDraftAssignment, Driver> result = run(drivers, rows);
        List<TourProcessingDraftAssignment> reversed = new ArrayList<>(rows);
        Collections.reverse(reversed);

        assertThat(result).isNotEmpty().isEqualTo(run(drivers, reversed));
        assertValidResult(drivers, rows, result, MAY_BUSINESS_DAYS);
    }

    private void assertValidResult(List<Driver> drivers, List<TourProcessingDraftAssignment> rows, Map<TourProcessingDraftAssignment, Driver> changes, int businessDays) {
        double cap = DriverHoursRules.surplusCap(businessDays);
        for (Driver driver : drivers) {
            DriverWorkload workload = new DriverWorkload(driver);
            rows.stream().filter(row -> row.getTour().isPrimary())
                    .filter(row -> row.getResourceType() != ResourceType.EXTERNAL)
                    .filter(row -> changes.containsKey(row) ? driver.equals(changes.get(row)) : driver.getName().equals(row.getDriver()))
                    .forEach(workload::add);
            DriverMonthTotals totals = workload.totals(cap, DriverHoursRules.monthWeekends(2026, 5));
            assertThat(totals.weekendsWorked()).isLessThanOrEqualTo(WeekendCap.forWeekendsInMonth(DriverHoursRules.monthWeekends(2026, 5).size()));
            assertThat(totals.overtimeHours()).isLessThanOrEqualTo(DriverHoursRules.MONTHLY_OVERTIME_CAP);
            assertThat(totals.surplusHours()).isLessThanOrEqualTo(cap);
            if (totals.overtimeHours() > 0 && totals.surplusHours() < cap * DriverHoursRules.SURPLUS_CUTOFF_RATIO) {
                DriverWorkload withoutMandatoryExtras = new DriverWorkload(driver);
                workload.tours().stream()
                        .filter(tour -> !(MandatoryCoverage.isMandatory(tour) && MandatoryCoverage.requiresExtras(tour)))
                        .forEach(withoutMandatoryExtras::add);
                assertThat(withoutMandatoryExtras.totals(cap, DriverHoursRules.monthWeekends(2026, 5)).overtimeHours()).isZero();
            }
            if (!"ACTIVE".equals(driver.getStatus())) {
                assertThat(workload.tours()).isEmpty();
            }
            Map<LocalDate, List<TourDaySpan>> days = workload.tours().stream()
                    .flatMap(tour -> DriverHoursRules.daySpans(tour).stream())
                    .filter(span -> span.hours() > 0)
                    .collect(Collectors.groupingBy(TourDaySpan::date));
            days.values().stream().filter(spans -> spans.size() > 1).forEach(spans ->
                    assertThat(spans.stream().mapToDouble(TourDaySpan::hours).sum())
                            .isLessThanOrEqualTo(DriverHoursRules.MAX_STACKED_DAY_HOURS));
            for (TourProcessingDraftAssignment tour : workload.tours()) {
                assertThat(workload.tours().stream().filter(other -> other != tour)
                        .anyMatch(other -> DriverHoursRules.toursOverlap(tour, other))).isFalse();
            }
        }
    }

    @Test
    void rejectsToursThatWouldExceedMonthlyOvertimeCap() {
        Driver ana = driver("Ana");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
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
        TourProcessingDraftAssignment microbusTour = withVehicle(sameDayTour("1", "2026-05-06", "08:00", "10:00"), " 301 - 295 ");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(microbusTour));

        assertThat(run(List.of(ana, beto), vehicles, rows)).containsEntry(microbusTour, beto);
        assertThat(run(List.of(ana), vehicles, rows)).isEmpty();
    }

    @Test
    void ignoresVehicleTypeWhenLoadedVehicleIsUnknownOrHasNoType() {
        Driver ana = driver("Ana", VehicleType.SEDAN);
        List<Vehicle> vehicles = List.of(vehicle("400-100", null, 5));
        TourProcessingDraftAssignment untypedVehicle = withVehicle(sameDayTour("1", "2026-05-06", "08:00", "10:00"), "400-100");
        TourProcessingDraftAssignment unknownPlate = withVehicle(sameDayTour("2", "2026-05-07", "08:00", "10:00"), "999-999");
        TourProcessingDraftAssignment vehicleTypeText = withVehicle(sameDayTour("3", "2026-05-08", "08:00", "10:00"), "Microbus");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(untypedVehicle, unknownPlate, vehicleTypeText));

        assertThat(run(List.of(ana), vehicles, rows)).containsOnlyKeys(untypedVehicle, unknownPlate, vehicleTypeText);
    }

    @Test
    void ignoresInactiveDrivers() {
        Driver ana = driver("Ana");
        ana.setStatus("INACTIVE");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(sameDayTour("1", "2026-05-06", "08:00", "10:00")));

        assertThat(run(List.of(ana), rows)).isEmpty();
    }

    @Test
    void explainsWhyADriverCannotTakeATourThatOverlapsAnotherOfTheirTours() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment nextDayTour = assigned(sameDayTour("200", "2026-05-06", "08:00", "17:00"), ana);
        TourProcessingDraftAssignment extendedTour = tour("100", "2026-05-05", "08:00", "2026-05-06", "12:00");
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
        TourProcessingDraftAssignment morning = assigned(sameDayTour("1", "2026-05-06", "07:30", "16:30"), ana);
        TourProcessingDraftAssignment evening = sameDayTour("2", "2026-05-06", "17:00", "21:00");
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
        TourProcessingDraftAssignment saturday = sameDayTour("1", "2026-05-09", "08:00", "12:00");
        DriverAutoAssigner assigner = new DriverAutoAssigner(2026, 5, MAY_BUSINESS_DAYS, List.of(ana), List.of());

        assertThat(assigner.rejectionFor(ana, saturday))
                .hasValueSatisfying(rejection ->
                        assertThat(rejection.reason()).isEqualTo(DriverRejection.Reason.WEEKEND_WITHOUT_SURPLUS));
    }

    @Test
    void acceptsATourWhenTheDriverIsFree() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment otherDay = assigned(sameDayTour("200", "2026-05-12", "08:00", "17:00"), ana);
        TourProcessingDraftAssignment extendedTour = tour("100", "2026-05-05", "08:00", "2026-05-06", "12:00");
        DriverAutoAssigner assigner = new DriverAutoAssigner(2026, 5, MAY_BUSINESS_DAYS, List.of(ana), List.of(otherDay));

        assertThat(assigner.rejectionFor(ana, extendedTour)).isEmpty();
    }

    private static final String[] MAY_WORKING_DAYS = {
            "2026-05-04", "2026-05-05", "2026-05-06", "2026-05-07", "2026-05-08",
            "2026-05-11", "2026-05-12", "2026-05-13", "2026-05-14", "2026-05-15",
            "2026-05-18", "2026-05-19", "2026-05-20", "2026-05-21", "2026-05-22",
            "2026-05-25", "2026-05-26", "2026-05-27", "2026-05-28", "2026-05-29",
    };

    private Driver afternoonDriver(String name) {
        Driver driver = driverWithSchedule(name, "12:00", "20:00");
        driver.setUlloaShifts(EnumSet.of(UlloaShift.AFTERNOON));
        return driver;
    }

    @Test
    void buildsSurplusInRoundsForBothDriversBeforeCoveringWeekendTours() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>();
        for (int day = 0; day < 9; day++) {
            rows.add(sameDayTour("A" + day, MAY_WORKING_DAYS[day], "16:30", "20:30"));
            rows.add(sameDayTour("B" + day, MAY_WORKING_DAYS[day], "16:30", "20:30"));
        }
        TourProcessingDraftAssignment firstWeekend = sameDayTour("W1", "2026-05-30", "08:00", "16:00");
        TourProcessingDraftAssignment secondWeekend = sameDayTour("W2", "2026-05-30", "08:00", "16:00");
        rows.addAll(List.of(firstWeekend, secondWeekend));
        Collections.reverse(rows);

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(beto, ana), rows);

        assertThat(result).hasSize(rows.size());
        assertThat(result.get(firstWeekend)).isNotEqualTo(result.get(secondWeekend));
        for (Driver driver : List.of(ana, beto)) {
            DriverWorkload workload = new DriverWorkload(driver);
            result.forEach((tour, assigned) -> {
                if (driver.equals(assigned)) {
                    workload.add(tour);
                }
            });
            DriverMonthTotals totals = workload.totals(37.5, DriverHoursRules.monthWeekends(2026, 5));
            assertThat(totals.surplusHours()).isEqualTo(36);
            assertThat(totals.overtimeHours()).isEqualTo(8);
        }
        assertValidResult(List.of(ana, beto), rows, result, MAY_BUSINESS_DAYS);
    }

    @Test
    void stopsWithAnExplainedDeficitWhenOnlyOrdinaryHoursAreAvailable() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment ordinary = sameDayTour("1", "2026-05-04", "08:00", "10:00");
        TourProcessingDraftAssignment weekend = withPassengers(sameDayTour("2", "2026-05-09", "08:00", "10:00"), 5);
        List<TourProcessingDraftAssignment> rows = List.of(ordinary, weekend);
        DriverAutoAssigner assigner = new DriverAutoAssigner(2026, 5, MAY_BUSINESS_DAYS, List.of(ana), rows);

        assertThat(assigner.assign(rows)).containsOnlyKeys(ordinary);
        assertThat(assigner.surplusDeficits()).containsEntry("Ana", 35.625);
        assertThat(assigner.reasonsForUnassigned(weekend, false)).anyMatch(reason -> reason.contains("95%"));
        assertThat(assigner.optimizationLimitReached()).isFalse();
    }

    @Test
    void keepsTheSameUlloaDriverOnConsecutiveDaysInsteadOfSpreadingTheShift() {
        Driver ana = afternoonDriver("Ana");
        Driver beto = afternoonDriver("Beto");
        List<TourProcessingDraftAssignment> rows = List.of(
                ulloaTour("3", "2026-05-06", UlloaShift.AFTERNOON),
                ulloaTour("1", "2026-05-04", UlloaShift.AFTERNOON),
                ulloaTour("2", "2026-05-05", UlloaShift.AFTERNOON));

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(beto, ana), rows);

        assertThat(result).hasSize(3);
        assertThat(result.values()).containsOnly(ana);
    }

    @Test
    void acceptsBackToBackShortToursWithinTheStackedDayLimit() {
        Driver ana = ulloaDriver("Ana", UlloaShift.SHORT_MORNING);
        TourProcessingDraftAssignment morning = ulloaTour("1", "2026-05-04", UlloaShift.SHORT_MORNING);
        TourProcessingDraftAssignment afternoon = sameDayTour("2", "2026-05-04", "12:00", "16:00");
        List<TourProcessingDraftAssignment> rows = List.of(morning, afternoon);

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(ana), rows);

        assertThat(result).containsEntry(morning, ana).containsEntry(afternoon, ana);
        assertValidResult(List.of(ana), rows, result, MAY_BUSINESS_DAYS);
    }

    @Test
    void neverCombinesUlloaShortMorningAndAfternoonIntoFifteenAndAHalfHours() {
        Driver ana = ulloaDriver("Ana", UlloaShift.SHORT_MORNING);
        TourProcessingDraftAssignment morning = ulloaTour("1", "2026-05-04", UlloaShift.SHORT_MORNING);
        TourProcessingDraftAssignment afternoon = ulloaTour("2", "2026-05-04", UlloaShift.AFTERNOON);
        List<TourProcessingDraftAssignment> rows = List.of(morning, afternoon);
        DriverAutoAssigner assigner = new DriverAutoAssigner(2026, 5, MAY_BUSINESS_DAYS, List.of(ana), rows);

        assertThat(assigner.assign(rows)).containsOnlyKeys(morning);
        assertThat(assigner.reasonsForUnassigned(afternoon, false)).anyMatch(reason -> reason.contains("12 horas"));
    }

    @Test
    void replacesALowerPrioritySurplusTourWithAFeasibleHigherPriorityWeekendTour() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment weekend = tour("1", "2026-05-08", "16:30", "2026-05-11", "08:00");
        TourProcessingDraftAssignment weekday = sameDayTour("2", "2026-05-11", "07:30", "20:30");
        weekday.getTour().setPriority(2);
        List<TourProcessingDraftAssignment> rows = List.of(weekday, weekend);
        DriverAutoAssigner assigner = new DriverAutoAssigner(2026, 5, 3, List.of(ana), rows);

        Map<TourProcessingDraftAssignment, Driver> result = assigner.assign(rows);

        assertThat(result).containsOnlyKeys(weekend).containsEntry(weekend, ana);
        assertValidResult(List.of(ana), rows, result, 3);
    }

    @Test
    void movesAnAutomaticAssignmentToCoverAPendingTourRequiringASpecificVehicleType() {
        Driver ana = driver("Ana", VehicleType.MICROBUS, VehicleType.SEDAN);
        Driver beto = driver("Beto", VehicleType.SEDAN);
        TourProcessingDraftAssignment generic = sameDayTour("1", "2026-05-04", "08:00", "12:00");
        TourProcessingDraftAssignment microbus = withVehicle(sameDayTour("2", "2026-05-04", "08:00", "12:00"), "301-295");
        microbus.getTour().setPriority(2);
        List<TourProcessingDraftAssignment> rows = List.of(generic, microbus);

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(ana, beto), List.of(vehicle("301-295", VehicleType.MICROBUS, 15)), rows);

        assertThat(result).containsEntry(generic, beto).containsEntry(microbus, ana);
        assertValidResult(List.of(ana, beto), rows, result, MAY_BUSINESS_DAYS);
    }

    @Test
    void prefersMoreSeparationBetweenRegularToursWhenOvertimeIsEqual() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        TourProcessingDraftAssignment next = sameDayTour("3", "2026-05-12", "08:00", "12:00");
        List<TourProcessingDraftAssignment> rows = List.of(
                assigned(sameDayTour("1", "2026-05-11", "08:00", "12:00"), ana),
                assigned(sameDayTour("2", "2026-05-04", "08:00", "12:00"), beto), next);

        assertThat(run(List.of(ana, beto), rows)).containsOnlyKeys(next).containsEntry(next, beto);
    }

    @Test
    void reportsInvalidInputAndNoEligibleDriversWithoutLooping() {
        TourProcessingDraftAssignment invalid = sameDayTour("1", "2026-05-04", "12:00", "08:00");
        TourProcessingDraftAssignment valid = sameDayTour("2", "2026-05-04", "08:00", "12:00");
        DriverAutoAssigner assigner = new DriverAutoAssigner(2026, 5, MAY_BUSINESS_DAYS, List.of(), List.of(invalid, valid));

        assertThat(assigner.assign(List.of(invalid, valid))).isEmpty();
        assertThat(assigner.reasonsForUnassigned(invalid, false)).anyMatch(reason -> reason.contains("regreso"));
        assertThat(assigner.reasonsForUnassigned(valid, false)).anyMatch(reason -> reason.contains("No hay choferes activos"));
    }

    @Test
    void assignsUlloaToursFirstToDriversConfiguredForTheirShift() {
        Driver ricardo = afternoonDriver("Ricardo");
        Driver daniel = afternoonDriver("Daniel");
        Driver allan = driver("Allan");
        TourProcessingDraftAssignment first = ulloaTour("1", "2026-05-05", UlloaShift.AFTERNOON);
        TourProcessingDraftAssignment second = ulloaTour("2", "2026-05-05", UlloaShift.AFTERNOON);
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(first, second));

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(allan, ricardo, daniel), rows);

        assertThat(result).containsOnlyKeys(first, second);
        assertThat(result.values()).containsExactlyInAnyOrder(ricardo, daniel);
    }

    @Test
    void leavesUlloaToursToTheNormalPassesWhenNoDriverHasTheShift() {
        Driver allan = driver("Allan");
        TourProcessingDraftAssignment afternoon = ulloaTour("1", "2026-05-05", UlloaShift.AFTERNOON);

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(allan), new ArrayList<>(List.of(afternoon)));

        assertThat(result).containsEntry(afternoon, allan);
    }

    @Test
    void ulloaPassIgnoresDriversConfiguredForAnotherShiftOrInactive() {
        Driver morning = ulloaDriver("Mora", UlloaShift.MORNING);
        Driver inactiveAfternoon = afternoonDriver("Inactivo");
        inactiveAfternoon.setStatus("INACTIVE");
        TourProcessingDraftAssignment afternoon = ulloaTour("1", "2026-05-05", UlloaShift.AFTERNOON);
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(afternoon));

        Map<TourProcessingDraftAssignment, Driver> result = new DriverAutoAssigner(
                2026, 5, MAY_BUSINESS_DAYS, List.of(morning, inactiveAfternoon), rows
        ).assignUlloaOnly(rows);

        assertThat(result).isEmpty();
    }

    @Test
    void ulloaOnlyModeNeverTouchesOtherTours() {
        Driver ricardo = afternoonDriver("Ricardo");
        TourProcessingDraftAssignment ulloa = ulloaTour("1", "2026-05-05", UlloaShift.AFTERNOON);
        TourProcessingDraftAssignment regular = sameDayTour("2", "2026-05-06", "08:00", "10:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(ulloa, regular));

        Map<TourProcessingDraftAssignment, Driver> result = new DriverAutoAssigner(
                2026, 5, MAY_BUSINESS_DAYS, List.of(ricardo), rows
        ).assignUlloaOnly(rows);

        assertThat(result).containsOnlyKeys(ulloa).containsEntry(ulloa, ricardo);
    }

    @Test
    void ulloaOnlyModeRespectsToursAlreadyAssigned() {
        Driver ricardo = afternoonDriver("Ricardo");
        Driver daniel = afternoonDriver("Daniel");
        TourProcessingDraftAssignment taken = assigned(ulloaTour("1", "2026-05-05", UlloaShift.AFTERNOON), daniel);
        TourProcessingDraftAssignment pending = ulloaTour("2", "2026-05-05", UlloaShift.AFTERNOON);
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(taken, pending));

        Map<TourProcessingDraftAssignment, Driver> result = new DriverAutoAssigner(
                2026, 5, MAY_BUSINESS_DAYS, List.of(ricardo, daniel), rows
        ).assignUlloaOnly(List.of(pending));

        assertThat(result).containsOnlyKeys(pending).containsEntry(pending, ricardo);
    }

    @Test
    void keepsEveryUlloaAfternoonOnItsConfiguredDriversThroughTheWholeMonth() {
        Driver ricardo = afternoonDriver("Ricardo");
        Driver daniel = afternoonDriver("Daniel");
        Driver allan = driver("Allan");
        Driver murillo = driver("Murillo");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>();
        List<TourProcessingDraftAssignment> afternoonTours = new ArrayList<>();
        for (int day = 0; day < MAY_WORKING_DAYS.length; day++) {
            TourProcessingDraftAssignment first = ulloaTour("U" + day + "A", MAY_WORKING_DAYS[day], UlloaShift.AFTERNOON);
            TourProcessingDraftAssignment second = ulloaTour("U" + day + "B", MAY_WORKING_DAYS[day], UlloaShift.AFTERNOON);
            afternoonTours.addAll(List.of(first, second));
            rows.addAll(List.of(first, second));
            rows.add(sameDayTour("R" + day, MAY_WORKING_DAYS[day], "06:30", "19:00"));
        }
        rows.add(sameDayTour("W1", "2026-05-09", "08:00", "16:00"));
        rows.add(sameDayTour("W2", "2026-05-23", "08:00", "16:00"));
        List<Driver> drivers = List.of(ricardo, daniel, allan, murillo);

        Map<TourProcessingDraftAssignment, Driver> result = run(drivers, rows);

        assertThat(afternoonTours).allSatisfy(tour -> assertThat(result.get(tour)).isIn(ricardo, daniel));
        assertThat(afternoonTours.stream().filter(tour -> result.get(tour) == ricardo).count()).isEqualTo(MAY_WORKING_DAYS.length);
        assertThat(afternoonTours.stream().filter(tour -> result.get(tour) == daniel).count()).isEqualTo(MAY_WORKING_DAYS.length);
        assertValidResult(drivers, rows, result, MAY_BUSINESS_DAYS);
    }

    @Test
    void countsTheAfternoonShiftAgainstTheDriversOwnOrdinarySchedule() {
        Driver ricardo = afternoonDriver("Ricardo");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>();
        for (String day : MAY_WORKING_DAYS) {
            rows.add(ulloaTour("U" + day, day, UlloaShift.AFTERNOON));
        }

        Map<TourProcessingDraftAssignment, Driver> result = run(List.of(ricardo), rows);

        assertThat(result).hasSize(MAY_WORKING_DAYS.length);
        DriverWorkload workload = new DriverWorkload(ricardo);
        rows.forEach(workload::add);
        DriverMonthTotals totals = workload.totals(DriverHoursRules.surplusCap(MAY_BUSINESS_DAYS), DriverHoursRules.monthWeekends(2026, 5));
        assertThat(totals.ordinaryHours()).isEqualTo(160.0);
        assertThat(totals.surplusHours() + totals.overtimeHours()).isEqualTo(40.0);
    }

    @Test
    void aDriverCanWorkAThirdWeekendInAMonthWithFiveWeekends() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment thirdWeekend = sameDayTour("9", "2026-05-16", "08:00", "10:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-02", "08:00", "10:00"), ana),
                assigned(sameDayTour("2", "2026-05-09", "08:00", "10:00"), ana),
                assigned(fullSurplusTour("5", "2026-05-11"), ana),
                assigned(fullSurplusTour("6", "2026-05-12"), ana),
                assigned(fullSurplusTour("7", "2026-05-13"), ana),
                thirdWeekend
        ));

        assertThat(run(List.of(ana), rows)).containsOnlyKeys(thirdWeekend).containsEntry(thirdWeekend, ana);
    }

    @Test
    void aDriverCannotWorkAFourthWeekendInAMonthWithFiveWeekendsAndAnotherDriverTakesIt() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        TourProcessingDraftAssignment fourthWeekend = sameDayTour("9", "2026-05-23", "08:00", "10:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-02", "08:00", "10:00"), ana),
                assigned(sameDayTour("2", "2026-05-09", "08:00", "10:00"), ana),
                assigned(sameDayTour("3", "2026-05-16", "08:00", "10:00"), ana),
                assigned(fullSurplusTour("5", "2026-05-11"), ana),
                assigned(fullSurplusTour("6", "2026-05-12"), ana),
                assigned(fullSurplusTour("7", "2026-05-13"), ana),
                assigned(fullSurplusTour("8", "2026-05-04"), beto),
                assigned(fullSurplusTour("10", "2026-05-05"), beto),
                assigned(fullSurplusTour("11", "2026-05-06"), beto),
                fourthWeekend
        ));

        assertThat(run(List.of(ana, beto), rows)).containsOnlyKeys(fourthWeekend).containsEntry(fourthWeekend, beto);
    }

    @Test
    void aDriverCannotWorkAThirdWeekendInAMonthWithFourWeekends() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment thirdWeekend = sameDayTour("9", "2027-02-20", "08:00", "10:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2027-02-06", "08:00", "10:00"), ana),
                assigned(sameDayTour("2", "2027-02-13", "08:00", "10:00"), ana),
                assigned(fullSurplusTour("5", "2027-02-08"), ana),
                assigned(fullSurplusTour("6", "2027-02-09"), ana),
                thirdWeekend
        ));
        DriverAutoAssigner assigner = new DriverAutoAssigner(2027, 2, 20, List.of(ana), rows);

        assertThat(DriverHoursRules.monthWeekends(2027, 2)).hasSize(4);
        assertThat(assigner.assign(List.of(thirdWeekend))).isEmpty();
        assertThat(assigner.reasonsForUnassigned(thirdWeekend, false)).anyMatch(reason -> reason.contains("máximo de 2"));
    }

    @Test
    void aDriverCanWorkTheSecondWeekendInAMonthWithFourWeekends() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment secondWeekend = sameDayTour("9", "2027-02-13", "08:00", "10:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2027-02-06", "08:00", "10:00"), ana),
                assigned(fullSurplusTour("5", "2027-02-08"), ana),
                assigned(fullSurplusTour("6", "2027-02-09"), ana),
                secondWeekend
        ));

        Map<TourProcessingDraftAssignment, Driver> result = new DriverAutoAssigner(2027, 2, 20, List.of(ana), rows).assign(List.of(secondWeekend));

        assertThat(result).containsOnlyKeys(secondWeekend).containsEntry(secondWeekend, ana);
    }

    @Test
    void octoberTwentyTwentySixHasFiveWeekendsSoTheFourthIsRefusedAndSaturdayThirtyOneStillCounts() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment saturdayThirtyOne = sameDayTour("9", "2026-10-31", "08:00", "10:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-10-03", "08:00", "10:00"), ana),
                assigned(sameDayTour("2", "2026-10-10", "08:00", "10:00"), ana),
                assigned(sameDayTour("3", "2026-10-17", "08:00", "10:00"), ana),
                assigned(fullSurplusTour("5", "2026-10-05"), ana),
                assigned(fullSurplusTour("6", "2026-10-06"), ana),
                assigned(fullSurplusTour("7", "2026-10-07"), ana),
                saturdayThirtyOne
        ));
        DriverAutoAssigner assigner = new DriverAutoAssigner(2026, 10, 22, List.of(ana), rows);

        assertThat(DriverHoursRules.monthWeekends(2026, 10)).hasSize(5).contains(LocalDate.of(2026, 10, 31));
        assertThat(assigner.assign(List.of(saturdayThirtyOne))).isEmpty();
        assertThat(assigner.reasonsForUnassigned(saturdayThirtyOne, false)).anyMatch(reason -> reason.contains("máximo de 3"));
    }

    @Test
    void anExistingManualAssignmentAboveTheWeekendCapIsPreservedAndStillAllowsWeekdayTours() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment weekday = sameDayTour("9", "2026-05-14", "08:00", "10:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-02", "08:00", "10:00"), ana),
                assigned(sameDayTour("2", "2026-05-09", "08:00", "10:00"), ana),
                assigned(sameDayTour("3", "2026-05-16", "08:00", "10:00"), ana),
                assigned(sameDayTour("4", "2026-05-23", "08:00", "10:00"), ana),
                assigned(fullSurplusTour("5", "2026-05-11"), ana),
                assigned(fullSurplusTour("6", "2026-05-12"), ana),
                assigned(fullSurplusTour("7", "2026-05-13"), ana),
                weekday
        ));

        assertThat(run(List.of(ana), rows)).containsOnlyKeys(weekday).containsEntry(weekday, ana);
    }

    private TourProcessingDraftAssignment requestedBy(TourProcessingDraftAssignment tour, String requester) {
        tour.getTour().setResponsible(requester);
        return tour;
    }

    @Test
    void assignsTheTourToTheDriverWhoseNameMatchesTheRequesterEvenWhenAnotherDriverIsLessLoaded() {
        Driver ana = driver("Ana Pérez");
        Driver beto = driver("Beto Mora");
        TourProcessingDraftAssignment requested = requestedBy(sameDayTour("2", "2026-05-05", "08:00", "10:00"), "ANA PEREZ");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-04", "08:00", "12:00"), ana), requested));

        assertThat(run(List.of(ana, beto), rows)).containsEntry(requested, ana);
        requested.getTour().setResponsible("Otra Persona");
        assertThat(run(List.of(ana, beto), rows)).containsEntry(requested, beto);
    }

    @Test
    void matchesRequestersByWordsIgnoringAccentsCaseAndExtraSpacesAndAdditionalSurnames() {
        Driver daniel = driver("Daniel Zúñiga Álvarez");
        Driver beto = driver("Beto Mora");
        TourProcessingDraftAssignment requested = requestedBy(sameDayTour("2", "2026-05-05", "08:00", "10:00"), "  daniel   ZUNIGA ");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-04", "08:00", "12:00"), daniel), requested));

        assertThat(run(List.of(daniel, beto), rows)).containsEntry(requested, daniel);
    }

    @Test
    void fallsBackToTheNormalFlowWhenTheRequesterDriverHasAScheduleConflict() {
        Driver ana = driver("Ana Pérez");
        Driver beto = driver("Beto Mora");
        TourProcessingDraftAssignment requested = requestedBy(sameDayTour("2", "2026-05-05", "09:00", "11:00"), "Ana Perez");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-05", "08:00", "12:00"), ana), requested));

        assertThat(run(List.of(ana, beto), rows)).containsEntry(requested, beto);
    }

    @Test
    void fallsBackToTheNormalFlowWhenTheRequesterDriverCannotTakeAnotherWeekend() {
        Driver ana = driver("Ana Pérez");
        Driver beto = driver("Beto Mora");
        TourProcessingDraftAssignment requested = requestedBy(sameDayTour("9", "2026-05-23", "08:00", "10:00"), "Ana Perez");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-02", "08:00", "10:00"), ana),
                assigned(sameDayTour("2", "2026-05-09", "08:00", "10:00"), ana),
                assigned(sameDayTour("3", "2026-05-16", "08:00", "10:00"), ana),
                assigned(fullSurplusTour("5", "2026-05-11"), ana),
                assigned(fullSurplusTour("6", "2026-05-12"), ana),
                assigned(fullSurplusTour("7", "2026-05-13"), ana),
                assigned(fullSurplusTour("8", "2026-05-04"), beto),
                assigned(fullSurplusTour("10", "2026-05-05"), beto),
                assigned(fullSurplusTour("11", "2026-05-06"), beto),
                requested));

        assertThat(run(List.of(ana, beto), rows)).containsEntry(requested, beto);
    }

    @Test
    void ignoresTheRequesterRuleWhenTheNameMatchesMoreThanOneActiveDriverOrNone() {
        Driver anaMora = driver("Ana Pérez Mora");
        Driver anaSolis = driver("Ana Pérez Solís");
        Driver beto = driver("Beto Rojas");
        TourProcessingDraftAssignment ambiguous = requestedBy(sameDayTour("3", "2026-05-06", "08:00", "10:00"), "Ana Perez");
        TourProcessingDraftAssignment unknown = requestedBy(sameDayTour("4", "2026-05-07", "08:00", "10:00"), "Carlos Ruiz");
        TourProcessingDraftAssignment singleWord = requestedBy(sameDayTour("5", "2026-05-08", "08:00", "10:00"), "Ana");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                assigned(sameDayTour("1", "2026-05-04", "08:00", "12:00"), anaMora),
                assigned(sameDayTour("2", "2026-05-05", "08:00", "12:00"), anaSolis),
                ambiguous, unknown, singleWord));

        List<Driver> catalog = List.of(anaMora, anaSolis, beto);

        Map<TourProcessingDraftAssignment, Driver> withRequesters = run(catalog, rows);
        ambiguous.getTour().setResponsible("Persona Ajena");
        unknown.getTour().setResponsible("Persona Ajena");
        singleWord.getTour().setResponsible("Persona Ajena");
        Map<TourProcessingDraftAssignment, Driver> withoutRequesters = run(catalog, rows);

        assertThat(withRequesters).containsOnlyKeys(ambiguous, unknown, singleWord).isEqualTo(withoutRequesters);
    }

    @Test
    void anInactiveDriverDoesNotMatchTheRequester() {
        Driver ana = driver("Ana Pérez");
        ana.setStatus("INACTIVE");
        Driver beto = driver("Beto Mora");
        TourProcessingDraftAssignment requested = requestedBy(sameDayTour("2", "2026-05-05", "08:00", "10:00"), "Ana Perez");

        assertThat(run(List.of(ana, beto), new ArrayList<>(List.of(requested)))).containsOnlyKeys(requested).containsEntry(requested, beto);
    }

    @Test
    void doesNotTouchToursThatAlreadyHaveADriverEvenIfTheRequesterIsAnotherDriver() {
        Driver ana = driver("Ana Pérez");
        Driver beto = driver("Beto Mora");
        TourProcessingDraftAssignment taken = assigned(requestedBy(sameDayTour("2", "2026-05-05", "08:00", "10:00"), "Ana Perez"), beto);

        assertThat(run(List.of(ana, beto), new ArrayList<>(List.of(taken)))).isEmpty();
    }

    @Test
    void theRequesterRuleDoesNotApplyToThePeripheralOnlyRun() {
        Driver ricardo = afternoonDriver("Ricardo Jovane");
        Driver daniel = afternoonDriver("Daniel Zuniga");
        TourProcessingDraftAssignment ulloa = requestedBy(ulloaTour("1", "2026-05-05", UlloaShift.AFTERNOON), "Daniel Zuniga");
        TourProcessingDraftAssignment regular = requestedBy(sameDayTour("2", "2026-05-06", "08:00", "10:00"), "Ricardo Jovane");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(ulloa, regular));

        Map<TourProcessingDraftAssignment, Driver> result = new DriverAutoAssigner(
                2026, 5, MAY_BUSINESS_DAYS, List.of(ricardo, daniel), rows
        ).assignUlloaOnly(rows);

        assertThat(result).containsOnlyKeys(ulloa);
    }

    @Test
    void theRequesterRuleRunsBeforeThePeripheralPass() {
        Driver ricardo = afternoonDriver("Ricardo Jovane");
        Driver daniel = afternoonDriver("Daniel Zuniga");
        TourProcessingDraftAssignment ulloa = requestedBy(ulloaTour("1", "2026-05-05", UlloaShift.AFTERNOON), "Daniel Zuniga");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(ulloa));

        assertThat(run(List.of(ricardo, daniel), rows)).containsEntry(ulloa, daniel);
    }

    @Test
    void thePeripheralPassUsesTheDriversOrdinaryScheduleToChooseTheShift() {
        Driver standardWindow = ulloaDriver("Mora", UlloaShift.AFTERNOON);
        Driver afternoonWindow = afternoonDriver("Tarde");
        TourProcessingDraftAssignment afternoon = ulloaTour("1", "2026-05-05", UlloaShift.AFTERNOON);
        TourProcessingDraftAssignment morning = ulloaTour("2", "2026-05-05", UlloaShift.MORNING);
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(afternoon, morning));

        Map<TourProcessingDraftAssignment, Driver> result = new DriverAutoAssigner(
                2026, 5, MAY_BUSINESS_DAYS, List.of(standardWindow, afternoonWindow), rows
        ).assignUlloaOnly(rows);

        assertThat(result).containsEntry(afternoon, afternoonWindow).containsEntry(morning, standardWindow);
    }
}
