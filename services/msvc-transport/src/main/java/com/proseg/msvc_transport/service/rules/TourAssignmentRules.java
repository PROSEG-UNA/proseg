package com.proseg.msvc_transport.service.rules;

import com.proseg.msvc_transport.entity.BorrowedVehicle;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.entity.VehicleType;
import com.proseg.msvc_transport.service.driverhours.DriverBusinessDays;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import com.proseg.msvc_transport.service.driverhours.DriverMonthTotals;
import com.proseg.msvc_transport.service.driverhours.DriverWorkload;
import com.proseg.msvc_transport.service.driverhours.ManualDays;
import com.proseg.msvc_transport.service.driverhours.TourDaySpan;
import com.proseg.msvc_transport.service.driverhours.WeekendCap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public final class TourAssignmentRules {

    public static final Set<RuleCode> CONFIRMABLE = EnumSet.of(
            RuleCode.DRIVER_OVERLAP,
            RuleCode.SURPLUS_CAP_EXCEEDED,
            RuleCode.OVERTIME_CAP_EXCEEDED,
            RuleCode.WEEKEND_CAP_EXCEEDED,
            RuleCode.VEHICLE_OVER_CAPACITY,
            RuleCode.PLATE_REPEATED_SAME_DAY,
            RuleCode.INCOMPATIBLE_VEHICLE,
            RuleCode.VEHICLE_UNAVAILABLE
    );

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final int MIN_EXTERNAL_PASSENGERS = 5;
    private static final int PRIORITY_ONE = 1;
    private static final double TOLERANCE = 0.005;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public enum RuleCode {
        DRIVER_OVERLAP,
        DAY_HOURS_EXCEEDED,
        SURPLUS_CAP_EXCEEDED,
        OVERTIME_CAP_EXCEEDED,
        OVERTIME_WITHOUT_SURPLUS,
        WEEKEND_CAP_EXCEEDED,
        INACTIVE_DRIVER,
        VEHICLE_OVERLAP,
        PLATE_REPEATED_SAME_DAY,
        VEHICLE_OVER_CAPACITY,
        INCOMPATIBLE_VEHICLE,
        VEHICLE_UNAVAILABLE,
        EXTERNAL_UNDER_MINIMUM,
        PRIORITY_ONE_UNATTENDED
    }

    public record RuleViolation(
            RuleCode code,
            String title,
            String message,
            String tourNumber,
            UUID tourId,
            UUID driverId,
            String plate
    ) {
    }

    public record Snapshot(
            List<TourProcessingDraftTour> tours,
            Map<UUID, Driver> driversById,
            Map<UUID, Vehicle> vehiclesById,
            DriverBusinessDays businessDays,
            ManualDays manualDays,
            Integer year,
            Integer month
    ) {
    }

    public List<RuleViolation> evaluate(Snapshot snapshot) {
        List<RuleViolation> violations = new ArrayList<>();
        List<TourProcessingDraftAssignment> attendedParts = attended(snapshot).stream()
                .flatMap(tour -> tour.getAssignments().stream())
                .toList();
        if (snapshot.businessDays() != null && snapshot.year() != null && snapshot.month() != null) {
            addDriverViolations(violations, snapshot, attendedParts);
        }
        addVehicleViolations(violations, snapshot, attendedParts);
        addExternalViolations(violations, snapshot);
        addPriorityViolations(violations, snapshot);
        return violations;
    }

    public List<RuleViolation> forTour(Snapshot snapshot, UUID tourId) {
        return evaluate(snapshot).stream().filter(violation -> tourId != null && tourId.equals(violation.tourId())).toList();
    }

    public List<RuleViolation> forDriver(Snapshot snapshot, UUID driverId) {
        return evaluate(snapshot).stream().filter(violation -> driverId != null && driverId.equals(violation.driverId())).toList();
    }

    public List<RuleViolation> forCode(Snapshot snapshot, RuleCode code) {
        return evaluate(snapshot).stream().filter(violation -> violation.code() == code).toList();
    }

    public Set<UUID> incompleteTourIds(Snapshot snapshot) {
        return idsOf(incomplete(snapshot));
    }

    public Set<UUID> partialTourIds(Snapshot snapshot) {
        return idsOf(partial(snapshot));
    }

    public Set<UUID> tourIdsViolating(Snapshot snapshot, RuleCode code) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (RuleViolation violation : evaluate(snapshot)) {
            if (code != null && violation.code() != code) {
                continue;
            }
            if (violation.tourId() != null) {
                ids.add(violation.tourId());
            }
        }
        return ids;
    }

    public List<RuleViolation> confirmableInvolving(Snapshot snapshot, UUID tourId, Set<UUID> driverIds, Set<String> plates) {
        Set<String> normalizedPlates = new LinkedHashSet<>();
        for (String plate : plates) {
            String normalized = DriverHoursRules.comparablePlate(plate);
            if (!normalized.isEmpty()) {
                normalizedPlates.add(normalized);
            }
        }
        List<RuleViolation> unique = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (RuleViolation violation : evaluate(snapshot)) {
            if (!CONFIRMABLE.contains(violation.code())) {
                continue;
            }
            boolean tour = tourId != null && tourId.equals(violation.tourId());
            boolean driver = violation.driverId() != null && driverIds.contains(violation.driverId());
            boolean plate = violation.plate() != null && normalizedPlates.contains(violation.plate());
            if (tour || driver || plate) {
                if (seen.add(violation.code() + "\n" + violation.message())) {
                    unique.add(violation);
                }
            }
        }
        return unique;
    }

    private void addDriverViolations(
            List<RuleViolation> violations,
            Snapshot snapshot,
            List<TourProcessingDraftAssignment> attendedParts
    ) {
        Set<LocalDate> monthWeekends = DriverHoursRules.monthWeekends(snapshot.year(), snapshot.month());
        int weekendCap = WeekendCap.forWeekendsInMonth(monthWeekends.size());
        Map<UUID, DriverWorkload> workloads = DriverWorkload.fromAssignments(
                attendedParts, snapshot.driversById(), snapshot.manualDays() == null ? ManualDays.NONE : snapshot.manualDays());

        List<Map.Entry<Driver, DriverWorkload>> ordered = workloads.entrySet().stream()
                .map(entry -> Map.entry(snapshot.driversById().get(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(entry -> entry.getKey().getName(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<RuleViolation> overlaps = new ArrayList<>();
        List<RuleViolation> stackedDays = new ArrayList<>();
        List<RuleViolation> surplusExceeded = new ArrayList<>();
        List<RuleViolation> overtimeExceeded = new ArrayList<>();
        List<RuleViolation> overtimeWithoutSurplus = new ArrayList<>();
        List<RuleViolation> weekendExceeded = new ArrayList<>();
        List<RuleViolation> inactiveDrivers = new ArrayList<>();

        for (Map.Entry<Driver, DriverWorkload> entry : ordered) {
            Driver driver = entry.getKey();
            DriverWorkload workload = entry.getValue();
            double surplusCap = snapshot.businessDays().surplusCapOf(driver);
            DriverMonthTotals totals = workload.totals(surplusCap, monthWeekends);
            String name = driver.getName();
            List<TourProcessingDraftAssignment> tours = workload.tours();
            if (!ACTIVE_STATUS.equalsIgnoreCase(driver.getStatus())) {
                addForEach(inactiveDrivers, RuleCode.INACTIVE_DRIVER, "Choferes inactivos con giras asignadas",
                        name + " no está activo y tiene " + tours.size() + " giras asignadas", tours, driver.getId(), null);
            }
            if (totals.surplusHours() > surplusCap + TOLERANCE) {
                addForEach(surplusExceeded, RuleCode.SURPLUS_CAP_EXCEEDED, "Choferes que superan el tope de sobresueldo del mes",
                        name + ": " + hours(totals.surplusHours()) + " h de sobresueldo, tope de " + hours(surplusCap) + " h",
                        tours, driver.getId(), null);
            }
            if (totals.overtimeHours() > DriverHoursRules.MONTHLY_OVERTIME_CAP + TOLERANCE) {
                addForEach(overtimeExceeded, RuleCode.OVERTIME_CAP_EXCEEDED, "Choferes que superan las 60 horas extra del mes",
                        name + ": " + hours(totals.overtimeHours()) + " h extra, tope de " + hours(DriverHoursRules.MONTHLY_OVERTIME_CAP) + " h",
                        tours, driver.getId(), null);
            }
            if (totals.overtimeHours() > TOLERANCE && totals.surplusHours() < surplusCap * DriverHoursRules.SURPLUS_CUTOFF_RATIO - TOLERANCE) {
                addForEach(overtimeWithoutSurplus, RuleCode.OVERTIME_WITHOUT_SURPLUS, "Choferes con horas extra sin llegar al 95% de sobresueldo",
                        name + ": " + hours(totals.overtimeHours()) + " h extra con "
                                + percent(surplusCap == 0 ? 0 : totals.surplusHours() / surplusCap) + " de sobresueldo",
                        tours, driver.getId(), null);
            }
            if (!monthWeekends.isEmpty() && totals.weekendsWorked() > weekendCap) {
                addForEach(weekendExceeded, RuleCode.WEEKEND_CAP_EXCEEDED, "Choferes que superan el tope de fines de semana del mes",
                        name + ": trabaja " + totals.weekendsWorked() + " fines de semana, tope de " + weekendCap,
                        tours, driver.getId(), null);
            }
            overlaps.addAll(overlapsOf(name, driver.getId(), tours));
            stackedDays.addAll(stackedDaysOf(name, driver.getId(), workload));
        }

        violations.addAll(overlaps);
        violations.addAll(stackedDays);
        violations.addAll(surplusExceeded);
        violations.addAll(overtimeExceeded);
        violations.addAll(overtimeWithoutSurplus);
        violations.addAll(weekendExceeded);
        violations.addAll(inactiveDrivers);
    }

    private List<RuleViolation> overlapsOf(String name, UUID driverId, List<TourProcessingDraftAssignment> tours) {
        List<TourProcessingDraftAssignment> ordered = tours.stream()
                .sorted(Comparator.comparing(TourProcessingDraftAssignment::getDepartureDate).thenComparing(TourProcessingDraftAssignment::getDepartureTime))
                .toList();
        List<RuleViolation> details = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            for (int j = i + 1; j < ordered.size(); j++) {
                TourProcessingDraftAssignment left = ordered.get(i);
                TourProcessingDraftAssignment right = ordered.get(j);
                if (!DriverWorkload.sameTour(left, right) && DriverHoursRules.toursOverlap(left, right)) {
                    String message = name + ": giras " + left.getNumber() + " y " + right.getNumber()
                            + " (" + left.getDepartureDate().format(DATE) + ")";
                    add(details, RuleCode.DRIVER_OVERLAP, "Choferes con giras que se cruzan", message, left, driverId, null);
                    add(details, RuleCode.DRIVER_OVERLAP, "Choferes con giras que se cruzan", message, right, driverId, null);
                }
            }
        }
        return details;
    }

    private List<RuleViolation> stackedDaysOf(String name, UUID driverId, DriverWorkload workload) {
        Map<LocalDate, List<String>> numbersByDate = new TreeMap<>();
        Map<LocalDate, List<TourProcessingDraftAssignment>> toursByDate = new TreeMap<>();
        for (TourProcessingDraftAssignment tour : workload.tours()) {
            for (TourDaySpan span : DriverHoursRules.daySpans(tour)) {
                numbersByDate.computeIfAbsent(span.date(), date -> new ArrayList<>()).add(tour.getNumber());
                toursByDate.computeIfAbsent(span.date(), date -> new ArrayList<>()).add(tour);
            }
        }
        Map<LocalDate, Double> hoursByDate = workload.hoursByDate();
        List<RuleViolation> details = new ArrayList<>();
        numbersByDate.forEach((date, numbers) -> {
            double dayHours = hoursByDate.getOrDefault(date, 0.0);
            if (numbers.size() > 1 && dayHours > DriverHoursRules.MAX_STACKED_DAY_HOURS + TOLERANCE) {
                String message = name + ": " + hours(dayHours) + " h el " + date.format(DATE) + " (giras " + String.join(", ", numbers) + ")";
                addForEach(details, RuleCode.DAY_HOURS_EXCEEDED, "Choferes con más de 12 horas en un día al sumar giras",
                        message, toursByDate.get(date), driverId, null);
            }
        });
        return details;
    }

    private void addVehicleViolations(
            List<RuleViolation> violations,
            Snapshot snapshot,
            List<TourProcessingDraftAssignment> attendedParts
    ) {
        Map<String, List<TourProcessingDraftAssignment>> partsByPlate = new LinkedHashMap<>();
        List<RuleViolation> overCapacity = new ArrayList<>();
        List<RuleViolation> incompatible = new ArrayList<>();
        List<RuleViolation> unavailable = new ArrayList<>();
        for (TourProcessingDraftAssignment part : attendedParts) {
            String overlapPlate = overlapPlateOf(part, snapshot);
            if (overlapPlate != null) {
                partsByPlate.computeIfAbsent(overlapPlate, key -> new ArrayList<>()).add(part);
            }
            Integer capacity = capacityOf(part, snapshot);
            String capacityPlate = overlapPlate != null ? overlapPlate : repeatedPlateOf(part, snapshot);
            if (capacity != null && part.getPassengers() != null && part.getPassengers() > capacity) {
                add(overCapacity, RuleCode.VEHICLE_OVER_CAPACITY, "Viajes con más pasajeros que la capacidad del vehículo",
                        tourLabel(part.getTour()) + ": vehículo " + capacityPlate + " lleva " + part.getPassengers()
                                + " pasajeros y su capacidad es " + capacity,
                        part, driverIdOf(part, snapshot), DriverHoursRules.comparablePlate(capacityPlate));
            }
            Vehicle vehicle = part.getResourceType() == ResourceType.INTERNAL
                    ? DriverHoursRules.assignedVehicle(part, snapshot.vehiclesById()) : null;
            Driver driver = part.getResourceType() == ResourceType.EXTERNAL ? null : DriverHoursRules.assignedDriver(part, snapshot.driversById());
            if (vehicle != null && driver != null && !DriverHoursRules.canDrive(driver, vehicle)) {
                add(incompatible, RuleCode.INCOMPATIBLE_VEHICLE, "Choferes asignados a un tipo de vehículo que no pueden manejar",
                        tourLabel(part.getTour()) + ": " + driver.getName() + " no está habilitado para el vehículo " + vehicle.getPlate(),
                        part, driver.getId(), DriverHoursRules.comparablePlate(vehicle.getPlate()));
            }
            BorrowedVehicle borrowed = part.getResourceType() == ResourceType.BORROWED_VEHICLE ? part.getBorrowedVehicle() : null;
            if (borrowed != null && driver != null && !canDriveType(driver, borrowed.getType())) {
                add(incompatible, RuleCode.INCOMPATIBLE_VEHICLE, "Choferes asignados a un tipo de vehículo que no pueden manejar",
                        tourLabel(part.getTour()) + ": " + driver.getName() + " no está habilitado para el vehículo " + borrowed.getPlate(),
                        part, driver.getId(), DriverHoursRules.comparablePlate(borrowed.getPlate()));
            }
            if (vehicle != null && !vehicle.isAvailable()) {
                add(unavailable, RuleCode.VEHICLE_UNAVAILABLE, "Vehículos no disponibles asignados a giras",
                        tourLabel(part.getTour()) + ": vehículo " + vehicle.getPlate() + " no está disponible",
                        part, driverIdOf(part, snapshot), DriverHoursRules.comparablePlate(vehicle.getPlate()));
            }
        }
        List<RuleViolation> sharedVehicles = new ArrayList<>();
        partsByPlate.forEach((plate, parts) -> {
            for (int i = 0; i < parts.size(); i++) {
                for (int j = i + 1; j < parts.size(); j++) {
                    TourProcessingDraftAssignment left = parts.get(i);
                    TourProcessingDraftAssignment right = parts.get(j);
                    if (!DriverWorkload.sameTour(left, right) && DriverHoursRules.toursOverlap(left, right)) {
                        String message = "Placa " + plate + ": giras " + left.getNumber() + " y " + right.getNumber()
                                + " (" + left.getDepartureDate().format(DATE) + ")";
                        if (sharedVehicles.stream().noneMatch(violation -> violation.message().equals(message))) {
                            add(sharedVehicles, RuleCode.VEHICLE_OVERLAP, "Vehículos asignados a giras que se cruzan", message,
                                    left, null, DriverHoursRules.comparablePlate(plate));
                            add(sharedVehicles, RuleCode.VEHICLE_OVERLAP, "Vehículos asignados a giras que se cruzan", message,
                                    right, null, DriverHoursRules.comparablePlate(plate));
                        }
                    }
                }
            }
        });
        violations.addAll(sharedVehicles);
        violations.addAll(plateRepeated(attendedParts, snapshot));
        violations.addAll(overCapacity);
        violations.addAll(incompatible);
        violations.addAll(unavailable);
    }

    private List<RuleViolation> plateRepeated(List<TourProcessingDraftAssignment> attendedParts, Snapshot snapshot) {
        List<RuleViolation> violations = new ArrayList<>();
        Set<String> pairs = new LinkedHashSet<>();
        for (int i = 0; i < attendedParts.size(); i++) {
            for (int j = i + 1; j < attendedParts.size(); j++) {
                TourProcessingDraftAssignment left = attendedParts.get(i);
                TourProcessingDraftAssignment right = attendedParts.get(j);
                if (DriverWorkload.sameTour(left, right) || !sharesCalendarDay(left, right)) {
                    continue;
                }
                String leftPlate = DriverHoursRules.comparablePlate(repeatedPlateOf(left, snapshot));
                String rightPlate = DriverHoursRules.comparablePlate(repeatedPlateOf(right, snapshot));
                if (leftPlate.isEmpty() || !leftPlate.equals(rightPlate)) {
                    continue;
                }
                String pair = leftPlate + ":" + tourKey(left) + ":" + tourKey(right);
                if (!pairs.add(pair)) {
                    continue;
                }
                String message = "Placa " + leftPlate + ": giras " + left.getNumber() + " y " + right.getNumber()
                        + " (" + sharedDate(left, right).format(DATE) + ")";
                if (DriverHoursRules.toursOverlap(left, right)) {
                    message += ". Los horarios se cruzan";
                }
                add(violations, RuleCode.PLATE_REPEATED_SAME_DAY, "Placas repetidas el mismo día en giras distintas", message, left, null, leftPlate);
                add(violations, RuleCode.PLATE_REPEATED_SAME_DAY, "Placas repetidas el mismo día en giras distintas", message, right, null, leftPlate);
            }
        }
        return violations;
    }

    private void addExternalViolations(List<RuleViolation> violations, Snapshot snapshot) {
        attended(snapshot).stream()
                .filter(tour -> tour.getAssignments().stream().anyMatch(part -> part.getResourceType() == ResourceType.EXTERNAL))
                .filter(tour -> tour.getPassengers() != null && tour.getPassengers() < MIN_EXTERNAL_PASSENGERS)
                .sorted(reportOrder())
                .forEach(tour -> add(violations, RuleCode.EXTERNAL_UNDER_MINIMUM, "Contratación externa con menos de 5 pasajeros",
                        tourLabel(tour) + ": " + tour.getPassengers() + " pasajeros", tour.primaryAssignment(), null, null));
    }

    private void addPriorityViolations(List<RuleViolation> violations, Snapshot snapshot) {
        Set<TourProcessingDraftTour> incomplete = Set.copyOf(incomplete(snapshot));
        snapshot.tours().stream()
                .filter(tour -> Integer.valueOf(PRIORITY_ONE).equals(tour.getPriority()))
                .filter(tour -> tour.getPassengers() != null && tour.getPassengers() < MIN_EXTERNAL_PASSENGERS)
                .filter(tour -> !tour.isApproved() || incomplete.contains(tour))
                .sorted(reportOrder())
                .forEach(tour -> add(violations, RuleCode.PRIORITY_ONE_UNATTENDED,
                        "Giras de prioridad 1 con menos de 5 pasajeros que quedan como No aprobada",
                        tourLabel(tour) + ": " + tour.getPassengers() + " pasajeros", tour.primaryAssignment(), null, null));
    }

    private List<TourProcessingDraftTour> attended(Snapshot snapshot) {
        return snapshot.tours().stream()
                .filter(TourProcessingDraftTour::isApproved)
                .filter(tour -> !tour.getAssignments().isEmpty() && tour.getAssignments().stream().allMatch(part -> isComplete(part, snapshot)))
                .toList();
    }

    private List<TourProcessingDraftTour> incomplete(Snapshot snapshot) {
        return snapshot.tours().stream()
                .filter(TourProcessingDraftTour::isApproved)
                .filter(tour -> tour.getAssignments().stream().noneMatch(part -> isComplete(part, snapshot)))
                .toList();
    }

    private List<TourProcessingDraftTour> partial(Snapshot snapshot) {
        return snapshot.tours().stream()
                .filter(TourProcessingDraftTour::isApproved)
                .filter(tour -> tour.getAssignments().stream().anyMatch(part -> isComplete(part, snapshot))
                        && tour.getAssignments().stream().anyMatch(part -> !isComplete(part, snapshot)))
                .toList();
    }

    private static Set<UUID> idsOf(List<TourProcessingDraftTour> tours) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (TourProcessingDraftTour tour : tours) {
            if (tour.getId() != null) {
                ids.add(tour.getId());
            }
        }
        return ids;
    }

    private boolean isComplete(TourProcessingDraftAssignment part, Snapshot snapshot) {
        return hasDriver(part, snapshot) && hasVehicle(part, snapshot);
    }

    private boolean hasDriver(TourProcessingDraftAssignment part, Snapshot snapshot) {
        if (part.getResourceType() == ResourceType.EXTERNAL) {
            return hasText(part.getDriver());
        }
        return DriverHoursRules.assignedDriver(part, snapshot.driversById()) != null;
    }

    private boolean hasVehicle(TourProcessingDraftAssignment part, Snapshot snapshot) {
        return switch (part.getResourceType()) {
            case EXTERNAL -> hasText(part.getVehicle());
            case BORROWED_VEHICLE -> part.getBorrowedVehicle() != null;
            case INTERNAL -> DriverHoursRules.assignedVehicle(part, snapshot.vehiclesById()) != null;
        };
    }

    private String overlapPlateOf(TourProcessingDraftAssignment part, Snapshot snapshot) {
        if (part.getResourceType() == ResourceType.BORROWED_VEHICLE) {
            BorrowedVehicle borrowed = part.getBorrowedVehicle();
            return borrowed == null ? null : borrowed.getPlate();
        }
        if (part.getResourceType() == ResourceType.INTERNAL) {
            Vehicle vehicle = DriverHoursRules.assignedVehicle(part, snapshot.vehiclesById());
            return vehicle == null ? null : vehicle.getPlate();
        }
        return null;
    }

    private String repeatedPlateOf(TourProcessingDraftAssignment part, Snapshot snapshot) {
        String overlap = overlapPlateOf(part, snapshot);
        if (overlap != null && !overlap.isBlank()) {
            return overlap;
        }
        return part.getVehicle();
    }

    private Integer capacityOf(TourProcessingDraftAssignment part, Snapshot snapshot) {
        if (part.getResourceType() == ResourceType.BORROWED_VEHICLE) {
            return part.getBorrowedVehicle() == null ? null : part.getBorrowedVehicle().getCapacity();
        }
        Vehicle vehicle = part.getResourceType() == ResourceType.INTERNAL
                ? DriverHoursRules.assignedVehicle(part, snapshot.vehiclesById()) : null;
        return vehicle == null ? null : vehicle.getCapacity();
    }

    private static boolean canDriveType(Driver driver, VehicleType type) {
        return type == null || driver.getAllowedVehicleTypes().contains(type);
    }

    private static boolean sharesCalendarDay(TourProcessingDraftAssignment left, TourProcessingDraftAssignment right) {
        LocalDate start = left.getDepartureDate().isAfter(right.getDepartureDate()) ? left.getDepartureDate() : right.getDepartureDate();
        LocalDate end = left.getReturnDate().isBefore(right.getReturnDate()) ? left.getReturnDate() : right.getReturnDate();
        return !start.isAfter(end);
    }

    private static LocalDate sharedDate(TourProcessingDraftAssignment left, TourProcessingDraftAssignment right) {
        return left.getDepartureDate().isAfter(right.getDepartureDate()) ? left.getDepartureDate() : right.getDepartureDate();
    }

    private static Object tourKey(TourProcessingDraftAssignment assignment) {
        return assignment.getTour().getId() != null ? assignment.getTour().getId() : assignment.getTour();
    }

    private static UUID driverIdOf(TourProcessingDraftAssignment part, Snapshot snapshot) {
        Driver driver = DriverHoursRules.assignedDriver(part, snapshot.driversById());
        return driver == null ? part.getDriverId() : driver.getId();
    }

    private static void addForEach(
            List<RuleViolation> violations,
            RuleCode code,
            String title,
            String message,
            List<TourProcessingDraftAssignment> tours,
            UUID driverId,
            String plate
    ) {
        Set<Object> seen = new LinkedHashSet<>();
        for (TourProcessingDraftAssignment tour : tours) {
            if (seen.add(tourKey(tour))) {
                add(violations, code, title, message, tour, driverId, plate);
            }
        }
    }

    private static void add(
            List<RuleViolation> violations,
            RuleCode code,
            String title,
            String message,
            TourProcessingDraftAssignment assignment,
            UUID driverId,
            String plate
    ) {
        TourProcessingDraftTour tour = assignment == null ? null : assignment.getTour();
        violations.add(new RuleViolation(
                code,
                title,
                message,
                tour == null ? null : tour.getNumber(),
                tour == null ? null : tour.getId(),
                driverId,
                plate == null || plate.isEmpty() ? null : plate
        ));
    }

    private static Comparator<TourProcessingDraftTour> reportOrder() {
        return Comparator
                .comparing(TourProcessingDraftTour::getPriority, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TourProcessingDraftTour::getDepartureDate)
                .thenComparing(TourProcessingDraftTour::getDepartureTime)
                .thenComparing(TourProcessingDraftTour::getRowNumber);
    }

    private static String tourLabel(TourProcessingDraftTour tour) {
        return "Gira " + tour.getNumber() + " (" + tour.getDepartureDate().format(DATE) + ")";
    }

    private static String hours(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String percent(double ratio) {
        return BigDecimal.valueOf(ratio * 100).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
