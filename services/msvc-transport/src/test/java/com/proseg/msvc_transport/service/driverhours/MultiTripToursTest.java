package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.Vehicle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.assigned;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.driver;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.sameDayTour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.vehicle;
import static org.assertj.core.api.Assertions.assertThat;

class MultiTripToursTest {

    private static final Set<java.time.LocalDate> MAY_WEEKENDS = DriverHoursRules.monthWeekends(2026, 5);
    private static final double SURPLUS_CAP = DriverHoursRules.surplusCap(28);

    private List<TourProcessingDraftAssignment> trips(TourProcessingDraftAssignment first, int count, int passengersPerTrip) {
        List<TourProcessingDraftAssignment> trips = new ArrayList<>();
        first.setPassengers(passengersPerTrip);
        first.getTour().setPassengers(passengersPerTrip * count);
        trips.add(first);
        for (int index = 1; index < count; index++) {
            trips.add(first.getTour().addAssignment(TourProcessingDraftAssignment.builder()
                    .id(UUID.randomUUID()).passengers(passengersPerTrip).resourceType(ResourceType.INTERNAL).build()));
        }
        return trips;
    }

    private void use(TourProcessingDraftAssignment trip, Driver driver, Vehicle vehicle) {
        assigned(trip, driver);
        trip.setVehicle(vehicle.getPlate());
        trip.setVehicleId(vehicle.getId());
    }

    private DriverWorkload workloadOf(Driver driver, List<TourProcessingDraftAssignment> rows) {
        DriverWorkload workload = new DriverWorkload(driver);
        rows.stream().filter(row -> driver.getId().equals(row.getDriverId())).forEach(workload::add);
        return workload;
    }

    @Test
    void countsTheTourHoursOnceForADriverWhoMakesFiveTrips() {
        Driver ana = driver("Ana");
        Vehicle bus = vehicle("301-295", null, 30);
        var trips = trips(sameDayTour("1", "2026-05-04", "07:30", "17:00"), 5, 30);
        trips.forEach(trip -> use(trip, ana, bus));

        var totals = workloadOf(ana, trips).totals(SURPLUS_CAP, MAY_WEEKENDS);

        assertThat(totals.surplusHours()).isEqualTo(0.5);
        assertThat(totals.overtimeHours()).isZero();
        assertThat(totals.ordinaryHours()).isEqualTo(9);
        assertThat(workloadOf(ana, trips).allocations(SURPLUS_CAP)).hasSize(1);
        assertThat(workloadOf(ana, trips).allocations(SURPLUS_CAP).get(0).tour()).isSameAs(trips.get(0));
    }

    @Test
    void givesEachOfTwoDriversTheHoursOnceWhenTheyAlternateSixTrips() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        Vehicle bus = vehicle("301-295", null, 30);
        Vehicle van = vehicle("301-296", null, 30);
        var trips = trips(sameDayTour("1", "2026-05-04", "07:30", "17:00"), 6, 30);
        for (int index = 0; index < trips.size(); index++) {
            use(trips.get(index), index % 2 == 0 ? ana : beto, index % 2 == 0 ? bus : van);
        }

        var anaTotals = workloadOf(ana, trips).totals(SURPLUS_CAP, MAY_WEEKENDS);
        var betoTotals = workloadOf(beto, trips).totals(SURPLUS_CAP, MAY_WEEKENDS);

        assertThat(anaTotals.surplusHours()).isEqualTo(0.5);
        assertThat(betoTotals.surplusHours()).isEqualTo(0.5);
        assertThat(workloadOf(ana, trips).tours()).containsExactly(trips.get(0));
        assertThat(workloadOf(beto, trips).tours()).containsExactly(trips.get(1));
    }

    @Test
    void doesNotStackTheTripsOfALongTourAgainstTheDailyLimit() {
        Driver ana = driver("Ana");
        Vehicle bus = vehicle("301-295", null, 30);
        var trips = trips(sameDayTour("1", "2026-05-04", "08:00", "18:00"), 5, 30);
        trips.forEach(trip -> use(trip, ana, bus));
        var otherTourSameDay = sameDayTour("2", "2026-05-04", "18:00", "21:00");
        var rows = new ArrayList<>(trips);
        rows.add(otherTourSameDay);
        var engine = new DriverAutoAssigner(2026, 5, 28, List.of(ana), List.of(bus), rows, ManualDays.NONE);

        assertThat(workloadOf(ana, trips).hoursByDate().get(java.time.LocalDate.of(2026, 5, 4))).isEqualTo(10);
        assertThat(engine.rejectionFor(ana, otherTourSameDay)).get()
                .extracting(DriverRejection::reason).isEqualTo(DriverRejection.Reason.DAY_HOURS_EXCEEDED);
        assertThat(engine.preservedAssignmentWarnings()).isEmpty();
    }

    @Test
    void autoAssignmentLeavesDefinedMultiTripToursAloneAndKeepsOtherResultsIntact() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        Vehicle bus = vehicle("301-295", null, 30);
        var trips = trips(sameDayTour("1", "2026-05-04", "08:00", "12:00"), 5, 30);
        trips.forEach(trip -> use(trip, ana, bus));
        var single = sameDayTour("2", "2026-05-11", "08:00", "12:00");
        var rows = new ArrayList<>(trips);
        rows.add(single);
        var singleTripEquivalent = assigned(sameDayTour("1", "2026-05-04", "08:00", "12:00"), ana);
        var comparableSingle = sameDayTour("2", "2026-05-11", "08:00", "12:00");

        var withTrips = new DriverAutoAssigner(2026, 5, 28, List.of(ana, beto), List.of(bus), rows, ManualDays.NONE).assign(List.of(single));
        var withoutTrips = new DriverAutoAssigner(2026, 5, 28, List.of(ana, beto), List.of(bus),
                List.of(singleTripEquivalent, comparableSingle), ManualDays.NONE).assign(List.of(comparableSingle));

        assertThat(withTrips).containsOnlyKeys(single);
        assertThat(withTrips.get(single)).isEqualTo(withoutTrips.get(comparableSingle));
        trips.forEach(trip -> assertThat(trip.getDriverId()).isEqualTo(ana.getId()));
    }

    @Test
    void autoAssignmentDoesNotReportFailuresForDefinedMultiTripToursWhenTheyArePending() {
        Driver ana = driver("Ana");
        Vehicle bus = vehicle("301-295", null, 30);
        var trips = trips(sameDayTour("1", "2026-05-04", "08:00", "12:00"), 3, 30);
        trips.forEach(trip -> use(trip, ana, bus));
        var engine = new DriverAutoAssigner(2026, 5, 28, List.of(ana), List.of(bus), trips, ManualDays.NONE);

        assertThat(engine.assign(trips)).isEmpty();
        assertThat(engine.preservedAssignmentWarnings()).isEmpty();
        assertThat(engine.reasonsForUnassigned(trips.get(1), false)).noneMatch(reason -> reason.contains("completa"));
    }

    @Test
    void exposesTripCountAndTotalCapacityOfATour() {
        Driver ana = driver("Ana");
        Vehicle bus = vehicle("301-295", null, 30);
        var trips = trips(sameDayTour("1", "2026-05-04", "08:00", "12:00"), 5, 30);
        trips.forEach(trip -> use(trip, ana, bus));

        TourTrips summary = TourTrips.of(trips.get(0).getTour(), DriverHoursRules.vehiclesById(List.of(bus)));

        assertThat(summary.trips()).isEqualTo(5);
        assertThat(summary.totalCapacity()).isEqualTo(150);
        assertThat(summary.assignedPassengers()).isEqualTo(150);
        assertThat(summary.distinctDrivers()).isEqualTo(1);
        assertThat(TourTrips.sharesResources(trips.get(0).getTour())).isTrue();
    }

    @Test
    void reportsUnknownTotalCapacityAndDistinctDriversForDifferentVehicles() {
        Driver ana = driver("Ana");
        Driver beto = driver("Beto");
        Vehicle bus = vehicle("301-295", null, 30);
        Vehicle unknown = vehicle("301-296", null, null);
        var trips = trips(sameDayTour("1", "2026-05-04", "08:00", "12:00"), 2, 20);
        use(trips.get(0), ana, bus);
        use(trips.get(1), beto, unknown);

        TourTrips summary = TourTrips.of(trips.get(0).getTour(), DriverHoursRules.vehiclesById(List.of(bus, unknown)));

        assertThat(summary.trips()).isEqualTo(2);
        assertThat(summary.totalCapacity()).isNull();
        assertThat(summary.distinctDrivers()).isEqualTo(2);
        assertThat(TourTrips.sharesResources(trips.get(0).getTour())).isFalse();
    }

    @Test
    void aSingleTripTourHasOneTripAndItsOwnCapacity() {
        Vehicle bus = vehicle("301-295", null, 30);
        var single = sameDayTour("1", "2026-05-04", "08:00", "12:00");
        single.setVehicleId(bus.getId());

        TourTrips summary = TourTrips.of(single.getTour(), DriverHoursRules.vehiclesById(List.of(bus)));

        assertThat(summary.trips()).isEqualTo(1);
        assertThat(summary.totalCapacity()).isEqualTo(30);
        assertThat(TourTrips.sharesResources(single.getTour())).isFalse();
    }
}
