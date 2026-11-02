package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;

import java.util.List;
import java.util.Map;

public final class DraftCatalogLinker {

    private final Map<String, Driver> driversByKey;
    private final Map<String, Vehicle> vehiclesByPlate;

    private DraftCatalogLinker(List<Driver> drivers, List<Vehicle> vehicles) {
        this.driversByKey = DriverHoursRules.catalogByKey(drivers);
        this.vehiclesByPlate = DriverHoursRules.vehicleCatalogByPlate(vehicles);
    }

    public static DraftCatalogLinker of(List<Driver> drivers, List<Vehicle> vehicles) {
        return new DraftCatalogLinker(drivers, vehicles);
    }

    public void link(TourProcessingDraftAssignment assignment) {
        if (assignment.getResourceType() == ResourceType.EXTERNAL) {
            assignment.setDriverId(null);
            assignment.setVehicleId(null);
            return;
        }
        Driver driver = driverOf(assignment.getDriver());
        assignment.setDriverId(driver == null ? null : driver.getId());
        if (driver != null) {
            assignment.setDriver(driver.getName());
        }
        if (assignment.getResourceType() == ResourceType.BORROWED_VEHICLE) {
            assignment.setVehicleId(null);
            return;
        }
        Vehicle vehicle = vehicleOf(assignment.getVehicle());
        assignment.setVehicleId(vehicle == null ? null : vehicle.getId());
        if (vehicle != null) {
            assignment.setVehicle(vehicle.getPlate());
        }
    }

    public Driver driverOf(String value) {
        String key = DriverHoursRules.driverKey(value);
        return key.isEmpty() ? null : driversByKey.get(key);
    }

    public Vehicle vehicleOf(String value) {
        return DriverHoursRules.catalogVehicle(value, vehiclesByPlate);
    }
}
