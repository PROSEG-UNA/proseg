package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.entity.VehicleType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.assigned;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.driver;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.sameDayTour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.tour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.vehicle;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.withVehicle;
import static org.assertj.core.api.Assertions.assertThat;

class VehicleAutoAssignerTest {

    private Map<TourProcessingDraftAssignment, Vehicle> run(List<Vehicle> vehicles, List<Driver> drivers, List<TourProcessingDraftAssignment> rows) {
        List<TourProcessingDraftAssignment> pending = rows.stream().filter(row -> row.getVehicle() == null).toList();
        var linker = com.proseg.msvc_transport.service.DraftCatalogLinker.of(drivers, vehicles);
        rows.forEach(linker::link);
        return new VehicleAutoAssigner(vehicles, drivers, rows).assign(pending);
    }

    private TourProcessingDraftAssignment tourFor(int passengers) {
        TourProcessingDraftAssignment tour = sameDayTour("1", "2026-05-06", "08:00", "10:00");
        tour.setPassengers(passengers);
        tour.getTour().setPassengers(passengers);
        return tour;
    }

    @Test
    void rejectsVehiclesWithoutCapacityAndTooSmallOnes() {
        Vehicle small = vehicle("100-001", VehicleType.SEDAN, 4);
        Vehicle unknownCapacity = vehicle("200-002", VehicleType.MICROBUS, null);
        TourProcessingDraftAssignment tour = tourFor(10);

        assertThat(run(List.of(small, unknownCapacity), List.of(), List.of(tour))).isEmpty();
    }

    @Test
    void acceptsVehicleWhoseCapacityEqualsPassengers() {
        Vehicle exact = vehicle("100-001", VehicleType.MICROBUS, 10);
        TourProcessingDraftAssignment tour = tourFor(10);

        assertThat(run(List.of(exact), List.of(), List.of(tour))).containsEntry(tour, exact);
    }

    @Test
    void skipsUnavailableVehicles() {
        Vehicle unavailable = vehicle("100-001", VehicleType.SEDAN, 5);
        unavailable.setAvailable(false);
        TourProcessingDraftAssignment tour = tourFor(3);

        assertThat(run(List.of(unavailable), List.of(), List.of(tour))).isEmpty();
    }

    @Test
    void skipsVehiclesAlreadyBusyInAnOverlappingTour() {
        Vehicle busy = vehicle("100-001", VehicleType.SEDAN, 5);
        Vehicle free = vehicle("200-002", VehicleType.SEDAN, 5);
        TourProcessingDraftAssignment pending = sameDayTour("2", "2026-05-06", "11:00", "14:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                withVehicle(sameDayTour("1", "2026-05-06", "08:00", "12:00"), " 100-001 "),
                pending
        ));

        assertThat(run(List.of(busy, free), List.of(), rows)).containsEntry(pending, free);
    }

    @Test
    void keepsTheSameVehicleForEveryDayAndSkipsVehiclesBusyMidTour() {
        Vehicle busyMidTour = vehicle("100-001", VehicleType.MICROBUS, 20);
        Vehicle free = vehicle("200-002", VehicleType.MICROBUS, 20);
        TourProcessingDraftAssignment multiDay = tour("2", "2026-05-05", "06:00", "2026-05-08", "18:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(
                withVehicle(sameDayTour("1", "2026-05-07", "09:00", "11:00"), "100-001"),
                multiDay
        ));

        assertThat(run(List.of(busyMidTour, free), List.of(), rows)).containsEntry(multiDay, free);
    }

    @Test
    void onlyPicksVehicleTypesTheLoadedDriverIsAllowedToDrive() {
        Driver ana = driver("Ana", VehicleType.SEDAN);
        Vehicle microbus = vehicle("100-001", VehicleType.MICROBUS, 15);
        Vehicle sedan = vehicle("200-002", VehicleType.SEDAN, 5);
        TourProcessingDraftAssignment tour = assigned(tourFor(3), ana);
        tour.setDriver("  ana ");

        assertThat(run(List.of(microbus, sedan), List.of(ana), List.of(tour))).containsEntry(tour, sedan);
        assertThat(run(List.of(microbus), List.of(ana), List.of(tour))).isEmpty();
    }

    @Test
    void ignoresDriverRestrictionsForUntypedVehiclesAndUnknownDrivers() {
        Driver ana = driver("Ana", VehicleType.SEDAN);
        Vehicle untyped = vehicle("100-001", null, 15);
        Vehicle microbus = vehicle("200-002", VehicleType.MICROBUS, 15);
        TourProcessingDraftAssignment withAna = assigned(sameDayTour("1", "2026-05-06", "08:00", "10:00"), ana);
        TourProcessingDraftAssignment withUnknownDriver = sameDayTour("2", "2026-05-07", "08:00", "10:00");
        withUnknownDriver.setDriver("Chofer Fantasma");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(withAna, withUnknownDriver));

        assertThat(run(List.of(untyped), List.of(ana), List.of(withAna))).containsEntry(withAna, untyped);
        assertThat(run(List.of(microbus), List.of(ana), rows)).containsOnlyKeys(withUnknownDriver);
    }

    @Test
    void breaksTiesByPlate() {
        Vehicle second = vehicle("SEG-200", VehicleType.SEDAN, 5);
        Vehicle first = vehicle("seg-100", VehicleType.SEDAN, 5);
        TourProcessingDraftAssignment tour = tourFor(3);

        assertThat(run(List.of(second, first), List.of(), List.of(tour))).containsEntry(tour, first);
    }

    @Test
    void processesToursInTheDriverPriorityOrder() {
        Vehicle onlyVehicle = vehicle("100-001", VehicleType.SEDAN, 5);
        TourProcessingDraftAssignment priorityTwo = sameDayTour("1", "2026-05-06", "08:00", "10:00");
        priorityTwo.getTour().setPriority(2);
        TourProcessingDraftAssignment priorityOne = sameDayTour("2", "2026-05-06", "09:00", "11:00");
        List<TourProcessingDraftAssignment> rows = new ArrayList<>(List.of(priorityTwo, priorityOne));

        assertThat(run(List.of(onlyVehicle), List.of(), rows)).containsOnlyKeys(priorityOne);
    }

    @Test
    void explainsWhyATourStaysWithoutVehicle() {
        Driver ana = driver("Ana", VehicleType.SEDAN);
        Vehicle sedan = vehicle("100-001", VehicleType.SEDAN, 5);
        Vehicle microbus = vehicle("200-002", VehicleType.MICROBUS, 20);
        TourProcessingDraftAssignment tour = tourFor(3);
        TourProcessingDraftAssignment assignedToAna = assigned(tourFor(3), ana);
        TourProcessingDraftAssignment first = sameDayTour("1", "2026-05-06", "08:00", "10:00");
        TourProcessingDraftAssignment second = sameDayTour("2", "2026-05-06", "09:00", "11:00");
        VehicleAutoAssigner busy = new VehicleAutoAssigner(List.of(sedan), List.of(), List.of(first, second));
        busy.assign(List.of(first, second));

        assertThat(new VehicleAutoAssigner(List.of(), List.of(), List.of(tour)).reasonsForUnassigned(tour))
                .containsExactly("No hay vehículos disponibles en el catálogo.");
        assertThat(new VehicleAutoAssigner(List.of(vehicle("300-003", VehicleType.SEDAN, 2)), List.of(), List.of(tour)).reasonsForUnassigned(tour))
                .containsExactly("Ningún vehículo disponible tiene capacidad registrada y suficiente para 3 pasajeros.");
        assertThat(new VehicleAutoAssigner(List.of(microbus), List.of(ana), List.of(assignedToAna)).reasonsForUnassigned(assignedToAna))
                .containsExactly("El chofer Ana no puede conducir los vehículos con capacidad suficiente para esta gira.");
        assertThat(busy.reasonsForUnassigned(second))
                .containsExactly("Los vehículos con capacidad suficiente están ocupados durante el horario de esta gira.");
    }

    @Test
    void assignsVehicleToTourWithDriverBeforeHigherPriorityTourWithoutDriver() {
        Driver ana = driver("Ana", VehicleType.SEDAN);
        Vehicle onlyVehicle = vehicle("100-001", VehicleType.SEDAN, 5);
        TourProcessingDraftAssignment withDriver = assigned(sameDayTour("1", "2026-05-06", "08:00", "10:00"), ana);
        withDriver.getTour().setPriority(2);
        TourProcessingDraftAssignment withoutDriver = sameDayTour("2", "2026-05-06", "09:00", "11:00");

        assertThat(run(List.of(onlyVehicle), List.of(ana), List.of(withoutDriver, withDriver)))
                .containsOnlyKeys(withDriver);
    }

    @Test
    void reservesLargerVehicleForTourWithMorePassengers() {
        Vehicle microbus = vehicle("100-001", VehicleType.MICROBUS, 20);
        Vehicle sedan = vehicle("200-002", VehicleType.SEDAN, 4);
        TourProcessingDraftAssignment small = tourFor(3);
        TourProcessingDraftAssignment large = sameDayTour("2", "2026-05-06", "09:00", "11:00");
        large.setPassengers(15);
        large.getTour().setPassengers(15);

        assertThat(run(List.of(microbus, sedan), List.of(), List.of(small, large)))
                .containsEntry(small, sedan)
                .containsEntry(large, microbus);
    }

    @Test
    void movesNewVehicleAssignmentToCompleteLaterTour() {
        Driver ana = driver("Ana", VehicleType.SEDAN, VehicleType.MICROBUS);
        Driver beto = driver("Beto", VehicleType.SEDAN);
        Vehicle sedan = vehicle("100-001", VehicleType.SEDAN, 4);
        Vehicle microbus = vehicle("200-002", VehicleType.MICROBUS, 10);
        TourProcessingDraftAssignment first = assigned(sameDayTour("1", "2026-05-06", "08:00", "10:00"), ana);
        TourProcessingDraftAssignment second = assigned(sameDayTour("2", "2026-05-06", "09:00", "11:00"), beto);

        assertThat(run(List.of(sedan, microbus), List.of(ana, beto), List.of(first, second)))
                .containsEntry(first, microbus)
                .containsEntry(second, sedan);
    }
}
