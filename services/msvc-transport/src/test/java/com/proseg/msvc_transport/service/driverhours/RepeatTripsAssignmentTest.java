package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.entity.Vehicle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.assigned;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.driver;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.sameDayTour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.vehicle;
import static org.assertj.core.api.Assertions.assertThat;

class RepeatTripsAssignmentTest {

    private static final int MAY_BUSINESS_DAYS = 28;

    private List<TourProcessingDraftAssignment> repeatTour(String number, String date, int totalPassengers, int trips) {
        TourProcessingDraftAssignment first = sameDayTour(number, date, "08:00", "10:00");
        TourProcessingDraftTour tour = first.getTour();
        tour.setPassengers(totalPassengers);
        tour.setRepeatsTrips(true);
        List<TourProcessingDraftAssignment> parts = new ArrayList<>();
        parts.add(first);
        for (int index = 1; index < trips; index++) {
            parts.add(tour.addAssignment(TourProcessingDraftAssignment.builder()
                    .id(UUID.randomUUID()).resourceType(ResourceType.INTERNAL).passengers(0).build()));
        }
        for (int index = 0; index < trips; index++) {
            parts.get(index).setPassengers(totalPassengers / trips + (index < totalPassengers % trips ? 1 : 0));
        }
        return parts;
    }

    private TourProcessingDraftAssignment normalTour(String number, String date, int passengers) {
        TourProcessingDraftAssignment tour = sameDayTour(number, date, "08:00", "10:00");
        tour.setPassengers(passengers);
        tour.getTour().setPassengers(passengers);
        return tour;
    }

    private void use(TourProcessingDraftAssignment part, Driver driver, Vehicle vehicle) {
        if (driver != null) {
            assigned(part, driver);
        }
        if (vehicle != null) {
            part.setVehicle(vehicle.getPlate());
            part.setVehicleId(vehicle.getId());
        }
    }

    private Map<TourProcessingDraftAssignment, Driver> assignDrivers(List<Driver> drivers, List<TourProcessingDraftAssignment> rows) {
        List<TourProcessingDraftAssignment> pending = rows.stream().filter(row -> row.getDriverId() == null).toList();
        return new DriverAutoAssigner(2026, 5, MAY_BUSINESS_DAYS, drivers, List.of(), rows, ManualDays.NONE).assign(pending);
    }

    private Map<TourProcessingDraftAssignment, Vehicle> assignVehicles(List<Vehicle> vehicles, List<TourProcessingDraftAssignment> rows) {
        List<TourProcessingDraftAssignment> pending = rows.stream().filter(row -> row.getVehicleId() == null).toList();
        return new VehicleAutoAssigner(vehicles, List.of(), rows).assign(pending);
    }

    @Test
    void recognizesARepeatTourOnlyWhenEveryTripSharesAtMostOneDriverAndOneVehicle() {
        List<TourProcessingDraftAssignment> parts = repeatTour("1", "2026-05-05", 100, 3);
        TourProcessingDraftTour tour = parts.get(0).getTour();
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");

        assertThat(TourTrips.isRepeatTour(tour)).isTrue();
        assertThat(TourTrips.isGroupedTour(tour)).isFalse();

        use(parts.get(0), ana, null);
        use(parts.get(2), ana, null);
        assertThat(TourTrips.isRepeatTour(tour)).isTrue();

        use(parts.get(1), beto, null);
        assertThat(TourTrips.isRepeatTour(tour)).isFalse();
        assertThat(TourTrips.isGroupedTour(tour)).isTrue();
    }

    @Test
    void doesNotTreatToursWithoutTheFlagOrWithExternalTripsAsRepeats() {
        List<TourProcessingDraftAssignment> withoutFlag = repeatTour("1", "2026-05-05", 100, 3);
        withoutFlag.get(0).getTour().setRepeatsTrips(false);
        List<TourProcessingDraftAssignment> withExternal = repeatTour("2", "2026-05-06", 100, 3);
        withExternal.get(1).setResourceType(ResourceType.EXTERNAL);

        assertThat(TourTrips.isRepeatTour(withoutFlag.get(0).getTour())).isFalse();
        assertThat(TourTrips.isGroupedTour(withoutFlag.get(0).getTour())).isTrue();
        assertThat(TourTrips.isRepeatTour(withExternal.get(0).getTour())).isFalse();
    }

    @Test
    void marksEveryTripAfterTheFirstAsFollower() {
        List<TourProcessingDraftAssignment> parts = repeatTour("1", "2026-05-05", 100, 3);

        assertThat(TourTrips.isRepeatFollower(parts.get(0))).isFalse();
        assertThat(TourTrips.isRepeatFollower(parts.get(1))).isTrue();
        assertThat(TourTrips.isRepeatFollower(parts.get(2))).isTrue();
    }

    @Test
    void alignsTheResourcesFoundOnAnyTripAndCopiesTheFirstTripToTheRest() {
        List<TourProcessingDraftAssignment> parts = repeatTour("1", "2026-05-05", 100, 3);
        Driver ana = driver("Ana");
        Vehicle bus = vehicle("301-295", null, 30);
        use(parts.get(2), ana, bus);

        TourTrips.alignRepeatTrips(parts.get(0).getTour());

        assertThat(parts).allSatisfy(part -> {
            assertThat(part.getDriverId()).isEqualTo(ana.getId());
            assertThat(part.getVehicleId()).isEqualTo(bus.getId());
        });

        Driver beto = driver("Beto");
        use(parts.get(0), beto, null);
        TourTrips.copyFirstTripResources(parts.get(0).getTour());

        assertThat(parts).allSatisfy(part -> assertThat(part.getDriverId()).isEqualTo(beto.getId()));
        assertThat(parts).allSatisfy(part -> assertThat(part.getVehicleId()).isEqualTo(bus.getId()));
    }

    @Test
    void assignsOneDriverToTheFirstTripAndIgnoresTheFollowers() {
        Driver ana = driver("Ana");
        List<TourProcessingDraftAssignment> parts = repeatTour("1", "2026-05-05", 100, 3);
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(parts);

        Map<TourProcessingDraftAssignment, Driver> result = assignDrivers(List.of(ana), rows);

        assertThat(result).containsOnlyKeys(parts.get(0));
        assertThat(result).containsEntry(parts.get(0), ana);
    }

    @Test
    void doesNotLetARepeatTourBlockTheSameDriverFromAnotherTourAtAnotherTime() {
        Driver ana = driver("Ana");
        List<TourProcessingDraftAssignment> parts = repeatTour("1", "2026-05-05", 100, 3);
        TourProcessingDraftAssignment later = sameDayTour("2", "2026-05-06", "08:00", "10:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(parts);
        rows.add(later);

        Map<TourProcessingDraftAssignment, Driver> result = assignDrivers(List.of(ana), rows);

        assertThat(result).containsEntry(parts.get(0), ana).containsEntry(later, ana).hasSize(2);
    }

    @Test
    void keepsTheDriverOfARepeatTourAndOnlyAssignsTheOthers() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        List<TourProcessingDraftAssignment> parts = repeatTour("1", "2026-05-05", 100, 3);
        parts.forEach(part -> assigned(part, ana));
        TourProcessingDraftAssignment overlapping = sameDayTour("2", "2026-05-05", "09:00", "11:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(parts);
        rows.add(overlapping);

        Map<TourProcessingDraftAssignment, Driver> result = assignDrivers(List.of(ana, beto), rows);

        assertThat(result).containsOnlyKeys(overlapping);
        assertThat(result).containsEntry(overlapping, beto);
    }

    @Test
    void choosesTheVehicleWithTheFewestTripsForARepeatTour() {
        Vehicle small = vehicle("100-001", null, 13);
        Vehicle medium = vehicle("100-002", null, 30);
        Vehicle large = vehicle("100-003", null, 45);
        List<TourProcessingDraftAssignment> parts = repeatTour("1", "2026-05-05", 100, 3);

        Map<TourProcessingDraftAssignment, Vehicle> result = assignVehicles(List.of(small, medium, large), new ArrayList<>(parts));

        assertThat(result).containsOnlyKeys(parts.get(0));
        assertThat(result).containsEntry(parts.get(0), large);
    }

    @Test
    void prefersTheSmallestVehicleThatCarriesEveryoneInOneTrip() {
        Vehicle small = vehicle("100-001", null, 13);
        Vehicle medium = vehicle("100-002", null, 30);
        Vehicle large = vehicle("100-003", null, 45);
        List<TourProcessingDraftAssignment> parts = repeatTour("1", "2026-05-05", 20, 1);

        Map<TourProcessingDraftAssignment, Vehicle> result = assignVehicles(List.of(small, medium, large), new ArrayList<>(parts));

        assertThat(result).containsEntry(parts.get(0), medium);
    }

    @Test
    void doesNotUseVehiclesBelowTheMinimumCapacityToRepeatTrips() {
        Vehicle sedan = vehicle("100-001", null, 5);
        Vehicle atv = vehicle("100-002", null, 7);
        List<TourProcessingDraftAssignment> parts = repeatTour("1", "2026-05-05", 100, 3);

        Map<TourProcessingDraftAssignment, Vehicle> result = assignVehicles(List.of(sedan, atv), new ArrayList<>(parts));

        assertThat(result).isEmpty();
        assertThat(VehicleAutoAssigner.MIN_REPEAT_CAPACITY).isEqualTo(13);
    }

    @Test
    void servesNormalToursBeforeRepeatToursWhenTheyCompeteForTheLargestVehicle() {
        Vehicle medium = vehicle("100-001", null, 30);
        Vehicle large = vehicle("100-002", null, 45);
        List<TourProcessingDraftAssignment> parts = repeatTour("1", "2026-05-05", 100, 3);
        TourProcessingDraftAssignment normal = normalTour("2", "2026-05-05", 40);
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(parts);
        rows.add(normal);

        Map<TourProcessingDraftAssignment, Vehicle> result = assignVehicles(List.of(medium, large), rows);

        assertThat(result).containsEntry(normal, large).containsEntry(parts.get(0), medium).hasSize(2);
    }

    @Test
    void leavesDistinctGroupsOutOfTheVehicleAssignerWhileRepeatToursStayIn() {
        Vehicle bus = vehicle("100-001", null, 45);
        TourProcessingDraftAssignment first = normalTour("1", "2026-05-05", 40);
        first.setPassengers(20);
        TourProcessingDraftAssignment second = first.getTour().addAssignment(
                TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).resourceType(ResourceType.INTERNAL).passengers(20).build());
        List<TourProcessingDraftAssignment> repeat = repeatTour("2", "2026-05-06", 40, 2);
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(first, second));
        rows.addAll(repeat);

        Map<TourProcessingDraftAssignment, Vehicle> result = assignVehicles(List.of(bus), rows);

        assertThat(result).containsOnlyKeys(repeat.get(0));
    }

    @Test
    void countsTheHoursOfARepeatTourOnceInTheDriverWorkload() {
        Driver ana = driver("Ana");
        Vehicle bus = vehicle("100-001", null, 30);
        List<TourProcessingDraftAssignment> parts = repeatTour("1", "2026-05-05", 90, 3);
        parts.forEach(part -> use(part, ana, bus));
        DriverWorkload workload = new DriverWorkload(ana);
        parts.forEach(workload::add);

        var totals = workload.totals(DriverHoursRules.surplusCap(MAY_BUSINESS_DAYS), DriverHoursRules.monthWeekends(2026, 5));

        assertThat(totals.ordinaryHours() + totals.surplusHours() + totals.overtimeHours()).isEqualTo(2.0);
        assertThat(workload.allocations(DriverHoursRules.surplusCap(MAY_BUSINESS_DAYS))).hasSize(1);
    }
}
