package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.CleaningDraftRow;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.entity.VehicleType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.UUID;

final class DriverHoursTestRows {

    private DriverHoursTestRows() {
    }

    static CleaningDraftRow tour(String number, String departureDate, String departureTime, String returnDate, String returnTime) {
        return CleaningDraftRow.builder()
                .id(UUID.randomUUID())
                .number(number)
                .departureDate(LocalDate.parse(departureDate))
                .departureTime(LocalTime.parse(departureTime))
                .returnDate(LocalDate.parse(returnDate))
                .returnTime(LocalTime.parse(returnTime))
                .priority(1)
                .passengers(3)
                .executingUnit("Escuela de Química")
                .duplicateRank(1)
                .duplicateGroupSize(1)
                .build();
    }

    static CleaningDraftRow sameDayTour(String number, String date, String departureTime, String returnTime) {
        return tour(number, date, departureTime, date, returnTime);
    }

    static CleaningDraftRow fullSurplusTour(String number, String date) {
        return sameDayTour(number, date, "00:00", "23:30");
    }

    static CleaningDraftRow assigned(CleaningDraftRow tour, Driver driver) {
        tour.setDriver(driver.getName());
        return tour;
    }

    static Driver driver(String name) {
        return Driver.builder().id(UUID.randomUUID()).name(name).status("ACTIVE").build();
    }

    static Driver driver(String name, VehicleType firstAllowed, VehicleType... otherAllowed) {
        Driver driver = driver(name);
        driver.setAllowedVehicleTypes(EnumSet.of(firstAllowed, otherAllowed));
        return driver;
    }

    static Vehicle vehicle(String plate, VehicleType type, Integer capacity) {
        return Vehicle.builder().id(UUID.randomUUID()).plate(plate).type(type).capacity(capacity).build();
    }

    static CleaningDraftRow withVehicle(CleaningDraftRow tour, String vehicle) {
        tour.setVehicle(vehicle);
        return tour;
    }
}
