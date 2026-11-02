package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
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
    private final Map<UUID, Driver> driversById;
    private final Map<UUID, VehicleWorkload> workloads;

    public VehicleAutoAssigner(List<Vehicle> vehicles, List<Driver> drivers, List<TourProcessingDraftAssignment> rows) {
        this.availableVehicles = vehicles.stream()
                .filter(Vehicle::isAvailable)
                .sorted(VEHICLE_ORDER)
                .toList();
        this.driversById = DriverHoursRules.driversById(drivers);
        this.workloads = VehicleWorkload.fromAssignments(rows, DriverHoursRules.vehiclesById(vehicles));
    }

    public Map<TourProcessingDraftAssignment, Vehicle> assign(List<TourProcessingDraftAssignment> pendingTours) {
        Map<TourProcessingDraftAssignment, Vehicle> assignments = new LinkedHashMap<>();
        for (TourProcessingDraftAssignment tour : pendingTours.stream().filter(part -> !part.getTour().isDistributed()).sorted(DriverAutoAssigner.ASSIGNMENT_ORDER).toList()) {
            pickVehicle(tour).ifPresent(vehicle -> {
                workloadOf(vehicle).add(tour);
                assignments.put(tour, vehicle);
            });
        }
        return assignments;
    }

    private Optional<Vehicle> pickVehicle(TourProcessingDraftAssignment tour) {
        Driver loadedDriver = DriverHoursRules.assignedDriver(tour, driversById);
        return availableVehicles.stream()
                .filter(vehicle -> hasRoomFor(vehicle, tour))
                .filter(vehicle -> loadedDriver == null || DriverHoursRules.canDrive(loadedDriver, vehicle))
                .filter(vehicle -> !workloadOf(vehicle).overlaps(tour))
                .findFirst();
    }

    private boolean hasRoomFor(Vehicle vehicle, TourProcessingDraftAssignment tour) {
        return vehicle.getCapacity() == null || vehicle.getCapacity() >= tour.getPassengers();
    }

    private VehicleWorkload workloadOf(Vehicle vehicle) {
        return workloads.computeIfAbsent(vehicle.getId(), id -> new VehicleWorkload());
    }
}
