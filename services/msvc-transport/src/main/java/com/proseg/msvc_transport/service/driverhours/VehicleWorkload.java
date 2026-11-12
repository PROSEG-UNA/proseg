package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.Vehicle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class VehicleWorkload {

    private final List<TourProcessingDraftAssignment> tours = new ArrayList<>();

    public static Map<UUID, VehicleWorkload> fromAssignments(List<TourProcessingDraftAssignment> rows, Map<UUID, Vehicle> vehiclesById) {
        Map<UUID, VehicleWorkload> workloads = new LinkedHashMap<>();
        for (TourProcessingDraftAssignment row : rows) {
            Vehicle vehicle = DriverHoursRules.assignedVehicle(row, vehiclesById);
            if (vehicle != null && row.isActive()) {
                workloads.computeIfAbsent(vehicle.getId(), id -> new VehicleWorkload()).add(row);
            }
        }
        return workloads;
    }

    public void add(TourProcessingDraftAssignment tour) {
        tours.add(tour);
    }

    public boolean overlaps(TourProcessingDraftAssignment tour) {
        return findOverlap(tour).isPresent();
    }

    public Optional<TourProcessingDraftAssignment> findOverlap(TourProcessingDraftAssignment tour) {
        return tours.stream().filter(existing -> !DriverWorkload.sharedVehicleTrip(existing, tour) && DriverHoursRules.toursOverlap(existing, tour)).findFirst();
    }
}
