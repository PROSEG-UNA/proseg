package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.DriverUnit;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.UlloaShift;
import com.proseg.msvc_transport.entity.Vehicle;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

public class DriverAutoAssigner {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final Set<Integer> GENERAL_SERVICES_PRIORITIES = Set.of(6, 7);
    private static final int LARGE_GROUP_PASSENGERS = 5;
    private static final int GENERAL_SERVICES_LEVEL = 1;
    private static final int MAX_OPTIMIZATION_PASSES = 20;
    private static final int MAX_OPTIMIZATION_ATTEMPTS = 10_000;
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    static final Comparator<TourProcessingDraftAssignment> ASSIGNMENT_ORDER = Comparator
            .comparingInt(DriverAutoAssigner::assignmentLevel)
            .thenComparing(TourProcessingDraftAssignment::getDepartureDate)
            .thenComparing(TourProcessingDraftAssignment::getNumber, DriverAutoAssigner::compareTourNumbers)
            .thenComparing(TourProcessingDraftAssignment::getDepartureTime)
            .thenComparing(TourProcessingDraftAssignment::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<TourProcessingDraftAssignment> REVERSE_ASSIGNMENT_ORDER = Comparator
            .comparingInt(DriverAutoAssigner::assignmentLevel).reversed()
            .thenComparing(TourProcessingDraftAssignment::getDepartureDate)
            .thenComparing(TourProcessingDraftAssignment::getNumber, DriverAutoAssigner::compareTourNumbers);

    private static final Comparator<Candidate> CANDIDATE_ORDER = Comparator
            .comparing(Candidate::sharesDayWithAnotherTour)
            .thenComparingDouble(Candidate::overtimeHours)
            .thenComparing(Comparator.comparingDouble(Candidate::gapHours).reversed())
            .thenComparingDouble(Candidate::accumulatedHours)
            .thenComparing(candidate -> candidate.driver().getName(), String.CASE_INSENSITIVE_ORDER);

    private static final Comparator<Driver> DRIVER_NAME_ORDER = Comparator.comparing(Driver::getName, String.CASE_INSENSITIVE_ORDER);

    private final DriverBusinessDays businessDays;
    private final Set<LocalDate> monthWeekends;
    private final int weekendCap;
    private final List<Driver> availableDrivers;
    private final RequesterMatcher requesterMatcher;
    private final Map<UUID, DriverWorkload> workloads;
    private final Map<UUID, Vehicle> vehiclesById;
    private final ManualDays manualDays;
    private final List<TourProcessingDraftAssignment> allRows;
    private final Map<TourProcessingDraftAssignment, Vehicle> plannedVehicles = new LinkedHashMap<>();
    private final Map<TourProcessingDraftAssignment, String> groupFailures = new LinkedHashMap<>();
    private int groupAttempts;

    private final Map<TourProcessingDraftAssignment, Driver> initialAssignments = new LinkedHashMap<>();
    private final Set<TourProcessingDraftAssignment> lockedTours = new LinkedHashSet<>();
    private final Set<TourProcessingDraftAssignment> conditionalTours = new LinkedHashSet<>();
    private MandatoryCoverage coverage;
    private int optimizationAttempts;
    private boolean optimizationLimitReached;

    public DriverAutoAssigner(
            int year,
            int month,
            DriverBusinessDays businessDays,
            List<Driver> catalog,
            List<Vehicle> vehicles,
            List<TourProcessingDraftAssignment> rows,
            ManualDays manualDays
    ) {
        this.manualDays = manualDays;
        this.allRows = List.copyOf(rows);
        this.businessDays = businessDays;
        this.monthWeekends = DriverHoursRules.monthWeekends(year, month);
        this.weekendCap = WeekendCap.forWeekendsInMonth(monthWeekends.size());
        this.availableDrivers = catalog.stream()
                .filter(driver -> ACTIVE_STATUS.equalsIgnoreCase(driver.getStatus())
                        && driver.getOrganizationalUnit() == DriverUnit.GENERAL_SERVICES)
                .sorted(DRIVER_NAME_ORDER)
                .toList();
        this.requesterMatcher = new RequesterMatcher(availableDrivers);
        Map<UUID, Driver> driversById = DriverHoursRules.driversById(catalog);
        List<TourProcessingDraftAssignment> internalRows = rows.stream()
                .filter(row -> row.getResourceType() != ResourceType.EXTERNAL)
                .toList();
        this.workloads = DriverWorkload.fromAssignments(
                internalRows.stream().filter(DriverAutoAssigner::hasValidSchedule).toList(), driversById, manualDays);
        internalRows.stream().filter(DriverHoursRules::isActive).forEach(row -> {
            Driver driver = DriverHoursRules.assignedDriver(row, driversById);
            if (driver != null) {
                initialAssignments.put(row, driver);
                lockedTours.add(row);
            }
        });
        this.vehiclesById = DriverHoursRules.vehiclesById(vehicles);
    }

    public DriverAutoAssigner(
            int year,
            int month,
            int businessDays,
            List<Driver> catalog,
            List<Vehicle> vehicles,
            List<TourProcessingDraftAssignment> rows,
            ManualDays manualDays
    ) {
        this(year, month, DriverBusinessDays.uniform(businessDays), catalog, vehicles, rows, manualDays);
    }

    public DriverAutoAssigner(
            int year,
            int month,
            int businessDays,
            List<Driver> catalog,
            List<TourProcessingDraftAssignment> rows
    ) {
        this(year, month, DriverBusinessDays.uniform(businessDays), catalog, List.of(), rows, ManualDays.NONE);
    }

    public Optional<DriverRejection> rejectionFor(Driver driver, TourProcessingDraftAssignment tour) {
        if (!hasValidSchedule(tour)) {
            return Optional.of(DriverRejection.invalidSchedule());
        }
        Vehicle vehicle = DriverHoursRules.assignedVehicle(tour, vehiclesById);
        if (vehicle != null && !DriverHoursRules.canDrive(driver, vehicle)) {
            return Optional.of(DriverRejection.incompatibleVehicle(driver));
        }
        return Optional.ofNullable(evaluate(driver, workloadOf(driver), tour, DriverHoursRules.daySpans(tour), true).rejection());
    }

    public List<String> reasonsForUnassigned(TourProcessingDraftAssignment tour, boolean ulloaOnly) {
        if (groupFailures.containsKey(tour)) {
            return List.of(groupFailures.get(tour));
        }
        if (!hasValidSchedule(tour)) {
            return List.of(DriverRejection.invalidSchedule().message());
        }
        List<Driver> eligible = availableDrivers.stream()
                .filter(driver -> !ulloaOnly || UlloaTours.shiftOf(tour).filter(shift -> UlloaTours.isCoveredBy(driver, shift)).isPresent())
                .toList();
        if (eligible.isEmpty()) {
            return List.of(ulloaOnly ? "No hay choferes activos de Servicios Generales configurados para este turno de Periféricas."
                    : "No hay choferes activos de Servicios Generales disponibles en el catálogo.");
        }
        return eligible.stream().map(driver -> rejectionFor(driver, tour)
                .map(DriverRejection::message)
                .orElse("El chofer " + driver.getName() + " cumple las reglas; la gira sigue pendiente de asignación."))
                .toList();
    }

    public Map<TourProcessingDraftAssignment, List<String>> preservedAssignmentWarnings() {
        Map<TourProcessingDraftAssignment, List<String>> warnings = new LinkedHashMap<>();
        initialAssignments.forEach((tour, driver) -> {
            if (driver.getOrganizationalUnit() != DriverUnit.GENERAL_SERVICES) {
                return;
            }
            List<String> reasons = new ArrayList<>();
            if (!availableDrivers.contains(driver)) {
                reasons.add("El chofer " + driver.getName() + " está inactivo. Se conservó la asignación existente.");
            }
            if (!hasValidSchedule(tour)) {
                reasons.add(DriverRejection.invalidSchedule().message());
            } else {
                Vehicle vehicle = DriverHoursRules.assignedVehicle(tour, vehiclesById);
                if (vehicle != null && !DriverHoursRules.canDrive(driver, vehicle)) {
                    reasons.add(DriverRejection.incompatibleVehicle(driver).message());
                }
                DriverRejection rejection = evaluate(driver, workloadExcluding(driver, tour), tour, DriverHoursRules.daySpans(tour), true).rejection();
                if (rejection != null) {
                    reasons.add(rejection.message());
                }
            }
            if (!reasons.isEmpty()) {
                warnings.put(tour, reasons);
            }
        });
        groupFailures.forEach((part, reason) -> {
            if (initialAssignments.containsKey(part)) warnings.put(part, List.of(reason));
        });
        return warnings;
    }

    public Map<String, Double> surplusDeficits() {
        Map<String, Double> deficits = new LinkedHashMap<>();
        for (Driver driver : availableDrivers) {
            double missing = surplusTargetHours(driver) - surplusHoursOf(driver);
            if (missing > DriverHoursRules.HOURS_TOLERANCE) {
                deficits.put(driver.getName(), missing);
            }
        }
        return deficits;
    }

    public Map<String, Double> surplusTargets() {
        Map<String, Double> targets = new LinkedHashMap<>();
        for (Driver driver : availableDrivers) {
            if (surplusTargetHours(driver) - surplusHoursOf(driver) > DriverHoursRules.HOURS_TOLERANCE) {
                targets.put(driver.getName(), surplusTargetHours(driver));
            }
        }
        return targets;
    }

    public double surplusTargetHours(Driver driver) {
        return businessDays.surplusTargetOf(driver);
    }

    public double defaultSurplusTargetHours() {
        return businessDays.defaultSurplusCap() * DriverHoursRules.SURPLUS_CUTOFF_RATIO;
    }

    public boolean optimizationLimitReached() {
        return optimizationLimitReached;
    }

    public MandatoryCoverageSummary coverageSummary() {
        return coverage == null ? MandatoryCoverageSummary.EMPTY : coverage.summary();
    }

    List<Driver> drivers() {
        return availableDrivers;
    }

    Map<UUID, DriverWorkload> workloads() {
        return workloads;
    }

    Set<TourProcessingDraftAssignment> lockedTours() {
        return lockedTours;
    }

    Set<TourProcessingDraftAssignment> conditionalTours() {
        return conditionalTours;
    }

    Set<LocalDate> monthWeekends() {
        return monthWeekends;
    }

    double surplusCapOf(Driver driver) {
        return businessDays.surplusCapOf(driver);
    }

    DriverMonthTotals totalsOf(Driver driver) {
        return workloadOf(driver).totals(surplusCapOf(driver), monthWeekends);
    }

    Optional<DriverRejection> rejectionIgnoringSurplus(Driver driver, TourProcessingDraftAssignment tour) {
        if (!hasValidSchedule(tour)) {
            return Optional.of(DriverRejection.invalidSchedule());
        }
        Vehicle vehicle = DriverHoursRules.assignedVehicle(tour, vehiclesById);
        if (vehicle != null && !DriverHoursRules.canDrive(driver, vehicle)) {
            return Optional.of(DriverRejection.incompatibleVehicle(driver));
        }
        return Optional.ofNullable(evaluate(driver, workloadOf(driver), tour, DriverHoursRules.daySpans(tour), false).rejection());
    }

    void assignConditionally(TourProcessingDraftAssignment tour, Driver driver, Map<TourProcessingDraftAssignment, Driver> assignments) {
        assignTour(tour, driver, assignments);
        conditionalTours.add(tour);
        lockedTours.add(tour);
    }

    void unassign(TourProcessingDraftAssignment tour, Driver driver, Map<TourProcessingDraftAssignment, Driver> assignments) {
        workloads.put(driver.getId(), workloadExcluding(driver, tour));
        assignments.remove(tour);
        conditionalTours.remove(tour);
        lockedTours.remove(tour);
    }

    boolean isValidFor(Driver driver) {
        return structurallyValid(driver, workloadOf(driver));
    }

    public Map<TourProcessingDraftAssignment, Driver> assign(List<TourProcessingDraftAssignment> pendingTours) {
        Map<TourProcessingDraftAssignment, Driver> assignments = new LinkedHashMap<>(initialAssignments);
        Set<TourProcessingDraftAssignment> tours = new LinkedHashSet<>(initialAssignments.keySet());
        pendingTours.stream()
                .filter(DriverHoursRules::isActive)
                .filter(tour -> tour.getResourceType() != ResourceType.EXTERNAL)
                .forEach(tours::add);
        assignRequesterPending(tours, assignments);
        assignUlloaPending(tours, assignments);
        coverage = new MandatoryCoverage(this, tours, assignments);
        coverage.run();
        assignSurplusRounds(tours, assignments);
        assignPending(tours, assignments);
        optimizeSchedule(tours, assignments);
        assignPending(tours, assignments);
        validateFinalAssignments(assignments);
        return changesOf(tours, assignments);
    }

    public Map<TourProcessingDraftAssignment, Driver> assignUlloaOnly(List<TourProcessingDraftAssignment> pendingTours) {
        Map<TourProcessingDraftAssignment, Driver> assignments = new LinkedHashMap<>(initialAssignments);
        Set<TourProcessingDraftAssignment> tours = new LinkedHashSet<>(initialAssignments.keySet());
        pendingTours.stream()
                .filter(DriverHoursRules::isActive)
                .filter(tour -> tour.getResourceType() != ResourceType.EXTERNAL)
                .filter(UlloaTours::isUlloaTour)
                .forEach(tours::add);
        assignUlloaPending(tours, assignments);
        validateFinalAssignments(assignments);
        return changesOf(tours, assignments);
    }

    private Map<TourProcessingDraftAssignment, Driver> changesOf(Set<TourProcessingDraftAssignment> tours, Map<TourProcessingDraftAssignment, Driver> assignments) {
        Map<TourProcessingDraftAssignment, Driver> changes = new LinkedHashMap<>();
        for (TourProcessingDraftAssignment tour : tours) {
            Driver driver = assignments.get(tour);
            if (!Objects.equals(driver, initialAssignments.get(tour))) {
                changes.put(tour, driver);
            }
        }
        return changes;
    }

    public Map<TourProcessingDraftAssignment, Vehicle> vehicleAssignments() {
        return Map.copyOf(plannedVehicles);
    }

    private boolean assignGroups(Set<TourProcessingDraftAssignment> scope, Map<TourProcessingDraftAssignment, Driver> assignments,
                                 boolean ulloaOnly, boolean surplusOnly) {
        List<TourProcessingDraftAssignment> representatives = scope.stream()
                .filter(part -> part.getTour().isDistributed()).filter(DriverAutoAssigner::hasValidSchedule)
                .filter(part -> !ulloaOnly || UlloaTours.isUlloaTour(part))
                .sorted(Comparator.comparingInt(DriverAutoAssigner::assignmentLevel)
                        .thenComparing(part -> part.getTour().getAssignments().stream().noneMatch(initialAssignments::containsKey))
                        .thenComparing(ASSIGNMENT_ORDER)).toList();
        Set<UUID> visited = new LinkedHashSet<>();
        boolean progressed = false;
        for (TourProcessingDraftAssignment representative : representatives) {
            if (!visited.add(representative.getTour().getId())) continue;
            List<TourProcessingDraftAssignment> parts = representative.getTour().getAssignments();
            if (parts.stream().mapToInt(TourProcessingDraftAssignment::getPassengers).sum() != representative.getTour().getPassengers()) {
                parts.forEach(part -> groupFailures.put(part, "La distribución de pasajeros no coincide con el total de la gira."));
                continue;
            }
            if (parts.stream().filter(p -> p.getResourceType() != ResourceType.EXTERNAL)
                    .allMatch(p -> assignments.containsKey(p) && (plannedVehicles.containsKey(p)
                            || p.getVehicleId() != null || p.getBorrowedVehicle() != null))) continue;
            groupAttempts = 0;
            Map<TourProcessingDraftAssignment, Driver> proposed = new LinkedHashMap<>();
            Map<TourProcessingDraftAssignment, Vehicle> vehicles = new LinkedHashMap<>();
            if (searchGroup(parts, assignments, proposed, vehicles, ulloaOnly, surplusOnly)) {
                proposed.forEach(assignments::put);
                plannedVehicles.putAll(vehicles);
                lockedTours.addAll(parts);
                parts.forEach(groupFailures::remove);
                progressed = true;
            } else {
                String reason = groupAttempts >= MAX_OPTIMIZATION_ATTEMPTS
                        ? "Se alcanzó el límite de búsqueda sin completar todos los viajes de esta gira. No se guardaron asignaciones parciales nuevas."
                        : "No se encontró una combinación completa de choferes y vehículos para todos los viajes de esta gira, respetando capacidad, horarios y horas. Se conservaron las asignaciones existentes.";
                parts.forEach(part -> groupFailures.put(part, reason));
            }
        }
        return progressed;
    }

    private boolean searchGroup(List<TourProcessingDraftAssignment> remaining, Map<TourProcessingDraftAssignment, Driver> assignments,
                                Map<TourProcessingDraftAssignment, Driver> proposed, Map<TourProcessingDraftAssignment, Vehicle> vehicles,
                                boolean ulloaOnly, boolean surplusOnly) {
        if (remaining.isEmpty()) return true;
        if (++groupAttempts >= MAX_OPTIMIZATION_ATTEMPTS) return false;
        TourProcessingDraftAssignment chosen = null;
        List<GroupChoice> choices = null;
        Set<UUID> possibleDrivers = new LinkedHashSet<>();
        Set<UUID> possibleVehicles = new LinkedHashSet<>();
        int internalParts = 0;
        for (TourProcessingDraftAssignment part : remaining) {
            if (part.getResourceType() == ResourceType.EXTERNAL) {
                if (DriverHoursRules.driverKey(part.getDriver()).isEmpty() || DriverHoursRules.plateKey(part.getVehicle()).isEmpty()) return false;
                continue;
            }
            List<GroupChoice> options = groupChoices(part, assignments, vehicles, ulloaOnly, surplusOnly);
            if (options.isEmpty()) return false;
            internalParts++;
            options.forEach(option -> {
                possibleDrivers.add(option.driver().getId());
                possibleVehicles.add(option.vehicle().getId());
            });
            if (choices == null || options.size() < choices.size()) {
                chosen = part;
                choices = options;
            }
        }
        if (chosen == null) return true;
        if (possibleDrivers.size() < internalParts || possibleVehicles.size() < internalParts) return false;
        TourProcessingDraftAssignment part = chosen;
        List<TourProcessingDraftAssignment> next = remaining.stream().filter(p -> p != part).toList();
        for (GroupChoice choice : choices) {
            if (groupAttempts >= MAX_OPTIMIZATION_ATTEMPTS) return false;
            boolean added = !assignments.containsKey(part);
            if (added) workloadOf(choice.driver()).add(part);
            proposed.put(part, choice.driver());
            vehicles.put(part, choice.vehicle());
            if (searchGroup(next, assignments, proposed, vehicles, ulloaOnly, surplusOnly)) return true;
            vehicles.remove(part);
            proposed.remove(part);
            if (added) workloads.put(choice.driver().getId(), workloadExcluding(choice.driver(), part));
        }
        return false;
    }

    private List<GroupChoice> groupChoices(TourProcessingDraftAssignment part, Map<TourProcessingDraftAssignment, Driver> assignments,
                                         Map<TourProcessingDraftAssignment, Vehicle> proposedVehicles, boolean ulloaOnly, boolean surplusOnly) {
        if (!hasValidSchedule(part)) return List.of();
        Driver fixed = assignments.get(part);
        List<Driver> drivers = fixed == null ? availableDrivers : List.of(fixed);
        List<Vehicle> vehicles;
        if (part.getResourceType() == ResourceType.BORROWED_VEHICLE) {
            var borrowed = part.getBorrowedVehicle();
            if (borrowed == null) return List.of();
            vehicles = List.of(Vehicle.builder().id(borrowed.getId()).plate(borrowed.getPlate())
                    .type(borrowed.getType()).capacity(borrowed.getCapacity()).available(true).build());
        } else if (part.getVehicleId() != null) {
            Vehicle fixedVehicle = vehiclesById.get(part.getVehicleId());
            vehicles = fixedVehicle == null ? List.of() : List.of(fixedVehicle);
        } else {
            vehicles = vehiclesById.values().stream().sorted(Comparator.comparing(Vehicle::getPlate)).toList();
        }
        List<GroupChoice> result = new ArrayList<>();
        for (Driver driver : drivers) {
            if (fixed == null && ulloaOnly && UlloaTours.shiftOf(part).filter(shift -> UlloaTours.isCoveredBy(driver, shift)).isEmpty()) continue;
            DriverWorkload workload = fixed == null ? workloadOf(driver) : workloadExcluding(driver, part);
            if (evaluate(driver, workload, part, DriverHoursRules.daySpans(part), true).candidate() == null) continue;
            if (surplusOnly && fixed == null) {
                double surplusCap = businessDays.surplusCapOf(driver);
                DriverMonthTotals before = workload.totals(surplusCap, monthWeekends);
                DriverMonthTotals after = workload.totalsWith(part, surplusCap, monthWeekends);
                if (meetsSurplusThreshold(driver, workload) || after.surplusHours() <= before.surplusHours()
                        || after.overtimeHours() > before.overtimeHours() + DriverHoursRules.HOURS_TOLERANCE) continue;
            }
            for (Vehicle vehicle : vehicles) {
                if (!vehicle.isAvailable() || vehicle.getCapacity() == null || vehicle.getCapacity() < part.getPassengers()
                        || !DriverHoursRules.canDrive(driver, vehicle) || vehicleBusy(vehicle, part, proposedVehicles)) continue;
                result.add(new GroupChoice(driver, vehicle));
            }
        }
        result.sort(Comparator.comparingDouble((GroupChoice c) -> workloadOf(c.driver()).totals(businessDays.surplusCapOf(c.driver()), monthWeekends).overtimeHours())
                .thenComparing(c -> c.driver().getName()).thenComparing(c -> c.vehicle().getPlate()));
        return result;
    }

    private boolean vehicleBusy(Vehicle vehicle, TourProcessingDraftAssignment part, Map<TourProcessingDraftAssignment, Vehicle> proposed) {
        for (TourProcessingDraftAssignment other : allRows) {
            if (other == part || !other.isActive() || other.getResourceType() == ResourceType.EXTERNAL) continue;
            Vehicle planned = proposed.getOrDefault(other, plannedVehicles.get(other));
            UUID id = planned != null ? planned.getId() : other.getBorrowedVehicle() != null
                    ? other.getBorrowedVehicle().getId() : other.getVehicleId();
            if (vehicle.getId().equals(id) && DriverHoursRules.toursOverlap(part, other)) return true;
        }
        return false;
    }

    private record GroupChoice(Driver driver, Vehicle vehicle) {}

    private void assignRequesterPending(Set<TourProcessingDraftAssignment> tours, Map<TourProcessingDraftAssignment, Driver> assignments) {
        for (TourProcessingDraftAssignment tour : pendingTours(tours, assignments)) {
            Optional<Driver> requester = requesterMatcher.match(tour.getTour().getResponsible());
            if (requester.isPresent() && canAssign(requester.get(), workloadOf(requester.get()), tour, true)) {
                assignTour(tour, requester.get(), assignments);
                lockedTours.add(tour);
            }
        }
    }

    private boolean assignUlloaPending(Set<TourProcessingDraftAssignment> tours, Map<TourProcessingDraftAssignment, Driver> assignments) {
        boolean assignedAny = false;
        boolean progressed;
        do {
            progressed = assignGroups(tours, assignments, true, false);
            for (TourProcessingDraftAssignment tour : pendingTours(tours, assignments)) {
                Optional<Driver> driver = UlloaTours.shiftOf(tour).flatMap(shift -> pickUlloaDriver(tour, shift));
                if (driver.isPresent()) {
                    assignTour(tour, driver.get(), assignments);
                    progressed = true;
                    assignedAny = true;
                }
            }
        } while (progressed);
        return assignedAny;
    }

    private Optional<Driver> pickUlloaDriver(TourProcessingDraftAssignment tour, UlloaShift shift) {
        return candidates(tour).stream()
                .filter(candidate -> UlloaTours.isCoveredBy(candidate.driver(), shift))
                .min(Comparator.comparingDouble((Candidate candidate) -> continuityGap(candidate.driver(), tour, shift))
                        .thenComparing(CANDIDATE_ORDER))
                .map(Candidate::driver);
    }

    private double continuityGap(Driver driver, TourProcessingDraftAssignment tour, UlloaShift shift) {
        return workloadOf(driver).tours().stream()
                .filter(existing -> UlloaTours.shiftOf(existing).filter(shift::equals).isPresent())
                .mapToDouble(existing -> gapBetween(existing, tour))
                .min().orElse(Double.POSITIVE_INFINITY);
    }

    void assignTour(TourProcessingDraftAssignment tour, Driver driver, Map<TourProcessingDraftAssignment, Driver> assignments) {
        workloadOf(driver).add(tour);
        assignments.put(tour, driver);
        if (UlloaTours.isUlloaTour(tour)) {
            lockedTours.add(tour);
        }
    }

    private void assignSurplusRounds(Set<TourProcessingDraftAssignment> tours, Map<TourProcessingDraftAssignment, Driver> assignments) {
        for (int level : tours.stream().filter(DriverAutoAssigner::hasValidSchedule).mapToInt(DriverAutoAssigner::assignmentLevel).distinct().sorted().toArray()) {
        Set<TourProcessingDraftAssignment> levelTours = new LinkedHashSet<>(tours.stream()
                .filter(DriverAutoAssigner::hasValidSchedule).filter(t -> assignmentLevel(t) == level).toList());
            boolean progressed;
            do {
                progressed = assignGroups(levelTours, assignments, false, true);
                List<TourProcessingDraftAssignment> pending = pendingTours(levelTours, assignments);
                List<SurplusOptions> options = new ArrayList<>();
                for (Driver driver : availableDrivers) {
                    DriverWorkload workload = workloadOf(driver);
                    double surplusCap = businessDays.surplusCapOf(driver);
                    DriverMonthTotals before = workload.totals(surplusCap, monthWeekends);
                    if (meetsSurplusThreshold(driver, workload)) {
                        continue;
                    }
                    List<TourProcessingDraftAssignment> feasible = pending.stream()
                            .filter(tour -> canAssign(driver, workload, tour, true))
                            .filter(tour -> {
                                DriverMonthTotals after = workload.totalsWith(tour, surplusCap, monthWeekends);
                                return after.surplusHours() > before.surplusHours() + DriverHoursRules.HOURS_TOLERANCE
                                        && after.overtimeHours() <= before.overtimeHours() + DriverHoursRules.HOURS_TOLERANCE;
                            }).toList();
                    if (!feasible.isEmpty()) {
                        options.add(new SurplusOptions(driver, before.surplusHours(), feasible));
                    }
                }
                options.sort(Comparator.comparingDouble(SurplusOptions::surplusHours)
                        .thenComparingInt(option -> option.tours().size())
                        .thenComparing(SurplusOptions::driver, DRIVER_NAME_ORDER));
                for (SurplusOptions option : options) {
                    Optional<TourProcessingDraftAssignment> next = option.tours().stream().filter(tour -> !assignments.containsKey(tour)).findFirst();
                    if (next.isPresent()) {
                        assignTour(next.get(), option.driver(), assignments);
                        progressed = true;
                    }
                }
            } while (progressed);
        }
    }

    private boolean assignPending(Set<TourProcessingDraftAssignment> tours, Map<TourProcessingDraftAssignment, Driver> assignments) {
        boolean assignedAny = false;
        boolean progressed;
        do {
            progressed = assignUlloaPending(tours, assignments);
            Set<UUID> visitedGroups = new LinkedHashSet<>();
            for (TourProcessingDraftAssignment tour : tours.stream().filter(DriverAutoAssigner::hasValidSchedule).sorted(ASSIGNMENT_ORDER).toList()) {
                if (tour.getTour().isDistributed()) {
                    if (visitedGroups.add(tour.getTour().getId())) {
                        progressed |= assignGroups(Set.of(tour), assignments, false, false);
                    }
                    continue;
                }
                if (assignments.containsKey(tour)) continue;
                Optional<Driver> driver = candidates(tour).stream().min(CANDIDATE_ORDER).map(Candidate::driver);
                if (driver.isPresent()) {
                    assignTour(tour, driver.get(), assignments);
                    progressed = true;
                }
            }
            assignedAny |= progressed;
        } while (progressed);
        return assignedAny;
    }

    List<TourProcessingDraftAssignment> pendingTours(Set<TourProcessingDraftAssignment> tours, Map<TourProcessingDraftAssignment, Driver> assignments) {
        return tours.stream().filter(tour -> !tour.getTour().isDistributed()).filter(tour -> !assignments.containsKey(tour))
                .filter(DriverAutoAssigner::hasValidSchedule).sorted(ASSIGNMENT_ORDER).toList();
    }

    private List<Candidate> candidates(TourProcessingDraftAssignment tour) {
        List<TourDaySpan> spans = DriverHoursRules.daySpans(tour);
        Vehicle loadedVehicle = DriverHoursRules.assignedVehicle(tour, vehiclesById);
        return availableDrivers.stream()
                .filter(driver -> loadedVehicle == null || DriverHoursRules.canDrive(driver, loadedVehicle))
                .map(driver -> evaluate(driver, workloadOf(driver), tour, spans, true).candidate())
                .filter(Objects::nonNull).sorted(CANDIDATE_ORDER).toList();
    }

    private void optimizeSchedule(Set<TourProcessingDraftAssignment> tours, Map<TourProcessingDraftAssignment, Driver> assignments) {
        for (int pass = 0; pass < MAX_OPTIMIZATION_PASSES; pass++) {
            boolean improved = false;
            List<TourProcessingDraftAssignment> movable = assignments.keySet().stream()
                    .filter(tour -> !lockedTours.contains(tour)).sorted(REVERSE_ASSIGNMENT_ORDER).toList();
            for (TourProcessingDraftAssignment tour : movable) {
                if (optimizationLimitReached) {
                    return;
                }
                Driver driver = assignments.get(tour);
                if (driver != null) {
                    improved |= tryReplacement(tour, driver, tours, assignments)
                            || tryTransfer(tour, driver, assignments)
                            || trySwap(tour, driver, assignments);
                }
            }
            improved |= assignPending(tours, assignments);
            if (!improved) {
                return;
            }
        }
        optimizationLimitReached = true;
    }

    private boolean trySwap(TourProcessingDraftAssignment tour, Driver driver, Map<TourProcessingDraftAssignment, Driver> assignments) {
        DriverWorkload withoutTour = workloadExcluding(driver, tour);
        for (Driver other : availableDrivers) {
            if (other.equals(driver)) {
                continue;
            }
            for (TourProcessingDraftAssignment replacement : workloadOf(other).tours().stream().sorted(ASSIGNMENT_ORDER).toList()) {
                if (optimizationLimitReached) {
                    return false;
                }
                if (!lockedTours.contains(replacement)
                        && canAssign(driver, withoutTour, replacement, true)
                        && canAssign(other, workloadExcluding(other, replacement), tour, true)
                        && tryCommit(Map.of(tour, other, replacement, driver), assignments)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryReplacement(TourProcessingDraftAssignment tour, Driver driver, Set<TourProcessingDraftAssignment> tours, Map<TourProcessingDraftAssignment, Driver> assignments) {
        DriverWorkload replacementWorkload = workloadExcluding(driver, tour);
        Map<TourProcessingDraftAssignment, Driver> changes = new LinkedHashMap<>();
        changes.put(tour, null);
        for (TourProcessingDraftAssignment replacement : pendingTours(tours, assignments)) {
            if (optimizationLimitReached) {
                return false;
            }
            if (!canAssign(driver, replacementWorkload, replacement, true)) {
                continue;
            }
            replacementWorkload.add(replacement);
            changes.put(replacement, driver);
            for (Candidate other : candidates(tour)) {
                if (!other.driver().equals(driver)) {
                    changes.put(tour, other.driver());
                    if (tryCommit(changes, assignments)) {
                        return true;
                    }
                }
            }
            changes.put(tour, null);
            if (tryCommit(changes, assignments)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryTransfer(TourProcessingDraftAssignment tour, Driver driver, Map<TourProcessingDraftAssignment, Driver> assignments) {
        for (Candidate other : candidates(tour)) {
            if (optimizationLimitReached) {
                return false;
            }
            if (!other.driver().equals(driver) && tryCommit(Map.of(tour, other.driver()), assignments)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryCommit(Map<TourProcessingDraftAssignment, Driver> changes, Map<TourProcessingDraftAssignment, Driver> assignments) {
        if (optimizationAttempts++ >= MAX_OPTIMIZATION_ATTEMPTS) {
            optimizationLimitReached = true;
            return false;
        }
        if (changes.keySet().stream().anyMatch(lockedTours::contains)) {
            return false;
        }
        Set<Driver> affected = new LinkedHashSet<>();
        changes.forEach((tour, driver) -> {
            if (assignments.get(tour) != null) {
                affected.add(assignments.get(tour));
            }
            if (driver != null) {
                affected.add(driver);
            }
        });
        Map<UUID, DriverWorkload> next = new LinkedHashMap<>();
        for (Driver driver : affected) {
            DriverWorkload workload = new DriverWorkload(driver, manualDays);
            workloadOf(driver).tours().stream().filter(tour -> !changes.containsKey(tour)).forEach(workload::add);
            changes.forEach((tour, assigned) -> {
                if (driver.equals(assigned)) {
                    workload.add(tour);
                }
            });
            if (!structurallyValid(driver, workload) || !respectsWeekendCap(driver, workload) || !keepsCoverageCondition(driver, workload)) {
                return false;
            }
            next.put(driver.getId(), workload);
        }
        Map<Integer, Integer> coverageChanges = new TreeMap<>();
        changes.forEach((tour, driver) -> coverageChanges.merge(assignmentLevel(tour),
                (driver == null ? 0 : 1) - (assignments.containsKey(tour) ? 1 : 0), Integer::sum));
        int comparison = coverageChanges.values().stream().filter(change -> change != 0)
                .findFirst().map(change -> -Integer.signum(change)).orElse(0);
        if (comparison == 0) {
            Map<UUID, DriverWorkload> current = new LinkedHashMap<>();
            affected.forEach(driver -> current.put(driver.getId(), workloadOf(driver)));
            ScheduleQuality before = quality(current);
            ScheduleQuality after = quality(next);
            comparison = compareQuality(after, before);
        }
        if (comparison >= 0) {
            return false;
        }
        workloads.putAll(next);
        changes.forEach((tour, driver) -> {
            if (driver == null) {
                assignments.remove(tour);
            } else {
                assignments.put(tour, driver);
                if (UlloaTours.isUlloaTour(tour)) {
                    lockedTours.add(tour);
                }
            }
        });
        return true;
    }

    private int compareQuality(ScheduleQuality after, ScheduleQuality before) {
        int comparison = compareHours(after.surplusDeficit(), before.surplusDeficit());
        if (comparison == 0) {
            comparison = Integer.compare(after.stackedDays(), before.stackedDays());
        }
        if (comparison == 0) {
            comparison = compareHours(after.squaredOvertime(), before.squaredOvertime());
        }
        if (comparison == 0) {
            comparison = compareHours(after.spacingPenalty(), before.spacingPenalty());
        }
        if (comparison == 0) {
            comparison = compareHours(after.squaredHours(), before.squaredHours());
        }
        return comparison;
    }

    private ScheduleQuality quality(Map<UUID, DriverWorkload> driverWorkloads) {
        double deficit = 0;
        double squaredOvertime = 0;
        double squaredHours = 0;
        double spacingPenalty = 0;
        int stackedDays = 0;
        for (Map.Entry<UUID, DriverWorkload> entry : driverWorkloads.entrySet()) {
            DriverWorkload workload = entry.getValue();
            double surplusCap = businessDays.surplusCapOf(entry.getKey());
            DriverMonthTotals totals = workload.totals(surplusCap, monthWeekends);
            deficit += Math.max(0, surplusCap * DriverHoursRules.SURPLUS_CUTOFF_RATIO - totals.surplusHours());
            squaredOvertime += Math.pow(totals.overtimeHours(), 2);
            squaredHours += Math.pow(totals.surplusAndOvertimeHours(), 2);
            Map<LocalDate, Integer> toursPerDay = new LinkedHashMap<>();
            workload.tours().forEach(tour -> DriverHoursRules.daySpans(tour).stream().filter(span -> span.hours() > 0)
                    .forEach(span -> toursPerDay.merge(span.date(), 1, Integer::sum)));
            stackedDays += (int) toursPerDay.values().stream().filter(count -> count > 1).count();
            List<TourProcessingDraftAssignment> regular = workload.tours().stream().filter(tour -> !UlloaTours.isUlloaTour(tour))
                    .sorted(Comparator.comparing(TourProcessingDraftAssignment::getDepartureDate).thenComparing(TourProcessingDraftAssignment::getDepartureTime)).toList();
            for (int index = 1; index < regular.size(); index++) {
                spacingPenalty += 1.0 / (1.0 + Math.max(0, gapBetween(regular.get(index - 1), regular.get(index))));
            }
        }
        return new ScheduleQuality(deficit, stackedDays, squaredOvertime, spacingPenalty, squaredHours);
    }

    boolean canAssign(Driver driver, DriverWorkload workload, TourProcessingDraftAssignment tour, boolean enforceSurplusThreshold) {
        Vehicle vehicle = DriverHoursRules.assignedVehicle(tour, vehiclesById);
        return (vehicle == null || DriverHoursRules.canDrive(driver, vehicle))
                && evaluate(driver, workload, tour, DriverHoursRules.daySpans(tour), enforceSurplusThreshold).candidate() != null;
    }

    private boolean respectsWeekendCap(Driver driver, DriverWorkload workload) {
        double surplusCap = businessDays.surplusCapOf(driver);
        int current = workloadOf(driver).totals(surplusCap, monthWeekends).weekendsWorked();
        return workload.totals(surplusCap, monthWeekends).weekendsWorked() <= Math.max(weekendCap, current);
    }

    private boolean keepsCoverageCondition(Driver driver, DriverWorkload workload) {
        DriverWorkload current = workloadOf(driver);
        if (conditionalTours.isEmpty() || !current.containsAnyOf(conditionalTours)) {
            return true;
        }
        double surplusCap = businessDays.surplusCapOf(driver);
        double currentSurplus = current.totals(surplusCap, monthWeekends).surplusHours();
        double target = surplusCap * DriverHoursRules.SURPLUS_CUTOFF_RATIO;
        return workload.totals(surplusCap, monthWeekends).surplusHours() >= Math.min(target, currentSurplus) - DriverHoursRules.HOURS_TOLERANCE;
    }

    private boolean structurallyValid(Driver driver, DriverWorkload workload) {
        if (!availableDrivers.contains(driver)) {
            return workload.tours().isEmpty();
        }
        for (TourProcessingDraftAssignment tour : workload.tours()) {
            if (!canAssign(driver, excluding(workload, tour), tour, true)) {
                return false;
            }
        }
        return true;
    }

    private void validateFinalAssignments(Map<TourProcessingDraftAssignment, Driver> assignments) {
        for (Map.Entry<TourProcessingDraftAssignment, Driver> assignment : assignments.entrySet()) {
            if (initialAssignments.containsKey(assignment.getKey())) {
                continue;
            }
            Driver driver = assignment.getValue();
            if (!availableDrivers.contains(driver)
                    || !canAssign(driver, workloadExcluding(driver, assignment.getKey()), assignment.getKey(), true)) {
                throw new IllegalStateException("La autoasignación terminó con una asignación de chofer inválida");
            }
        }
    }

    private static int compareHours(double left, double right) {
        return Math.abs(left - right) <= DriverHoursRules.HOURS_TOLERANCE ? 0 : Double.compare(left, right);
    }

    boolean meetsSurplusThreshold(Driver driver, DriverWorkload workload) {
        double surplusCap = businessDays.surplusCapOf(driver);
        double surplus = workload.totals(surplusCap, monthWeekends).surplusHours();
        return surplus >= surplusCap * DriverHoursRules.SURPLUS_CUTOFF_RATIO - DriverHoursRules.HOURS_TOLERANCE;
    }

    double surplusHoursOf(Driver driver) {
        return workloadOf(driver).totals(businessDays.surplusCapOf(driver), monthWeekends).surplusHours();
    }

    DriverWorkload workloadExcluding(Driver driver, TourProcessingDraftAssignment excluded) {
        return excluding(workloadOf(driver), excluded);
    }

    private DriverWorkload excluding(DriverWorkload workload, TourProcessingDraftAssignment excluded) {
        return workload.without(excluded);
    }

    static boolean hasValidSchedule(TourProcessingDraftAssignment tour) {
        return tour.getDepartureDate() != null && tour.getReturnDate() != null
                && tour.getDepartureTime() != null && tour.getReturnTime() != null
                && tour.getPriority() != null && tour.getPassengers() != null && tour.getNumber() != null
                && LocalDateTime.of(tour.getReturnDate(), tour.getReturnTime())
                        .isAfter(LocalDateTime.of(tour.getDepartureDate(), tour.getDepartureTime()));
    }

    private static double gapBetween(TourProcessingDraftAssignment left, TourProcessingDraftAssignment right) {
        LocalDateTime leftStart = LocalDateTime.of(left.getDepartureDate(), left.getDepartureTime());
        LocalDateTime leftEnd = LocalDateTime.of(left.getReturnDate(), left.getReturnTime());
        LocalDateTime rightStart = LocalDateTime.of(right.getDepartureDate(), right.getDepartureTime());
        LocalDateTime rightEnd = LocalDateTime.of(right.getReturnDate(), right.getReturnTime());
        return (leftStart.isBefore(rightStart) ? Duration.between(leftEnd, rightStart)
                : Duration.between(rightEnd, leftStart)).toMinutes() / 60.0;
    }

    private Evaluation evaluate(Driver driver, DriverWorkload workload, TourProcessingDraftAssignment tour, List<TourDaySpan> spans, boolean enforceSurplusThreshold) {
        Optional<TourProcessingDraftAssignment> overlapping = workload.findOverlap(tour);
        if (overlapping.isPresent()) {
            return Evaluation.rejected(DriverRejection.overlap(driver, overlapping.get()));
        }

        Map<LocalDate, Double> hoursByDate = workload.hoursByDate();
        boolean sharesDay = false;
        for (TourDaySpan span : spans) {
            double hoursAlreadyThatDay = hoursByDate.getOrDefault(span.date(), 0.0);
            if (hoursAlreadyThatDay <= 0) {
                continue;
            }
            sharesDay = true;
            if (hoursAlreadyThatDay + span.hours() > DriverHoursRules.MAX_STACKED_DAY_HOURS + DriverHoursRules.HOURS_TOLERANCE) {
                return Evaluation.rejected(
                        DriverRejection.dayHoursExceeded(driver, span.date(), hoursAlreadyThatDay, span.hours()));
            }
        }

        double surplusCap = businessDays.surplusCapOf(driver);
        DriverMonthTotals before = workload.totals(surplusCap, monthWeekends);
        DriverMonthTotals after = workload.totalsWith(tour, surplusCap, monthWeekends);
        if (after.weekendsWorked() > before.weekendsWorked() && after.weekendsWorked() > weekendCap) {
            return Evaluation.rejected(DriverRejection.weekendCapExceeded(driver, after.weekendsWorked(), weekendCap));
        }
        if (enforceSurplusThreshold && !conditionalTours.contains(tour)
                && overtimeWithoutConditionalTours(workload, tour, surplusCap, after) > DriverHoursRules.HOURS_TOLERANCE
                && after.surplusHours() < surplusCap * DriverHoursRules.SURPLUS_CUTOFF_RATIO - DriverHoursRules.HOURS_TOLERANCE) {
            return Evaluation.rejected(DriverRejection.weekendWithoutSurplus(driver, after.surplusHours(), surplusCap));
        }
        if (after.overtimeHours() > DriverHoursRules.MONTHLY_OVERTIME_CAP + DriverHoursRules.HOURS_TOLERANCE) {
            return Evaluation.rejected(DriverRejection.overtimeCapExceeded(driver, after.overtimeHours()));
        }

        double gap = workload.tours().stream().mapToDouble(existing -> gapBetween(existing, tour)).min()
                .orElse(Double.POSITIVE_INFINITY);
        return Evaluation.accepted(new Candidate(driver, sharesDay,
                before.overtimeHours(), gap, before.surplusAndOvertimeHours()));
    }

    private double overtimeWithoutConditionalTours(DriverWorkload workload, TourProcessingDraftAssignment tour, double surplusCap, DriverMonthTotals after) {
        if (conditionalTours.isEmpty() || !workload.containsAnyOf(conditionalTours)) {
            return after.overtimeHours();
        }
        return workload.withoutAnyOf(conditionalTours).totalsWith(tour, surplusCap, monthWeekends).overtimeHours();
    }

    DriverWorkload workloadOf(Driver driver) {
        return workloads.computeIfAbsent(driver.getId(), id -> new DriverWorkload(driver, manualDays));
    }

    static int assignmentLevel(TourProcessingDraftAssignment tour) {
        int priority = tour.getPriority();
        boolean largeGroup = tour.getTour().getPassengers() >= LARGE_GROUP_PASSENGERS;
        if (GENERAL_SERVICES_PRIORITIES.contains(priority) && DriverHoursRules.isGeneralServices(tour.getExecutingUnit())) {
            return GENERAL_SERVICES_LEVEL;
        }
        if (priority <= 1) {
            return largeGroup ? 2 : 0;
        }
        return 2 * priority - 1 + (largeGroup ? 1 : 0);
    }

    private static int compareTourNumbers(String left, String right) {
        String leftTrimmed = left.trim();
        String rightTrimmed = right.trim();
        if (DIGITS.matcher(leftTrimmed).matches() && DIGITS.matcher(rightTrimmed).matches()) {
            return new BigInteger(leftTrimmed).compareTo(new BigInteger(rightTrimmed));
        }
        return leftTrimmed.compareToIgnoreCase(rightTrimmed);
    }

    private record Candidate(
            Driver driver,
            boolean sharesDayWithAnotherTour,
            double overtimeHours,
            double gapHours,
            double accumulatedHours
    ) {}

    private record SurplusOptions(Driver driver, double surplusHours, List<TourProcessingDraftAssignment> tours) {}

    private record ScheduleQuality(double surplusDeficit, int stackedDays,
                                   double squaredOvertime, double spacingPenalty, double squaredHours) {}

    private record Evaluation(Candidate candidate, DriverRejection rejection) {

        static Evaluation accepted(Candidate candidate) {
            return new Evaluation(candidate, null);
        }

        static Evaluation rejected(DriverRejection rejection) {
            return new Evaluation(null, rejection);
        }
    }
}
