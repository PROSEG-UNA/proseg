package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.TourApprovalStatus;
import com.proseg.msvc_transport.entity.UlloaShift;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.entity.VehicleType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.UUID;

final class DriverHoursTestRows {

    private DriverHoursTestRows() {
    }

    static TourProcessingDraftAssignment tour(String number, String departureDate, String departureTime, String returnDate, String returnTime) {
        TourProcessingDraftTour parent = TourProcessingDraftTour.builder()
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
        return parent.addAssignment(TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).passengers(3).build());
    }

    static TourProcessingDraftAssignment sameDayTour(String number, String date, String departureTime, String returnTime) {
        return tour(number, date, departureTime, date, returnTime);
    }

    static TourProcessingDraftAssignment fullSurplusTour(String number, String date) {
        return sameDayTour(number, date, "00:00", "23:30");
    }

    static TourProcessingDraftAssignment assigned(TourProcessingDraftAssignment tour, Driver driver) {
        tour.setDriver(driver.getName());
        tour.setDriverId(driver.getId());
        return tour;
    }

    static TourProcessingDraftAssignment notApproved(TourProcessingDraftAssignment tour) {
        tour.getTour().setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
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

    static Driver driverWithSchedule(String name, String start, String end) {
        Driver driver = driver(name);
        driver.setOrdinaryStart(LocalTime.parse(start));
        driver.setOrdinaryEnd(LocalTime.parse(end));
        return driver;
    }

    static Driver ulloaDriver(String name, UlloaShift first, UlloaShift... others) {
        Driver driver = driver(name);
        driver.setUlloaShifts(EnumSet.of(first, others));
        return driver;
    }

    static TourProcessingDraftAssignment ulloaTour(String number, String date, UlloaShift shift) {
        TourProcessingDraftAssignment tour = tour(number, date, shift.departureTime().toString(), date, shift.returnTime().toString());
        tour.getTour().setPriority(6);
        tour.setPassengers(30);
        tour.getTour().setPassengers(30);
        tour.getTour().setExecutingUnit("Programa De Servicios Generales");
        tour.getTour().setDestination("Ulloa (barreal)");
        return tour;
    }

    static Vehicle vehicle(String plate, VehicleType type, Integer capacity) {
        return Vehicle.builder().id(UUID.randomUUID()).plate(plate).type(type).capacity(capacity).build();
    }

    static TourProcessingDraftAssignment withVehicle(TourProcessingDraftAssignment tour, String vehicle) {
        tour.setVehicle(vehicle);
        return tour;
    }
}
