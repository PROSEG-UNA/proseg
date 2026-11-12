package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class MandatoryCoverage {

    static final int MAX_RELOCATION_ATTEMPTS = 12;
    static final int MAX_RETRIES_PER_DRIVER = 3;

    private static final double TOLERANCE = DriverHoursRules.HOURS_TOLERANCE;

    private final DriverAutoAssigner assigner;
    private final Set<TourProcessingDraftAssignment> tours;
    private final Map<TourProcessingDraftAssignment, Driver> assignments;
    private final Map<UUID, Double> reachableSurplus = new HashMap<>();
    private List<Driver> coveringDrivers = new ArrayList<>();
    private Map<UUID, List<TourProcessingDraftAssignment>> coveredTours = new LinkedHashMap<>();
    private Map<UUID, List<TourProcessingDraftAssignment>> concentratedTours = new LinkedHashMap<>();

    MandatoryCoverage(DriverAutoAssigner assigner, Set<TourProcessingDraftAssignment> tours, Map<TourProcessingDraftAssignment, Driver> assignments) {
        this.assigner = assigner;
        this.tours = tours;
        this.assignments = assignments;
    }

    static boolean isMandatory(TourProcessingDraftAssignment tour) {
        return DriverAutoAssigner.hasValidSchedule(tour) && !tour.getTour().isDistributed() && DriverAutoAssigner.assignmentLevel(tour) == 0;
    }

    static boolean requiresExtras(TourProcessingDraftAssignment tour) {
        return DriverHoursRules.daySpans(tour).stream().anyMatch(span -> span.weekendHours() > 0);
    }

    void run() {
        List<TourProcessingDraftAssignment> targets = tours.stream()
                .filter(tour -> isMandatory(tour) && requiresExtras(tour) && !assignments.containsKey(tour))
                .sorted(DriverAutoAssigner.ASSIGNMENT_ORDER).toList();
        if (targets.isEmpty()) {
            return;
        }
        targets.forEach(tour -> cover(tour, Set.of()));
        concentrateAll();
        relocateDeficientDrivers();
    }

    private boolean cover(TourProcessingDraftAssignment tour, Set<Driver> excluded) {
        List<Driver> feasible = assigner.drivers().stream()
                .filter(driver -> !excluded.contains(driver))
                .filter(driver -> assigner.canAssign(driver, assigner.workloadOf(driver), tour, false))
                .toList();
        if (feasible.isEmpty()) {
            return false;
        }
        List<Driver> reusable = feasible.stream().filter(coveringDrivers::contains).toList();
        Driver chosen = choose(reusable.isEmpty() ? feasible : reusable);
        assigner.assignConditionally(tour, chosen, assignments);
        if (!coveringDrivers.contains(chosen)) {
            coveringDrivers.add(chosen);
        }
        coveredTours.computeIfAbsent(chosen.getId(), id -> new ArrayList<>()).add(tour);
        reachableSurplus.remove(chosen.getId());
        return true;
    }

    private Driver choose(List<Driver> pool) {
        double topAccumulated = pool.stream().mapToDouble(assigner::surplusHoursOf).max().orElse(0);
        List<Driver> leaders = pool.stream().filter(driver -> assigner.surplusHoursOf(driver) >= topAccumulated - TOLERANCE).toList();
        return leaders.stream().min(Comparator
                .comparingDouble((Driver driver) -> -reachableOf(driver))
                .thenComparingDouble(driver -> assigner.totalsOf(driver).overtimeHours())
                .thenComparingInt(driver -> assigner.drivers().indexOf(driver))).orElseThrow();
    }

    private double reachableOf(Driver driver) {
        return reachableSurplus.computeIfAbsent(driver.getId(), id -> simulateReachableSurplus(driver));
    }

    private double simulateReachableSurplus(Driver driver) {
        DriverWorkload simulated = assigner.workloadOf(driver).copy();
        double surplusCap = assigner.surplusCapOf(driver);
        DriverMonthTotals current = simulated.totals(surplusCap, assigner.monthWeekends());
        for (TourProcessingDraftAssignment tour : assigner.pendingTours(tours, assignments)) {
            if (current.surplusHours() >= surplusCap - TOLERANCE) {
                break;
            }
            if (!assigner.canAssign(driver, simulated, tour, false)) {
                continue;
            }
            DriverMonthTotals after = simulated.totalsWith(tour, surplusCap, assigner.monthWeekends());
            if (after.surplusHours() > current.surplusHours() + TOLERANCE && after.overtimeHours() <= current.overtimeHours() + TOLERANCE) {
                simulated.add(tour);
                current = after;
            }
        }
        return current.surplusHours();
    }

    private void concentrateAll() {
        for (Driver driver : List.copyOf(coveringDrivers)) {
            concentrate(driver);
        }
    }

    private void concentrate(Driver driver) {
        while (!meetsTarget(driver)) {
            Optional<TourProcessingDraftAssignment> next = nextSurplusTour(driver);
            if (next.isEmpty()) {
                return;
            }
            assigner.assignTour(next.get(), driver, assignments);
            concentratedTours.computeIfAbsent(driver.getId(), id -> new ArrayList<>()).add(next.get());
        }
    }

    private Optional<TourProcessingDraftAssignment> nextSurplusTour(Driver driver) {
        DriverWorkload workload = assigner.workloadOf(driver);
        double surplusCap = assigner.surplusCapOf(driver);
        DriverMonthTotals before = workload.totals(surplusCap, assigner.monthWeekends());
        for (TourProcessingDraftAssignment tour : assigner.pendingTours(tours, assignments)) {
            if (!assigner.canAssign(driver, workload, tour, true)) {
                continue;
            }
            DriverMonthTotals after = workload.totalsWith(tour, surplusCap, assigner.monthWeekends());
            if (after.surplusHours() > before.surplusHours() + TOLERANCE && after.overtimeHours() <= before.overtimeHours() + TOLERANCE) {
                return Optional.of(tour);
            }
        }
        return Optional.empty();
    }

    private boolean meetsTarget(Driver driver) {
        return assigner.meetsSurplusThreshold(driver, assigner.workloadOf(driver));
    }

    private void relocateDeficientDrivers() {
        int attempts = 0;
        for (Driver deficient : List.copyOf(coveringDrivers)) {
            if (!coveringDrivers.contains(deficient) || meetsTarget(deficient)) {
                continue;
            }
            Set<Driver> excluded = new LinkedHashSet<>(List.of(deficient));
            for (int retry = 0; retry < MAX_RETRIES_PER_DRIVER && attempts < MAX_RELOCATION_ATTEMPTS; retry++) {
                attempts++;
                Optional<Set<Driver>> stillDeficient = tryRelocate(deficient, excluded);
                if (stillDeficient.isEmpty() || stillDeficient.get().isEmpty()) {
                    break;
                }
                excluded.addAll(stillDeficient.get());
            }
        }
    }

    private Optional<Set<Driver>> tryRelocate(Driver origin, Set<Driver> excluded) {
        Snapshot snapshot = snapshot();
        int deficientBefore = deficientCoveringDrivers().size();
        int attendedBefore = attendedMandatoryCount();
        Set<Driver> invalidBefore = invalidDrivers();
        boolean committed = false;
        try {
            List<TourProcessingDraftAssignment> orphans = release(origin);
            for (TourProcessingDraftAssignment orphan : orphans) {
                if (!cover(orphan, excluded)) {
                    return Optional.of(Set.of());
                }
            }
            concentrateAll();
            Set<Driver> stillDeficient = deficientCoveringDrivers();
            committed = stillDeficient.size() < deficientBefore
                    && attendedMandatoryCount() >= attendedBefore
                    && invalidBefore.containsAll(invalidDrivers());
            return committed ? Optional.empty() : Optional.of(stillDeficient);
        } finally {
            if (!committed) {
                restore(snapshot);
            }
        }
    }

    private List<TourProcessingDraftAssignment> release(Driver origin) {
        List<TourProcessingDraftAssignment> orphans = coveredTours.getOrDefault(origin.getId(), List.of()).stream()
                .sorted(DriverAutoAssigner.ASSIGNMENT_ORDER).toList();
        List<TourProcessingDraftAssignment> concentrated = List.copyOf(concentratedTours.getOrDefault(origin.getId(), List.of()));
        orphans.forEach(tour -> assigner.unassign(tour, origin, assignments));
        concentrated.forEach(tour -> assigner.unassign(tour, origin, assignments));
        coveringDrivers.remove(origin);
        coveredTours.remove(origin.getId());
        concentratedTours.remove(origin.getId());
        reachableSurplus.clear();
        return orphans;
    }

    private Set<Driver> deficientCoveringDrivers() {
        return coveringDrivers.stream().filter(driver -> !meetsTarget(driver)).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private int attendedMandatoryCount() {
        return (int) tours.stream().filter(tour -> isMandatory(tour) && assignments.containsKey(tour)).count();
    }

    private Set<Driver> invalidDrivers() {
        return assigner.drivers().stream().filter(driver -> !assigner.isValidFor(driver)).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private record Snapshot(
            Map<UUID, DriverWorkload> workloads,
            Map<TourProcessingDraftAssignment, Driver> assignments,
            Set<TourProcessingDraftAssignment> locked,
            Set<TourProcessingDraftAssignment> conditional,
            List<Driver> covering,
            Map<UUID, List<TourProcessingDraftAssignment>> covered,
            Map<UUID, List<TourProcessingDraftAssignment>> concentrated
    ) {}

    private Snapshot snapshot() {
        Map<UUID, DriverWorkload> workloadCopies = new LinkedHashMap<>();
        assigner.workloads().forEach((id, workload) -> workloadCopies.put(id, workload.copy()));
        return new Snapshot(workloadCopies, new LinkedHashMap<>(assignments), new LinkedHashSet<>(assigner.lockedTours()),
                new LinkedHashSet<>(assigner.conditionalTours()), new ArrayList<>(coveringDrivers), copyOf(coveredTours), copyOf(concentratedTours));
    }

    private void restore(Snapshot snapshot) {
        assigner.workloads().clear();
        assigner.workloads().putAll(snapshot.workloads());
        assignments.clear();
        assignments.putAll(snapshot.assignments());
        assigner.lockedTours().clear();
        assigner.lockedTours().addAll(snapshot.locked());
        assigner.conditionalTours().clear();
        assigner.conditionalTours().addAll(snapshot.conditional());
        coveringDrivers = snapshot.covering();
        coveredTours = snapshot.covered();
        concentratedTours = snapshot.concentrated();
        reachableSurplus.clear();
    }

    private static Map<UUID, List<TourProcessingDraftAssignment>> copyOf(Map<UUID, List<TourProcessingDraftAssignment>> source) {
        Map<UUID, List<TourProcessingDraftAssignment>> copy = new LinkedHashMap<>();
        source.forEach((id, list) -> copy.put(id, new ArrayList<>(list)));
        return copy;
    }

    MandatoryCoverageSummary summary() {
        List<TourProcessingDraftAssignment> mandatory = tours.stream().filter(MandatoryCoverage::isMandatory)
                .sorted(DriverAutoAssigner.ASSIGNMENT_ORDER).toList();
        int fulfilled = 0;
        List<MandatoryCoverageSummary.TourNote> warnings = new ArrayList<>();
        List<MandatoryCoverageSummary.TourNote> unattended = new ArrayList<>();
        for (TourProcessingDraftAssignment tour : mandatory) {
            Driver driver = assignments.get(tour);
            if (driver == null) {
                unattended.add(new MandatoryCoverageSummary.TourNote(tour.getTour().getId(), tour.getNumber(), unattendedReason(tour)));
            } else if (requiresExtras(tour) && assigner.drivers().contains(driver) && !meetsTarget(driver)) {
                warnings.add(new MandatoryCoverageSummary.TourNote(tour.getTour().getId(), tour.getNumber(), warningMessage(driver)));
            } else {
                fulfilled++;
            }
        }
        return new MandatoryCoverageSummary(mandatory.size(), fulfilled, warnings.size(), unattended.size(), warnings, unattended,
                coveringSummary(), driversBelowTarget().size(), driversBelowTargetByConcentration().size());
    }

    private List<MandatoryCoverageSummary.CoveringDriver> coveringSummary() {
        return coveringDrivers.stream().map(driver -> {
            double surplus = assigner.surplusHoursOf(driver);
            double surplusCap = assigner.surplusCapOf(driver);
            List<String> numbers = coveredTours.getOrDefault(driver.getId(), List.of()).stream()
                    .map(TourProcessingDraftAssignment::getNumber).toList();
            return new MandatoryCoverageSummary.CoveringDriver(driver.getName(), surplus, assigner.surplusTargetHours(driver),
                    surplusCap <= 0 ? 0 : 100 * surplus / surplusCap, numbers);
        }).toList();
    }

    private List<Driver> driversBelowTarget() {
        return assigner.drivers().stream().filter(driver -> !meetsTarget(driver)).toList();
    }

    private List<Driver> driversBelowTargetByConcentration() {
        List<TourProcessingDraftAssignment> concentrated = coveringDrivers.stream()
                .flatMap(driver -> concentratedTours.getOrDefault(driver.getId(), List.of()).stream()).toList();
        return driversBelowTarget().stream().filter(driver -> !coveringDrivers.contains(driver))
                .filter(driver -> concentrated.stream().anyMatch(tour -> couldHaveTaken(driver, tour))).toList();
    }

    private boolean couldHaveTaken(Driver driver, TourProcessingDraftAssignment tour) {
        DriverWorkload workload = assigner.workloadOf(driver);
        if (!assigner.canAssign(driver, workload, tour, false)) {
            return false;
        }
        double surplusCap = assigner.surplusCapOf(driver);
        return workload.totalsWith(tour, surplusCap, assigner.monthWeekends()).surplusHours()
                > workload.totals(surplusCap, assigner.monthWeekends()).surplusHours() + TOLERANCE;
    }

    private String warningMessage(Driver driver) {
        double surplus = assigner.surplusHoursOf(driver);
        double surplusCap = assigner.surplusCapOf(driver);
        return "Asignada a " + driver.getName() + " con sobresueldo al " + hours(surplusCap <= 0 ? 0 : floorToTenth(100 * surplus / surplusCap))
                + "% (" + hours(surplus) + " de " + hours(surplusCap) + " horas). Ningún otro chofer pudo cubrirla llegando al 95%.";
    }

    private String unattendedReason(TourProcessingDraftAssignment tour) {
        if (assigner.drivers().isEmpty()) {
            return "No hay choferes activos de Servicios Generales disponibles en el catálogo.";
        }
        Map<DriverRejection.Reason, Integer> counts = new EnumMap<>(DriverRejection.Reason.class);
        for (Driver driver : assigner.drivers()) {
            Optional<DriverRejection> rejection = assigner.rejectionFor(driver, tour);
            if (rejection.isEmpty()) {
                return "Hay choferes que cumplen las reglas, pero la gira sigue pendiente de asignación.";
            }
            counts.merge(rejection.get().reason(), 1, Integer::sum);
        }
        return "Ningún chofer puede tomarla: " + counts.entrySet().stream()
                .map(entry -> entry.getValue() + " por " + label(entry.getKey())).collect(Collectors.joining(", ")) + ".";
    }

    private static String label(DriverRejection.Reason reason) {
        return switch (reason) {
            case INVALID_SCHEDULE -> "datos incompletos";
            case INCOMPATIBLE_VEHICLE -> "vehículo no compatible";
            case OVERLAP -> "cruce de horario";
            case DAY_HOURS_EXCEEDED -> "tope de horas por día";
            case WEEKEND_WITHOUT_SURPLUS -> "sobresueldo menor al 95%";
            case OVERTIME_CAP_EXCEEDED -> "tope de horas extra";
            case WEEKEND_CAP_EXCEEDED -> "tope de fines de semana";
        };
    }

    private static double floorToTenth(double value) {
        return Math.floor(value * 10) / 10;
    }

    private static String hours(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
