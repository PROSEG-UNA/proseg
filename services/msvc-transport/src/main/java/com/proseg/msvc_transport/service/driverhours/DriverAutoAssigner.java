package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.CleaningDraftRow;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.Vehicle;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class DriverAutoAssigner {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String GENERAL_SERVICES_UNIT = "programa de servicios generales";
    private static final Set<Integer> GENERAL_SERVICES_PRIORITIES = Set.of(6, 7);
    private static final int LARGE_GROUP_PASSENGERS = 5;
    private static final int GENERAL_SERVICES_LEVEL = 1;
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
        this.workloads = DriverWorkload.fromDraftRows(rows, DriverHoursRules.catalogByKey(catalog));
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
        Map<CleaningDraftRow, Driver> assignments = new LinkedHashMap<>();
        for (CleaningDraftRow tour : pendingTours.stream().sorted(ASSIGNMENT_ORDER).toList()) {
            pickDriver(tour).ifPresent(driver -> {
                workloadOf(driver).add(tour);
                assignments.put(tour, driver);
            });
        }
        optimizeWeekendAssignments(assignments);
        return assignments;
    }

    private Optional<Driver> pickDriver(CleaningDraftRow tour) {
        List<TourDaySpan> spans = DriverHoursRules.daySpans(tour);
        Vehicle loadedVehicle = DriverHoursRules.catalogVehicle(tour.getVehicle(), vehicleCatalogByPlate);
        return availableDrivers.stream()
                .filter(driver -> loadedVehicle == null || DriverHoursRules.canDrive(driver, loadedVehicle))
                .map(driver -> evaluate(driver, workloadOf(driver), tour, spans, false).candidate())
                .filter(Objects::nonNull)
                .min(CANDIDATE_ORDER)
                .map(Candidate::driver);
    }

    private void optimizeWeekendAssignments(Map<CleaningDraftRow, Driver> assignments) {
        List<CleaningDraftRow> weekendTours = assignments.keySet().stream()
                .filter(DriverAutoAssigner::touchesWeekend)
                .sorted(REVERSE_ASSIGNMENT_ORDER)
                .toList();
        for (CleaningDraftRow weekendTour : weekendTours) {
            trySwapIntoThreshold(weekendTour, assignments);
        }
    }

    private void trySwapIntoThreshold(CleaningDraftRow weekendTour, Map<CleaningDraftRow, Driver> assignments) {
        Driver driverA = assignments.get(weekendTour);
        DriverWorkload aWithoutTour = workloadExcluding(driverA, weekendTour);
        if (meetsSurplusThreshold(aWithoutTour)) {
            return;
        }

        List<TourDaySpan> weekendSpans = DriverHoursRules.daySpans(weekendTour);
        Vehicle weekendVehicle = DriverHoursRules.catalogVehicle(weekendTour.getVehicle(), vehicleCatalogByPlate);

        for (Driver driverB : availableDrivers.stream().filter(driver -> driver != driverA).sorted(DRIVER_NAME_ORDER).toList()) {
            for (CleaningDraftRow candidateTour : workloadOf(driverB).tours()) {
                if (touchesWeekend(candidateTour)) {
                    continue;
                }
                if (swapIsFeasible(driverA, aWithoutTour, weekendTour, weekendSpans, weekendVehicle, driverB, candidateTour)) {
                    commitSwap(driverA, weekendTour, driverB, candidateTour, assignments);
                    return;
                }
            }
        }
    }

    private boolean swapIsFeasible(
            Driver driverA, DriverWorkload aWithoutTour, CleaningDraftRow weekendTour, List<TourDaySpan> weekendSpans, Vehicle weekendVehicle,
            Driver driverB, CleaningDraftRow candidateTour
    ) {
        if (weekendVehicle != null && !DriverHoursRules.canDrive(driverB, weekendVehicle)) {
            return false;
        }
        Vehicle candidateVehicle = DriverHoursRules.catalogVehicle(candidateTour.getVehicle(), vehicleCatalogByPlate);
        if (candidateVehicle != null && !DriverHoursRules.canDrive(driverA, candidateVehicle)) {
            return false;
        }

        DriverWorkload bWithoutCandidate = workloadExcluding(driverB, candidateTour);
        if (!meetsSurplusThreshold(bWithoutCandidate)) {
            return false;
        }

        List<TourDaySpan> candidateSpans = DriverHoursRules.daySpans(candidateTour);
        return evaluate(driverA, aWithoutTour, candidateTour, candidateSpans, false).candidate() != null
                && evaluate(driverB, bWithoutCandidate, weekendTour, weekendSpans, false).candidate() != null;
    }

    private void commitSwap(
            Driver driverA, CleaningDraftRow weekendTour, Driver driverB, CleaningDraftRow candidateTour,
            Map<CleaningDraftRow, Driver> assignments
    ) {
        DriverWorkload nextA = workloadExcluding(driverA, weekendTour);
        nextA.add(candidateTour);
        workloads.put(driverA.getId(), nextA);

        DriverWorkload nextB = workloadExcluding(driverB, candidateTour);
        nextB.add(weekendTour);
        workloads.put(driverB.getId(), nextB);

        assignments.put(weekendTour, driverB);
        assignments.put(candidateTour, driverA);
    }

    private boolean meetsSurplusThreshold(DriverWorkload workload) {
        double surplus = workload.totals(surplusCap, monthWeekends).surplusHours();
        return surplus >= surplusCap * DriverHoursRules.SURPLUS_CUTOFF_RATIO - DriverHoursRules.HOURS_TOLERANCE;
    }

    private DriverWorkload workloadExcluding(Driver driver, CleaningDraftRow excluded) {
        DriverWorkload copy = new DriverWorkload();
        workloadOf(driver).tours().stream().filter(tour -> tour != excluded).forEach(copy::add);
        return copy;
    }

    private static boolean touchesWeekend(CleaningDraftRow tour) {
        return DriverHoursRules.daySpans(tour).stream().anyMatch(span -> span.weekendHours() > 0);
    }

    private Evaluation evaluate(Driver driver, DriverWorkload workload, CleaningDraftRow tour, List<TourDaySpan> spans, boolean enforceWeekendSurplusThreshold) {
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
        boolean touchesWeekend = spans.stream().anyMatch(span -> span.weekendHours() > 0);
        if (enforceWeekendSurplusThreshold && touchesWeekend
                && before.surplusHours() < surplusCap * DriverHoursRules.SURPLUS_CUTOFF_RATIO - DriverHoursRules.HOURS_TOLERANCE) {
            return Evaluation.rejected(DriverRejection.weekendWithoutSurplus(driver, before.surplusHours(), surplusCap));
        }

        DriverMonthTotals after = workload.totalsWith(tour, surplusCap, monthWeekends);
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

    private record Evaluation(Candidate candidate, DriverRejection rejection) {

        static Evaluation accepted(Candidate candidate) {
            return new Evaluation(candidate, null);
        }

        static Evaluation rejected(DriverRejection rejection) {
            return new Evaluation(null, rejection);
        }
    }
}
