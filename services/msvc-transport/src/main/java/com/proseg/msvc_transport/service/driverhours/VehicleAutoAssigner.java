package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.CleaningDraftRow;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.Vehicle;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class VehicleAutoAssigner {

    private static final Comparator<Vehicle> VEHICLE_ORDER =
            Comparator.comparing(vehicle -> DriverHoursRules.plateKey(vehicle.getPlate()));

    private final List<Vehicle> availableVehicles;
    private final Map<String, Driver> driverCatalogByKey;
    private final Map<UUID, VehicleWorkload> workloads;

    public VehicleAutoAssigner(List<Vehicle> vehicles, List<Driver> drivers, List<CleaningDraftRow> rows) {
        this.availableVehicles = vehicles.stream()
                .filter(Vehicle::isAvailable)
                .sorted(VEHICLE_ORDER)
                .toList();
        this.driverCatalogByKey = DriverHoursRules.catalogByKey(drivers);
        this.workloads = VehicleWorkload.fromDraftRows(rows, DriverHoursRules.vehicleCatalogByPlate(vehicles));
    }

    public Map<CleaningDraftRow, Vehicle> assign(List<CleaningDraftRow> pendingTours) {
        Map<CleaningDraftRow, Vehicle> assignments = new LinkedHashMap<>();
        for (CleaningDraftRow tour : pendingTours.stream().sorted(DriverAutoAssigner.ASSIGNMENT_ORDER).toList()) {
            pickVehicle(tour).ifPresent(vehicle -> {
                workloadOf(vehicle).add(tour);
                assignments.put(tour, vehicle);
            });
        }
        return assignments;
    }

    private Optional<Vehicle> pickVehicle(CleaningDraftRow tour) {
        Driver loadedDriver = driverCatalogByKey.get(DriverHoursRules.driverKey(tour.getDriver()));
        return availableVehicles.stream()
                .filter(vehicle -> hasRoomFor(vehicle, tour))
                .filter(vehicle -> loadedDriver == null || DriverHoursRules.canDrive(loadedDriver, vehicle))
                .filter(vehicle -> !workloadOf(vehicle).overlaps(tour))
                .findFirst();
    }

    private boolean hasRoomFor(Vehicle vehicle, CleaningDraftRow tour) {
        return vehicle.getCapacity() == null || vehicle.getCapacity() >= tour.getPassengers();
    }

    private VehicleWorkload workloadOf(Vehicle vehicle) {
        return workloads.computeIfAbsent(vehicle.getId(), id -> new VehicleWorkload());
    }
}
