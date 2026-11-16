package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.entity.VehicleType;
import com.proseg.msvc_transport.service.DraftCatalogLinker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.assigned;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.driver;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.sameDayTour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.tour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.vehicle;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.withPassengers;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.withPriority;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.withVehicle;
import static org.assertj.core.api.Assertions.assertThat;

@Timeout(120)
class ScheduleOptimizerTest {

    private static final int YEAR = 2026;
    private static final int MONTH = 5;
    private static final int LEVELS = 40;

    private record Objective(int[] coverage, DriverAutoAssigner.ScheduleQuality quality) {
    }

    private static DriverAutoAssigner reference(List<Driver> drivers, List<TourProcessingDraftAssignment> rows, int businessDays) {
        return new DriverAutoAssigner(YEAR, MONTH, businessDays, drivers, List.of(), rows, ManualDays.NONE);
    }

    private static Objective objectiveOf(DriverAutoAssigner reference, List<Driver> drivers, Map<TourProcessingDraftAssignment, Driver> owners) {
        Map<UUID, DriverWorkload> workloads = new LinkedHashMap<>();
        drivers.forEach(driver -> workloads.put(driver.getId(), new DriverWorkload(driver)));
        int[] coverage = new int[LEVELS];
        owners.forEach((row, driver) -> {
            if (driver != null) {
                workloads.get(driver.getId()).add(row);
                coverage[DriverAutoAssigner.assignmentLevel(row)]++;
            }
        });
        return new Objective(coverage, reference.quality(workloads));
    }

    private static int compare(DriverAutoAssigner reference, Objective left, Objective right) {
        for (int level = 0; level < LEVELS; level++) {
            if (left.coverage()[level] != right.coverage()[level]) {
                return left.coverage()[level] > right.coverage()[level] ? -1 : 1;
            }
        }
        return reference.compareQuality(left.quality(), right.quality());
    }

    private static boolean valid(DriverAutoAssigner reference, List<Driver> drivers, Map<TourProcessingDraftAssignment, Driver> owners) {
        for (Driver driver : drivers) {
            DriverWorkload workload = new DriverWorkload(driver);
            owners.forEach((row, owner) -> {
                if (driver.equals(owner)) {
                    workload.add(row);
                }
            });
            if (!reference.structurallyValid(driver, workload)) {
                return false;
            }
        }
        return true;
    }

    private static Map<TourProcessingDraftAssignment, Driver> ownersAfter(List<TourProcessingDraftAssignment> rows, List<Driver> drivers,
                                                                          Map<TourProcessingDraftAssignment, Driver> changes) {
        Map<UUID, Driver> byId = new HashMap<>();
        drivers.forEach(driver -> byId.put(driver.getId(), driver));
        Map<TourProcessingDraftAssignment, Driver> owners = new LinkedHashMap<>();
        for (TourProcessingDraftAssignment row : rows) {
            Driver owner = changes.containsKey(row) ? changes.get(row) : row.getDriverId() == null ? null : byId.get(row.getDriverId());
            owners.put(row, owner);
        }
        return owners;
    }

    private static List<TourProcessingDraftAssignment> randomRows(Random random, int count, int days, int startDay) {
        List<TourProcessingDraftAssignment> rows = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            LocalDate departure = LocalDate.of(YEAR, MONTH, startDay).plusDays(random.nextInt(days));
            LocalDate arrival = departure.plusDays(random.nextInt(10) == 0 ? 1 : 0);
            int startHour = 5 + random.nextInt(10);
            int endHour = Math.min(23, startHour + 3 + random.nextInt(11));
            TourProcessingDraftAssignment row = tour(Integer.toString(index + 1), departure.toString(), String.format("%02d:00", startHour),
                    arrival.toString(), String.format("%02d:30", endHour));
            withPriority(row, 2 + random.nextInt(3));
            withPassengers(row, random.nextBoolean() ? 3 : 8);
            rows.add(row);
        }
        return rows;
    }

    private static List<Driver> drivers(int count) {
        List<Driver> drivers = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            drivers.add(driver("Chofer " + (char) ('A' + index)));
        }
        return drivers;
    }

    private Map<TourProcessingDraftAssignment, Driver> assign(DriverAutoAssigner assigner, List<TourProcessingDraftAssignment> rows) {
        return assigner.assign(rows.stream().filter(row -> row.getDriverId() == null).toList());
    }

    @Test
    void reachesTheBestPossibleScheduleByVisitingEveryCombinationOnSmallMonths() {
        for (int seed = 0; seed < 14; seed++) {
            Random random = new Random(seed);
            int businessDays = seed % 2 == 0 ? 3 : 28;
            List<Driver> drivers = drivers(3);
            List<TourProcessingDraftAssignment> rows = randomRows(random, 7, 6, 4);

            DriverAutoAssigner assigner = new DriverAutoAssigner(YEAR, MONTH, businessDays, drivers, List.of(), rows, ManualDays.NONE);
            Map<TourProcessingDraftAssignment, Driver> result = assign(assigner, rows);

            DriverAutoAssigner reference = reference(drivers, rows, businessDays);
            Map<TourProcessingDraftAssignment, Driver> found = ownersAfter(rows, drivers, result);
            assertThat(valid(reference, drivers, found)).as("seed %d valid", seed).isTrue();
            assertThat(assigner.searchStats().referenceRejections()).isZero();

            Objective best = null;
            int options = drivers.size() + 1;
            long combinations = (long) Math.pow(options, rows.size());
            for (long code = 0; code < combinations; code++) {
                Map<TourProcessingDraftAssignment, Driver> candidate = new LinkedHashMap<>();
                long remaining = code;
                for (TourProcessingDraftAssignment row : rows) {
                    int choice = (int) (remaining % options);
                    remaining /= options;
                    candidate.put(row, choice == 0 ? null : drivers.get(choice - 1));
                }
                if (!valid(reference, drivers, candidate)) {
                    continue;
                }
                Objective objective = objectiveOf(reference, drivers, candidate);
                if (best == null || compare(reference, objective, best) < 0) {
                    best = objective;
                }
            }

            assertThat(best).isNotNull();
            assertThat(compare(reference, objectiveOf(reference, drivers, found), best))
                    .as("seed %d must match the exhaustive optimum", seed).isZero();
            assertThat(assigner.searchStats().provenOptimal()).as("seed %d proven", seed).isTrue();
        }
    }

    @Test
    void neverLeavesAScheduleWorseThanTheGreedyPassesOnABusyMonth() {
        for (int seed = 100; seed < 106; seed++) {
            Random random = new Random(seed);
            List<Driver> drivers = drivers(5);
            List<TourProcessingDraftAssignment> rows = randomRows(random, 70, 7, 4);

            DriverAutoAssigner greedy = new DriverAutoAssigner(YEAR, MONTH, 20, drivers, List.of(), rows, ManualDays.NONE);
            greedy.disableSearch();
            Map<TourProcessingDraftAssignment, Driver> greedyResult = assign(greedy, rows);
            DriverAutoAssigner searching = new DriverAutoAssigner(YEAR, MONTH, 20, drivers, List.of(), rows, ManualDays.NONE);
            Map<TourProcessingDraftAssignment, Driver> searchResult = assign(searching, rows);

            DriverAutoAssigner reference = reference(drivers, rows, 20);
            Map<TourProcessingDraftAssignment, Driver> before = ownersAfter(rows, drivers, greedyResult);
            Map<TourProcessingDraftAssignment, Driver> after = ownersAfter(rows, drivers, searchResult);
            assertThat(valid(reference, drivers, after)).as("seed %d valid", seed).isTrue();
            assertThat(searching.searchStats().referenceRejections()).isZero();
            assertThat(compare(reference, objectiveOf(reference, drivers, after), objectiveOf(reference, drivers, before)))
                    .as("seed %d must not be worse", seed).isLessThanOrEqualTo(0);
        }
    }

    @Test
    void keepsEveryExistingAssignmentAndCompletesTheRestWhateverTheStartingState() {
        for (int seed = 200; seed < 206; seed++) {
            Random random = new Random(seed);
            List<Driver> drivers = drivers(5);
            List<TourProcessingDraftAssignment> rows = randomRows(random, 60, 7, 4);

            DriverAutoAssigner full = new DriverAutoAssigner(YEAR, MONTH, 20, drivers, List.of(), rows, ManualDays.NONE);
            Map<TourProcessingDraftAssignment, Driver> fullResult = assign(full, rows);
            Map<TourProcessingDraftAssignment, Driver> kept = new LinkedHashMap<>();
            List<TourProcessingDraftAssignment> ordered = rows.stream().filter(fullResult::containsKey)
                    .sorted(Comparator.comparing(TourProcessingDraftAssignment::getDepartureDate).thenComparing(TourProcessingDraftAssignment::getNumber)).toList();
            for (int index = 0; index < ordered.size(); index += 3) {
                kept.put(ordered.get(index), fullResult.get(ordered.get(index)));
            }
            rows.forEach(row -> {
                row.setDriver(null);
                row.setDriverId(null);
            });
            kept.forEach((row, driver) -> assigned(row, driver));

            DriverAutoAssigner greedy = new DriverAutoAssigner(YEAR, MONTH, 20, drivers, List.of(), rows, ManualDays.NONE);
            greedy.disableSearch();
            Map<TourProcessingDraftAssignment, Driver> greedyResult = assign(greedy, rows);
            DriverAutoAssigner searching = new DriverAutoAssigner(YEAR, MONTH, 20, drivers, List.of(), rows, ManualDays.NONE);
            Map<TourProcessingDraftAssignment, Driver> searchResult = assign(searching, rows);

            assertThat(searchResult.keySet()).doesNotContainAnyElementsOf(kept.keySet());
            DriverAutoAssigner reference = reference(drivers, rows, 20);
            Map<TourProcessingDraftAssignment, Driver> before = ownersAfter(rows, drivers, greedyResult);
            Map<TourProcessingDraftAssignment, Driver> after = ownersAfter(rows, drivers, searchResult);
            kept.forEach((row, driver) -> assertThat(after.get(row)).isEqualTo(driver));
            for (Driver driver : drivers) {
                DriverWorkload keptLoad = new DriverWorkload(driver);
                kept.forEach((row, owner) -> {
                    if (driver.equals(owner)) {
                        keptLoad.add(row);
                    }
                });
                DriverWorkload finalLoad = new DriverWorkload(driver);
                after.forEach((row, owner) -> {
                    if (driver.equals(owner)) {
                        finalLoad.add(row);
                    }
                });
                if (reference.structurallyValid(driver, keptLoad)) {
                    assertThat(reference.structurallyValid(driver, finalLoad)).as("seed %d driver %s", seed, driver.getName()).isTrue();
                } else {
                    assertThat(finalLoad.tours()).as("seed %d driver %s keeps only its manual tours", seed, driver.getName())
                            .hasSameSizeAs(keptLoad.tours());
                }
            }
            assertThat(searching.searchStats().referenceRejections()).isZero();
            assertThat(compare(reference, objectiveOf(reference, drivers, after), objectiveOf(reference, drivers, before)))
                    .as("seed %d must not be worse", seed).isLessThanOrEqualTo(0);
        }
    }

    @Test
    void replacesOneLongTourWithTwoShortOnesOfTheSameLevelWhenItServesMoreRequests() {
        Driver ana = driver("Ana");
        TourProcessingDraftAssignment longTour = withPriority(tour("1", "2026-05-05", "08:00", "2026-05-07", "17:00"), 2);
        TourProcessingDraftAssignment morning = withPriority(sameDayTour("2", "2026-05-05", "08:00", "12:00"), 2);
        TourProcessingDraftAssignment afternoon = withPriority(sameDayTour("3", "2026-05-06", "08:00", "12:00"), 2);
        List<TourProcessingDraftAssignment> rows = List.of(longTour, morning, afternoon);

        DriverAutoAssigner assigner = new DriverAutoAssigner(YEAR, MONTH, 28, List.of(ana), List.of(), rows, ManualDays.NONE);
        Map<TourProcessingDraftAssignment, Driver> result = assigner.assign(rows);

        assertThat(result).containsEntry(morning, ana).containsEntry(afternoon, ana);
        assertThat(result.get(longTour)).isNull();
    }

    @Test
    void relocatesATourToAnotherDriverToFreeTheOnlyDriverAllowedToTakeAnother() {
        Driver ana = driver("Ana", VehicleType.MICROBUS, VehicleType.SEDAN);
        Driver beto = driver("Beto", VehicleType.SEDAN);
        Vehicle microbus = vehicle("100", VehicleType.MICROBUS, 20);
        Vehicle sedan = vehicle("200", VehicleType.SEDAN, 4);
        TourProcessingDraftAssignment sedanTour = withVehicle(withPriority(sameDayTour("1", "2026-05-05", "08:00", "12:00"), 2), "200");
        TourProcessingDraftAssignment microbusTour = withVehicle(withPriority(sameDayTour("2", "2026-05-05", "09:00", "11:00"), 3), "100");
        List<TourProcessingDraftAssignment> rows = List.of(sedanTour, microbusTour);
        DraftCatalogLinker linker = DraftCatalogLinker.of(List.of(ana, beto), List.of(microbus, sedan));
        rows.forEach(linker::link);

        DriverAutoAssigner assigner = new DriverAutoAssigner(YEAR, MONTH, 28, List.of(ana, beto), List.of(microbus, sedan), rows, ManualDays.NONE);
        Map<TourProcessingDraftAssignment, Driver> result = assigner.assign(rows);

        assertThat(result).containsEntry(sedanTour, beto).containsEntry(microbusTour, ana);
    }

    @Test
    void producesTheSameScheduleWhateverTheOrderOfTheRows() {
        Random random = new Random(7);
        List<Driver> drivers = drivers(4);
        List<TourProcessingDraftAssignment> rows = randomRows(random, 50, 6, 4);
        List<TourProcessingDraftAssignment> reversed = new ArrayList<>(rows);
        java.util.Collections.reverse(reversed);

        Map<TourProcessingDraftAssignment, Driver> forward = new DriverAutoAssigner(YEAR, MONTH, 20, drivers, List.of(), rows, ManualDays.NONE).assign(rows);
        Map<TourProcessingDraftAssignment, Driver> backward = new DriverAutoAssigner(YEAR, MONTH, 20, drivers, List.of(), reversed, ManualDays.NONE).assign(reversed);

        assertThat(forward).isNotEmpty().isEqualTo(backward);
    }
}
