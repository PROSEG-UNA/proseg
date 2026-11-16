package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.entity.Vehicle;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record TourTrips(int trips, Integer totalCapacity, int assignedPassengers, int distinctDrivers) {

    public static TourTrips of(TourProcessingDraftTour tour, Map<UUID, Vehicle> vehiclesById) {
        List<TourProcessingDraftAssignment> parts = tour.getAssignments();
        Integer totalCapacity = parts.isEmpty() ? null : 0;
        int assignedPassengers = 0;
        for (TourProcessingDraftAssignment part : parts) {
            Integer capacity = capacityOf(part, vehiclesById);
            totalCapacity = totalCapacity == null || capacity == null ? null : totalCapacity + capacity;
            assignedPassengers += part.getPassengers() == null ? 0 : part.getPassengers();
        }
        return new TourTrips(Math.max(1, parts.size()), totalCapacity, assignedPassengers, distinctDrivers(tour));
    }

    public static int distinctDrivers(TourProcessingDraftTour tour) {
        Set<String> drivers = new HashSet<>();
        List<TourProcessingDraftAssignment> parts = tour.getAssignments();
        for (int index = 0; index < parts.size(); index++) {
            drivers.add(driverKey(parts.get(index), index));
        }
        return Math.max(1, drivers.size());
    }

    public static boolean sharesResources(TourProcessingDraftTour tour) {
        if (tour.isRepeatsTrips()) {
            return true;
        }
        Set<UUID> drivers = new HashSet<>();
        Set<Object> vehicles = new HashSet<>();
        for (TourProcessingDraftAssignment part : tour.getAssignments()) {
            if (part.getResourceType() == ResourceType.EXTERNAL) {
                continue;
            }
            if (part.getDriverId() != null && !drivers.add(part.getDriverId())) {
                return true;
            }
            Object vehicle = part.getResourceType() == ResourceType.BORROWED_VEHICLE
                    ? (part.getBorrowedVehicle() == null ? null : part.getBorrowedVehicle().getId())
                    : part.getVehicleId();
            if (vehicle != null && !vehicles.add(vehicle)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isRepeatTour(TourProcessingDraftTour tour) {
        if (!tour.isRepeatsTrips() || tour.getAssignments().isEmpty()) {
            return false;
        }
        Set<UUID> drivers = new HashSet<>();
        Set<UUID> vehicles = new HashSet<>();
        for (TourProcessingDraftAssignment part : tour.getAssignments()) {
            if (part.getResourceType() != ResourceType.INTERNAL) {
                return false;
            }
            if (part.getDriverId() != null) {
                drivers.add(part.getDriverId());
            }
            if (part.getVehicleId() != null) {
                vehicles.add(part.getVehicleId());
            }
        }
        return drivers.size() <= 1 && vehicles.size() <= 1;
    }

    public static boolean isRepeatFollower(TourProcessingDraftAssignment part) {
        TourProcessingDraftTour tour = part.getTour();
        return tour != null && part != tour.primaryAssignment() && isRepeatTour(tour);
    }

    public static boolean isGroupedTour(TourProcessingDraftTour tour) {
        return tour.isDistributed() && !isRepeatTour(tour);
    }

    public static void alignRepeatTrips(TourProcessingDraftTour tour) {
        if (!isRepeatTour(tour)) {
            return;
        }
        TourProcessingDraftAssignment driverSource = tour.getAssignments().stream()
                .filter(part -> part.getDriverId() != null).findFirst().orElse(null);
        TourProcessingDraftAssignment vehicleSource = tour.getAssignments().stream()
                .filter(part -> part.getVehicleId() != null).findFirst().orElse(null);
        for (TourProcessingDraftAssignment part : tour.getAssignments()) {
            if (driverSource != null) {
                part.setDriver(driverSource.getDriver());
                part.setDriverId(driverSource.getDriverId());
            }
            if (vehicleSource != null) {
                part.setVehicle(vehicleSource.getVehicle());
                part.setVehicleId(vehicleSource.getVehicleId());
            }
        }
    }

    public static void copyFirstTripResources(TourProcessingDraftTour tour) {
        TourProcessingDraftAssignment first = tour.primaryAssignment();
        for (TourProcessingDraftAssignment part : tour.getAssignments()) {
            if (part != first) {
                part.setDriver(first.getDriver());
                part.setDriverId(first.getDriverId());
                part.setVehicle(first.getVehicle());
                part.setVehicleId(first.getVehicleId());
                part.setDriverAutoAssigned(first.isDriverAutoAssigned());
                part.setVehicleAutoAssigned(first.isVehicleAutoAssigned());
            }
        }
    }

    private static Integer capacityOf(TourProcessingDraftAssignment part, Map<UUID, Vehicle> vehiclesById) {
        if (part.getResourceType() == ResourceType.BORROWED_VEHICLE) {
            return part.getBorrowedVehicle() == null ? null : part.getBorrowedVehicle().getCapacity();
        }
        if (part.getResourceType() == ResourceType.EXTERNAL) {
            return null;
        }
        Vehicle vehicle = DriverHoursRules.assignedVehicle(part, vehiclesById);
        return vehicle == null ? null : vehicle.getCapacity();
    }

    private static String driverKey(TourProcessingDraftAssignment part, int index) {
        if (part.getResourceType() != ResourceType.EXTERNAL && part.getDriverId() != null) {
            return "id:" + part.getDriverId();
        }
        String name = DriverHoursRules.driverKey(part.getDriver());
        return name.isEmpty() ? "part:" + index : "name:" + name;
    }
}
