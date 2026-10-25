package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.CleaningDraftRow;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.Vehicle;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

public class DriverAutoAssigner {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String GENERAL_SERVICES_UNIT = "programa de servicios generales";
    private static final Set<Integer> GENERAL_SERVICES_PRIORITIES = Set.of(6, 7);
    private static final int LARGE_GROUP_PASSENGERS = 5;
    private static final int GENERAL_SERVICES_LEVEL = 1;
    private static final int MAX_OPTIMIZATION_PASSES = 20;
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    static final Comparator<CleaningDraftRow> ASSIGNMENT_ORDER = Comparator
            .comparingInt(DriverAutoAssigner::assignmentLevel)
            .thenComparing(CleaningDraftRow::getDepartureDate)
            .thenComparing(CleaningDraftRow::getNumber, DriverAutoAssigner::compareTourNumbers);

    private static final Comparator<CleaningDraftRow> REVERSE_ASSIGNMENT_ORDER = Comparator
            .comparingInt(DriverAutoAssigner::assignmentLevel).reversed()
            .thenComparing(CleaningDraftRow::getDepartureDate)
            .thenComparing(CleaningDraftRow::getNumber, DriverAutoAssigner::compareTourNumbers);

    private static final Comparator<Candidate> CANDIDATE_ORDER = Comparator
            .comparing(Candidate::losesLastFreeWeekend)
            .thenComparing(Candidate::sharesDayWithAnotherTour)
            .thenComparingDouble(Candidate::accumulatedHours)
            .thenComparing(candidate -> candidate.driver().getName(), String.CASE_INSENSITIVE_ORDER);

    private static final Comparator<Driver> DRIVER_NAME_ORDER = Comparator.comparing(Driver::getName, String.CASE_INSENSITIVE_ORDER);

    private final double surplusCap;
    private final Set<LocalDate> monthWeekends;
    private final List<Driver> availableDrivers;
    private final Map<UUID, DriverWorkload> workloads;
    private final Map<String, Vehicle> vehicleCatalogByPlate;
    private final Map<CleaningDraftRow, Driver> initialAssignments = new LinkedHashMap<>();

    public DriverAutoAssigner(
            int year,
            int month,
            int businessDays,
            List<Driver> catalog,
            List<Vehicle> vehicles,
            List<CleaningDraftRow> rows
    ) {
        this.surplusCap = DriverHoursRules.surplusCap(businessDays);
        this.monthWeekends = DriverHoursRules.monthWeekends(year, month);
        this.availableDrivers = catalog.stream()
                .filter(driver -> ACTIVE_STATUS.equalsIgnoreCase(driver.getStatus()))
                .toList();
        Map<String, Driver> catalogByKey = DriverHoursRules.catalogByKey(catalog);
        List<CleaningDraftRow> internalRows = rows.stream()
                .filter(row -> row.getResourceType() != ResourceType.EXTERNAL)
                .toList();
        this.workloads = DriverWorkload.fromDraftRows(internalRows, catalogByKey);
        internalRows.stream().filter(DriverHoursRules::isPrimary).forEach(row -> {
            Driver driver = catalogByKey.get(DriverHoursRules.driverKey(row.getDriver()));
            if (driver != null) {
                initialAssignments.put(row, driver);
            }
        });
        this.vehicleCatalogByPlate = DriverHoursRules.vehicleCatalogByPlate(vehicles);
    }

    public DriverAutoAssigner(
            int year,
            int month,
            int businessDays,
            List<Driver> catalog,
            List<CleaningDraftRow> rows
    ) {
        this(year, month, businessDays, catalog, List.of(), rows);
    }

    public Optional<DriverRejection> rejectionFor(Driver driver, CleaningDraftRow tour) {
        return Optional.ofNullable(evaluate(driver, workloadOf(driver), tour, DriverHoursRules.daySpans(tour), true).rejection());
    }

    public Map<CleaningDraftRow, Driver> assign(List<CleaningDraftRow> pendingTours) {
        Map<CleaningDraftRow, Driver> assignments = new LinkedHashMap<>(initialAssignments);
        Set<CleaningDraftRow> tours = new LinkedHashSet<>(initialAssignments.keySet());
        pendingTours.stream()
                .filter(DriverHoursRules::isPrimary)
                .filter(tour -> tour.getResourceType() != ResourceType.EXTERNAL)
                .forEach(tours::add);
        assignPending(tours, assignments, false);
        optimizeSurplus(tours, assignments);
        releaseInvalidAssignments(assignments);
        assignPending(tours, assignments, true);
        validateFinalAssignments(assignments);

        Map<CleaningDraftRow, Driver> changes = new LinkedHashMap<>();
        for (CleaningDraftRow tour : tours) {
            Driver driver = assignments.get(tour);
            if (!Objects.equals(driver, initialAssignments.get(tour))) {
                changes.put(tour, driver);
            }
        }
        return changes;
    }

    private void assignPending(Set<CleaningDraftRow> tours, Map<CleaningDraftRow, Driver> assignments, boolean enforceSurplusThreshold) {
        for (CleaningDraftRow tour : pendingTours(tours, assignments)) {
            pickDriver(tour, enforceSurplusThreshold).ifPresent(driver -> {
                workloadOf(driver).add(tour);
                assignments.put(tour, driver);
            });
        }
    }

    private List<CleaningDraftRow> pendingTours(Set<CleaningDraftRow> tours, Map<CleaningDraftRow, Driver> assignments) {
        return tours.stream().filter(tour -> !assignments.containsKey(tour)).sorted(ASSIGNMENT_ORDER).toList();
    }

    private Optional<Driver> pickDriver(CleaningDraftRow tour, boolean enforceSurplusThreshold) {
        return orderedCandidates(tour, enforceSurplusThreshold).stream().findFirst();
    }

    private List<Driver> orderedCandidates(CleaningDraftRow tour, boolean enforceSurplusThreshold) {
        List<TourDaySpan> spans = DriverHoursRules.daySpans(tour);
        Vehicle loadedVehicle = DriverHoursRules.catalogVehicle(tour.getVehicle(), vehicleCatalogByPlate);
        return availableDrivers.stream()
                .filter(driver -> loadedVehicle == null || DriverHoursRules.canDrive(driver, loadedVehicle))
                .map(driver -> evaluate(driver, workloadOf(driver), tour, spans, enforceSurplusThreshold).candidate())
                .filter(Objects::nonNull)
                .sorted(CANDIDATE_ORDER)
                .map(Candidate::driver)
                .toList();
    }

    private void optimizeSurplus(Set<CleaningDraftRow> tours, Map<CleaningDraftRow, Driver> assignments) {
        for (int pass = 0; pass < MAX_OPTIMIZATION_PASSES; pass++) {
            boolean improved = fillSurplusFromPending(tours, assignments);
            List<CleaningDraftRow> assignedTours = assignments.keySet().stream().sorted(REVERSE_ASSIGNMENT_ORDER).toList();
            for (CleaningDraftRow tour : assignedTours) {
                Driver driver = assignments.get(tour);
                if (driver == null || !availableDrivers.contains(driver) || meetsSurplusThreshold(workloadOf(driver))
                        || !generatesOvertime(workloadOf(driver), tour)) {
                    continue;
                }
                improved |= trySwap(tour, driver, assignments)
                        || tryReplacement(tour, driver, tours, assignments)
                        || tryTransfer(tour, driver, assignments);
            }
            if (!improved) {
                return;
            }
        }
    }

    private boolean fillSurplusFromPending(Set<CleaningDraftRow> tours, Map<CleaningDraftRow, Driver> assignments) {
        boolean improved = false;
        for (CleaningDraftRow tour : pendingTours(tours, assignments)) {
            if (touchesWeekend(tour)) {
                continue;
            }
            for (Driver driver : orderedCandidates(tour, false)) {
                if (!meetsSurplusThreshold(workloadOf(driver))
                        && tryCommit(Map.of(tour, driver), assignments)) {
                    improved = true;
                    break;
                }
            }
        }
        return improved;
    }

    private boolean trySwap(CleaningDraftRow tour, Driver driver, Map<CleaningDraftRow, Driver> assignments) {
        DriverWorkload withoutTour = workloadExcluding(driver, tour);
        for (Driver other : availableDrivers.stream().filter(candidate -> !candidate.equals(driver)).sorted(DRIVER_NAME_ORDER).toList()) {
            for (CleaningDraftRow replacement : workloadOf(other).tours().stream().sorted(ASSIGNMENT_ORDER).toList()) {
                if (touchesWeekend(replacement) || !canAssign(driver, withoutTour, replacement, false)
                        || !canAssign(other, workloadExcluding(other, replacement), tour, true)) {
                    continue;
                }
                if (tryCommit(Map.of(tour, other, replacement, driver), assignments)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryReplacement(CleaningDraftRow tour, Driver driver, Set<CleaningDraftRow> tours, Map<CleaningDraftRow, Driver> assignments) {
        DriverWorkload replacementWorkload = workloadExcluding(driver, tour);
        Map<CleaningDraftRow, Driver> changes = new LinkedHashMap<>();
        changes.put(tour, null);
        for (CleaningDraftRow replacement : pendingTours(tours, assignments)) {
            if (touchesWeekend(replacement) || !canAssign(driver, replacementWorkload, replacement, false)) {
                continue;
            }
            DriverMonthTotals before = replacementWorkload.totals(surplusCap, monthWeekends);
            DriverMonthTotals after = replacementWorkload.totalsWith(replacement, surplusCap, monthWeekends);
            if (after.surplusHours() <= before.surplusHours() + DriverHoursRules.HOURS_TOLERANCE
                    || after.overtimeHours() > before.overtimeHours() + DriverHoursRules.HOURS_TOLERANCE) {
                continue;
            }
            replacementWorkload.add(replacement);
            changes.put(replacement, driver);
            if (meetsSurplusThreshold(replacementWorkload)) {
                break;
            }
        }
        if (changes.size() == 1) {
            return false;
        }
        for (Driver other : orderedCandidates(tour, true).stream().filter(candidate -> !candidate.equals(driver)).toList()) {
            changes.put(tour, other);
            if (tryCommit(changes, assignments)) {
                return true;
            }
        }
        changes.put(tour, null);
        return tryCommit(changes, assignments);
    }

    private boolean tryTransfer(CleaningDraftRow tour, Driver driver, Map<CleaningDraftRow, Driver> assignments) {
        return orderedCandidates(tour, true).stream().filter(other -> !other.equals(driver))
                .anyMatch(other -> tryCommit(Map.of(tour, other), assignments));
    }

    private boolean tryCommit(Map<CleaningDraftRow, Driver> changes, Map<CleaningDraftRow, Driver> assignments) {
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
            DriverWorkload workload = new DriverWorkload();
            workloadOf(driver).tours().stream().filter(tour -> !changes.containsKey(tour)).forEach(workload::add);
            changes.forEach((tour, assigned) -> {
                if (driver.equals(assigned)) {
                    workload.add(tour);
                }
            });
            if (!structurallyValid(driver, workload)) {
                return false;
            }
            next.put(driver.getId(), workload);
        }
        ScheduleQuality before = quality(affected.stream().map(this::workloadOf).toList());
        ScheduleQuality after = quality(new ArrayList<>(next.values()));
        int comparison = Integer.compare(after.invalidOvertimeTours(), before.invalidOvertimeTours());
        if (comparison == 0) {
            comparison = compareHours(after.surplusDeficit(), before.surplusDeficit());
        }
        if (comparison == 0) {
            Map<Integer, Integer> coverageChanges = new TreeMap<>();
            changes.forEach((tour, driver) -> coverageChanges.merge(assignmentLevel(tour),
                    (driver == null ? 0 : 1) - (assignments.containsKey(tour) ? 1 : 0), Integer::sum));
            comparison = coverageChanges.values().stream().filter(change -> change != 0)
                    .findFirst().map(change -> -Integer.signum(change)).orElse(0);
        }
        if (comparison == 0) {
            comparison = compareHours(after.squaredHours(), before.squaredHours());
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
            }
        });
        return true;
    }

    private ScheduleQuality quality(List<DriverWorkload> driverWorkloads) {
        int invalidOvertimeTours = 0;
        double deficit = 0;
        double squaredHours = 0;
        for (DriverWorkload workload : driverWorkloads) {
            DriverMonthTotals totals = workload.totals(surplusCap, monthWeekends);
            double missingSurplus = Math.max(0, surplusCap * DriverHoursRules.SURPLUS_CUTOFF_RATIO - totals.surplusHours());
            deficit += missingSurplus;
            if (missingSurplus > DriverHoursRules.HOURS_TOLERANCE) {
                invalidOvertimeTours += (int) workload.allocations(surplusCap).stream()
                        .filter(allocation -> allocation.overtimeHours() > DriverHoursRules.HOURS_TOLERANCE).count();
            }
            squaredHours += Math.pow(totals.surplusAndOvertimeHours(), 2);
        }
        return new ScheduleQuality(invalidOvertimeTours, deficit, squaredHours);
    }

    private boolean canAssign(Driver driver, DriverWorkload workload, CleaningDraftRow tour, boolean enforceSurplusThreshold) {
        Vehicle vehicle = DriverHoursRules.catalogVehicle(tour.getVehicle(), vehicleCatalogByPlate);
        return (vehicle == null || DriverHoursRules.canDrive(driver, vehicle))
                && evaluate(driver, workload, tour, DriverHoursRules.daySpans(tour), enforceSurplusThreshold).candidate() != null;
    }

    private boolean structurallyValid(Driver driver, DriverWorkload workload) {
        if (!availableDrivers.contains(driver)) {
            return workload.tours().isEmpty();
        }
        for (CleaningDraftRow tour : workload.tours()) {
            if (!canAssign(driver, excluding(workload, tour), tour, false)) {
                return false;
            }
        }
        return true;
    }

    private boolean generatesOvertime(DriverWorkload workload, CleaningDraftRow tour) {
        return workload.allocations(surplusCap).stream()
                .anyMatch(allocation -> allocation.tour() == tour
                        && allocation.overtimeHours() > DriverHoursRules.HOURS_TOLERANCE);
    }

    private void releaseInvalidAssignments(Map<CleaningDraftRow, Driver> assignments) {
        boolean released;
        do {
            released = false;
            for (CleaningDraftRow tour : assignments.keySet().stream().sorted(REVERSE_ASSIGNMENT_ORDER).toList()) {
                Driver driver = assignments.get(tour);
                DriverWorkload withoutTour = workloadExcluding(driver, tour);
                boolean invalidOvertime = !meetsSurplusThreshold(workloadOf(driver))
                        && generatesOvertime(workloadOf(driver), tour);
                if (!availableDrivers.contains(driver) || !canAssign(driver, withoutTour, tour, false) || invalidOvertime) {
                    workloads.put(driver.getId(), withoutTour);
                    assignments.remove(tour);
                    released = true;
                }
            }
        } while (released);
    }

    private void validateFinalAssignments(Map<CleaningDraftRow, Driver> assignments) {
        for (Map.Entry<CleaningDraftRow, Driver> assignment : assignments.entrySet()) {
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

    private boolean meetsSurplusThreshold(DriverWorkload workload) {
        double surplus = workload.totals(surplusCap, monthWeekends).surplusHours();
        return surplus >= surplusCap * DriverHoursRules.SURPLUS_CUTOFF_RATIO - DriverHoursRules.HOURS_TOLERANCE;
    }

    private DriverWorkload workloadExcluding(Driver driver, CleaningDraftRow excluded) {
        return excluding(workloadOf(driver), excluded);
    }

    private DriverWorkload excluding(DriverWorkload workload, CleaningDraftRow excluded) {
        DriverWorkload copy = new DriverWorkload();
        workload.tours().stream().filter(tour -> tour != excluded).forEach(copy::add);
        return copy;
    }

    private static boolean touchesWeekend(CleaningDraftRow tour) {
        return DriverHoursRules.daySpans(tour).stream().anyMatch(span -> span.weekendHours() > 0);
    }

    private Evaluation evaluate(Driver driver, DriverWorkload workload, CleaningDraftRow tour, List<TourDaySpan> spans, boolean enforceSurplusThreshold) {
        Optional<CleaningDraftRow> overlapping = workload.findOverlap(tour);
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

        DriverMonthTotals before = workload.totals(surplusCap, monthWeekends);
        DriverMonthTotals after = workload.totalsWith(tour, surplusCap, monthWeekends);
        if (enforceSurplusThreshold && after.overtimeHours() > DriverHoursRules.HOURS_TOLERANCE
                && after.surplusHours() < surplusCap * DriverHoursRules.SURPLUS_CUTOFF_RATIO - DriverHoursRules.HOURS_TOLERANCE) {
            return Evaluation.rejected(DriverRejection.weekendWithoutSurplus(driver, after.surplusHours(), surplusCap));
        }
        if (after.overtimeHours() > DriverHoursRules.MONTHLY_OVERTIME_CAP + DriverHoursRules.HOURS_TOLERANCE) {
            return Evaluation.rejected(DriverRejection.overtimeCapExceeded(driver, after.overtimeHours()));
        }

        boolean losesLastFreeWeekend = after.weekendsWorked() > before.weekendsWorked()
                && after.weekendsWorked() >= monthWeekends.size();
        return Evaluation.accepted(new Candidate(driver, losesLastFreeWeekend, sharesDay, before.surplusAndOvertimeHours()));
    }

    private DriverWorkload workloadOf(Driver driver) {
        return workloads.computeIfAbsent(driver.getId(), id -> new DriverWorkload());
    }

    static int assignmentLevel(CleaningDraftRow tour) {
        int priority = tour.getPriority();
        boolean largeGroup = tour.getPassengers() >= LARGE_GROUP_PASSENGERS;
        if (GENERAL_SERVICES_PRIORITIES.contains(priority) && isGeneralServices(tour.getExecutingUnit())) {
            return GENERAL_SERVICES_LEVEL;
        }
        if (priority <= 1) {
            return largeGroup ? 2 : 0;
        }
        return 2 * priority - 1 + (largeGroup ? 1 : 0);
    }

    private static boolean isGeneralServices(String executingUnit) {
        return executingUnit != null
                && executingUnit.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT).equals(GENERAL_SERVICES_UNIT);
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
            boolean losesLastFreeWeekend,
            boolean sharesDayWithAnotherTour,
            double accumulatedHours
    ) {}

    private record ScheduleQuality(int invalidOvertimeTours, double surplusDeficit, double squaredHours) {}

    private record Evaluation(Candidate candidate, DriverRejection rejection) {

        static Evaluation accepted(Candidate candidate) {
            return new Evaluation(candidate, null);
        }

        static Evaluation rejected(DriverRejection rejection) {
            return new Evaluation(null, rejection);
        }
    }
}
