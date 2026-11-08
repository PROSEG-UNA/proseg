package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.tourprocessing.FinalCommissionCheckDto;
import com.proseg.msvc_transport.dto.tourprocessing.FinalCommissionReportDto;
import com.proseg.msvc_transport.entity.BorrowedVehicle;
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
import com.proseg.msvc_transport.service.driverhours.DriverMonthTotals;
import com.proseg.msvc_transport.service.driverhours.DriverWorkload;
import com.proseg.msvc_transport.service.driverhours.OrdinaryWindow;
import com.proseg.msvc_transport.service.driverhours.TourDaySpan;
import com.proseg.msvc_transport.service.driverhours.TourTrips;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class FinalCommissionService {

    public static final String NOT_APPROVED_CATEGORY = "NOT_APPROVED";

    private static final String DRAFT_RESOURCE = "Borrador de depuración";
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final int MIN_EXTERNAL_PASSENGERS = 5;
    private static final int PRIORITY_ONE = 1;
    private static final double TOLERANCE = 0.005;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
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
        ensureNoPartialDistributions(snapshot);
        if (!snapshot.incomplete().isEmpty()) {
            throw TransportException.unprocessable("FINAL_COMMISSION_INCOMPLETE_TOURS",
                    "Hay " + snapshot.incomplete().size() + " giras sin chofer o vehículo. Confirma que pasen a No aprobada antes de generar la Comisión Final");
        }
        List<UUID> attendedIds = snapshot.tours().stream()
                .filter(TourProcessingDraftTour::isApproved)
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
                        .map(tour -> toReportTour(tour, snapshot, daysByAssignment))
                        .toList())
                .build();
    }

    private FinalCommissionReportDto.Tour toReportTour(
            TourProcessingDraftTour tour,
            Snapshot snapshot,
            Map<UUID, List<TourProcessingDraftDay>> daysByAssignment
    ) {
        TourProcessingDraftAssignment main = tour.primaryAssignment();
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
                .category(!tour.isApproved() || main == null ? NOT_APPROVED_CATEGORY : main.getResourceType().name())
                .vehicles(TourTrips.distinctDrivers(tour))
                .ordinaryStart(window.start())
                .ordinaryEnd(window.end())
                .days(reportDays(tour, main, daysByAssignment))
                .build();
    }

    private List<FinalCommissionReportDto.Day> reportDays(
            TourProcessingDraftTour tour,
            TourProcessingDraftAssignment main,
            Map<UUID, List<TourProcessingDraftDay>> daysByAssignment
    ) {
        List<TourProcessingDraftDay> stored = main == null || main.getId() == null || !tour.isApproved()
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
        List<String> partial = snapshot.partial().stream()
                .map(tour -> tourLabel(tour) + ": viajes sin chofer o vehículo: " + missingParts(tour, snapshot))
                .toList();
        if (!partial.isEmpty()) {
            blockers.add(issue("PARTIAL_DISTRIBUTION",
                    "Giras divididas en varios viajes donde algunos viajes tienen chofer y vehículo y otros no. Cada viaje debe tenerlos, o ninguno de ellos",
                    partial));
        }
        if (draft.getStatus() != TourProcessingDraftStatus.DRAFT && !snapshot.incomplete().isEmpty()) {
            blockers.add(issue("CLOSED_WITH_INCOMPLETE_TOURS",
                    draft.getStatus() == TourProcessingDraftStatus.CLOSED
                            ? "Giras sin chofer o sin vehículo en una depuración terminada. Devuélvela a borrador para resolverlas"
                            : "Giras sin chofer o sin vehículo en un borrador registrado, que ya no se puede editar",
                    incompleteDetails(snapshot)));
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
            errors.add(issue("OVERSIZED", "Giras que necesitan más de un viaje y no están divididas", oversized.stream()
                    .sorted(REPORT_ORDER)
                    .map(tour -> tourLabel(tour) + ": " + tour.getPassengers() + " pasajeros para una capacidad de "
                            + TourProcessingDraftService.capacityOf(tour.primaryAssignment(), snapshot.vehiclesById(), fleetMaximum))
                    .toList()));
        }
        return errors;
    }

    private FinalCommissionCheckDto.Issue rejections(Snapshot snapshot) {
        List<String> details = incompleteDetails(snapshot);
        return details.isEmpty() ? null : issue("NOT_APPROVED", "Giras sin chofer o sin vehículo que cambiarán a estado No aprobada", details);
    }

    private List<String> incompleteDetails(Snapshot snapshot) {
        return snapshot.incomplete().stream()
                .sorted(REPORT_ORDER)
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
        List<FinalCommissionCheckDto.Issue> warnings = new ArrayList<>();
        List<TourProcessingDraftAssignment> attendedParts = snapshot.attended().stream()
                .flatMap(tour -> tour.getAssignments().stream())
                .toList();
        if (draft.getBusinessDays() != null && draft.getYear() != null && draft.getMonth() != null) {
            addDriverWarnings(warnings, draft, snapshot, attendedParts);
        }
        addVehicleWarnings(warnings, snapshot, attendedParts);

        List<String> externalUnderMinimum = snapshot.attended().stream()
                .filter(tour -> tour.getAssignments().stream().anyMatch(part -> part.getResourceType() == ResourceType.EXTERNAL))
                .filter(tour -> tour.getPassengers() != null && tour.getPassengers() < MIN_EXTERNAL_PASSENGERS)
                .sorted(REPORT_ORDER)
                .map(tour -> tourLabel(tour) + ": " + tour.getPassengers() + " pasajeros")
                .toList();
        add(warnings, "EXTERNAL_UNDER_MINIMUM", "Contratación externa con menos de 5 pasajeros", externalUnderMinimum);

        Set<TourProcessingDraftTour> incomplete = Set.copyOf(snapshot.incomplete());
        List<String> priorityOneUnattended = snapshot.tours().stream()
                .filter(tour -> Objects.equals(tour.getPriority(), PRIORITY_ONE))
                .filter(tour -> tour.getPassengers() != null && tour.getPassengers() < MIN_EXTERNAL_PASSENGERS)
                .filter(tour -> !tour.isApproved() || incomplete.contains(tour))
                .sorted(REPORT_ORDER)
                .map(tour -> tourLabel(tour) + ": " + tour.getPassengers() + " pasajeros")
                .toList();
        add(warnings, "PRIORITY_ONE_UNATTENDED", "Giras de prioridad 1 con menos de 5 pasajeros que quedan como No aprobada", priorityOneUnattended);
        return warnings;
    }

    private void addDriverWarnings(
            List<FinalCommissionCheckDto.Issue> warnings,
            TourProcessingDraft draft,
            Snapshot snapshot,
            List<TourProcessingDraftAssignment> attendedParts
    ) {
        DriverBusinessDays businessDays = businessDaysService.forDraft(draft);
        Set<LocalDate> monthWeekends = DriverHoursRules.monthWeekends(draft.getYear(), draft.getMonth());
        Map<UUID, DriverWorkload> workloads = DriverWorkload.fromAssignments(
                attendedParts, snapshot.driversById(), draftHoursService.manualDays(draft.getId()));

        List<String> overlaps = new ArrayList<>();
        List<String> stackedDays = new ArrayList<>();
        List<String> surplusExceeded = new ArrayList<>();
        List<String> overtimeExceeded = new ArrayList<>();
        List<String> overtimeWithoutSurplus = new ArrayList<>();
        List<String> withoutFreeWeekend = new ArrayList<>();
        List<String> inactiveDrivers = new ArrayList<>();

        workloads.entrySet().stream()
                .map(entry -> Map.entry(snapshot.driversById().get(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(entry -> entry.getKey().getName(), String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> {
                    Driver driver = entry.getKey();
                    DriverWorkload workload = entry.getValue();
                    double surplusCap = businessDays.surplusCapOf(driver);
                    DriverMonthTotals totals = workload.totals(surplusCap, monthWeekends);
                    String name = driver.getName();
                    if (!ACTIVE_STATUS.equalsIgnoreCase(driver.getStatus())) {
                        inactiveDrivers.add(name + " no está activo y tiene " + workload.tours().size() + " giras asignadas");
                    }
                    if (totals.surplusHours() > surplusCap + TOLERANCE) {
                        surplusExceeded.add(name + ": " + hours(totals.surplusHours()) + " h de sobresueldo, tope de " + hours(surplusCap) + " h");
                    }
                    if (totals.overtimeHours() > DriverHoursRules.MONTHLY_OVERTIME_CAP + TOLERANCE) {
                        overtimeExceeded.add(name + ": " + hours(totals.overtimeHours()) + " h extra, tope de "
                                + hours(DriverHoursRules.MONTHLY_OVERTIME_CAP) + " h");
                    }
                    if (totals.overtimeHours() > TOLERANCE && totals.surplusHours() < surplusCap * DriverHoursRules.SURPLUS_CUTOFF_RATIO - TOLERANCE) {
                        overtimeWithoutSurplus.add(name + ": " + hours(totals.overtimeHours()) + " h extra con "
                                + percent(surplusCap == 0 ? 0 : totals.surplusHours() / surplusCap) + " de sobresueldo");
                    }
                    if (!monthWeekends.isEmpty() && totals.weekendsWorked() >= monthWeekends.size()) {
                        withoutFreeWeekend.add(name + ": trabaja los " + monthWeekends.size() + " fines de semana del mes");
                    }
                    overlaps.addAll(overlapsOf(name, workload.tours()));
                    stackedDays.addAll(stackedDaysOf(name, workload));
                });

        add(warnings, "DRIVER_OVERLAP", "Choferes con giras que se cruzan", overlaps);
        add(warnings, "DAY_HOURS_EXCEEDED", "Choferes con más de 12 horas en un día al sumar giras", stackedDays);
        add(warnings, "SURPLUS_CAP_EXCEEDED", "Choferes que superan el tope de sobresueldo del mes", surplusExceeded);
        add(warnings, "OVERTIME_CAP_EXCEEDED", "Choferes que superan las 60 horas extra del mes", overtimeExceeded);
        add(warnings, "OVERTIME_WITHOUT_SURPLUS", "Choferes con horas extra sin llegar al 95% de sobresueldo", overtimeWithoutSurplus);
        add(warnings, "NO_FREE_WEEKEND", "Choferes sin un fin de semana libre en el mes", withoutFreeWeekend);
        add(warnings, "INACTIVE_DRIVER", "Choferes inactivos con giras asignadas", inactiveDrivers);
    }

    private List<String> overlapsOf(String name, List<TourProcessingDraftAssignment> tours) {
        List<TourProcessingDraftAssignment> ordered = tours.stream()
                .sorted(Comparator.comparing(TourProcessingDraftAssignment::getDepartureDate).thenComparing(TourProcessingDraftAssignment::getDepartureTime))
                .toList();
        List<String> details = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            for (int j = i + 1; j < ordered.size(); j++) {
                TourProcessingDraftAssignment left = ordered.get(i);
                TourProcessingDraftAssignment right = ordered.get(j);
                if (left.getTour() != right.getTour() && DriverHoursRules.toursOverlap(left, right)) {
                    details.add(name + ": giras " + left.getNumber() + " y " + right.getNumber() + " (" + left.getDepartureDate().format(DATE) + ")");
                }
            }
        }
        return details;
    }

    private List<String> stackedDaysOf(String name, DriverWorkload workload) {
        Map<LocalDate, List<String>> numbersByDate = new TreeMap<>();
        for (TourProcessingDraftAssignment tour : workload.tours()) {
            for (TourDaySpan span : DriverHoursRules.daySpans(tour)) {
                numbersByDate.computeIfAbsent(span.date(), date -> new ArrayList<>()).add(tour.getNumber());
            }
        }
        Map<LocalDate, Double> hoursByDate = workload.hoursByDate();
        List<String> details = new ArrayList<>();
        numbersByDate.forEach((date, numbers) -> {
            double dayHours = hoursByDate.getOrDefault(date, 0.0);
            if (numbers.size() > 1 && dayHours > DriverHoursRules.MAX_STACKED_DAY_HOURS + TOLERANCE) {
                details.add(name + ": " + hours(dayHours) + " h el " + date.format(DATE) + " (giras " + String.join(", ", numbers) + ")");
            }
        });
        return details;
    }

    private void addVehicleWarnings(List<FinalCommissionCheckDto.Issue> warnings, Snapshot snapshot, List<TourProcessingDraftAssignment> attendedParts) {
        Map<String, List<TourProcessingDraftAssignment>> partsByPlate = new LinkedHashMap<>();
        List<String> overCapacity = new ArrayList<>();
        List<String> incompatible = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();
        for (TourProcessingDraftAssignment part : attendedParts) {
            String plate = plateOf(part, snapshot);
            if (plate != null) {
                partsByPlate.computeIfAbsent(plate, key -> new ArrayList<>()).add(part);
            }
            Integer capacity = capacityOf(part, snapshot);
            if (part.getTour().isDistributed() && capacity != null && part.getPassengers() != null && part.getPassengers() > capacity) {
                overCapacity.add(tourLabel(part.getTour()) + ": vehículo " + plate + " lleva " + part.getPassengers()
                        + " pasajeros y su capacidad es " + capacity);
            }
            Vehicle vehicle = part.getResourceType() == ResourceType.INTERNAL ? DriverHoursRules.assignedVehicle(part, snapshot.vehiclesById()) : null;
            Driver driver = part.getResourceType() == ResourceType.EXTERNAL ? null : DriverHoursRules.assignedDriver(part, snapshot.driversById());
            if (vehicle != null && driver != null && !DriverHoursRules.canDrive(driver, vehicle)) {
                incompatible.add(tourLabel(part.getTour()) + ": " + driver.getName() + " no está habilitado para el vehículo " + vehicle.getPlate());
            }
            if (vehicle != null && !vehicle.isAvailable()) {
                unavailable.add(tourLabel(part.getTour()) + ": vehículo " + vehicle.getPlate() + " no está disponible");
            }
        }
        List<String> sharedVehicles = new ArrayList<>();
        partsByPlate.forEach((plate, parts) -> {
            for (int i = 0; i < parts.size(); i++) {
                for (int j = i + 1; j < parts.size(); j++) {
                    TourProcessingDraftAssignment left = parts.get(i);
                    TourProcessingDraftAssignment right = parts.get(j);
                    if (!DriverWorkload.sameTour(left, right) && DriverHoursRules.toursOverlap(left, right)) {
                        String detail = "Placa " + plate + ": giras " + left.getNumber() + " y " + right.getNumber()
                                + " (" + left.getDepartureDate().format(DATE) + ")";
                        if (!sharedVehicles.contains(detail)) {
                            sharedVehicles.add(detail);
                        }
                    }
                }
            }
        });
        add(warnings, "VEHICLE_OVERLAP", "Vehículos asignados a giras que se cruzan", sharedVehicles);
        add(warnings, "VEHICLE_OVER_CAPACITY", "Viajes con más pasajeros que la capacidad del vehículo", overCapacity);
        add(warnings, "INCOMPATIBLE_VEHICLE", "Choferes asignados a un tipo de vehículo que no pueden manejar", incompatible);
        add(warnings, "VEHICLE_UNAVAILABLE", "Vehículos no disponibles asignados a giras", unavailable);
    }

    private String plateOf(TourProcessingDraftAssignment part, Snapshot snapshot) {
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

    private Integer capacityOf(TourProcessingDraftAssignment part, Snapshot snapshot) {
        if (part.getResourceType() == ResourceType.BORROWED_VEHICLE) {
            return part.getBorrowedVehicle() == null ? null : part.getBorrowedVehicle().getCapacity();
        }
        Vehicle vehicle = part.getResourceType() == ResourceType.INTERNAL ? DriverHoursRules.assignedVehicle(part, snapshot.vehiclesById()) : null;
        return vehicle == null ? null : vehicle.getCapacity();
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
        return FinalCommissionCheckDto.Issue.builder().code(code).title(title).details(details).build();
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
