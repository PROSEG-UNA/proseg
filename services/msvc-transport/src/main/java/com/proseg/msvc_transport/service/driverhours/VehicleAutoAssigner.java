package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.Vehicle;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VehicleAutoAssigner {

    private static final int MAX_REASSIGNMENT_ATTEMPTS = 10_000;
    private static final Comparator<Vehicle> VEHICLE_ORDER = Comparator
            .comparing(Vehicle::getCapacity, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(vehicle -> DriverHoursRules.plateKey(vehicle.getPlate()));

    private final List<Vehicle> availableVehicles;
    private final Map<UUID, Driver> driversById;
    private final Map<UUID, VehicleWorkload> workloads;
    private final Set<TourProcessingDraftAssignment> searchLimitTours = new LinkedHashSet<>();

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
        Set<String> visited = new LinkedHashSet<>();
        for (TourProcessingDraftAssignment tour : pendingTours.stream().filter(part -> !part.getTour().isDistributed())
                .sorted(Comparator.comparing((TourProcessingDraftAssignment part) -> part.getDriverId() == null)
                        .thenComparing(DriverAutoAssigner.ASSIGNMENT_ORDER)).toList()) {
            if (!visited.add(tour.getNumber().toLowerCase(Locale.ROOT) + ":" + tour.getPart())) continue;
            int[] attempts = {0};
            Map<TourProcessingDraftAssignment, Vehicle> proposed = search(tour, assignments, Set.of(), attempts);
            if (proposed != null) {
                assignments.clear();
                assignments.putAll(proposed);
            } else if (attempts[0] >= MAX_REASSIGNMENT_ATTEMPTS) {
                searchLimitTours.add(tour);
            }
        }
        return assignments;
    }

    public List<String> reasonsForUnassigned(TourProcessingDraftAssignment tour) {
        if (availableVehicles.isEmpty()) {
            return List.of("No hay vehículos disponibles en el catálogo.");
        }
        if (availableVehicles.stream().noneMatch(vehicle -> hasRoomFor(vehicle, tour))) {
            return List.of("Ningún vehículo disponible tiene capacidad registrada y suficiente para " + tour.getPassengers() + " pasajeros.");
        }
        Driver loadedDriver = DriverHoursRules.assignedDriver(tour, driversById);
        if (loadedDriver != null && availableVehicles.stream().filter(vehicle -> hasRoomFor(vehicle, tour))
                .noneMatch(vehicle -> DriverHoursRules.canDrive(loadedDriver, vehicle))) {
            return List.of("El chofer " + loadedDriver.getName() + " no puede conducir los vehículos con capacidad suficiente para esta gira.");
        }
        if (searchLimitTours.contains(tour)) {
            return List.of("Se alcanzó el límite de búsqueda sin encontrar un vehículo para esta gira.");
        }
        return List.of("Los vehículos con capacidad suficiente están ocupados durante el horario de esta gira.");
    }

    private Map<TourProcessingDraftAssignment, Vehicle> search(TourProcessingDraftAssignment tour,
                                                                Map<TourProcessingDraftAssignment, Vehicle> assignments,
                                                                Set<TourProcessingDraftAssignment> visited, int[] attempts) {
        if (visited.contains(tour) || ++attempts[0] >= MAX_REASSIGNMENT_ATTEMPTS) return null;
        Set<TourProcessingDraftAssignment> nextVisited = new LinkedHashSet<>(visited);
        nextVisited.add(tour);
        for (Vehicle vehicle : candidates(tour, assignments)) {
            List<TourProcessingDraftAssignment> conflicting = assignments.entrySet().stream()
                    .filter(entry -> entry.getValue().getId().equals(vehicle.getId())
                            && DriverHoursRules.toursOverlap(entry.getKey(), tour))
                    .map(Map.Entry::getKey).toList();
            if (conflicting.stream().anyMatch(visited::contains)) continue;
            Map<TourProcessingDraftAssignment, Vehicle> proposed = new LinkedHashMap<>(assignments);
            proposed.put(tour, vehicle);
            boolean complete = true;
            for (TourProcessingDraftAssignment other : conflicting) {
                if (!vehicle.equals(proposed.get(other))) continue;
                proposed.remove(other);
                proposed = search(other, proposed, nextVisited, attempts);
                if (proposed == null) {
                    complete = false;
                    break;
                }
            }
            if (complete) return proposed;
        }
        return null;
    }

    private List<Vehicle> candidates(TourProcessingDraftAssignment tour, Map<TourProcessingDraftAssignment, Vehicle> assignments) {
        Driver loadedDriver = DriverHoursRules.assignedDriver(tour, driversById);
        return availableVehicles.stream()
                .filter(vehicle -> hasRoomFor(vehicle, tour))
                .filter(vehicle -> loadedDriver == null || DriverHoursRules.canDrive(loadedDriver, vehicle))
                .filter(vehicle -> !workloadOf(vehicle).overlaps(tour))
                .sorted(Comparator.comparing((Vehicle vehicle) -> assignments.entrySet().stream()
                                .anyMatch(entry -> entry.getValue().getId().equals(vehicle.getId())
                                        && DriverHoursRules.toursOverlap(entry.getKey(), tour)))
                        .thenComparing(VEHICLE_ORDER))
                .toList();
    }

    private boolean hasRoomFor(Vehicle vehicle, TourProcessingDraftAssignment tour) {
        return vehicle.getCapacity() != null && vehicle.getCapacity() >= tour.getPassengers();
    }

    private VehicleWorkload workloadOf(Vehicle vehicle) {
        return workloads.computeIfAbsent(vehicle.getId(), id -> new VehicleWorkload());
    }
}
