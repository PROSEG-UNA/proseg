package com.proseg.msvc_transport.service.rules;

import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourApprovalStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.entity.VehicleType;
import com.proseg.msvc_transport.service.driverhours.DriverBusinessDays;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import com.proseg.msvc_transport.service.driverhours.ManualDays;
import com.proseg.msvc_transport.service.rules.TourAssignmentRules.RuleCode;
import com.proseg.msvc_transport.service.rules.TourAssignmentRules.RuleViolation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TourAssignmentRulesTest {

    private final TourAssignmentRules rules = new TourAssignmentRules();
    private final List<Driver> drivers = new java.util.ArrayList<>();
    private final List<Vehicle> vehicles = new java.util.ArrayList<>();
    private final Driver ana = driver("Ana");
    private final Vehicle bus = vehicle("ABC-123", 30, VehicleType.MICROBUS, true);

    @Test
    void warnsWhenTwoToursOfTheSameDriverOverlapAndStaysQuietWhenTheyDoNot() {
        TourProcessingDraftTour morning = tour(1, "100", "2026-05-06", "08:00", "10:00", ana, bus, 10);
        TourProcessingDraftTour crossed = tour(2, "200", "2026-05-06", "09:00", "12:00", ana, vehicle("DEF-456", 30, VehicleType.SEDAN, true), 10);
        TourProcessingDraftTour later = tour(3, "300", "2026-05-06", "12:00", "14:00", ana, vehicle("GHI-789", 30, VehicleType.SEDAN, true), 10);

        assertThat(codes(List.of(morning, crossed))).contains(RuleCode.DRIVER_OVERLAP);
        assertThat(codes(List.of(morning, later))).doesNotContain(RuleCode.DRIVER_OVERLAP);
    }

    @Test
    void doesNotWarnWhenTheSameDriverAndVehicleRepeatInsideOneTour() {
        TourProcessingDraftTour tour = tour(1, "100", "2026-05-06", "08:00", "12:00", ana, bus, 20);
        tour.addAssignment(assignment(bus, ana, 10));
        tour.getAssignments().get(0).setPassengers(10);

        List<RuleCode> codes = codes(List.of(tour));

        assertThat(codes).doesNotContain(RuleCode.DRIVER_OVERLAP, RuleCode.PLATE_REPEATED_SAME_DAY, RuleCode.VEHICLE_OVER_CAPACITY);
    }

    @Test
    void warnsWhenSurplusOrOvertimePassTheCapAndStaysQuietAtTheCap() {
        TourProcessingDraftTour surplusTour = tour(1, "100", "2026-05-06", "08:00", "12:00", ana, bus, 10);
        TourProcessingDraftTour overtimeTour = tour(2, "200", "2026-05-07", "08:00", "12:00", ana, bus, 10);

        assertThat(codes(List.of(surplusTour), manual(surplusTour, "2026-05-06", 40, 0))).contains(RuleCode.SURPLUS_CAP_EXCEEDED);
        assertThat(codes(List.of(surplusTour), manual(surplusTour, "2026-05-06", 37.5, 0))).doesNotContain(RuleCode.SURPLUS_CAP_EXCEEDED);
        assertThat(codes(List.of(overtimeTour), manual(overtimeTour, "2026-05-07", 36, 61))).contains(RuleCode.OVERTIME_CAP_EXCEEDED);
        assertThat(codes(List.of(overtimeTour), manual(overtimeTour, "2026-05-07", 36, 60))).doesNotContain(RuleCode.OVERTIME_CAP_EXCEEDED);
    }

    @Test
    void warnsWhenWeekendWorkPassesTheMonthlyCap() {
        List<TourProcessingDraftTour> four = List.of(
                tour(1, "100", "2026-05-02", "08:00", "09:00", ana, bus, 4),
                tour(2, "200", "2026-05-09", "08:00", "09:00", ana, bus, 4),
                tour(3, "300", "2026-05-16", "08:00", "09:00", ana, bus, 4),
                tour(4, "400", "2026-05-23", "08:00", "09:00", ana, bus, 4)
        );
        List<TourProcessingDraftTour> three = four.subList(0, 3);

        assertThat(codes(four)).contains(RuleCode.WEEKEND_CAP_EXCEEDED);
        assertThat(codes(three)).doesNotContain(RuleCode.WEEKEND_CAP_EXCEEDED);
    }

    @Test
    void warnsPerTripWhenPassengersExceedCapacity() {
        TourProcessingDraftTour over = tour(1, "100", "2026-05-06", "08:00", "12:00", ana, bus, 40);
        over.addAssignment(assignment(bus, ana, 31));
        over.getAssignments().get(0).setPassengers(9);
        TourProcessingDraftTour within = tour(2, "200", "2026-05-07", "08:00", "12:00", ana, bus, 40);
        within.addAssignment(assignment(bus, ana, 20));
        within.getAssignments().get(0).setPassengers(20);

        assertThat(codes(List.of(over))).contains(RuleCode.VEHICLE_OVER_CAPACITY);
        assertThat(codes(List.of(within))).doesNotContain(RuleCode.VEHICLE_OVER_CAPACITY);
    }

    @Test
    void warnsWhenAnExternalPlateMatchesAnOwnPlateTheSameDay() {
        TourProcessingDraftTour own = tour(1, "100", "2026-05-06", "08:00", "10:00", ana, bus, 10);
        TourProcessingDraftTour external = external("200", "2026-05-06", "12:00", "14:00", "abc 123");
        TourProcessingDraftTour anotherDay = external("300", "2026-05-07", "08:00", "10:00", "ABC-123");
        TourProcessingDraftTour crossed = external("400", "2026-05-06", "09:00", "11:00", "abc-123");

        assertThat(messages(List.of(own, external), RuleCode.PLATE_REPEATED_SAME_DAY))
                .contains("Placa ABC123: giras 100 y 200 (06/05/2026)");
        assertThat(codes(List.of(own, anotherDay))).doesNotContain(RuleCode.PLATE_REPEATED_SAME_DAY);
        assertThat(messages(List.of(own, crossed), RuleCode.PLATE_REPEATED_SAME_DAY))
                .anyMatch(message -> message.contains("Los horarios se cruzan"));
    }

    @Test
    void warnsWhenTheDriverCannotUseTheVehicleOrTheVehicleIsUnavailable() {
        Driver sedanOnly = driver("Ana");
        sedanOnly.setAllowedVehicleTypes(EnumSet.of(VehicleType.SEDAN));
        Vehicle unavailable = vehicle("ZZZ-1", 30, VehicleType.SEDAN, false);
        Driver anyone = driver("Beto");

        assertThat(codes(List.of(tour(1, "100", "2026-05-06", "08:00", "10:00", sedanOnly, bus, 10)))).contains(RuleCode.INCOMPATIBLE_VEHICLE);
        assertThat(codes(List.of(tour(2, "200", "2026-05-06", "08:00", "10:00", anyone, bus, 10)))).doesNotContain(RuleCode.INCOMPATIBLE_VEHICLE);
        assertThat(codes(List.of(tour(3, "300", "2026-05-07", "08:00", "10:00", anyone, unavailable, 10)))).contains(RuleCode.VEHICLE_UNAVAILABLE);
        assertThat(codes(List.of(tour(4, "400", "2026-05-07", "08:00", "10:00", anyone, bus, 10)))).doesNotContain(RuleCode.VEHICLE_UNAVAILABLE);
    }

    @Test
    void tourIdsViolatingReturnsEveryTourOfACode() {
        TourProcessingDraftTour left = tour(1, "100", "2026-05-06", "08:00", "12:00", ana, bus, 10);
        TourProcessingDraftTour right = tour(2, "200", "2026-05-06", "09:00", "13:00", ana, vehicle("DEF-456", 30, VehicleType.SEDAN, true), 10);
        TourAssignmentRules.Snapshot snapshot = snapshot(List.of(left, right), ManualDays.NONE);

        assertThat(rules.tourIdsViolating(snapshot, RuleCode.DRIVER_OVERLAP)).containsExactlyInAnyOrder(left.getId(), right.getId());
        assertThat(rules.tourIdsViolating(snapshot, null)).contains(left.getId(), right.getId());
    }

    @Test
    void priorityOneUnderMinimumReturnsEveryPriorityOneTourWithFewPassengers() {
        TourProcessingDraftTour attended = tour(1, "100", "2026-05-06", "08:00", "12:00", ana, bus, 3);
        attended.setPriority(1);
        TourProcessingDraftTour large = tour(2, "200", "2026-05-07", "08:00", "12:00", ana, bus, 10);
        large.setPriority(1);
        TourProcessingDraftTour otherPriority = tour(3, "300", "2026-05-08", "08:00", "12:00", ana, bus, 3);

        assertThat(rules.priorityOneUnderMinimumTourIds(snapshot(List.of(attended, large, otherPriority), ManualDays.NONE)))
                .containsExactly(attended.getId());
    }

    @Test
    void priorityOneUnattendedIgnoresNotApprovedToursAndOnlyCountsApprovedOnesWithoutDriverOrVehicle() {
        TourProcessingDraftTour attended = tour(1, "100", "2026-05-06", "08:00", "12:00", ana, bus, 3);
        attended.setPriority(1);
        TourProcessingDraftTour withoutDriver = tour(2, "200", "2026-05-07", "08:00", "12:00", ana, bus, 3);
        withoutDriver.setPriority(1);
        withoutDriver.primaryAssignment().setDriver(null);
        withoutDriver.primaryAssignment().setDriverId(null);
        TourProcessingDraftTour notApproved = tour(3, "300", "2026-05-08", "08:00", "12:00", ana, bus, 3);
        notApproved.setPriority(1);
        notApproved.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        notApproved.primaryAssignment().setDriver(null);
        notApproved.primaryAssignment().setDriverId(null);

        assertThat(rules.priorityOneUnattendedTourIds(snapshot(List.of(attended, withoutDriver, notApproved), ManualDays.NONE)))
                .containsExactly(withoutDriver.getId());
    }

    private TourAssignmentRules.Snapshot snapshot(List<TourProcessingDraftTour> tours, ManualDays manualDays) {
        return new TourAssignmentRules.Snapshot(
                tours,
                DriverHoursRules.driversById(drivers),
                DriverHoursRules.vehiclesById(vehicles),
                DriverBusinessDays.uniform(28),
                manualDays,
                2026,
                5
        );
    }

    private List<RuleCode> codes(List<TourProcessingDraftTour> tours) {
        return codes(tours, ManualDays.NONE);
    }

    private List<RuleCode> codes(List<TourProcessingDraftTour> tours, ManualDays manualDays) {
        return rules.evaluate(snapshot(tours, manualDays)).stream().map(RuleViolation::code).distinct().toList();
    }

    private List<String> messages(List<TourProcessingDraftTour> tours, RuleCode code) {
        return rules.forCode(snapshot(tours, ManualDays.NONE), code).stream().map(RuleViolation::message).distinct().toList();
    }

    private ManualDays manual(TourProcessingDraftTour tour, String date, double surplus, double overtime) {
        TourProcessingDraftDay day = TourProcessingDraftDay.builder()
                .assignment(tour.primaryAssignment())
                .date(LocalDate.parse(date))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(12, 0))
                .ordinaryHours(4.0)
                .surplusHours(surplus)
                .overtimeHours(overtime)
                .manuallyEdited(true)
                .build();
        return ManualDays.of(List.of(day));
    }

    private Driver driver(String name) {
        Driver created = Driver.builder().id(UUID.randomUUID()).name(name).status("ACTIVE").build();
        drivers.add(created);
        return created;
    }

    private Vehicle vehicle(String plate, int capacity, VehicleType type, boolean available) {
        Vehicle created = Vehicle.builder().id(UUID.randomUUID()).plate(plate).capacity(capacity).type(type).available(available).build();
        vehicles.add(created);
        return created;
    }

    private TourProcessingDraftTour tour(int row, String number, String date, String start, String end, Driver driver, Vehicle vehicle, int passengers) {
        TourProcessingDraftTour tour = TourProcessingDraftTour.builder()
                .id(UUID.randomUUID())
                .rowNumber(row)
                .number(number)
                .passengers(passengers)
                .executingUnit("Escuela")
                .responsible("Ana")
                .destination("Limón")
                .durationDays(1)
                .priority(2)
                .departureDate(LocalDate.parse(date))
                .departureTime(LocalTime.parse(start))
                .returnDate(LocalDate.parse(date))
                .returnTime(LocalTime.parse(end))
                .duplicateRank(1)
                .duplicateGroupSize(1)
                .build();
        tour.addAssignment(assignment(vehicle, driver, passengers));
        return tour;
    }

    private TourProcessingDraftAssignment assignment(Vehicle vehicle, Driver driver, int passengers) {
        return TourProcessingDraftAssignment.builder()
                .id(UUID.randomUUID())
                .passengers(passengers)
                .resourceType(ResourceType.INTERNAL)
                .driver(driver.getName())
                .driverId(driver.getId())
                .vehicle(vehicle.getPlate())
                .vehicleId(vehicle.getId())
                .build();
    }

    private TourProcessingDraftTour external(String number, String date, String start, String end, String plate) {
        TourProcessingDraftTour tour = TourProcessingDraftTour.builder()
                .id(UUID.randomUUID())
                .rowNumber(Integer.parseInt(number))
                .number(number)
                .passengers(8)
                .executingUnit("Escuela")
                .responsible("Ana")
                .destination("Limón")
                .durationDays(1)
                .priority(2)
                .departureDate(LocalDate.parse(date))
                .departureTime(LocalTime.parse(start))
                .returnDate(LocalDate.parse(date))
                .returnTime(LocalTime.parse(end))
                .duplicateRank(1)
                .duplicateGroupSize(1)
                .build();
        tour.addAssignment(TourProcessingDraftAssignment.builder()
                .id(UUID.randomUUID())
                .passengers(8)
                .resourceType(ResourceType.EXTERNAL)
                .driver("Contratado")
                .vehicle(plate)
                .build());
        return tour;
    }
}
