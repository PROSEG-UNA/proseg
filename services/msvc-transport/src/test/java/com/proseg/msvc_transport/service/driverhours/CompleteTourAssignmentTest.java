package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.*;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.*;
import static org.assertj.core.api.Assertions.assertThat;

@Timeout(15)
class CompleteTourAssignmentTest {
    private TourProcessingDraftAssignment addPart(TourProcessingDraftAssignment first, int passengers) {
        first.getTour().setPassengers(first.getTour().getPassengers() + passengers);
        return first.getTour().addAssignment(TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).passengers(passengers).build());
    }

    private DriverAutoAssigner assigner(List<Driver> drivers, List<Vehicle> vehicles, List<TourProcessingDraftAssignment> rows) {
        return new DriverAutoAssigner(2026, 5, 28, drivers, vehicles, rows, ManualDays.NONE);
    }

    @Test
    void skipsUnavailableVehiclesWhenAssigningDriversAndVehicles() {
        var first = sameDayTour("1", "2026-05-04", "08:00", "12:00");
        var second = addPart(first, 3);
        var rows = List.of(first, second);
        var unavailable = vehicle("9", null, 5);
        unavailable.setAvailable(false);
        var engine = assigner(List.of(driver("Ana"), driver("Beto")),
                List.of(unavailable, vehicle("1", null, 5), vehicle("2", null, 5)), rows);

        assertThat(engine.assign(rows)).hasSize(2);
        assertThat(engine.vehicleAssignments().values()).extracting(Vehicle::getPlate).containsExactlyInAnyOrder("1", "2");
    }

    @Test
    void doesNotAssignATourWhenEveryVehicleIsUnavailable() {
        var first = sameDayTour("1", "2026-05-04", "08:00", "12:00");
        var second = addPart(first, 3);
        var rows = List.of(first, second);
        var unavailable = vehicle("9", null, 5);
        unavailable.setAvailable(false);
        var engine = assigner(List.of(driver("Ana"), driver("Beto")), List.of(unavailable), rows);

        assertThat(engine.assign(rows)).isEmpty();
        assertThat(engine.vehicleAssignments()).isEmpty();
    }

    @Test
    void assignsEveryPartWithDifferentDriversAndVehicles() {
        var first = sameDayTour("1", "2026-05-04", "08:00", "12:00");
        var second = addPart(first, 3);
        var rows = List.of(first, second);
        var engine = assigner(List.of(driver("Ana"), driver("Beto")),
                List.of(vehicle("1", null, 5), vehicle("2", null, 5)), rows);
        var result = engine.assign(rows);
        assertThat(result).hasSize(2);
        assertThat(new HashSet<>(result.values())).hasSize(2);
        assertThat(new HashSet<>(engine.vehicleAssignments().values())).hasSize(2);
        assertThat(first.getDriverId()).isNull();
        assertThat(first.getVehicleId()).isNull();
    }

    @Test
    void doesNotReserveDriversWhenVehiclesCannotCoverTheWholeTour() {
        var first = sameDayTour("1", "2026-05-04", "08:00", "12:00");
        var second = addPart(first, 3);
        var single = sameDayTour("2", "2026-05-04", "08:00", "12:00");
        var rows = List.of(first, second, single);
        var engine = assigner(List.of(driver("Ana"), driver("Beto")), List.of(vehicle("1", null, 5)), rows);
        assertThat(engine.assign(rows)).containsOnlyKeys(single);
        assertThat(engine.vehicleAssignments()).isEmpty();
        assertThat(engine.reasonsForUnassigned(first, false)).anyMatch(reason -> reason.contains("completa"));
    }

    @Test
    void preservesManualPartAndCompletesItsRemainingResources() {
        var ana = driver("Ana");
        var beto = driver("Beto");
        var first = assigned(sameDayTour("1", "2026-05-04", "08:00", "12:00"), ana);
        var second = addPart(first, 3);
        var rows = List.of(first, second);
        var engine = assigner(List.of(ana, beto), List.of(vehicle("1", null, 5), vehicle("2", null, 5)), rows);
        assertThat(engine.assign(List.of(second))).containsOnlyKeys(second).containsEntry(second, beto);
        assertThat(first.getDriverId()).isEqualTo(ana.getId());
        assertThat(engine.vehicleAssignments()).hasSize(2);
    }

    @Test
    void rollsBackSearchWhenTwoLargePartsCompeteForOneBus() {
        var first = sameDayTour("1", "2026-05-04", "08:00", "12:00");
        first.setPassengers(40);
        first.getTour().setPassengers(40);
        var second = addPart(first, 40);
        var third = addPart(first, 5);
        var single = sameDayTour("2", "2026-05-04", "08:00", "12:00");
        var rows = List.of(first, second, third, single);
        var engine = assigner(List.of(driver("Ana"), driver("Beto"), driver("Caro")),
                List.of(vehicle("1", null, 40), vehicle("2", null, 5), vehicle("3", null, 5)), rows);
        assertThat(engine.assign(rows)).containsOnlyKeys(single);
        assertThat(engine.vehicleAssignments()).isEmpty();
    }

    @Test
    void vehicleOnlyAssignerCannotCreatePartialDistributedTours() {
        var first = sameDayTour("1", "2026-05-04", "08:00", "12:00");
        var second = addPart(first, 3);
        var rows = List.of(first, second);
        assertThat(new VehicleAutoAssigner(List.of(vehicle("1", null, 5)), List.of(), rows).assign(rows)).isEmpty();
    }
}
