package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.Vehicle;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ScheduleOptimizer {

    record Stats(long transactions, int improvements, boolean limitReached, boolean provenOptimal, int referenceRejections, int changedTours) {
    }

    private static final double TOL = DriverHoursRules.HOURS_TOLERANCE;
    private static final int PASS_LIMIT = 80;
    private static final int EVICTION_CANDIDATES = 4;
    private static final int ROW_TRANSACTION_CAP = 160;
    private static final int CHAIN_NODE_CAP = 300;
    private static final long PAIR_TOTAL_NODES = 60_000L;
    private static final int MAX_EVICTED = 3;
    private static final int CHAIN_DEPTH = 3;
    private static final int FILL_CANDIDATES = 8;
    private static final int EXACT_ROUNDS = 3;
    private static final int FULL_SEARCH_ROWS = 16;
    private static final long FULL_SEARCH_NODES = 2_000_000L;
    private static final int PAIR_ROWS_LIMIT = 30;
    private static final int PAIR_WAITING_ROWS = 8;
    private static final long PAIR_NODES = 3_000L;
    private static final int SCRATCH = 4096;
    private static final int QUALITY_PARTS = 5;

    private static final Comparator<TourProcessingDraftAssignment> CHRONOLOGICAL = Comparator
            .comparing(TourProcessingDraftAssignment::getDepartureDate)
            .thenComparing(TourProcessingDraftAssignment::getDepartureTime)
            .thenComparing(TourProcessingDraftAssignment::getNumber);

    private static final class Profile {
        TourProcessingDraftAssignment ref;
        int chrono;
        long start;
        long end;
        int level;
        boolean movable;
        boolean conditional;
        boolean ulloa;
        int[] days;
        double[] hours;
        double[] rawHours;
        int weekendMask;
        double[] off;
        double[] ordinary;
        double manualSurplus;
        double baseOvertime;
        double cost;
        boolean[] allowed;
    }

    private record Metrics(double ordinary, double surplus, double overtime, int weekendMask, boolean hasConditional,
                           double deficit, double squaredOvertime, double squaredHours, double spacing, int stacked) {
    }

    private record Delta(int[] coverage, double[] quality) {

        boolean improving() {
            for (int level = 0; level < coverage.length; level++) {
                if (coverage[level] != 0) {
                    return coverage[level] > 0;
                }
            }
            for (int part = 0; part < QUALITY_PARTS; part++) {
                if (Math.abs(quality[part]) > TOL) {
                    return quality[part] < 0;
                }
            }
            return false;
        }
    }

    private interface Move {
        void apply();
    }

    private final DriverAutoAssigner assigner;
    private final List<Driver> drivers;
    private final int driverCount;
    private final double[] cap;
    private final double[] target;
    private final int[] windowOf;
    private final int weekendCap;
    private final Profile[] profiles;
    private final int[] owner;
    private final int[] initialOwner;
    private final int[][] load;
    private final int[] loadSize;
    private final Metrics[] metrics;
    private final boolean[] frozen;
    private final int levelCount;
    private final long transactionLimit;

    private final int[] undoRows = new int[SCRATCH];
    private final int[] undoFrom = new int[SCRATCH];
    private int undoSize;
    private final int[] covDelta;
    private final int[] touched = new int[SCRATCH];
    private final boolean[] touchedFlag;
    private int touchedCount;
    private final Metrics[] pending;
    private final int[] scratchDays = new int[SCRATCH];
    private final double[] scratchSum = new double[SCRATCH];
    private final int[] scratchCount = new int[SCRATCH];

    private long transactions;
    private int chainNodes;
    private long exactNodes;
    private int improvements;
    private boolean limitReached;
    private boolean provenOptimal;
    private int referenceRejections;
    private int changedTours;

    ScheduleOptimizer(DriverAutoAssigner assigner, Set<TourProcessingDraftAssignment> tours,
                      Map<TourProcessingDraftAssignment, Driver> assignments, long transactionLimit) {
        this.assigner = assigner;
        this.drivers = assigner.drivers();
        this.driverCount = drivers.size();
        this.transactionLimit = transactionLimit;
        this.weekendCap = WeekendCap.forWeekendsInMonth(assigner.monthWeekends().size());
        this.cap = new double[driverCount];
        this.target = new double[driverCount];
        this.windowOf = new int[driverCount];
        List<OrdinaryWindow> windows = new ArrayList<>();
        for (int index = 0; index < driverCount; index++) {
            Driver driver = drivers.get(index);
            cap[index] = assigner.surplusCapOf(driver);
            target[index] = cap[index] * DriverHoursRules.SURPLUS_CUTOFF_RATIO;
            OrdinaryWindow window = OrdinaryWindow.of(driver);
            if (!windows.contains(window)) {
                windows.add(window);
            }
            windowOf[index] = windows.indexOf(window);
        }

        List<TourProcessingDraftAssignment> rows = new ArrayList<>();
        List<Integer> rowOwners = new ArrayList<>();
        Set<TourProcessingDraftAssignment> seen = new LinkedHashSet<>();
        for (int index = 0; index < driverCount; index++) {
            for (TourProcessingDraftAssignment row : assigner.workloadOf(drivers.get(index)).tours()) {
                if (seen.add(row)) {
                    rows.add(row);
                    rowOwners.add(index);
                }
            }
        }
        for (TourProcessingDraftAssignment row : tours) {
            if (!assignments.containsKey(row) && !row.getTour().isDistributed() && DriverAutoAssigner.hasValidSchedule(row)
                    && row.isActive() && row.getResourceType() == ResourceType.INTERNAL && seen.add(row)) {
                rows.add(row);
                rowOwners.add(-1);
            }
        }

        List<TourProcessingDraftAssignment> ordered = new ArrayList<>(rows);
        ordered.sort(CHRONOLOGICAL);
        Map<TourProcessingDraftAssignment, Integer> chronoOf = new LinkedHashMap<>();
        for (int index = 0; index < ordered.size(); index++) {
            chronoOf.put(ordered.get(index), index);
        }

        List<LocalDate> weekendKeys = new ArrayList<>(assigner.monthWeekends());
        this.profiles = new Profile[rows.size()];
        this.owner = new int[rows.size()];
        this.initialOwner = new int[rows.size()];
        int maxLevel = 0;
        for (int index = 0; index < rows.size(); index++) {
            profiles[index] = profileOf(rows.get(index), chronoOf.get(rows.get(index)), windows, weekendKeys);
            owner[index] = rowOwners.get(index);
            initialOwner[index] = owner[index];
            maxLevel = Math.max(maxLevel, profiles[index].level);
        }
        this.levelCount = maxLevel + 1;
        this.covDelta = new int[levelCount];

        this.load = new int[driverCount][];
        this.loadSize = new int[driverCount];
        for (int index = 0; index < driverCount; index++) {
            load[index] = new int[16];
        }
        Integer[] byChrono = new Integer[rows.size()];
        for (int index = 0; index < byChrono.length; index++) {
            byChrono[index] = index;
        }
        Arrays.sort(byChrono, Comparator.comparingInt(index -> profiles[index].chrono));
        for (int index : byChrono) {
            if (owner[index] >= 0) {
                append(owner[index], index);
            }
        }
        this.metrics = new Metrics[driverCount];
        this.pending = new Metrics[driverCount];
        this.frozen = new boolean[driverCount];
        this.touchedFlag = new boolean[driverCount];
        for (int index = 0; index < driverCount; index++) {
            metrics[index] = measure(index, load[index], loadSize[index]);
            frozen[index] = !structurallyValid(index, load[index], loadSize[index], metrics[index]);
        }
    }

    private Profile profileOf(TourProcessingDraftAssignment row, int chrono, List<OrdinaryWindow> windows, List<LocalDate> weekendKeys) {
        Profile profile = new Profile();
        profile.ref = row;
        profile.chrono = chrono;
        profile.start = DriverHoursRules.startMinutes(row);
        profile.end = DriverHoursRules.endMinutes(row);
        profile.level = DriverAutoAssigner.assignmentLevel(row);
        profile.conditional = assigner.conditionalTours().contains(row);
        profile.ulloa = UlloaTours.isUlloaTour(row);
        profile.movable = !assigner.lockedTours().contains(row) && !row.getTour().isDistributed();
        List<TourDaySpan> spans = DriverHoursRules.daySpans(row);
        Map<LocalDate, DayAllocation> manual = assigner.manualDays().of(row);
        int count = spans.size();
        profile.days = new int[count];
        profile.hours = new double[count];
        profile.rawHours = new double[count];
        profile.off = new double[windows.size()];
        profile.ordinary = new double[windows.size()];
        for (int index = 0; index < count; index++) {
            TourDaySpan span = spans.get(index);
            DayAllocation edited = manual.get(span.date());
            profile.days[index] = (int) span.date().toEpochDay();
            profile.rawHours[index] = span.hours();
            TourDaySpan effective = edited == null ? span : edited.span();
            profile.hours[index] = effective.hours();
            if (effective.weekend() && effective.hours() > 0) {
                int bit = weekendKeys.indexOf(DriverHoursRules.weekendKey(effective.date()));
                if (bit >= 0) {
                    profile.weekendMask |= 1 << bit;
                }
            }
            if (edited == null) {
                profile.baseOvertime += span.weekendHours();
                for (int window = 0; window < windows.size(); window++) {
                    profile.off[window] += span.offScheduleWeekdayHours(windows.get(window));
                    profile.ordinary[window] += span.ordinaryHours(windows.get(window));
                }
            } else {
                profile.manualSurplus += edited.surplusHours();
                profile.baseOvertime += edited.overtimeHours();
                for (int window = 0; window < windows.size(); window++) {
                    profile.ordinary[window] += edited.ordinaryHours();
                }
            }
        }
        profile.cost = profile.off[0] + profile.baseOvertime;
        profile.allowed = new boolean[driverCount];
        Vehicle vehicle = DriverHoursRules.assignedVehicle(row, assigner.vehiclesById());
        for (int index = 0; index < driverCount; index++) {
            profile.allowed[index] = vehicle == null || DriverHoursRules.canDrive(drivers.get(index), vehicle);
        }
        return profile;
    }

    Stats run() {
        localSearch();
        exhaustiveSearch();
        return stats();
    }

    private void localSearch() {
        for (int pass = 0; pass < PASS_LIMIT && !limitReached; pass++) {
            boolean improved = insertPass();
            improved |= fillPass();
            improved |= relocatePass();
            improved |= swapPass();
            if (!improved) {
                return;
            }
        }
    }

    private void exhaustiveSearch() {
        for (int round = 0; round < EXACT_ROUNDS && !limitReached; round++) {
            List<Integer> movable = new ArrayList<>();
            for (int index = 0; index < profiles.length; index++) {
                if (profiles[index].movable && (owner[index] < 0 || !frozen[owner[index]])) {
                    movable.add(index);
                }
            }
            boolean improved = false;
            if (movable.size() <= FULL_SEARCH_ROWS) {
                int[] drivers = new int[driverCount];
                int count = 0;
                for (int driver = 0; driver < driverCount; driver++) {
                    if (!frozen[driver]) {
                        drivers[count++] = driver;
                    }
                }
                improved = exactRepair(toArray(movable), Arrays.copyOf(drivers, count), FULL_SEARCH_NODES, true);
                if (provenOptimal) {
                    return;
                }
            } else {
                for (int first = 0; first < driverCount && !limitReached && exactNodes < PAIR_TOTAL_NODES; first++) {
                    for (int second = first + 1; second < driverCount && !limitReached && exactNodes < PAIR_TOTAL_NODES; second++) {
                        if (!frozen[first] && !frozen[second]) {
                            improved |= repairPair(first, second);
                        }
                    }
                }
            }
            if (!improved) {
                return;
            }
            localSearch();
        }
    }

    private boolean repairPair(int first, int second) {
        List<Integer> rows = new ArrayList<>();
        for (int driver : new int[]{first, second}) {
            for (int index = 0; index < loadSize[driver]; index++) {
                if (profiles[load[driver][index]].movable) {
                    rows.add(load[driver][index]);
                }
            }
        }
        if (rows.size() > PAIR_ROWS_LIMIT) {
            return false;
        }
        List<Integer> waiting = new ArrayList<>();
        for (int index = 0; index < profiles.length; index++) {
            if (owner[index] < 0 && profiles[index].movable && (fitsAround(index, first) || fitsAround(index, second))) {
                waiting.add(index);
            }
        }
        waiting.sort(Comparator.comparingInt((Integer index) -> profiles[index].level).thenComparingDouble(index -> profiles[index].cost)
                .thenComparingInt(index -> profiles[index].chrono));
        rows.addAll(waiting.subList(0, Math.min(PAIR_WAITING_ROWS, waiting.size())));
        return exactRepair(toArray(rows), new int[]{first, second}, PAIR_NODES, false);
    }

    private boolean fitsAround(int row, int driver) {
        Profile profile = profiles[row];
        if (!profile.allowed[driver]) {
            return false;
        }
        for (int index = 0; index < loadSize[driver]; index++) {
            Profile other = profiles[load[driver][index]];
            if (!other.movable && other.start < profile.end && profile.start < other.end) {
                return false;
            }
        }
        return true;
    }

    private static final class Exact {
        int[] rows;
        int[] drivers;
        int[] origin;
        int[][] remaining;
        Delta best;
        int[] bestOwners;
        long nodes;
        long budget;
        boolean exhausted;
    }

    private boolean exactRepair(int[] rows, int[] drivers, long budget, boolean full) {
        Integer[] sorted = Arrays.stream(rows).boxed().sorted(Comparator.comparingInt((Integer index) -> profiles[index].level)
                .thenComparingInt(index -> profiles[index].chrono)).toArray(Integer[]::new);
        Exact exact = new Exact();
        exact.rows = Arrays.stream(sorted).mapToInt(Integer::intValue).toArray();
        exact.drivers = drivers;
        exact.budget = budget;
        exact.origin = new int[exact.rows.length];
        exact.remaining = new int[exact.rows.length + 1][levelCount];
        for (int index = exact.rows.length - 1; index >= 0; index--) {
            System.arraycopy(exact.remaining[index + 1], 0, exact.remaining[index], 0, levelCount);
            exact.remaining[index][profiles[exact.rows[index]].level]++;
        }
        beginTransaction();
        for (int index = 0; index < exact.rows.length; index++) {
            exact.origin[index] = owner[exact.rows[index]];
            move(exact.rows[index], -1);
        }
        search(0, exact);
        rollbackTransaction();
        if (full && !exact.exhausted && !limitReached) {
            provenOptimal = true;
        }
        if (exact.best == null) {
            return false;
        }
        evaluate(() -> {
            for (int index = 0; index < exact.rows.length; index++) {
                move(exact.rows[index], exact.bestOwners[index]);
            }
        }, true);
        improvements++;
        return true;
    }

    private void search(int index, Exact exact) {
        if (limitReached || exact.exhausted) {
            return;
        }
        if (++exact.nodes > exact.budget || ++exactNodes > PAIR_TOTAL_NODES + FULL_SEARCH_NODES) {
            exact.exhausted = true;
            return;
        }
        int[] optimistic = new int[levelCount];
        for (int level = 0; level < levelCount; level++) {
            optimistic[level] = covDelta[level] + exact.remaining[index][level];
        }
        int[] threshold = exact.best == null ? new int[levelCount] : exact.best.coverage();
        if (compareCoverage(optimistic, threshold) > 0) {
            return;
        }
        if (index == exact.rows.length) {
            Delta delta = assess();
            if (delta != null && delta.improving() && (exact.best == null || compare(delta, exact.best) < 0)) {
                exact.best = delta;
                exact.bestOwners = new int[exact.rows.length];
                for (int position = 0; position < exact.rows.length; position++) {
                    exact.bestOwners[position] = owner[exact.rows[position]];
                }
            }
            return;
        }
        int row = exact.rows[index];
        int origin = exact.origin[index];
        List<Integer> options = new ArrayList<>();
        if (origin >= 0 && contains(exact.drivers, origin)) {
            options.add(origin);
        }
        for (int driver : exact.drivers) {
            if (driver != origin && profiles[row].allowed[driver]) {
                options.add(driver);
            }
        }
        options.add(-1);
        for (int option : options) {
            int mark = undoSize;
            if (option >= 0) {
                move(row, option);
                if (!partiallyFeasible(option)) {
                    undoTo(mark);
                    continue;
                }
            }
            search(index + 1, exact);
            undoTo(mark);
            if (exact.exhausted || limitReached) {
                return;
            }
        }
    }

    private boolean partiallyFeasible(int driver) {
        int[] rows = load[driver];
        int size = loadSize[driver];
        if (!quickValid(driver, rows, size)) {
            return false;
        }
        return structurallyValid(driver, rows, size, measure(driver, rows, size, false), false);
    }

    private static int compareCoverage(int[] left, int[] right) {
        for (int level = 0; level < left.length; level++) {
            if (left[level] != right[level]) {
                return left[level] > right[level] ? -1 : 1;
            }
        }
        return 0;
    }

    private static boolean contains(int[] values, int value) {
        for (int candidate : values) {
            if (candidate == value) {
                return true;
            }
        }
        return false;
    }

    private static int[] toArray(Collection<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    Map<TourProcessingDraftAssignment, Driver> result() {
        Map<TourProcessingDraftAssignment, Driver> changes = new LinkedHashMap<>();
        changedTours = 0;
        Set<Integer> changedDrivers = new LinkedHashSet<>();
        for (int index = 0; index < profiles.length; index++) {
            if (owner[index] != initialOwner[index]) {
                changedDrivers.add(owner[index]);
                changedDrivers.add(initialOwner[index]);
            }
        }
        changedDrivers.remove(-1);
        for (int driver : changedDrivers) {
            DriverWorkload workload = new DriverWorkload(drivers.get(driver), assigner.manualDays());
            for (int index = 0; index < loadSize[driver]; index++) {
                workload.add(profiles[load[driver][index]].ref);
            }
            if (!assigner.structurallyValid(drivers.get(driver), workload)) {
                referenceRejections++;
                return Map.of();
            }
        }
        for (int index = 0; index < profiles.length; index++) {
            if (owner[index] != initialOwner[index]) {
                changes.put(profiles[index].ref, owner[index] < 0 ? null : drivers.get(owner[index]));
                changedTours++;
            }
        }
        return changes;
    }

    Stats stats() {
        return new Stats(transactions, improvements, limitReached, provenOptimal, referenceRejections, changedTours);
    }

    private boolean insertPass() {
        boolean improved = false;
        List<Integer> unassigned = new ArrayList<>();
        for (int index = 0; index < profiles.length; index++) {
            if (owner[index] < 0 && profiles[index].movable) {
                unassigned.add(index);
            }
        }
        unassigned.sort(Comparator.comparingInt((Integer index) -> profiles[index].level).thenComparingInt(index -> profiles[index].chrono));
        for (int row : unassigned) {
            if (limitReached) {
                break;
            }
            if (owner[row] >= 0) {
                continue;
            }
            improved |= placeDirect(row) || placeByChain(row) || placeWithEviction(row);
        }
        return improved;
    }

    private boolean placeByChain(int row) {
        chainNodes = 0;
        Delta delta = evaluate(() -> chain(row, CHAIN_DEPTH, new boolean[driverCount], profiles[row].level), true);
        if (delta == null || !delta.improving()) {
            return false;
        }
        improvements++;
        return true;
    }

    private boolean chain(int row, int depth, boolean[] visited, int rootLevel) {
        if (++chainNodes > CHAIN_NODE_CAP) {
            return false;
        }
        Profile profile = profiles[row];
        List<int[]> options = new ArrayList<>();
        for (int driver = 0; driver < driverCount; driver++) {
            if (visited[driver] || frozen[driver] || !profile.allowed[driver]) {
                continue;
            }
            int conflicts = 0;
            boolean blocked = false;
            for (int index = 0; index < loadSize[driver] && !blocked; index++) {
                Profile other = profiles[load[driver][index]];
                if (other.start < profile.end && profile.start < other.end) {
                    if (!other.movable) {
                        blocked = true;
                    }
                    conflicts++;
                }
            }
            if (!blocked && (conflicts == 0 || depth > 0 || canDrop(conflicts, driver, profile, rootLevel))) {
                options.add(new int[]{driver, conflicts});
            }
        }
        options.sort(Comparator.comparingInt((int[] option) -> option[1]).thenComparingInt(option -> option[0]));
        for (int[] option : options) {
            if (limitReached) {
                return false;
            }
            int driver = option[0];
            int mark = undoSize;
            List<Integer> displaced = new ArrayList<>();
            for (int index = 0; index < loadSize[driver]; index++) {
                Profile other = profiles[load[driver][index]];
                if (other.start < profile.end && profile.start < other.end) {
                    displaced.add(load[driver][index]);
                }
            }
            displaced.forEach(other -> move(other, -1));
            move(row, driver);
            visited[driver] = true;
            displaced.sort(Comparator.comparingInt((Integer index) -> profiles[index].level).thenComparingInt(index -> profiles[index].chrono));
            boolean settled = true;
            for (int other : displaced) {
                if (depth > 0 && chain(other, depth - 1, visited, rootLevel)) {
                    continue;
                }
                if (profiles[other].level > rootLevel) {
                    continue;
                }
                settled = false;
                break;
            }
            if (settled && feasibleNow(driver)) {
                visited[driver] = false;
                return true;
            }
            undoTo(mark);
            visited[driver] = false;
        }
        return false;
    }

    private boolean canDrop(int conflicts, int driver, Profile profile, int rootLevel) {
        for (int index = 0; index < loadSize[driver]; index++) {
            Profile other = profiles[load[driver][index]];
            if (other.start < profile.end && profile.start < other.end && other.level <= rootLevel) {
                return false;
            }
        }
        return conflicts > 0;
    }

    private boolean fillPass() {
        boolean improved = false;
        for (int driver = 0; driver < driverCount && !limitReached; driver++) {
            if (frozen[driver]) {
                continue;
            }
            List<Integer> candidates = new ArrayList<>();
            for (int index = 0; index < loadSize[driver]; index++) {
                if (profiles[load[driver][index]].movable) {
                    candidates.add(load[driver][index]);
                }
            }
            candidates.sort(Comparator.comparingDouble((Integer index) -> -profiles[index].cost).thenComparingInt(index -> profiles[index].chrono));
            List<Integer> shortlist = candidates.subList(0, Math.min(FILL_CANDIDATES, candidates.size()));
            List<int[]> sets = new ArrayList<>();
            for (int first = 0; first < shortlist.size(); first++) {
                sets.add(new int[]{shortlist.get(first)});
                for (int second = first + 1; second < shortlist.size(); second++) {
                    sets.add(new int[]{shortlist.get(first), shortlist.get(second)});
                }
            }
            Delta best = null;
            int[] bestSet = null;
            for (int[] set : sets) {
                int target = driver;
                Delta delta = evaluate(() -> freeAndFill(target, set), false);
                if (delta != null && delta.improving() && (best == null || compare(delta, best) < 0)) {
                    best = delta;
                    bestSet = set;
                }
            }
            if (bestSet != null) {
                int target = driver;
                int[] chosen = bestSet;
                evaluate(() -> freeAndFill(target, chosen), true);
                improvements++;
                improved = true;
            }
        }
        return improved;
    }

    private void freeAndFill(int driver, int[] freed) {
        for (int row : freed) {
            move(row, -1);
        }
        Set<Integer> skipped = new LinkedHashSet<>();
        for (int row : freed) {
            boolean placed = false;
            for (int other = 0; other < driverCount && !placed; other++) {
                if (other == driver || !profiles[row].allowed[other] || frozen[other] || overlapsLoad(other, row)) {
                    continue;
                }
                move(row, other);
                placed = feasibleNow(other);
                if (!placed) {
                    move(row, -1);
                }
            }
            if (!placed) {
                skipped.add(row);
            }
        }
        List<Integer> waiting = new ArrayList<>();
        for (int index = 0; index < profiles.length; index++) {
            if (owner[index] < 0 && profiles[index].movable && !skipped.contains(index) && profiles[index].allowed[driver]) {
                waiting.add(index);
            }
        }
        waiting.sort(Comparator.comparingInt((Integer index) -> profiles[index].level).thenComparingDouble(index -> profiles[index].cost)
                .thenComparingInt(index -> profiles[index].chrono));
        for (int row : waiting) {
            move(row, driver);
            if (!feasibleNow(driver)) {
                move(row, -1);
            }
        }
    }

    private void undoTo(int mark) {
        while (undoSize > mark) {
            undoSize--;
            int row = undoRows[undoSize];
            int previous = undoFrom[undoSize];
            adjustCoverage(row, owner[row], previous);
            rawMove(row, previous);
        }
    }

    private boolean placeDirect(int row) {
        Delta best = null;
        int bestDriver = -1;
        for (int driver = 0; driver < driverCount; driver++) {
            if (!profiles[row].allowed[driver] || frozen[driver]) {
                continue;
            }
            int candidate = driver;
            Delta delta = evaluate(() -> move(row, candidate), false);
            if (delta != null && delta.improving() && (best == null || compare(delta, best) < 0)) {
                best = delta;
                bestDriver = driver;
            }
        }
        if (bestDriver < 0) {
            return false;
        }
        int chosen = bestDriver;
        evaluate(() -> move(row, chosen), true);
        improvements++;
        return true;
    }

    private boolean placeWithEviction(int row) {
        long rowStart = transactions;
        Profile profile = profiles[row];
        Delta best = null;
        int bestDriver = -1;
        int[] bestEviction = null;
        for (int driver = 0; driver < driverCount; driver++) {
            if (!profile.allowed[driver] || frozen[driver] || limitReached || transactions - rowStart > ROW_TRANSACTION_CAP) {
                continue;
            }
            List<Integer> forced = new ArrayList<>();
            List<Integer> extras = new ArrayList<>();
            boolean blocked = false;
            for (int index = 0; index < loadSize[driver] && !blocked; index++) {
                int other = load[driver][index];
                Profile candidate = profiles[other];
                boolean overlaps = candidate.start < profile.end && profile.start < candidate.end;
                if (overlaps && !candidate.movable) {
                    blocked = true;
                } else if (overlaps) {
                    forced.add(other);
                } else if (candidate.movable && candidate.level >= profile.level) {
                    extras.add(other);
                }
            }
            if (blocked || forced.size() > MAX_EVICTED) {
                continue;
            }
            extras.sort(Comparator.comparingDouble((Integer index) -> -profiles[index].cost).thenComparingInt(index -> profiles[index].chrono));
            List<Integer> shortlist = extras.subList(0, Math.min(EVICTION_CANDIDATES, extras.size()));
            List<int[]> evictions = new ArrayList<>();
            if (!forced.isEmpty()) {
                evictions.add(toArray(forced));
            }
            for (int first = 0; first < shortlist.size() && forced.size() + 1 <= MAX_EVICTED; first++) {
                evictions.add(toArray(forced, shortlist.get(first)));
                for (int second = first + 1; second < shortlist.size() && forced.size() + 2 <= MAX_EVICTED; second++) {
                    evictions.add(toArray(forced, shortlist.get(first), shortlist.get(second)));
                }
            }
            for (int[] eviction : evictions) {
                int candidateDriver = driver;
                Delta delta = evaluate(() -> evict(row, candidateDriver, eviction), false);
                if (delta != null && delta.improving() && (best == null || compare(delta, best) < 0)) {
                    best = delta;
                    bestDriver = driver;
                    bestEviction = eviction;
                }
            }
        }
        if (bestDriver < 0) {
            return false;
        }
        int chosenDriver = bestDriver;
        int[] chosenEviction = bestEviction;
        evaluate(() -> evict(row, chosenDriver, chosenEviction), true);
        improvements++;
        return true;
    }

    private void evict(int row, int driver, int[] eviction) {
        for (int evicted : eviction) {
            move(evicted, -1);
        }
        move(row, driver);
        int[] ordered = eviction.clone();
        for (int first = 1; first < ordered.length; first++) {
            int value = ordered[first];
            int position = first;
            while (position > 0 && precedes(value, ordered[position - 1])) {
                ordered[position] = ordered[position - 1];
                position--;
            }
            ordered[position] = value;
        }
        for (int evicted : ordered) {
            for (int other = 0; other < driverCount; other++) {
                if (other == driver || !profiles[evicted].allowed[other] || frozen[other] || overlapsLoad(other, evicted)) {
                    continue;
                }
                move(evicted, other);
                if (feasibleNow(other)) {
                    break;
                }
                move(evicted, -1);
            }
        }
    }

    private boolean precedes(int left, int right) {
        Profile first = profiles[left];
        Profile second = profiles[right];
        return first.level != second.level ? first.level < second.level : first.chrono < second.chrono;
    }

    private boolean overlapsLoad(int driver, int row) {
        Profile profile = profiles[row];
        for (int index = 0; index < loadSize[driver]; index++) {
            Profile other = profiles[load[driver][index]];
            if (other.start < profile.end && profile.start < other.end) {
                return true;
            }
        }
        return false;
    }

    private boolean relocatePass() {
        boolean improved = false;
        List<Integer> assigned = new ArrayList<>();
        for (int index = 0; index < profiles.length; index++) {
            if (owner[index] >= 0 && profiles[index].movable) {
                assigned.add(index);
            }
        }
        assigned.sort(Comparator.comparingInt((Integer index) -> profiles[index].level).thenComparingInt(index -> profiles[index].chrono));
        for (int row : assigned) {
            if (limitReached) {
                break;
            }
            int origin = owner[row];
            if (frozen[origin]) {
                continue;
            }
            Delta best = null;
            int bestDriver = -1;
            for (int driver = 0; driver < driverCount; driver++) {
                if (driver == origin || !profiles[row].allowed[driver] || frozen[driver]) {
                    continue;
                }
                int candidate = driver;
                Delta delta = evaluate(() -> move(row, candidate), false);
                if (delta != null && delta.improving() && (best == null || compare(delta, best) < 0)) {
                    best = delta;
                    bestDriver = driver;
                }
            }
            if (bestDriver >= 0) {
                int chosen = bestDriver;
                evaluate(() -> move(row, chosen), true);
                improvements++;
                improved = true;
            }
        }
        return improved;
    }

    private boolean swapPass() {
        boolean improved = false;
        List<Integer> assigned = new ArrayList<>();
        for (int index = 0; index < profiles.length; index++) {
            if (owner[index] >= 0 && profiles[index].movable) {
                assigned.add(index);
            }
        }
        assigned.sort(Comparator.comparingInt((Integer index) -> profiles[index].chrono));
        for (int first = 0; first < assigned.size() && !limitReached; first++) {
            int rowA = assigned.get(first);
            Delta best = null;
            int bestRow = -1;
            for (int second = first + 1; second < assigned.size(); second++) {
                int rowB = assigned.get(second);
                int driverA = owner[rowA];
                int driverB = owner[rowB];
                if (driverA == driverB || frozen[driverA] || frozen[driverB]
                        || !profiles[rowA].allowed[driverB] || !profiles[rowB].allowed[driverA]) {
                    continue;
                }
                Delta delta = evaluate(() -> {
                    move(rowA, driverB);
                    move(rowB, driverA);
                }, false);
                if (delta != null && delta.improving() && (best == null || compare(delta, best) < 0)) {
                    best = delta;
                    bestRow = rowB;
                }
            }
            if (bestRow >= 0) {
                int chosen = bestRow;
                int driverA = owner[rowA];
                int driverB = owner[chosen];
                evaluate(() -> {
                    move(rowA, driverB);
                    move(chosen, driverA);
                }, true);
                improvements++;
                improved = true;
            }
        }
        return improved;
    }

    private Delta evaluate(Move proposal, boolean commit) {
        beginTransaction();
        proposal.apply();
        Delta delta = assess();
        if (delta != null && commit && delta.improving()) {
            commitTransaction();
        } else {
            rollbackTransaction();
        }
        return delta;
    }

    private Delta assess() {
        if (++transactions > transactionLimit) {
            limitReached = true;
            return null;
        }
        double[] before = new double[QUALITY_PARTS];
        double[] after = new double[QUALITY_PARTS];
        for (int index = 0; index < touchedCount; index++) {
            int driver = touched[index];
            if (frozen[driver]) {
                return null;
            }
            int[] rows = load[driver];
            int size = loadSize[driver];
            if (!quickValid(driver, rows, size)) {
                return null;
            }
            Metrics updated = measure(driver, rows, size);
            if (!structurallyValid(driver, rows, size, updated) || !transitionOk(driver, updated, metrics[driver])) {
                return null;
            }
            pending[driver] = updated;
            accumulate(before, metrics[driver]);
            accumulate(after, updated);
        }
        double[] quality = new double[QUALITY_PARTS];
        for (int part = 0; part < QUALITY_PARTS; part++) {
            quality[part] = after[part] - before[part];
        }
        return new Delta(Arrays.copyOf(covDelta, levelCount), quality);
    }

    private static void accumulate(double[] sums, Metrics value) {
        sums[0] += value.deficit();
        sums[1] += value.stacked();
        sums[2] += value.squaredOvertime();
        sums[3] += value.spacing();
        sums[4] += value.squaredHours();
    }

    private static int compare(Delta left, Delta right) {
        return compare(left.coverage(), left.quality(), right.coverage(), right.quality());
    }

    private static int compare(int[] leftCoverage, double[] leftQuality, int[] rightCoverage, double[] rightQuality) {
        for (int level = 0; level < leftCoverage.length; level++) {
            if (leftCoverage[level] != rightCoverage[level]) {
                return leftCoverage[level] > rightCoverage[level] ? -1 : 1;
            }
        }
        for (int part = 0; part < QUALITY_PARTS; part++) {
            double difference = leftQuality[part] - rightQuality[part];
            if (Math.abs(difference) > TOL) {
                return difference < 0 ? -1 : 1;
            }
        }
        return 0;
    }

    private void beginTransaction() {
        undoSize = 0;
        touchedCount = 0;
        Arrays.fill(covDelta, 0);
    }

    private void commitTransaction() {
        for (int index = 0; index < touchedCount; index++) {
            int driver = touched[index];
            metrics[driver] = pending[driver];
            touchedFlag[driver] = false;
        }
        touchedCount = 0;
        undoSize = 0;
    }

    private void rollbackTransaction() {
        for (int index = undoSize - 1; index >= 0; index--) {
            rawMove(undoRows[index], undoFrom[index]);
        }
        for (int index = 0; index < touchedCount; index++) {
            touchedFlag[touched[index]] = false;
        }
        touchedCount = 0;
        undoSize = 0;
    }

    private void move(int row, int to) {
        int from = owner[row];
        if (from == to) {
            return;
        }
        undoRows[undoSize] = row;
        undoFrom[undoSize] = from;
        undoSize++;
        if (from >= 0) {
            touch(from);
        }
        if (to >= 0) {
            touch(to);
        }
        adjustCoverage(row, from, to);
        rawMove(row, to);
    }

    private void adjustCoverage(int row, int from, int to) {
        int level = profiles[row].level;
        if (from < 0 && to >= 0) {
            covDelta[level]++;
        } else if (from >= 0 && to < 0) {
            covDelta[level]--;
        }
    }

    private void touch(int driver) {
        if (!touchedFlag[driver]) {
            touchedFlag[driver] = true;
            touched[touchedCount++] = driver;
        }
    }

    private void rawMove(int row, int to) {
        int from = owner[row];
        if (from >= 0) {
            removeFromLoad(from, row);
        }
        if (to >= 0) {
            insertIntoLoad(to, row);
        }
        owner[row] = to;
    }

    private void append(int driver, int row) {
        if (loadSize[driver] == load[driver].length) {
            load[driver] = Arrays.copyOf(load[driver], load[driver].length * 2);
        }
        load[driver][loadSize[driver]++] = row;
    }

    private void insertIntoLoad(int driver, int row) {
        if (loadSize[driver] == load[driver].length) {
            load[driver] = Arrays.copyOf(load[driver], load[driver].length * 2);
        }
        int[] rows = load[driver];
        int position = loadSize[driver];
        int chrono = profiles[row].chrono;
        while (position > 0 && profiles[rows[position - 1]].chrono > chrono) {
            rows[position] = rows[position - 1];
            position--;
        }
        rows[position] = row;
        loadSize[driver]++;
    }

    private void removeFromLoad(int driver, int row) {
        int[] rows = load[driver];
        int size = loadSize[driver];
        int position = 0;
        while (rows[position] != row) {
            position++;
        }
        System.arraycopy(rows, position + 1, rows, position, size - position - 1);
        loadSize[driver]--;
    }

    private boolean feasibleNow(int driver) {
        int[] rows = load[driver];
        int size = loadSize[driver];
        if (!quickValid(driver, rows, size)) {
            return false;
        }
        Metrics updated = measure(driver, rows, size, false);
        return structurallyValid(driver, rows, size, updated) && transitionOk(driver, updated, metrics[driver]);
    }

    private boolean quickValid(int driver, int[] rows, int size) {
        long maxEnd = Long.MIN_VALUE;
        for (int index = 0; index < size; index++) {
            Profile profile = profiles[rows[index]];
            if (!profile.allowed[driver] || profile.start < maxEnd) {
                return false;
            }
            maxEnd = Math.max(maxEnd, profile.end);
        }
        return true;
    }

    private boolean structurallyValid(int driver, int[] rows, int size, Metrics value) {
        return structurallyValid(driver, rows, size, value, true);
    }

    private boolean structurallyValid(int driver, int[] rows, int size, Metrics value, boolean complete) {
        if (size == 0) {
            return true;
        }
        int entries = 0;
        boolean hasConditional = false;
        boolean hasRegular = false;
        for (int index = 0; index < size; index++) {
            Profile profile = profiles[rows[index]];
            if (!profile.allowed[driver]) {
                return false;
            }
            if (profile.conditional) {
                hasConditional = true;
            } else {
                hasRegular = true;
            }
            for (int span = 0; span < profile.days.length; span++) {
                int slot = 0;
                while (slot < entries && scratchDays[slot] != profile.days[span]) {
                    slot++;
                }
                if (slot == entries) {
                    scratchDays[entries] = profile.days[span];
                    scratchSum[entries] = 0;
                    scratchCount[entries] = 0;
                    entries++;
                }
                scratchSum[slot] += profile.hours[span];
                scratchCount[slot]++;
            }
        }
        for (int slot = 0; slot < entries; slot++) {
            if (scratchCount[slot] >= 2 && scratchSum[slot] > DriverHoursRules.MAX_STACKED_DAY_HOURS + TOL) {
                return false;
            }
        }
        if (value.overtime() > DriverHoursRules.MONTHLY_OVERTIME_CAP + TOL) {
            return false;
        }
        int weekends = Integer.bitCount(value.weekendMask());
        if (weekends > weekendCap) {
            for (int skipped = 0; skipped < size; skipped++) {
                int remainingMask = 0;
                for (int index = 0; index < size; index++) {
                    if (index != skipped) {
                        remainingMask |= profiles[rows[index]].weekendMask;
                    }
                }
                if (Integer.bitCount(remainingMask) < weekends) {
                    return false;
                }
            }
        }
        if (complete && hasRegular) {
            double overtime = hasConditional ? overtimeWithoutConditional(driver, rows, size) : value.overtime();
            if (overtime > TOL && value.surplus() < target[driver] - TOL) {
                return false;
            }
        }
        return true;
    }

    private double overtimeWithoutConditional(int driver, int[] rows, int size) {
        int[] filtered = new int[size];
        int count = 0;
        for (int index = 0; index < size; index++) {
            if (!profiles[rows[index]].conditional) {
                filtered[count++] = rows[index];
            }
        }
        return measure(driver, filtered, count, false).overtime();
    }

    private boolean transitionOk(int driver, Metrics updated, Metrics current) {
        int currentWeekends = Integer.bitCount(current.weekendMask());
        if (Integer.bitCount(updated.weekendMask()) > Math.max(weekendCap, currentWeekends)) {
            return false;
        }
        if (current.hasConditional()) {
            double protectedSurplus = Math.min(target[driver], current.surplus());
            return updated.surplus() >= protectedSurplus - TOL;
        }
        return true;
    }

    private Metrics measure(int driver, int[] rows, int size) {
        return measure(driver, rows, size, true);
    }

    private Metrics measure(int driver, int[] rows, int size, boolean withQuality) {
        double capacity = cap[driver];
        int window = windowOf[driver];
        double reserved = 0;
        for (int index = 0; index < size; index++) {
            reserved += profiles[rows[index]].manualSurplus;
        }
        double accumulated = 0;
        double surplus = 0;
        double overtime = 0;
        double ordinary = 0;
        int mask = 0;
        boolean hasConditional = false;
        for (int index = 0; index < size; index++) {
            Profile profile = profiles[rows[index]];
            reserved -= profile.manualSurplus;
            double tourSurplus = surplusFor(accumulated + profile.manualSurplus, capacity, profile.off[window], reserved);
            accumulated += profile.manualSurplus + tourSurplus;
            surplus += profile.manualSurplus + tourSurplus;
            overtime += profile.baseOvertime + profile.off[window] - tourSurplus;
            ordinary += profile.ordinary[window];
            mask |= profile.weekendMask;
            hasConditional |= profile.conditional;
        }
        surplus = round(surplus);
        overtime = round(overtime);
        ordinary = round(ordinary);
        if (!withQuality) {
            return new Metrics(ordinary, surplus, overtime, mask, hasConditional, 0, 0, 0, 0, 0);
        }

        int entries = 0;
        double spacing = 0;
        Profile previous = null;
        for (int index = 0; index < size; index++) {
            Profile profile = profiles[rows[index]];
            for (int span = 0; span < profile.days.length; span++) {
                if (profile.rawHours[span] > 0) {
                    int slot = 0;
                    while (slot < entries && scratchDays[slot] != profile.days[span]) {
                        slot++;
                    }
                    if (slot == entries) {
                        scratchDays[entries] = profile.days[span];
                        scratchCount[entries] = 0;
                        entries++;
                    }
                    scratchCount[slot]++;
                }
            }
            if (!profile.ulloa) {
                if (previous != null) {
                    double gap = (previous.start < profile.start ? profile.start - previous.end : previous.start - profile.end) / 60.0;
                    spacing += 1.0 / (1.0 + Math.max(0, gap));
                }
                previous = profile;
            }
        }
        int stacked = 0;
        for (int slot = 0; slot < entries; slot++) {
            if (scratchCount[slot] > 1) {
                stacked++;
            }
        }
        double deficit = Math.max(0, target[driver] - surplus);
        return new Metrics(ordinary, surplus, overtime, mask, hasConditional, deficit, overtime * overtime,
                (surplus + overtime) * (surplus + overtime), spacing, stacked);
    }

    private static double surplusFor(double accumulated, double capacity, double offSchedule, double reservedManual) {
        double remaining = capacity - accumulated - reservedManual;
        boolean exhausted = remaining <= DriverHoursRules.HOURS_TOLERANCE;
        boolean pastCutoffWithoutRoom = accumulated >= capacity * DriverHoursRules.SURPLUS_CUTOFF_RATIO && remaining < offSchedule;
        if (exhausted || pastCutoffWithoutRoom) {
            return 0;
        }
        return Math.min(offSchedule, remaining);
    }

    private static double round(double hours) {
        return Math.round(hours * 100) / 100.0;
    }

    private static int[] toArray(List<Integer> base, Integer... extra) {
        int[] values = new int[base.size() + extra.length];
        for (int index = 0; index < base.size(); index++) {
            values[index] = base.get(index);
        }
        for (int index = 0; index < extra.length; index++) {
            values[base.size() + index] = extra[index];
        }
        return values;
    }
}
