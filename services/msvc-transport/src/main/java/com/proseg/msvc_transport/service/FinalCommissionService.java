package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.tourprocessing.FinalCommissionCheckDto;
import com.proseg.msvc_transport.dto.tourprocessing.FinalCommissionReportDto;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.DriverUnit;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourApprovalStatus;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.VehicleRepository;
import com.proseg.msvc_transport.service.driverhours.DriverBusinessDays;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import com.proseg.msvc_transport.service.driverhours.ManualDays;
import com.proseg.msvc_transport.service.driverhours.OrdinaryWindow;
import com.proseg.msvc_transport.service.rules.TourAssignmentRules;
import com.proseg.msvc_transport.service.driverhours.TourTrips;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class FinalCommissionService {

    public static final String NOT_APPROVED_CATEGORY = "NOT_APPROVED";

    private static final String DRAFT_RESOURCE = "Borrador de depuración";
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final TourAssignmentRules assignmentRules = new TourAssignmentRules();
    private static final Comparator<TourProcessingDraftTour> REPORT_ORDER = Comparator
            .comparing(TourProcessingDraftTour::getPriority, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(TourProcessingDraftTour::getDepartureDate)
            .thenComparing(TourProcessingDraftTour::getDepartureTime)
            .thenComparing(TourProcessingDraftTour::getRowNumber);

    private final TourProcessingDraftRepository draftRepository;
    private final DraftTourStore tourStore;
    private final TourProcessingDraftDayRepository dayRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final DraftHoursService draftHoursService;
    private final DriverBusinessDaysService businessDaysService;

    @Transactional(readOnly = true)
    public FinalCommissionCheckDto check(UUID draftId) {
        TourProcessingDraft draft = getDraft(draftId);
        Snapshot snapshot = snapshot(draft);
        return FinalCommissionCheckDto.builder()
                .blockers(blockers(draft, snapshot))
                .errors(errors(draft, snapshot))
                .warnings(warnings(draft, snapshot))
                .rejections(rejections(snapshot))
                .build();
    }

    @Transactional
    public int rejectIncompleteTours(UUID draftId) {
        TourProcessingDraft draft = draftRepository.findByIdForUpdate(draftId)
                .orElseThrow(() -> TransportException.notFound(DRAFT_RESOURCE, draftId.toString()));
        if (draft.getStatus() != TourProcessingDraftStatus.DRAFT) {
            throw draft.getStatus() == TourProcessingDraftStatus.CLOSED
                    ? TransportException.conflict("TOUR_PROCESSING_DRAFT_CLOSED", "No se puede editar una depuración terminada. Devuélvela a borrador")
                    : TransportException.conflict("TOUR_PROCESSING_DRAFT_ALREADY_REGISTERED", "No se puede editar un borrador que ya fue registrado");
        }
        requireBusinessDays(draft);
        Snapshot snapshot = snapshot(draft);
        ensureNoPartialDistributions(snapshot);
        List<TourProcessingDraftTour> incomplete = snapshot.incomplete();
        if (incomplete.isEmpty()) {
            return 0;
        }
        List<TourProcessingDraftAssignment> assignments = snapshot.tours().stream().flatMap(tour -> tour.getAssignments().stream()).toList();
        var slotsBefore = DraftHoursService.workloadSlots(assignments);
        incomplete.forEach(tour -> tour.setApprovalStatus(TourApprovalStatus.NOT_APPROVED));
        tourStore.saveAll(incomplete);
        draftHoursService.syncChanged(draft, slotsBefore, assignments);
        return incomplete.size();
    }

    @Transactional(readOnly = true)
    public FinalCommissionReportDto report(UUID draftId) {
        TourProcessingDraft draft = getDraft(draftId);
        Snapshot snapshot = snapshot(draft);
        Set<TourProcessingDraftTour> withheld = new LinkedHashSet<>();
        withheld.addAll(snapshot.incomplete());
        withheld.addAll(snapshot.partial());
        List<UUID> attendedIds = snapshot.tours().stream()
                .filter(tour -> tour.isApproved() && !withheld.contains(tour))
                .map(TourProcessingDraftTour::primaryAssignment)
                .filter(Objects::nonNull)
                .map(TourProcessingDraftAssignment::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<UUID, List<TourProcessingDraftDay>> daysByAssignment = attendedIds.isEmpty() ? Map.of()
                : dayRepository.findByAssignmentIdIn(attendedIds).stream()
                .collect(Collectors.groupingBy(day -> day.getAssignment().getId()));
        DriverBusinessDays businessDays = businessDaysService.forDraft(draft);
        List<Driver> activeRoster = snapshot.driversById().values().stream()
                .filter(driver -> driver.getOrganizationalUnit() == DriverUnit.GENERAL_SERVICES)
                .filter(driver -> ACTIVE_STATUS.equalsIgnoreCase(driver.getStatus()))
                .toList();
        return FinalCommissionReportDto.builder()
                .year(draft.getYear())
                .month(draft.getMonth())
                .businessDays(draft.getBusinessDays())
                .surplusCapPerDriver(businessDays.defaultSurplusCap())
                .activeDrivers(activeRoster.size())
                .activeDriversBusinessDays(activeRoster.stream().mapToInt(businessDays::businessDaysOf).sum())
                .activeDriversSurplusCap(activeRoster.stream().mapToDouble(businessDays::surplusCapOf).sum())
                .tours(snapshot.tours().stream()
                        .sorted(REPORT_ORDER)
                        .map(tour -> toReportTour(tour, snapshot, daysByAssignment, !withheld.contains(tour)))
                        .toList())
                .build();
    }

    private FinalCommissionReportDto.Tour toReportTour(
            TourProcessingDraftTour tour,
            Snapshot snapshot,
            Map<UUID, List<TourProcessingDraftDay>> daysByAssignment,
            boolean countsAsApproved
    ) {
        TourProcessingDraftAssignment main = tour.primaryAssignment();
        boolean approved = countsAsApproved && tour.isApproved();
        OrdinaryWindow window = OrdinaryWindow.of(main == null ? null : DriverHoursRules.assignedDriver(main, snapshot.driversById()));
        return FinalCommissionReportDto.Tour.builder()
                .number(tour.getNumber())
                .passengers(tour.getPassengers())
                .executingUnit(tour.getExecutingUnit())
                .responsible(tour.getResponsible())
                .destination(tour.getDestination())
                .durationDays(tour.getDurationDays())
                .priority(tour.getPriority())
                .departureDate(tour.getDepartureDate())
                .departureTime(tour.getDepartureTime())
                .returnDate(tour.getReturnDate())
                .returnTime(tour.getReturnTime())
                .category(categoryOf(tour, approved))
                .vehicles(TourTrips.distinctDrivers(tour))
                .ordinaryStart(window.start())
                .ordinaryEnd(window.end())
                .days(reportDays(tour, main, daysByAssignment, approved))
                .build();
    }

    private static String categoryOf(TourProcessingDraftTour tour, boolean countsAsApproved) {
        TourProcessingDraftAssignment main = tour.primaryAssignment();
        if (!countsAsApproved || main == null) {
            return NOT_APPROVED_CATEGORY;
        }
        return main.getResourceType().name();
    }

    private List<FinalCommissionReportDto.Day> reportDays(
            TourProcessingDraftTour tour,
            TourProcessingDraftAssignment main,
            Map<UUID, List<TourProcessingDraftDay>> daysByAssignment,
            boolean countsAsApproved
    ) {
        List<TourProcessingDraftDay> stored = main == null || main.getId() == null || !countsAsApproved
                ? List.of()
                : daysByAssignment.getOrDefault(main.getId(), List.of());
        if (!stored.isEmpty()) {
            return stored.stream()
                    .sorted(Comparator.comparing(TourProcessingDraftDay::getDate))
                    .map(day -> {
                        double start = DriverHoursRules.hourOf(day.getStartTime());
                        double end = DriverHoursRules.hourOf(day.getEndTime());
                        return new FinalCommissionReportDto.Day(day.getDate(), start, end < start ? end + 24 : end);
                    })
                    .toList();
        }
        TourProcessingDraftAssignment source = main != null ? main : TourProcessingDraftAssignment.builder().tour(tour).build();
        return DriverHoursRules.daySpans(source).stream()
                .map(span -> new FinalCommissionReportDto.Day(span.date(), span.start(), span.end()))
                .toList();
    }

    private List<FinalCommissionCheckDto.Issue> blockers(TourProcessingDraft draft, Snapshot snapshot) {
        List<FinalCommissionCheckDto.Issue> blockers = new ArrayList<>();
        List<TourProcessingDraftTour> partialTours = snapshot.partial().stream().sorted(REPORT_ORDER).toList();
        List<String> partial = partialTours.stream()
                .map(tour -> tourLabel(tour) + ": viajes sin chofer o vehículo: " + missingParts(tour, snapshot))
                .toList();
        if (!partial.isEmpty()) {
            blockers.add(issue("PARTIAL_DISTRIBUTION",
                    "Giras divididas en varios viajes donde algunos viajes tienen chofer y vehículo y otros no. Cada viaje debe tenerlos, o ninguno de ellos",
                    partial, refs(partialTours, partial)));
        }
        if (draft.getStatus() != TourProcessingDraftStatus.DRAFT && !snapshot.incomplete().isEmpty()) {
            List<TourProcessingDraftTour> incomplete = orderedIncomplete(snapshot);
            List<String> details = incompleteDetails(incomplete, snapshot);
            blockers.add(issue("CLOSED_WITH_INCOMPLETE_TOURS",
                    draft.getStatus() == TourProcessingDraftStatus.CLOSED
                            ? "Giras sin chofer o sin vehículo en una depuración terminada. Devuélvela a borrador para resolverlas"
                            : "Giras sin chofer o sin vehículo en un borrador registrado, que ya no se puede editar",
                    details, refs(incomplete, details)));
        }
        return blockers;
    }

    private String missingParts(TourProcessingDraftTour tour, Snapshot snapshot) {
        List<TourProcessingDraftAssignment> parts = tour.getAssignments();
        return IntStream.range(0, parts.size())
                .filter(index -> !snapshot.isComplete(parts.get(index)))
                .mapToObj(index -> (index + 1) + " de " + parts.size())
                .collect(Collectors.joining(", "));
    }

    private List<FinalCommissionCheckDto.Issue> errors(TourProcessingDraft draft, Snapshot snapshot) {
        List<FinalCommissionCheckDto.Issue> errors = new ArrayList<>();
        List<TourProcessingDraftAssignment> approvedParts = snapshot.approved().stream()
                .flatMap(tour -> tour.getAssignments().stream())
                .toList();
        if (!snapshot.approved().isEmpty() && approvedParts.stream().noneMatch(snapshot::hasDriver)) {
            errors.add(issue("NO_DRIVERS", "No hay choferes asignados",
                    List.of("Ninguna gira aprobada tiene un chofer del catálogo asignado.")));
        }
        if (!snapshot.approved().isEmpty() && approvedParts.stream().noneMatch(snapshot::hasVehicle)) {
            errors.add(issue("NO_VEHICLES", "No hay vehículos asignados",
                    List.of("Ninguna gira aprobada tiene un vehículo del catálogo asignado.")));
        }
        int duplicateRows = draft.getDuplicateRows() == null ? 0 : draft.getDuplicateRows();
        int conflictingRows = draft.getConflictingRows() == null ? 0 : draft.getConflictingRows();
        if (duplicateRows > 0 || conflictingRows > 0) {
            List<String> details = new ArrayList<>();
            if (duplicateRows > 0) {
                details.add(duplicateRows + " filas repetidas. La Comisión Final solo incluye la primera aparición de cada número de gira.");
            }
            if (conflictingRows > 0) {
                details.add(conflictingRows + " filas tienen un número de gira repetido con datos distintos.");
            }
            errors.add(issue("DUPLICATES", "Hay giras duplicadas", details));
        }
        List<TourProcessingDraftTour> oversized = TourProcessingDraftService.oversizedToursOf(snapshot.tours(), snapshot.vehicles());
        if (!oversized.isEmpty()) {
            int fleetMaximum = TourProcessingDraftService.fleetMaximumCapacity(snapshot.vehicles());
            List<TourProcessingDraftTour> ordered = oversized.stream().sorted(REPORT_ORDER).toList();
            List<String> details = ordered.stream()
                    .map(tour -> tourLabel(tour) + ": " + tour.getPassengers() + " pasajeros para una capacidad de "
                            + TourProcessingDraftService.capacityOf(tour.primaryAssignment(), snapshot.vehiclesById(), fleetMaximum))
                    .toList();
            errors.add(issue("OVERSIZED", "Giras que necesitan más de un viaje y no están divididas", details, refs(ordered, details)));
        }
        return errors;
    }

    private FinalCommissionCheckDto.Issue rejections(Snapshot snapshot) {
        List<TourProcessingDraftTour> incomplete = orderedIncomplete(snapshot);
        List<String> details = incompleteDetails(incomplete, snapshot);
        return details.isEmpty() ? null : issue("NOT_APPROVED", "Giras sin chofer o sin vehículo", details, refs(incomplete, details));
    }

    private List<TourProcessingDraftTour> orderedIncomplete(Snapshot snapshot) {
        return snapshot.incomplete().stream().sorted(REPORT_ORDER).toList();
    }

    private List<String> incompleteDetails(List<TourProcessingDraftTour> incomplete, Snapshot snapshot) {
        return incomplete.stream()
                .map(tour -> tourLabel(tour) + ", prioridad " + tour.getPriority() + ", " + tour.getPassengers()
                        + " pasajeros: " + missingResources(tour, snapshot))
                .toList();
    }

    private String missingResources(TourProcessingDraftTour tour, Snapshot snapshot) {
        boolean driver = tour.getAssignments().stream().anyMatch(snapshot::hasDriver);
        boolean vehicle = tour.getAssignments().stream().anyMatch(snapshot::hasVehicle);
        if (!driver && !vehicle) {
            return "sin chofer ni vehículo";
        }
        return driver ? "sin vehículo" : "sin chofer";
    }

    private List<FinalCommissionCheckDto.Issue> warnings(TourProcessingDraft draft, Snapshot snapshot) {
        DriverBusinessDays businessDays = draft.getBusinessDays() != null && draft.getYear() != null && draft.getMonth() != null
                ? businessDaysService.forDraft(draft) : null;
        List<TourAssignmentRules.RuleViolation> violations = assignmentRules.evaluate(new TourAssignmentRules.Snapshot(
                snapshot.tours(),
                snapshot.driversById(),
                snapshot.vehiclesById(),
                businessDays,
                draft.getId() == null ? ManualDays.NONE : draftHoursService.manualDays(draft.getId()),
                draft.getYear(),
                draft.getMonth()
        ));
        Set<UUID> beyondFleet = toursBeyondFleet(snapshot);
        List<FinalCommissionCheckDto.Issue> warnings = new ArrayList<>();
        Map<TourAssignmentRules.RuleCode, String> titles = new LinkedHashMap<>();
        Map<TourAssignmentRules.RuleCode, List<String>> details = new LinkedHashMap<>();
        Map<TourAssignmentRules.RuleCode, List<FinalCommissionCheckDto.TourRef>> tours = new LinkedHashMap<>();
        for (TourAssignmentRules.RuleViolation violation : violations) {
            if (violation.code() == TourAssignmentRules.RuleCode.VEHICLE_OVER_CAPACITY
                    && violation.tourId() != null
                    && beyondFleet.contains(violation.tourId())) {
                continue;
            }
            titles.putIfAbsent(violation.code(), violation.title());
            List<String> lines = details.computeIfAbsent(violation.code(), code -> new ArrayList<>());
            if (!lines.contains(violation.message())) {
                lines.add(violation.message());
            }
            if (violation.tourId() != null) {
                addTour(tours.computeIfAbsent(violation.code(), code -> new ArrayList<>()), violation);
            }
        }
        details.forEach((code, lines) -> warnings.add(issue(code.name(), titles.get(code), lines, tours.getOrDefault(code, List.of()))));
        return warnings;
    }

    private Set<UUID> toursBeyondFleet(Snapshot snapshot) {
        int fleetMaximum = TourProcessingDraftService.fleetMaximumCapacity(snapshot.vehicles());
        return TourProcessingDraftService.oversizedToursOf(snapshot.tours(), snapshot.vehicles()).stream()
                .filter(tour -> tour.getPassengers() != null && tour.getPassengers() > fleetMaximum)
                .map(TourProcessingDraftTour::getId)
                .collect(Collectors.toSet());
    }

    private void ensureNoPartialDistributions(Snapshot snapshot) {
        if (!snapshot.partial().isEmpty()) {
            throw TransportException.unprocessable("FINAL_COMMISSION_PARTIAL_DISTRIBUTION",
                    "Hay " + snapshot.partial().size() + " giras divididas en varios viajes donde solo algunos viajes tienen chofer y vehículo");
        }
    }

    private Snapshot snapshot(TourProcessingDraft draft) {
        List<TourProcessingDraftTour> tours = tourStore.findPrimariesByDraftId(draft.getId()).stream()
                .filter(TourProcessingDraftTour::isPrimary)
                .toList();
        List<Vehicle> vehicles = vehicleRepository.findAll();
        return new Snapshot(tours, DriverHoursRules.driversById(driverRepository.findAll()), DriverHoursRules.vehiclesById(vehicles), vehicles);
    }

    private TourProcessingDraft getDraft(UUID draftId) {
        TourProcessingDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> TransportException.notFound(DRAFT_RESOURCE, draftId.toString()));
        requireBusinessDays(draft);
        return draft;
    }

    private void requireBusinessDays(TourProcessingDraft draft) {
        if (draft.getYear() == null || draft.getMonth() == null || draft.getBusinessDays() == null) {
            throw TransportException.unprocessable("TOUR_PROCESSING_DRAFT_WITHOUT_PERIOD", "El borrador no tiene mes ni días hábiles definidos");
        }
    }

    private static void add(List<FinalCommissionCheckDto.Issue> issues, String code, String title, List<String> details) {
        if (!details.isEmpty()) {
            issues.add(issue(code, title, details));
        }
    }

    private static FinalCommissionCheckDto.Issue issue(String code, String title, List<String> details) {
        return issue(code, title, details, List.of());
    }

    private static FinalCommissionCheckDto.Issue issue(
            String code,
            String title,
            List<String> details,
            List<FinalCommissionCheckDto.TourRef> tours
    ) {
        return FinalCommissionCheckDto.Issue.builder().code(code).title(title).details(details).tours(tours).build();
    }

    private static List<FinalCommissionCheckDto.TourRef> refs(List<TourProcessingDraftTour> tours, List<String> details) {
        List<FinalCommissionCheckDto.TourRef> refs = new ArrayList<>();
        for (int index = 0; index < tours.size(); index++) {
            TourProcessingDraftTour tour = tours.get(index);
            if (tour.getId() == null) {
                continue;
            }
            refs.add(FinalCommissionCheckDto.TourRef.builder()
                    .id(tour.getId())
                    .number(tour.getNumber())
                    .label(index < details.size() ? details.get(index) : tourLabel(tour))
                    .build());
        }
        return refs;
    }

    private static void addTour(List<FinalCommissionCheckDto.TourRef> tours, TourAssignmentRules.RuleViolation violation) {
        boolean present = tours.stream().anyMatch(tour -> violation.tourId().equals(tour.getId()) && violation.message().equals(tour.getLabel()));
        if (!present) {
            tours.add(FinalCommissionCheckDto.TourRef.builder()
                    .id(violation.tourId())
                    .number(violation.tourNumber())
                    .label(violation.message())
                    .build());
        }
    }

    private static String tourLabel(TourProcessingDraftTour tour) {
        return "Gira " + tour.getNumber() + " (" + tour.getDepartureDate().format(DATE) + ")";
    }

    private record Snapshot(
            List<TourProcessingDraftTour> tours,
            Map<UUID, Driver> driversById,
            Map<UUID, Vehicle> vehiclesById,
            List<Vehicle> vehicles
    ) {

        List<TourProcessingDraftTour> approved() {
            return tours.stream().filter(TourProcessingDraftTour::isApproved).toList();
        }

        List<TourProcessingDraftTour> attended() {
            return approved().stream()
                    .filter(tour -> !tour.getAssignments().isEmpty() && tour.getAssignments().stream().allMatch(this::isComplete))
                    .toList();
        }

        List<TourProcessingDraftTour> incomplete() {
            return approved().stream().filter(tour -> tour.getAssignments().stream().noneMatch(this::isComplete)).toList();
        }

        List<TourProcessingDraftTour> partial() {
            return approved().stream()
                    .filter(tour -> tour.getAssignments().stream().anyMatch(this::isComplete)
                            && tour.getAssignments().stream().anyMatch(part -> !isComplete(part)))
                    .toList();
        }

        boolean isComplete(TourProcessingDraftAssignment part) {
            return hasDriver(part) && hasVehicle(part);
        }

        boolean hasDriver(TourProcessingDraftAssignment part) {
            if (part.getResourceType() == ResourceType.EXTERNAL) {
                return hasText(part.getDriver());
            }
            return DriverHoursRules.assignedDriver(part, driversById) != null;
        }

        boolean hasVehicle(TourProcessingDraftAssignment part) {
            return switch (part.getResourceType()) {
                case EXTERNAL -> hasText(part.getVehicle());
                case BORROWED_VEHICLE -> part.getBorrowedVehicle() != null;
                case INTERNAL -> DriverHoursRules.assignedVehicle(part, vehiclesById) != null;
            };
        }

        private static boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
