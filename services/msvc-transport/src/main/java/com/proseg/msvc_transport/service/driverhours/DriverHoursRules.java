package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.DriverUnit;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.Vehicle;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DriverHoursRules {

    public static final double ORDINARY_START = 7.5;
    public static final double ORDINARY_END = 16.5;
    public static final double AFTERNOON_ORDINARY_START = 12;
    public static final double AFTERNOON_ORDINARY_END = 20;
    public static final double MAX_ORDINARY_HOURS_PER_DAY = ORDINARY_END - ORDINARY_START;
    public static final double MULTI_DAY_START = 7.5;
    public static final double WEEKDAY_MULTI_DAY_END = 20.5;
    public static final double WEEKEND_MULTI_DAY_END = 19.5;
    public static final double FIRST_DAY_WEEKDAY_HOURS = 13;
    public static final double FIRST_DAY_WEEKEND_HOURS = 12;
    public static final double MAX_STACKED_DAY_HOURS = 12;
    public static final double MONTHLY_OVERTIME_CAP = 60;
    public static final double SURPLUS_CUTOFF_RATIO = 0.95;
    public static final double HOURS_TOLERANCE = 1e-9;

    private static final int SURPLUS_HOURS_PER_PERIOD = 40;
    private static final int DAYS_PER_PERIOD = 30;
    private static final int PLATE_MAX_LENGTH = 20;
    private static final String GENERAL_SERVICES_UNIT = "programa de servicios generales";

    private DriverHoursRules() {
    }

    public static boolean isGeneralServices(String executingUnit) {
        return executingUnit != null
                && executingUnit.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT).equals(GENERAL_SERVICES_UNIT);
    }

    public static String driverKey(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public static Map<String, Driver> catalogByKey(List<Driver> drivers) {
        Map<String, Driver> catalog = new LinkedHashMap<>();
        drivers.forEach(driver -> catalog.putIfAbsent(driverKey(driver.getName()), driver));
        return catalog;
    }

    public static String plateKey(String value) {
        String plate = value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        return plate.length() <= PLATE_MAX_LENGTH ? plate : plate.substring(0, PLATE_MAX_LENGTH);
    }

    public static String comparablePlate(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");
    }

    public static boolean looksLikePlate(String value) {
        return value != null && value.chars().anyMatch(Character::isDigit);
    }

    public static Map<String, Vehicle> vehicleCatalogByPlate(List<Vehicle> vehicles) {
        Map<String, Vehicle> catalog = new LinkedHashMap<>();
        vehicles.forEach(vehicle -> catalog.putIfAbsent(plateKey(vehicle.getPlate()), vehicle));
        return catalog;
    }

    public static Vehicle catalogVehicle(String rawVehicle, Map<String, Vehicle> vehicleCatalogByPlate) {
        return looksLikePlate(rawVehicle) ? vehicleCatalogByPlate.get(plateKey(rawVehicle)) : null;
    }

    public static Map<UUID, Driver> driversById(List<Driver> drivers) {
        Map<UUID, Driver> catalog = new LinkedHashMap<>();
        drivers.forEach(driver -> catalog.put(driver.getId(), driver));
        return catalog;
    }

    public static Map<UUID, Vehicle> vehiclesById(List<Vehicle> vehicles) {
        Map<UUID, Vehicle> catalog = new LinkedHashMap<>();
        vehicles.forEach(vehicle -> catalog.put(vehicle.getId(), vehicle));
        return catalog;
    }

    public static Driver assignedDriver(TourProcessingDraftAssignment assignment, Map<UUID, Driver> driversById) {
        return assignment.getDriverId() == null ? null : driversById.get(assignment.getDriverId());
    }

    public static boolean countsTowardOwnDriver(TourProcessingDraftAssignment assignment, Driver driver) {
        return assignment.getResourceType() == ResourceType.INTERNAL
                && driver != null
                && driver.getOrganizationalUnit() == DriverUnit.GENERAL_SERVICES;
    }

    public static Vehicle assignedVehicle(TourProcessingDraftAssignment assignment, Map<UUID, Vehicle> vehiclesById) {
        return assignment.getVehicleId() == null ? null : vehiclesById.get(assignment.getVehicleId());
    }

    public static boolean canDrive(Driver driver, Vehicle vehicle) {
        return vehicle.getType() == null || driver.getAllowedVehicleTypes().contains(vehicle.getType());
    }

    public static boolean toursOverlap(TourProcessingDraftAssignment left, TourProcessingDraftAssignment right) {
        return startOf(left).isBefore(endOf(right)) && startOf(right).isBefore(endOf(left));
    }

    public static boolean isActive(TourProcessingDraftAssignment assignment) {
        return assignment.isActive();
    }

    public static double surplusCap(int businessDays) {
        int halfHourNumerator = 2 * SURPLUS_HOURS_PER_PERIOD * businessDays;
        int halfHours = (halfHourNumerator + DAYS_PER_PERIOD - 1) / DAYS_PER_PERIOD;
        return halfHours / 2.0;
    }

    public static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    public static LocalDate weekendKey(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));
    }

    public static Set<LocalDate> monthWeekends(int year, int month) {
        YearMonth period = YearMonth.of(year, month);
        Set<LocalDate> weekends = new LinkedHashSet<>();
        for (LocalDate day = period.atDay(1); !day.isAfter(period.atEndOfMonth()); day = day.plusDays(1)) {
            if (isWeekend(day)) {
                weekends.add(weekendKey(day));
            }
        }
        return weekends;
    }

    public static List<TourDaySpan> daySpans(TourProcessingDraftAssignment tour) {
        LocalDate departureDate = tour.getDepartureDate();
        LocalDate returnDate = tour.getReturnDate();
        double departure = hourOf(tour.getDepartureTime());
        double arrival = hourOf(tour.getReturnTime());

        if (!returnDate.isAfter(departureDate)) {
            return List.of(new TourDaySpan(departureDate, departure, Math.max(departure, arrival)));
        }

        List<TourDaySpan> spans = new ArrayList<>();
        double firstDayHours = isWeekend(departureDate) ? FIRST_DAY_WEEKEND_HOURS : FIRST_DAY_WEEKDAY_HOURS;
        double firstDayEnd = Math.min(departure + firstDayHours, WEEKDAY_MULTI_DAY_END);
        spans.add(new TourDaySpan(departureDate, departure, Math.max(departure, firstDayEnd)));
        for (LocalDate day = departureDate.plusDays(1); day.isBefore(returnDate); day = day.plusDays(1)) {
            spans.add(new TourDaySpan(day, MULTI_DAY_START, isWeekend(day) ? WEEKEND_MULTI_DAY_END : WEEKDAY_MULTI_DAY_END));
        }
        spans.add(new TourDaySpan(returnDate, MULTI_DAY_START, Math.max(MULTI_DAY_START, arrival)));
        return spans;
    }

    public static double hourOf(LocalTime time) {
        return time.getHour() + time.getMinute() / 60.0;
    }

    public static LocalTime timeOf(double hour) {
        int wholeHour = (int) hour;
        int minute = (int) Math.round((hour - wholeHour) * 60);
        if (minute == 60) {
            wholeHour += 1;
            minute = 0;
        }
        return LocalTime.of(wholeHour % 24, minute);
    }

    private static LocalDateTime startOf(TourProcessingDraftAssignment tour) {
        return LocalDateTime.of(tour.getDepartureDate(), tour.getDepartureTime());
    }

    private static LocalDateTime endOf(TourProcessingDraftAssignment tour) {
        return LocalDateTime.of(tour.getReturnDate(), tour.getReturnTime());
    }
}
