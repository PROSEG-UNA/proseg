package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.CleaningDraftRow;
import com.proseg.msvc_transport.entity.Vehicle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VehicleWorkload {

    private final List<CleaningDraftRow> tours = new ArrayList<>();

    public static Map<UUID, VehicleWorkload> fromDraftRows(List<CleaningDraftRow> rows, Map<String, Vehicle> vehicleCatalogByPlate) {
        Map<UUID, VehicleWorkload> workloads = new LinkedHashMap<>();
        for (CleaningDraftRow row : rows) {
            Vehicle vehicle = DriverHoursRules.catalogVehicle(row.getVehicle(), vehicleCatalogByPlate);
            if (vehicle != null && DriverHoursRules.isPrimary(row)) {
                workloads.computeIfAbsent(vehicle.getId(), id -> new VehicleWorkload()).add(row);
            }
        }
        return workloads;
    }

    public void add(CleaningDraftRow tour) {
        tours.add(tour);
    }

    public boolean overlaps(CleaningDraftRow tour) {
        return tours.stream().anyMatch(existing -> DriverHoursRules.toursOverlap(existing, tour));
    }
}
