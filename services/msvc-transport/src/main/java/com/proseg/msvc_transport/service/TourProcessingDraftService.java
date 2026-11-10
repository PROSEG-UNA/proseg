package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftAssignmentDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftCreateRequestDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftImportResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowDayRequestDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowDayResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowIssueDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowRequestDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDriverAutoAssignResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingImportColumnDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingImportSchemaDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingVehicleAutoAssignResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourSplitMode;
import com.proseg.msvc_transport.dto.response.OvertimeRequestReportDto;
import com.proseg.msvc_transport.entity.BorrowedVehicle;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.ObservationKeyword;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourApprovalStatus;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.importer.TransportImportField;
import com.proseg.msvc_transport.repository.BorrowedVehicleRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftAssignmentRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftTourRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.ObservationKeywordRepository;
import com.proseg.msvc_transport.repository.VehicleRepository;
import com.proseg.msvc_transport.security.CurrentUserResolver;
import com.proseg.msvc_transport.service.driverhours.DriverAutoAssigner;
import com.proseg.msvc_transport.service.driverhours.MandatoryCoverageSummary;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import com.proseg.msvc_transport.service.driverhours.DriverRejection;
import com.proseg.msvc_transport.service.driverhours.DriverWorkload;
import com.proseg.msvc_transport.service.driverhours.ManualDays;
import com.proseg.msvc_transport.service.driverhours.TourTrips;
import com.proseg.msvc_transport.service.driverhours.UlloaTours;
import com.proseg.msvc_transport.service.driverhours.VehicleAutoAssigner;
import com.proseg.msvc_transport.service.driverhours.VehicleWorkload;
import com.proseg.msvc_transport.specification.GenericSpecifications;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class TourProcessingDraftService {

    private static final int MAX_ROWS = 5000;
    private static final String DRAFT_RESOURCE = "Borrador de depuración";
    private static final String ROW_RESOURCE = "Fila del borrador";
    private static final String DAY_RESOURCE = "Día de la gira";
    private static final Set<DriverRejection.Reason> REAPPROVAL_BLOCKING_REASONS =
            EnumSet.of(DriverRejection.Reason.OVERLAP, DriverRejection.Reason.DAY_HOURS_EXCEEDED);
    private static final Locale SPANISH = Locale.forLanguageTag("es");
    private static final List<String> FIELD_ORDER = Arrays.stream(TransportImportField.values())
            .map(TransportImportField::getAttribute)
            .toList();

    private final TourProcessingDraftRepository draftRepository;
    private final DraftTourStore tourRepository;
    private final TourProcessingDraftAssignmentRepository assignmentRepository;
    private final TourProcessingDraftDayRepository dayRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final BorrowedVehicleRepository borrowedVehicleRepository;
    private final DraftHoursService draftHoursService;
    private final DriverBusinessDaysService businessDaysService;
    private final OvertimeRequestReportService overtimeRequestReportService;
    private final CurrentUserResolver currentUserResolver;
    private final Validator validator;
    private final JdbcTemplate jdbc;
    private final ObservationKeywordRepository observationKeywordRepository;

    public TourProcessingImportSchemaDto getImportSchema() {
        List<TourProcessingImportColumnDto> columns = Arrays.stream(TransportImportField.values())
                .map(field -> TourProcessingImportColumnDto.builder()
                        .attribute(field.getAttribute())
                        .names(field.getNames())
                        .type(field.getType().name())
                        .required(field.isRequired())
                        .build())
                .toList();
        return TourProcessingImportSchemaDto.builder().columns(columns).build();
    }

    @Transactional
    public TourProcessingDraftImportResponseDto create(TourProcessingDraftCreateRequestDto request) {
        jdbc.queryForObject("select pg_advisory_xact_lock(?, ?)", String.class, request.getYear(), request.getMonth());
        ensureMonthIsAvailable(request.getYear(), request.getMonth());
        List<TourProcessingDraftRowRequestDto> rows = request.getRows() == null ? List.of() : request.getRows();
        if (rows.isEmpty()) {
            throw TransportException.badRequest("TOUR_PROCESSING_DRAFT_EMPTY", "No hay filas para importar");
        }
        if (rows.size() > MAX_ROWS) {
            throw TransportException.badRequest(
                    "TOUR_PROCESSING_TOO_MANY_ROWS",
                    "La importación supera el máximo de " + MAX_ROWS + " filas"
            );
        }

        List<TourProcessingDraftRowIssueDto> errors = validateRows(rows);
        if (!errors.isEmpty()) {
            return TourProcessingDraftImportResponseDto.builder()
                    .received(rows.size())
                    .errors(errors)
                    .build();
        }

        TourProcessingDraft draft = TourProcessingDraft.builder()
                .fileName(request.getFileName().trim())
                .fileType(request.getFileType().trim().toUpperCase(Locale.ROOT))
                .status(TourProcessingDraftStatus.DRAFT)
                .importedBy(currentUserResolver.resolve())
                .importedAt(LocalDateTime.now())
                .totalRows(rows.size())
                .blocksDetected(request.getBlocksDetected() == null ? 0 : request.getBlocksDetected())
                .year(request.getYear())
                .month(request.getMonth())
                .businessDays(request.getBusinessDays())
                .build();

        DraftCatalogLinker linker = catalogLinker();
        List<TourProcessingDraftTour> tours = rows.stream()
                .map(row -> toTour(row, draft, linker))
                .toList();
        markDuplicates(tours);
        summarize(draft, tours);

        TourProcessingDraft saved = draftRepository.save(draft);
        tourRepository.saveAll(tours);
        draftHoursService.syncAll(saved, assignmentsOf(tours));

        return TourProcessingDraftImportResponseDto.builder()
                .draftId(saved.getId())
                .received(rows.size())
                .duplicateRows(saved.getDuplicateRows())
                .conflictingRows(saved.getConflictingRows())
                .errors(List.of())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<TourProcessingDraftResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<TourProcessingDraft> spec = Specification
                .where(GenericSpecifications.withSearch(TourProcessingDraft.class, search))
                .and(GenericSpecifications.withColumnFilters(TourProcessingDraft.class, filters));
        Sort sort = GenericSpecifications.sanitizeSort(TourProcessingDraft.class, pageable.getSort());
        Sort effectiveSort = sort.isUnsorted() ? Sort.by(Sort.Order.desc("importedAt")) : sort;
        return draftRepository.findAll(spec, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), effectiveSort))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TourProcessingDraftResponseDto findById(UUID id) {
        TourProcessingDraft draft = getDraft(id);
        TourProcessingDraftResponseDto response = toResponse(draft);
        if (draft.getStatus() == TourProcessingDraftStatus.DRAFT) {
            List<TourProcessingDraftTour> tours = tourRepository.findPrimariesByDraftId(id);
            response.setOversizedRows(oversizedToursOf(tours, vehicleRepository.findAll()).size());
        }
        return response;
    }

    @Transactional
    public TourProcessingDraftResponseDto finish(UUID id) {
        TourProcessingDraft draft = getEditableDraft(id);
        draft.setStatus(TourProcessingDraftStatus.CLOSED);
        return toResponse(draft);
    }

    @Transactional
    public TourProcessingDraftResponseDto reopen(UUID id) {
        TourProcessingDraft draft = draftRepository.findByIdForUpdate(id)
                .orElseThrow(() -> TransportException.notFound(DRAFT_RESOURCE, id.toString()));
        if (draft.getStatus() != TourProcessingDraftStatus.CLOSED) {
            throw TransportException.conflict("TOUR_PROCESSING_DRAFT_NOT_CLOSED", "Solo se puede reabrir una depuración terminada");
        }
        draft.setStatus(TourProcessingDraftStatus.DRAFT);
        return toResponse(draft);
    }

    @Transactional(readOnly = true)
    public Page<TourProcessingDraftRowResponseDto> findRows(UUID draftId, String search, Map<String, String> filters, Pageable pageable) {
        getDraft(draftId);
        Page<TourProcessingDraftTour> page = tourRepository.findPage(draftId, search, filters, pageable);
        return new PageImpl<>(toRowResponses(draftId, page.getContent()), page.getPageable(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public TourProcessingDraftRowResponseDto findRow(UUID draftId, UUID rowId) {
        getDraft(draftId);
        TourProcessingDraftTour tour = tourRepository.findByIdAndDraftId(rowId, draftId)
                .or(() -> assignmentRepository.findByIdAndDraftId(rowId, draftId).map(TourProcessingDraftAssignment::getTour))
                .orElseThrow(() -> TransportException.notFound(ROW_RESOURCE, rowId.toString()));
        return toRowResponses(draftId, List.of(tour)).get(0);
    }

    @Transactional(readOnly = true)
    public OvertimeRequestReportDto getSchedule(UUID draftId) {
        return overtimeRequestReportService.buildSchedule(getDraft(draftId));
    }

    @Transactional(readOnly = true)
    public OvertimeRequestReportDto getSchedule(UUID draftId, LocalDate date) {
        return date == null ? getSchedule(draftId) : overtimeRequestReportService.buildDailySchedule(getDraft(draftId), date);
    }

    @Transactional(readOnly = true)
    public OvertimeRequestReportDto getDriverScheduleSummaries(UUID draftId) {
        return overtimeRequestReportService.listDriverSummaries(getDraft(draftId));
    }

    @Transactional(readOnly = true)
    public Page<OvertimeRequestReportDto.TourBlock> getDriverSchedule(UUID draftId, UUID driverId, Pageable pageable) {
        return overtimeRequestReportService.buildDriverPage(getDraft(draftId), driverId, pageable);
    }

    @Transactional
    public TourProcessingDraftRowResponseDto createRow(UUID draftId, TourProcessingDraftRowRequestDto request) {
        TourProcessingDraft draft = getEditableDraft(draftId);
        List<TourProcessingDraftTour> tours = new ArrayList<>(tourRepository.findAllByDraftIdOrderByRowNumberAsc(draftId));
        Map<UUID, Set<DraftHoursService.WorkloadSlot>> slotsBefore = DraftHoursService.workloadSlots(assignmentsOf(tours));
        if (tours.size() >= MAX_ROWS) {
            throw TransportException.badRequest(
                    "TOUR_PROCESSING_TOO_MANY_ROWS",
                    "El borrador supera el máximo de " + MAX_ROWS + " filas"
            );
        }

        request.setRowNumber(tours.stream().mapToInt(TourProcessingDraftTour::getRowNumber).max().orElse(0) + 1);
        List<String> reasons = validateRow(request);
        if (!reasons.isEmpty()) {
            throw TransportException.unprocessable("TOUR_PROCESSING_DRAFT_ROW_INVALID", String.join(". ", reasons));
        }
        ensureAssignmentLimit(tours, requestedParts(request));

        DraftCatalogLinker linker = catalogLinker();
        validateBorrowedVehicleDrivers(request, linker);
        TourProcessingDraftTour tour = toTour(request, draft, linker);
        tourRepository.save(tour);
        tours.add(tour);
        markDuplicates(tours);
        validateBorrowedVehicles(assignmentsOf(tours));
        summarize(draft, tours);
        draft.setTotalRows(tours.size());
        tourRepository.saveAll(tours);
        draftHoursService.syncChanged(draft, slotsBefore, assignmentsOf(tours));
        return toRowResponses(draftId, List.of(tour)).get(0);
    }

    @Transactional
    public TourProcessingDraftRowResponseDto updateRow(UUID draftId, UUID rowId, TourProcessingDraftRowRequestDto request) {
        TourProcessingDraft draft = getEditableDraft(draftId);
        TourProcessingDraftTour tour = getEditableTour(draftId, rowId, "Edita la distribución desde la gira principal");
        if (tour.isDistributed() && request.getAssignments() == null) {
            throw TransportException.unprocessable("TOUR_PROCESSING_DISTRIBUTION_REQUIRED", "Incluye la distribución de pasajeros al editar esta gira");
        }

        List<String> reasons = validateRow(request);
        if (!reasons.isEmpty()) {
            throw TransportException.unprocessable("TOUR_PROCESSING_DRAFT_ROW_INVALID", String.join(". ", reasons));
        }

        List<TourProcessingDraftTour> tours = tourRepository.findAllByDraftIdOrderByRowNumberAsc(draftId).stream()
                .map(current -> current.getId().equals(tour.getId()) ? tour : current).toList();
        List<TourProcessingDraftAssignment> assignments = assignmentsOf(tours);
        Map<UUID, Set<DraftHoursService.WorkloadSlot>> slotsBefore = DraftHoursService.workloadSlots(assignments);
        DraftCatalogLinker linker = catalogLinker();
        validateBorrowedVehicleDrivers(request, linker);
        validateAssignedDriverCanAttend(draft, tour, request, assignments, linker);
        ensureAssignmentLimit(tours, requestedParts(request) - tour.getAssignments().size());

        boolean reapproved = request.getApprovalStatus() == TourApprovalStatus.APPROVED && !tour.isApproved();
        applyTourValues(tour, request);
        applyAssignments(tour, request, linker);
        tourRepository.flush();
        markDuplicates(tours);
        if (reapproved) {
            validateReapprovedToursHaveNoConflicts(draft, tour.getAssignments(), assignmentsOf(tours));
        }
        validateBorrowedVehicles(assignmentsOf(tours));
        summarize(draft, tours);
        tourRepository.saveAll(tours);
        draftHoursService.syncChanged(draft, slotsBefore, assignmentsOf(tours));
        return toRowResponses(draftId, List.of(tour)).get(0);
    }

    @Transactional
    public TourProcessingDraftRowResponseDto updateRowApprovalStatus(UUID draftId, UUID rowId, TourApprovalStatus approvalStatus) {
        TourProcessingDraft draft = getEditableDraft(draftId);
        TourProcessingDraftTour tour = getEditableTour(draftId, rowId, "Edita la distribución desde la gira principal");

        List<TourProcessingDraftTour> tours = tourRepository.findAllByDraftIdOrderByRowNumberAsc(draftId).stream()
                .map(current -> current.getId().equals(tour.getId()) ? tour : current).toList();
        List<TourProcessingDraftAssignment> assignments = assignmentsOf(tours);
        Map<UUID, Set<DraftHoursService.WorkloadSlot>> slotsBefore = DraftHoursService.workloadSlots(assignments);
        boolean reapproved = approvalStatus == TourApprovalStatus.APPROVED && !tour.isApproved();
        if (reapproved) {
            validateReapprovedToursHaveNoConflicts(draft, tour.getAssignments(), assignments);
        }
        tour.setApprovalStatus(approvalStatus);
        tourRepository.save(tour);
        draftHoursService.syncChanged(draft, slotsBefore, assignments);
        return toRowResponses(draftId, List.of(tour)).get(0);
    }

    @Transactional
    public TourProcessingDraftRowResponseDto updateObservationReviewed(UUID draftId, UUID rowId, boolean reviewed) {
        getEditableDraft(draftId);
        TourProcessingDraftTour tour = getEditableTour(draftId, rowId, "Marca la observación desde la gira principal");
        tour.setObservationReviewed(reviewed);
        tourRepository.save(tour);
        return toRowResponses(draftId, List.of(tour)).get(0);
    }

    @Transactional
    public TourProcessingDraftRowDayResponseDto updateRowDay(UUID draftId, UUID rowId, UUID dayId, TourProcessingDraftRowDayRequestDto request) {
        TourProcessingDraft draft = getEditableDraft(draftId);
        TourProcessingDraftDay day = dayRepository.findByIdAndDraftId(dayId, draftId)
                .orElseThrow(() -> TransportException.notFound(DAY_RESOURCE, dayId.toString()));
        TourProcessingDraftAssignment assignment = day.getAssignment();
        if (!rowId.equals(assignment.getId()) && !rowId.equals(assignment.getTour().getId())) {
            throw TransportException.notFound(ROW_RESOURCE, rowId.toString());
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw TransportException.unprocessable(
                    "TOUR_PROCESSING_DRAFT_ROW_DAY_INVALID",
                    "La hora de fin debe ser posterior a la hora de inicio"
            );
        }
        if (request.getOrdinaryHours() > DriverHoursRules.MAX_ORDINARY_HOURS_PER_DAY + DriverHoursRules.HOURS_TOLERANCE) {
            throw TransportException.unprocessable(
                    "TOUR_PROCESSING_DRAFT_ROW_DAY_ORDINARY_LIMIT",
                    "Las horas ordinarias no pueden superar las " + (int) DriverHoursRules.MAX_ORDINARY_HOURS_PER_DAY + " horas por día"
            );
        }

        validateDriverMonthlyCaps(draft, assignment, dayId, request);

        day.setStartTime(request.getStartTime());
        day.setEndTime(request.getEndTime());
        day.setOrdinaryHours(request.getOrdinaryHours());
        day.setSurplusHours(request.getSurplusHours());
        day.setOvertimeHours(request.getOvertimeHours());
        day.setManuallyEdited(true);
        TourProcessingDraftDay saved = dayRepository.save(day);

        draftHoursService.syncDriver(draft, assignmentsOf(tourRepository.findAllByDraftIdOrderByRowNumberAsc(draftId)), assignment.getDriverId());

        return toDayResponse(saved);
    }

    private void validateDriverMonthlyCaps(
            TourProcessingDraft draft,
            TourProcessingDraftAssignment assignment,
            UUID dayId,
            TourProcessingDraftRowDayRequestDto request
    ) {
        if (assignment.getResourceType() != ResourceType.INTERNAL) {
            return;
        }
        UUID driverId = assignment.getDriverId();
        double otherSurplus = 0;
        double otherOvertime = 0;
        if (driverId != null) {
            for (TourProcessingDraftDay other : dayRepository.findAllByDraftId(draft.getId())) {
                if (other.getId().equals(dayId) || !driverId.equals(other.getAssignment().getDriverId())) continue;
                if (other.getAssignment().getResourceType() != ResourceType.INTERNAL) continue;
                otherSurplus += other.getSurplusHours();
                otherOvertime += other.getOvertimeHours();
            }
        }

        double surplusCap = businessDaysService.surplusCap(draft, driverId);
        double newSurplusTotal = otherSurplus + request.getSurplusHours();
        double newOvertimeTotal = otherOvertime + request.getOvertimeHours();

        if (newSurplusTotal > surplusCap + DriverHoursRules.HOURS_TOLERANCE) {
            throw TransportException.unprocessable(
                    "TOUR_PROCESSING_DRAFT_ROW_DAY_SURPLUS_CAP_EXCEEDED",
                    "Las horas sobresueldo acumuladas del chofer en el mes superarían el tope de " + surplusCap + " horas"
            );
        }
        if (newOvertimeTotal > DriverHoursRules.MONTHLY_OVERTIME_CAP + DriverHoursRules.HOURS_TOLERANCE) {
            throw TransportException.unprocessable(
                    "TOUR_PROCESSING_DRAFT_ROW_DAY_OVERTIME_CAP_EXCEEDED",
                    "Las horas extra acumuladas del chofer en el mes superarían el tope de " + (int) DriverHoursRules.MONTHLY_OVERTIME_CAP + " horas"
            );
        }

        boolean overtimeBelowSurplusThreshold = request.getOvertimeHours() > DriverHoursRules.HOURS_TOLERANCE
                && newSurplusTotal < surplusCap * DriverHoursRules.SURPLUS_CUTOFF_RATIO - DriverHoursRules.HOURS_TOLERANCE;
        if (overtimeBelowSurplusThreshold && !request.isConfirmOvertimeBelowSurplusThreshold()) {
            throw TransportException.conflict(
                    "TOUR_PROCESSING_DRAFT_ROW_DAY_OVERTIME_NEEDS_CONFIRMATION",
                    "El chofer todavía no alcanza el 95% de su tope de sobresueldo del mes"
            );
        }
    }

    @Transactional
    public void deleteRow(UUID draftId, UUID rowId) {
        TourProcessingDraft draft = getEditableDraft(draftId);
        TourProcessingDraftTour tour = getEditableTour(draftId, rowId, "Elimina la distribución desde la gira principal");

        List<TourProcessingDraftTour> tours = new ArrayList<>(tourRepository.findAllByDraftIdOrderByRowNumberAsc(draftId));
        Map<UUID, Set<DraftHoursService.WorkloadSlot>> slotsBefore = DraftHoursService.workloadSlots(assignmentsOf(tours));
        tours.removeIf(current -> current.getId().equals(tour.getId()));
        tourRepository.delete(tour);
        tourRepository.flush();
        markDuplicates(tours);
        summarize(draft, tours);
        draft.setTotalRows(tours.size());
        tourRepository.saveAll(tours);
        draftHoursService.syncChanged(draft, slotsBefore, assignmentsOf(tours));
    }

    @Transactional
    public int deleteDuplicateRows(UUID draftId) {
        TourProcessingDraft draft = getEditableDraft(draftId);
        List<TourProcessingDraftTour> tours = tourRepository.findAllByDraftIdOrderByRowNumberAsc(draftId);
        if (tours.stream().anyMatch(TourProcessingDraftTour::isConflicting)) {
            throw TransportException.conflict(
                    "TOUR_PROCESSING_DRAFT_HAS_CONFLICTS",
                    "No se pueden eliminar las filas repetidas mientras haya un Numero repetido con datos distintos"
            );
        }

        List<TourProcessingDraftTour> duplicates = tours.stream().filter(tour -> !tour.isPrimary()).toList();
        List<TourProcessingDraftTour> remaining = tours.stream().filter(TourProcessingDraftTour::isPrimary).toList();
        Map<UUID, Set<DraftHoursService.WorkloadSlot>> slotsBefore = DraftHoursService.workloadSlots(assignmentsOf(tours));

        if (!duplicates.isEmpty()) {
            tourRepository.deleteAll(duplicates);
            tourRepository.flush();
        }
        markDuplicates(remaining);
        summarize(draft, remaining);
        draft.setTotalRows(remaining.size());
        tourRepository.saveAll(remaining);
        draftHoursService.syncChanged(draft, slotsBefore, assignmentsOf(remaining));
        return duplicates.size();
    }

    @Transactional
    public int splitOversizedRows(UUID draftId) {
        return splitOversizedRows(draftId, TourSplitMode.DISTINCT_VEHICLES);
    }

    @Transactional
    public int splitOversizedRows(UUID draftId, TourSplitMode mode) {
        TourProcessingDraft draft = getEditableDraft(draftId);
        List<TourProcessingDraftTour> tours = tourRepository.findAllByDraftIdOrderByRowNumberAsc(draftId);
        Map<UUID, Set<DraftHoursService.WorkloadSlot>> slotsBefore = DraftHoursService.workloadSlots(assignmentsOf(tours));
        List<Vehicle> vehicles = vehicleRepository.findAll();
        List<TourProcessingDraftTour> oversized = oversizedToursOf(tours, vehicles);
        if (oversized.isEmpty()) {
            return 0;
        }

        Map<UUID, Vehicle> vehiclesById = DriverHoursRules.vehiclesById(vehicles);
        int fleetMaximum = fleetMaximumCapacity(vehicles);
        Map<TourProcessingDraftTour, Integer> partsByTour = new LinkedHashMap<>();
        oversized.forEach(tour -> partsByTour.put(tour,
                partsNeeded(tour.getPassengers(), capacityOf(tour.primaryAssignment(), vehiclesById, fleetMaximum))));
        ensureAssignmentLimit(tours, partsByTour.values().stream().mapToInt(parts -> parts - 1).sum());

        partsByTour.forEach((tour, parts) -> splitIntoParts(tour, parts, mode));
        tourRepository.flush();
        markDuplicates(tours);
        summarize(draft, tours);
        tourRepository.saveAll(tours);
        draftHoursService.syncChanged(draft, slotsBefore, assignmentsOf(tours));
        return oversized.size();
    }

    private void splitIntoParts(TourProcessingDraftTour tour, int parts, TourSplitMode mode) {
        int totalPassengers = tour.getPassengers();
        TourProcessingDraftAssignment first = tour.primaryAssignment();
        first.setPassengers(passengersOfPart(totalPassengers, parts, 1));
        for (int part = 2; part <= parts; part++) {
            TourProcessingDraftAssignment.TourProcessingDraftAssignmentBuilder trip = TourProcessingDraftAssignment.builder()
                    .resourceType(ResourceType.INTERNAL)
                    .passengers(passengersOfPart(totalPassengers, parts, part));
            if (mode == TourSplitMode.SAME_DRIVER_AND_VEHICLE) {
                trip.resourceType(first.getResourceType())
                        .driver(first.getDriver())
                        .driverId(first.getDriverId())
                        .vehicle(first.getVehicle())
                        .vehicleId(first.getVehicleId())
                        .borrowedVehicle(first.getBorrowedVehicle());
            }
            tour.addAssignment(trip.build());
        }
    }

    private int partsNeeded(int passengers, int capacity) {
        return (passengers + capacity - 1) / capacity;
    }

    private int passengersOfPart(int totalPassengers, int parts, int part) {
        return totalPassengers / parts + (part <= totalPassengers % parts ? 1 : 0);
    }

    static List<TourProcessingDraftTour> oversizedToursOf(List<TourProcessingDraftTour> tours, List<Vehicle> vehicles) {
        Map<UUID, Vehicle> vehiclesById = DriverHoursRules.vehiclesById(vehicles);
        int fleetMaximum = fleetMaximumCapacity(vehicles);
        return tours.stream()
                .filter(tour -> !tour.isDistributed())
                .filter(tour -> tour.primaryAssignment() != null)
                .filter(tour -> tour.primaryAssignment().getResourceType() != ResourceType.EXTERNAL)
                .filter(tour -> tour.isPrimary() && tour.isApproved())
                .filter(tour -> {
                    int capacity = capacityOf(tour.primaryAssignment(), vehiclesById, fleetMaximum);
                    return capacity > 0 && tour.getPassengers() > capacity;
                })
                .toList();
    }

    static int capacityOf(TourProcessingDraftAssignment assignment, Map<UUID, Vehicle> vehiclesById, int fleetMaximum) {
        if (assignment.getResourceType() == ResourceType.BORROWED_VEHICLE) {
            BorrowedVehicle borrowed = assignment.getBorrowedVehicle();
            return borrowed == null || borrowed.getCapacity() == null ? 0 : borrowed.getCapacity();
        }
        Vehicle selected = DriverHoursRules.assignedVehicle(assignment, vehiclesById);
        return selected == null || selected.getCapacity() == null ? fleetMaximum : selected.getCapacity();
    }

    static int fleetMaximumCapacity(List<Vehicle> vehicles) {
        return vehicles.stream()
                .filter(Vehicle::isAvailable)
                .map(Vehicle::getCapacity)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }

    @Transactional
    public void clearAssignments(UUID draftId) {
        TourProcessingDraft draft = getEditableDraft(draftId);
        List<TourProcessingDraftTour> tours = tourRepository.findAllByDraftIdOrderByRowNumberAsc(draftId);
        List<TourProcessingDraftAssignment> assignments = assignmentsOf(tours);
        Map<UUID, Set<DraftHoursService.WorkloadSlot>> slotsBefore = DraftHoursService.workloadSlots(assignments);
        for (TourProcessingDraftAssignment assignment : assignments) {
            assignment.setDriver(null);
            assignment.setDriverId(null);
            assignment.setVehicle(null);
            assignment.setVehicleId(null);
            assignment.setBorrowedVehicle(null);
        }
        markDuplicates(tours);
        summarize(draft, tours);
        tourRepository.saveAll(tours);
        draftHoursService.syncChanged(draft, slotsBefore, assignments);
    }

    @Transactional
    public TourProcessingDriverAutoAssignResponseDto autoAssignDrivers(UUID draftId) {
        return autoAssignDrivers(draftId, assignment -> true, false);
    }

    @Transactional
    public TourProcessingDriverAutoAssignResponseDto autoAssignUlloaDrivers(UUID draftId) {
        return autoAssignDrivers(draftId, UlloaTours::isUlloaTour, true);
    }

    private TourProcessingDriverAutoAssignResponseDto autoAssignDrivers(UUID draftId, Predicate<TourProcessingDraftAssignment> scope, boolean ulloaOnly) {
        TourProcessingDraft draft = getEditableDraft(draftId);
        requireAutoAssignPeriod(draft);

        List<TourProcessingDraftTour> tours = tourRepository.findAllByDraftIdOrderByRowNumberAsc(draftId);
        List<TourProcessingDraftAssignment> assignments = assignmentsOf(tours);
        Map<UUID, Set<DraftHoursService.WorkloadSlot>> slotsBefore = DraftHoursService.workloadSlots(assignments);
        List<Driver> catalog = driverRepository.findAll();

        int clearedTours = clearUnknownDrivers(assignments, scope);
        List<TourProcessingDraftAssignment> pendingTours = pendingDriverTours(assignments, scope);
        DriverAutoAssigner assigner = new DriverAutoAssigner(
                draft.getYear(),
                draft.getMonth(),
                businessDaysService.forDraft(draft),
                catalog,
                vehicleRepository.findAll(),
                assignments,
                draftHoursService.manualDays(draftId)
        );
        Map<TourProcessingDraftAssignment, Driver> result = ulloaOnly ? assigner.assignUlloaOnly(pendingTours) : assigner.assign(pendingTours);

        Map<String, Vehicle> groupedVehicles = new HashMap<>();
        assigner.vehicleAssignments().forEach((part, vehicle) -> groupedVehicles.put(assignmentKey(part), vehicle));
        assignments.stream().filter(part -> part.getResourceType() == ResourceType.INTERNAL)
                .filter(part -> part.getVehicleId() == null && groupedVehicles.containsKey(assignmentKey(part)))
                .forEach(part -> {
                    Vehicle vehicle = groupedVehicles.get(assignmentKey(part));
                    part.setVehicleId(vehicle.getId());
                    part.setVehicle(vehicle.getPlate());
                });

        Map<String, Driver> driverByKey = new HashMap<>();
        result.forEach((assignment, driver) -> driverByKey.put(assignmentKey(assignment), driver));
        assignments.stream()
                .filter(assignment -> assignment.getResourceType() == ResourceType.INTERNAL)
                .filter(assignment -> driverByKey.containsKey(assignmentKey(assignment)))
                .forEach(assignment -> {
                    Driver driver = driverByKey.get(assignmentKey(assignment));
                    assignment.setDriver(driver == null ? null : driver.getName());
                    assignment.setDriverId(driver == null ? null : driver.getId());
                });

        markDuplicates(tours);
        summarize(draft, tours);
        tourRepository.saveAll(tours);
        draftHoursService.syncChanged(draft, slotsBefore, assignments);

        return TourProcessingDriverAutoAssignResponseDto.builder()
                .clearedTours(clearedTours)
                .assignedTours((int) result.values().stream().filter(Objects::nonNull).count())
                .unassignedTours(pendingDriverTours(assignments, scope).size())
                .unassignedDetails(pendingDriverTours(assignments, scope).stream()
                        .map(assignment -> new TourProcessingDriverAutoAssignResponseDto.TourIssue(assignment.getTour().getId(), assignment.getNumber(),
                                assigner.reasonsForUnassigned(assignment, ulloaOnly))).distinct().toList())
                .preservedAssignmentWarnings(assigner.preservedAssignmentWarnings().entrySet().stream()
                        .filter(entry -> scope.test(entry.getKey()))
                        .map(entry -> new TourProcessingDriverAutoAssignResponseDto.TourIssue(entry.getKey().getTour().getId(),
                                entry.getKey().getNumber(), entry.getValue())).distinct().toList())
                .surplusDeficits(assigner.surplusDeficits())
                .surplusTargets(assigner.surplusTargets())
                .surplusTargetHours(assigner.defaultSurplusTargetHours())
                .optimizationLimitReached(assigner.optimizationLimitReached())
                .mandatoryCoverage(mandatoryCoverageOf(assigner.coverageSummary()))
                .priorityOneUnassigned(priorityOneUnassigned(pendingDriverTours(assignments, scope)))
                .build();
    }

    private static TourProcessingDriverAutoAssignResponseDto.MandatoryCoverage mandatoryCoverageOf(MandatoryCoverageSummary summary) {
        return new TourProcessingDriverAutoAssignResponseDto.MandatoryCoverage(
                summary.total(),
                summary.fulfilled(),
                summary.withWarning(),
                summary.unattended(),
                summary.warnings().stream().map(TourProcessingDraftService::coverageIssueOf).toList(),
                summary.unattendedTours().stream().map(TourProcessingDraftService::coverageIssueOf).toList(),
                summary.coveringDrivers().stream().map(driver -> new TourProcessingDriverAutoAssignResponseDto.CoveringDriver(
                        driver.name(), driver.surplusHours(), driver.targetHours(), driver.surplusPercent(), driver.tourNumbers())).toList(),
                summary.driversBelowTarget(),
                summary.driversBelowTargetByConcentration()
        );
    }

    private static TourProcessingDriverAutoAssignResponseDto.TourIssue coverageIssueOf(MandatoryCoverageSummary.TourNote note) {
        return new TourProcessingDriverAutoAssignResponseDto.TourIssue(note.tourId(), note.number(), List.of(note.message()));
    }

    private static int priorityOneUnassigned(List<TourProcessingDraftAssignment> pending) {
        return (int) pending.stream()
                .filter(assignment -> assignment.getPriority() != null && assignment.getPriority() <= 1)
                .map(assignment -> assignment.getTour().getId())
                .distinct()
                .count();
    }

    private void requireAutoAssignPeriod(TourProcessingDraft draft) {
        if (draft.getYear() == null || draft.getMonth() == null || draft.getBusinessDays() == null) {
            throw TransportException.unprocessable(
                    "TOUR_PROCESSING_DRAFT_WITHOUT_PERIOD",
                    "El borrador no tiene mes ni días hábiles definidos, no se pueden autoasignar choferes"
            );
        }
    }

    private int clearUnknownDrivers(List<TourProcessingDraftAssignment> assignments, Predicate<TourProcessingDraftAssignment> scope) {
        int clearedTours = 0;
        for (TourProcessingDraftAssignment assignment : assignments) {
            if (assignment.getResourceType() != ResourceType.INTERNAL || !scope.test(assignment)) {
                continue;
            }
            if (assignment.getDriverId() == null && !DriverHoursRules.driverKey(assignment.getDriver()).isEmpty()) {
                assignment.setDriver(null);
                clearedTours += assignment.getTour().isPrimary() ? 1 : 0;
            }
        }
        return clearedTours;
    }

    private List<TourProcessingDraftAssignment> pendingDriverTours(List<TourProcessingDraftAssignment> assignments, Predicate<TourProcessingDraftAssignment> scope) {
        return assignments.stream()
                .filter(TourProcessingDraftAssignment::isActive)
                .filter(assignment -> assignment.getResourceType() == ResourceType.INTERNAL)
                .filter(assignment -> assignment.getDriverId() == null)
                .filter(scope)
                .toList();
    }

    @Transactional
    public TourProcessingVehicleAutoAssignResponseDto autoAssignVehicles(UUID draftId) {
        TourProcessingDraft draft = getEditableDraft(draftId);
        List<TourProcessingDraftTour> tours = tourRepository.findAllByDraftIdOrderByRowNumberAsc(draftId);
        List<TourProcessingDraftAssignment> assignments = assignmentsOf(tours);
        List<Vehicle> vehicles = vehicleRepository.findAll();
        List<Driver> drivers = driverRepository.findAll();
        Map<UUID, Vehicle> vehiclesById = DriverHoursRules.vehiclesById(vehicles);
        List<TourProcessingVehicleAutoAssignResponseDto.TourIssue> preservedWarnings = new ArrayList<>();
        for (TourProcessingDraftAssignment assignment : assignments) {
            if (!assignment.isActive() || assignment.getResourceType() != ResourceType.INTERNAL || assignment.getVehicleId() == null) continue;
            Vehicle vehicle = vehiclesById.get(assignment.getVehicleId());
            List<String> reasons = new ArrayList<>();
            if (vehicle == null) {
                reasons.add("El vehículo asignado no existe en el catálogo. Se conservó la asignación existente.");
            } else {
                if (vehicle.getCapacity() == null) {
                    reasons.add("El vehículo " + vehicle.getPlate() + " no tiene capacidad de pasajeros registrada. Se conservó la asignación existente.");
                } else if (vehicle.getCapacity() < assignment.getPassengers()) {
                    reasons.add("El vehículo " + vehicle.getPlate() + " no tiene capacidad para " + assignment.getPassengers() + " pasajeros. Se conservó la asignación existente.");
                }
                if (!vehicle.isAvailable()) {
                    reasons.add("El vehículo " + vehicle.getPlate() + " no está disponible. Se conservó la asignación existente.");
                }
            }
            if (!reasons.isEmpty()) preservedWarnings.add(vehicleIssue(assignment, reasons));
        }
        int groupedVehiclesBefore = (int) assignments.stream().filter(a -> a.getTour().isDistributed())
                .filter(a -> a.getVehicleId() != null).count();
        int clearedTours = 0;
        for (TourProcessingDraftAssignment assignment : assignments) {
            if (assignment.getResourceType() != ResourceType.INTERNAL) {
                continue;
            }
            if (assignment.getVehicle() != null && assignment.getVehicleId() == null) {
                assignment.setVehicle(null);
                clearedTours += assignment.getTour().isPrimary() ? 1 : 0;
            }
        }

        Predicate<TourProcessingDraftAssignment> assignedDriverGroups = assignment -> assignment.getTour().isDistributed()
                && assignment.getTour().getAssignments().stream().anyMatch(part -> part.getDriverId() != null);
        if (assignments.stream().anyMatch(assignment -> assignment.isActive() && assignedDriverGroups.test(assignment))) {
            autoAssignDrivers(draftId, assignedDriverGroups, false);
        }

        List<TourProcessingDraftAssignment> pendingWithDriver = assignments.stream()
                .filter(TourProcessingDraftAssignment::isActive)
                .filter(assignment -> assignment.getResourceType() == ResourceType.INTERNAL)
                .filter(assignment -> !assignment.getTour().isDistributed())
                .filter(assignment -> assignment.getDriverId() != null && assignment.getVehicleId() == null)
                .toList();
        Map<TourProcessingDraftAssignment, Vehicle> assignedWithDriver = new VehicleAutoAssigner(vehicles, drivers, assignments)
                .assign(pendingWithDriver);
        applyVehicleAssignments(assignments, assignedWithDriver);

        TourProcessingDriverAutoAssignResponseDto groupResult = null;
        if (assignments.stream().anyMatch(a -> a.isActive() && a.getTour().isDistributed())) {
            groupResult = autoAssignDrivers(draftId, a -> a.getTour().isDistributed(), false);
        }
        int groupedVehiclesAdded = (int) assignments.stream().filter(a -> a.getTour().isDistributed())
                .filter(a -> a.getVehicleId() != null).count() - groupedVehiclesBefore;

        List<TourProcessingDraftAssignment> pendingTours = assignments.stream()
                .filter(TourProcessingDraftAssignment::isActive)
                .filter(assignment -> assignment.getResourceType() == ResourceType.INTERNAL)
                .filter(assignment -> assignment.getVehicleId() == null)
                .toList();
        VehicleAutoAssigner assigner = new VehicleAutoAssigner(vehicles, drivers, assignments);
        Map<TourProcessingDraftAssignment, Vehicle> result = assigner.assign(pendingTours);

        applyVehicleAssignments(assignments, result);

        Map<UUID, List<String>> groupReasons = new HashMap<>();
        if (groupResult != null) {
            groupResult.getUnassignedDetails().forEach(issue -> groupReasons.putIfAbsent(issue.rowId(), issue.reasons()));
            groupResult.getPreservedAssignmentWarnings().forEach(issue -> groupReasons.putIfAbsent(issue.rowId(), issue.reasons()));
        }
        List<TourProcessingVehicleAutoAssignResponseDto.TourIssue> unassignedDetails = new ArrayList<>();
        for (TourProcessingDraftAssignment assignment : assignments) {
            if (!assignment.isActive()) continue;
            if (assignment.getResourceType() == ResourceType.INTERNAL && assignment.getVehicleId() == null) {
                List<String> reasons = assignment.getTour().isDistributed()
                        ? groupReasons.getOrDefault(assignment.getTour().getId(),
                                List.of("No se encontró una combinación completa de choferes y vehículos para todos los viajes de esta gira."))
                        : assigner.reasonsForUnassigned(assignment);
                unassignedDetails.add(vehicleIssue(assignment, reasons));
            } else if (assignment.getResourceType() == ResourceType.BORROWED_VEHICLE && assignment.getBorrowedVehicle() == null) {
                unassignedDetails.add(vehicleIssue(assignment, List.of("Selecciona el vehículo prestado de esta gira.")));
            }
        }

        markDuplicates(tours);
        summarize(draft, tours);

        tourRepository.saveAll(tours);
        return TourProcessingVehicleAutoAssignResponseDto.builder()
                .clearedTours(clearedTours)
                .assignedTours(assignedWithDriver.size() + result.size() + groupedVehiclesAdded)
                .unassignedTours(unassignedDetails.size())
                .unassignedDetails(unassignedDetails)
                .preservedAssignmentWarnings(preservedWarnings)
                .build();
    }

    private static TourProcessingVehicleAutoAssignResponseDto.TourIssue vehicleIssue(
            TourProcessingDraftAssignment assignment, List<String> reasons) {
        String number = assignment.getNumber();
        if (assignment.getTour().isDistributed()) number += " - viaje " + assignment.getPart();
        return new TourProcessingVehicleAutoAssignResponseDto.TourIssue(assignment.getId(), number, reasons);
    }

    private void applyVehicleAssignments(List<TourProcessingDraftAssignment> assignments,
                                         Map<TourProcessingDraftAssignment, Vehicle> result) {
        Map<String, Vehicle> vehicleByKey = new HashMap<>();
        result.forEach((assignment, vehicle) -> vehicleByKey.put(assignmentKey(assignment), vehicle));
        assignments.stream()
                .filter(assignment -> assignment.getResourceType() == ResourceType.INTERNAL)
                .filter(assignment -> assignment.getVehicleId() == null)
                .filter(assignment -> vehicleByKey.containsKey(assignmentKey(assignment)))
                .forEach(assignment -> {
                    Vehicle vehicle = vehicleByKey.get(assignmentKey(assignment));
                    assignment.setVehicle(vehicle.getPlate());
                    assignment.setVehicleId(vehicle.getId());
                });
    }

    @Transactional
    public void delete(UUID id) {
        draftRepository.delete(getEditableDraft(id));
    }

    private TourProcessingDraft getDraft(UUID id) {
        return draftRepository.findById(id)
                .orElseThrow(() -> TransportException.notFound(DRAFT_RESOURCE, id.toString()));
    }

    private TourProcessingDraft getEditableDraft(UUID id) {
        TourProcessingDraft draft = draftRepository.findByIdForUpdate(id)
                .orElseThrow(() -> TransportException.notFound(DRAFT_RESOURCE, id.toString()));
        if (draft.getStatus() != TourProcessingDraftStatus.DRAFT) {
            throw TransportException.conflict(
                    draft.getStatus() == TourProcessingDraftStatus.CLOSED
                            ? "TOUR_PROCESSING_DRAFT_CLOSED" : "TOUR_PROCESSING_DRAFT_ALREADY_REGISTERED",
                    draft.getStatus() == TourProcessingDraftStatus.CLOSED
                            ? "No se puede editar una depuración terminada. Devuélvela a borrador"
                            : "No se puede editar un borrador que ya fue registrado"
            );
        }
        return draft;
    }

    private TourProcessingDraftTour getEditableTour(UUID draftId, UUID rowId, String partMessage) {
        Optional<TourProcessingDraftTour> tour = tourRepository.findByIdAndDraftId(rowId, draftId);
        if (tour.isPresent()) {
            return tour.get();
        }
        TourProcessingDraftAssignment assignment = assignmentRepository.findByIdAndDraftId(rowId, draftId)
                .orElseThrow(() -> TransportException.notFound(ROW_RESOURCE, rowId.toString()));
        if (assignment.getPart() > 1) {
            throw TransportException.unprocessable("TOUR_PROCESSING_DISTRIBUTION_PART", partMessage);
        }
        return assignment.getTour();
    }

    private void ensureMonthIsAvailable(Integer year, Integer month) {
        if (draftRepository.existsByYearAndMonthAndStatus(year, month, TourProcessingDraftStatus.REGISTERED)) {
            throw TransportException.conflict(
                    "TOUR_PROCESSING_MONTH_ALREADY_REGISTERED",
                    "La depuración de " + monthLabel(year, month) + " ya fue registrada. No se puede importar otra del mismo mes"
            );
        }
        if (draftRepository.existsByYearAndMonthAndStatus(year, month, TourProcessingDraftStatus.DRAFT)) {
            throw TransportException.conflict(
                    "TOUR_PROCESSING_DRAFT_MONTH_EXISTS",
                    "Ya existe un borrador de depuración de " + monthLabel(year, month)
                            + " pendiente de registrar. Regístralo o elimínalo antes de importar otro del mismo mes"
            );
        }
        if (draftRepository.existsByYearAndMonthAndStatus(year, month, TourProcessingDraftStatus.CLOSED)) {
            throw TransportException.conflict(
                    "TOUR_PROCESSING_MONTH_ALREADY_CLOSED",
                    "La depuración de " + monthLabel(year, month) + " ya fue terminada. Reábrela para continuar"
            );
        }
    }

    private String monthLabel(Integer year, Integer month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, SPANISH) + " de " + year;
    }

    private void ensureAssignmentLimit(List<TourProcessingDraftTour> tours, int addedAssignments) {
        if (addedAssignments > 0 && assignmentsOf(tours).size() + addedAssignments > MAX_ROWS) {
            throw TransportException.badRequest("TOUR_PROCESSING_TOO_MANY_ROWS", "El borrador supera el máximo de " + MAX_ROWS + " filas");
        }
    }

    private int requestedParts(TourProcessingDraftRowRequestDto request) {
        return request.getAssignments() == null ? 1 : request.getAssignments().size();
    }

    private void validateReapprovedToursHaveNoConflicts(
            TourProcessingDraft draft,
            List<TourProcessingDraftAssignment> reapproved,
            List<TourProcessingDraftAssignment> assignments
    ) {
        List<TourProcessingDraftAssignment> others = assignments.stream()
                .filter(other -> !reapproved.contains(other))
                .toList();
        List<Driver> driverCatalog = driverRepository.findAll();
        Map<UUID, Vehicle> vehiclesById = DriverHoursRules.vehiclesById(vehicleRepository.findAll());
        Map<UUID, VehicleWorkload> vehicleWorkloads = VehicleWorkload.fromAssignments(others, vehiclesById);
        ManualDays manualDays = draftHoursService.manualDays(draft.getId());

        for (TourProcessingDraftAssignment tour : reapproved) {
            if (!tour.getTour().isPrimary()) {
                continue;
            }
            validateBorrowedVehicleAvailability(tour, others);
            validateReapprovedVehicleIsFree(tour, vehiclesById, vehicleWorkloads);
            validateReapprovedDriverCanAttend(draft, tour, driverCatalog, others, manualDays);
        }
    }

    private void validateReapprovedVehicleIsFree(
            TourProcessingDraftAssignment tour,
            Map<UUID, Vehicle> vehiclesById,
            Map<UUID, VehicleWorkload> vehicleWorkloads
    ) {
        Vehicle vehicle = DriverHoursRules.assignedVehicle(tour, vehiclesById);
        VehicleWorkload workload = vehicle == null ? null : vehicleWorkloads.get(vehicle.getId());
        if (workload == null) {
            return;
        }
        workload.findOverlap(tour).ifPresent(other -> {
            throw TransportException.unprocessable(
                    "TOUR_PROCESSING_DRAFT_ROW_VEHICLE_OVERLAP",
                    "No se puede aprobar la gira porque el vehículo " + vehicle.getPlate() + " ya está asignado a la gira "
                            + other.getNumber() + ", que se cruza con las fechas de esta gira."
            );
        });
    }

    private void validateReapprovedDriverCanAttend(
            TourProcessingDraft draft,
            TourProcessingDraftAssignment tour,
            List<Driver> driverCatalog,
            List<TourProcessingDraftAssignment> others,
            ManualDays manualDays
    ) {
        boolean periodDefined = draft.getYear() != null && draft.getMonth() != null && draft.getBusinessDays() != null;
        if (!periodDefined || tour.getResourceType() == ResourceType.EXTERNAL) {
            return;
        }
        Driver driver = DriverHoursRules.assignedDriver(tour, DriverHoursRules.driversById(driverCatalog));
        if (driver == null) {
            return;
        }
        new DriverAutoAssigner(draft.getYear(), draft.getMonth(), businessDaysService.forDraft(draft), driverCatalog, List.of(), others, manualDays)
                .rejectionFor(driver, tour)
                .filter(rejection -> REAPPROVAL_BLOCKING_REASONS.contains(rejection.reason()))
                .ifPresent(rejection -> {
                    throw TransportException.unprocessable(
                            "TOUR_PROCESSING_DRAFT_ROW_DRIVER_UNAVAILABLE",
                            "No se puede aprobar la gira porque el chofer asignado no puede atenderla.\n\n" + rejection.message()
                    );
                });
    }

    private void validateAssignedDriverCanAttend(
            TourProcessingDraft draft,
            TourProcessingDraftTour tour,
            TourProcessingDraftRowRequestDto request,
            List<TourProcessingDraftAssignment> assignments,
            DraftCatalogLinker linker
    ) {
        TourProcessingDraftAssignment current = tour.primaryAssignment();
        boolean durationIncreased = request.getDurationDays() > tour.getDurationDays();
        boolean periodDefined = draft.getYear() != null && draft.getMonth() != null && draft.getBusinessDays() != null;
        if (!durationIncreased || !periodDefined || current == null || current.getDriverId() == null || !current.isActive()) {
            return;
        }
        Driver requestedDriver = linker.driverOf(requestedDriverText(request));
        if (requestedDriver == null || !requestedDriver.getId().equals(current.getDriverId())) {
            return;
        }

        TourProcessingDraftTour candidateTour = new TourProcessingDraftTour();
        applyTourValues(candidateTour, request);
        TourProcessingDraftAssignment candidate = TourProcessingDraftAssignment.builder()
                .tour(candidateTour)
                .part(1)
                .passengers(current.getPassengers())
                .resourceType(current.getResourceType())
                .driverId(current.getDriverId())
                .vehicleId(current.getVehicleId())
                .build();
        List<TourProcessingDraftAssignment> others = assignments.stream()
                .filter(other -> other != current)
                .toList();
        new DriverAutoAssigner(draft.getYear(), draft.getMonth(), businessDaysService.forDraft(draft), driverRepository.findAll(), List.of(), others,
                draftHoursService.manualDays(draft.getId()))
                .rejectionFor(requestedDriver, candidate)
                .filter(rejection -> rejection.reason() != DriverRejection.Reason.WEEKEND_CAP_EXCEEDED)
                .ifPresent(rejection -> {
                    throw TransportException.unprocessable(
                            "TOUR_PROCESSING_DRAFT_ROW_DRIVER_UNAVAILABLE",
                            "No se puede aumentar la duración de la gira porque el chofer asignado no puede atenderla.\n\n"
                                    + rejection.message()
                    );
                });
    }

    private void validateBorrowedVehicleDrivers(TourProcessingDraftRowRequestDto request, DraftCatalogLinker linker) {
        List<String> drivers = new ArrayList<>();
        if (request.getAssignments() == null) {
            if (request.getResourceType() == ResourceType.BORROWED_VEHICLE) {
                drivers.add(request.getDriver());
            }
        } else {
            request.getAssignments().stream()
                    .filter(assignment -> assignment != null && assignment.getResourceType() == ResourceType.BORROWED_VEHICLE)
                    .forEach(assignment -> drivers.add(assignment.getDriver()));
        }
        for (String driver : drivers) {
            if (driver != null && !driver.isBlank() && linker.driverOf(driver) == null) {
                throw TransportException.unprocessable(
                        "TOUR_PROCESSING_DRAFT_ROW_BORROWED_DRIVER_NOT_FOUND",
                        "El chofer \"" + driver.trim() + "\" no existe en el sistema. Ingrésalo como chofer de otra unidad antes de guardar la gira de préstamo de vehículo"
                );
            }
        }
    }

    private String requestedDriverText(TourProcessingDraftRowRequestDto request) {
        List<TourProcessingDraftAssignmentDto> requested = request.getAssignments();
        return requested == null || requested.isEmpty() ? request.getDriver() : requested.get(0).getDriver();
    }

    private List<TourProcessingDraftRowIssueDto> validateRows(List<TourProcessingDraftRowRequestDto> rows) {
        List<TourProcessingDraftRowIssueDto> errors = new ArrayList<>();
        for (TourProcessingDraftRowRequestDto row : rows) {
            List<String> reasons = validateRow(row);
            reasons.forEach(reason -> errors.add(TourProcessingDraftRowIssueDto.builder()
                    .row(row.getRowNumber())
                    .reason(reason)
                    .build()));
        }
        return errors;
    }

    private List<String> validateRow(TourProcessingDraftRowRequestDto row) {
        List<String> violations = validator.validate(row).stream()
                .sorted(Comparator.comparingInt(this::violationOrder))
                .map(ConstraintViolation::getMessage)
                .toList();
        if (!violations.isEmpty()) {
            return violations;
        }
        if (row.getReturnDate().isBefore(row.getDepartureDate())) {
            return List.of("La fecha de regreso es anterior a la fecha de salida");
        }
        if (row.getReturnDate().isEqual(row.getDepartureDate()) && row.getReturnTime().isBefore(row.getDepartureTime())) {
            return List.of("La hora de regreso es anterior a la hora de salida en una gira de un solo día");
        }
        if (row.getAssignments() != null) {
            List<TourProcessingDraftAssignmentDto> assignments = row.getAssignments();
            if (assignments.isEmpty()) return List.of("Agrega al menos un vehículo a la distribución");
            Map<String, Vehicle> catalog = DriverHoursRules.vehicleCatalogByPlate(vehicleRepository.findAll());
            for (TourProcessingDraftAssignmentDto assignment : assignments) {
                if (assignment == null || assignment.getPassengers() == null || assignment.getPassengers() < 1) {
                    return List.of("Cada vehículo debe tener al menos un pasajero");
                }
                ResourceType type = assignment.getResourceType() == null ? ResourceType.INTERNAL : assignment.getResourceType();
                String plate = DriverHoursRules.plateKey(assignment.getVehicle());
                if (type == ResourceType.BORROWED_VEHICLE) {
                    if (assignment.getBorrowedVehicleId() == null) return List.of("Selecciona el vehículo prestado de cada asignación");
                    BorrowedVehicle borrowed = borrowedVehicleRepository.findByIdAndIsDeletedFalse(assignment.getBorrowedVehicleId()).orElse(null);
                    if (borrowed == null) return List.of("El vehículo prestado seleccionado no existe");
                    if (borrowed.getCapacity() != null && borrowed.getCapacity() < assignment.getPassengers()) {
                        return List.of("El vehículo prestado " + borrowed.getPlate() + " no tiene capacidad para " + assignment.getPassengers() + " pasajeros");
                    }
                } else if (type == ResourceType.INTERNAL) {
                    Vehicle vehicle = catalog.get(plate);
                    if (vehicle != null && vehicle.getCapacity() != null && vehicle.getCapacity() < assignment.getPassengers()) {
                        return List.of("El vehículo " + vehicle.getPlate() + " no tiene capacidad para " + assignment.getPassengers() + " pasajeros");
                    }
                }
            }
            return List.of();
        }
        if (row.getResourceType() == ResourceType.BORROWED_VEHICLE) {
            if (row.getBorrowedVehicleId() == null) {
                return List.of("Selecciona el vehículo prestado para esta gira");
            }
            BorrowedVehicle borrowedVehicle = borrowedVehicleRepository.findByIdAndIsDeletedFalse(row.getBorrowedVehicleId()).orElse(null);
            if (borrowedVehicle == null) {
                return List.of("El vehículo prestado seleccionado no existe");
            }
            if (borrowedVehicle.getCapacity() != null && borrowedVehicle.getCapacity() < row.getPassengers()) {
                return List.of("El vehículo prestado no tiene capacidad para " + row.getPassengers() + " pasajeros");
            }
        }
        return List.of();
    }

    private BorrowedVehicle resolveBorrowedVehicle(UUID borrowedVehicleId) {
        if (borrowedVehicleId == null) {
            return null;
        }
        return borrowedVehicleRepository.findByIdAndIsDeletedFalse(borrowedVehicleId)
                .orElseThrow(() -> TransportException.notFound("Vehículo prestado", borrowedVehicleId.toString()));
    }

    private void validateBorrowedVehicles(List<TourProcessingDraftAssignment> assignments) {
        assignments.stream()
                .filter(TourProcessingDraftAssignment::isActive)
                .forEach(assignment -> validateBorrowedVehicleAvailability(assignment, assignments));
    }

    private void validateBorrowedVehicleAvailability(TourProcessingDraftAssignment assignment, List<TourProcessingDraftAssignment> assignments) {
        if (assignment.getResourceType() != ResourceType.BORROWED_VEHICLE || assignment.getBorrowedVehicle() == null) {
            return;
        }
        UUID borrowedVehicleId = assignment.getBorrowedVehicle().getId();
        boolean overlaps = assignments.stream()
                .filter(other -> other != assignment && !DriverWorkload.sameTour(other, assignment))
                .filter(other -> other.getResourceType() == ResourceType.BORROWED_VEHICLE)
                .filter(other -> other.getBorrowedVehicle() != null && other.getBorrowedVehicle().getId().equals(borrowedVehicleId))
                .filter(TourProcessingDraftAssignment::isActive)
                .anyMatch(other -> DriverHoursRules.toursOverlap(other, assignment));
        if (overlaps) {
            throw TransportException.unprocessable(
                    "TOUR_PROCESSING_DRAFT_ROW_BORROWED_VEHICLE_OVERLAP",
                    "El vehículo prestado \"" + assignment.getBorrowedVehicle().getPlate() + "\" ya está asignado a otra gira en ese rango de fechas"
            );
        }
    }

    private int violationOrder(ConstraintViolation<TourProcessingDraftRowRequestDto> violation) {
        int index = FIELD_ORDER.indexOf(violation.getPropertyPath().toString());
        return index < 0 ? -1 : index;
    }

    private static String numberKey(TourProcessingDraftTour tour) {
        return tour.getNumber().toLowerCase(Locale.ROOT);
    }

    private static String assignmentKey(TourProcessingDraftAssignment assignment) {
        return numberKey(assignment.getTour()) + ":" + assignment.getPart();
    }

    private void markDuplicates(List<TourProcessingDraftTour> tours) {
        Map<String, List<TourProcessingDraftTour>> groups = new LinkedHashMap<>();
        for (TourProcessingDraftTour tour : tours) {
            groups.computeIfAbsent(numberKey(tour), key -> new ArrayList<>()).add(tour);
        }
        for (List<TourProcessingDraftTour> group : groups.values()) {
            List<Object> firstContent = contentOf(group.get(0));
            for (int index = 0; index < group.size(); index++) {
                TourProcessingDraftTour tour = group.get(index);
                tour.setDuplicateRank(index + 1);
                tour.setDuplicateGroupSize(group.size());
                tour.setConflicting(index > 0 && !contentOf(tour).equals(firstContent));
            }
        }
    }

    private List<Object> contentOf(TourProcessingDraftTour tour) {
        List<Object> content = new ArrayList<>(Arrays.asList(
                tour.getNumber(),
                tour.getPassengers(),
                tour.getExecutingUnit(),
                tour.getResponsible(),
                tour.getDestination(),
                tour.getDurationDays(),
                tour.getPriority(),
                tour.getModality(),
                tour.getDepartureTime(),
                tour.getReturnTime(),
                tour.getDepartureDate(),
                tour.getReturnDate(),
                tour.getObservations()
        ));
        tour.getAssignments().forEach(assignment -> content.add(Arrays.asList(
                assignment.getDriver(),
                assignment.getVehicle(),
                assignment.getResourceType(),
                assignment.getBorrowedVehicle() == null ? null : assignment.getBorrowedVehicle().getId(),
                assignment.getPassengers()
        )));
        return content;
    }

    private void summarize(TourProcessingDraft draft, List<TourProcessingDraftTour> tours) {
        draft.setDuplicateRows((int) tours.stream().filter(tour -> !tour.isPrimary()).count());
        draft.setConflictingRows((int) tours.stream().filter(TourProcessingDraftTour::isConflicting).count());
        draft.setMinDepartureDate(tours.stream()
                .map(TourProcessingDraftTour::getDepartureDate)
                .min(LocalDate::compareTo)
                .orElse(null));
        draft.setMaxReturnDate(tours.stream()
                .map(TourProcessingDraftTour::getReturnDate)
                .max(LocalDate::compareTo)
                .orElse(null));
    }

    private static List<TourProcessingDraftAssignment> assignmentsOf(List<TourProcessingDraftTour> tours) {
        return tours.stream().flatMap(tour -> tour.getAssignments().stream()).toList();
    }

    private DraftCatalogLinker catalogLinker() {
        return DraftCatalogLinker.of(driverRepository.findAll(), vehicleRepository.findAll());
    }

    private TourProcessingDraftTour toTour(TourProcessingDraftRowRequestDto request, TourProcessingDraft draft, DraftCatalogLinker linker) {
        TourProcessingDraftTour tour = TourProcessingDraftTour.builder()
                .draft(draft)
                .rowNumber(request.getRowNumber())
                .build();
        applyTourValues(tour, request);
        applyAssignments(tour, request, linker);
        return tour;
    }

    private void applyTourValues(TourProcessingDraftTour tour, TourProcessingDraftRowRequestDto request) {
        tour.setNumber(request.getNumber().trim());
        tour.setPassengers(request.getPassengers());
        tour.setExecutingUnit(request.getExecutingUnit().trim());
        tour.setResponsible(request.getResponsible().trim());
        tour.setDestination(request.getDestination().trim());
        tour.setDurationDays(request.getDurationDays());
        tour.setPriority(request.getPriority());
        tour.setModality(trimToNull(request.getModality()));
        tour.setDepartureTime(request.getDepartureTime());
        tour.setReturnTime(request.getReturnTime());
        tour.setDepartureDate(request.getDepartureDate());
        tour.setReturnDate(request.getReturnDate());
        String observations = trimToNull(request.getObservations());
        if (!Objects.equals(observations, tour.getObservations())) {
            tour.setObservationReviewed(false);
        }
        tour.setObservations(observations);
        if (request.getApprovalStatus() != null) {
            tour.setApprovalStatus(request.getApprovalStatus());
        }
    }

    private void applyAssignments(TourProcessingDraftTour tour, TourProcessingDraftRowRequestDto request, DraftCatalogLinker linker) {
        List<TourProcessingDraftAssignmentDto> requested = request.getAssignments() != null
                ? request.getAssignments()
                : List.of(TourProcessingDraftAssignmentDto.builder()
                .driver(request.getDriver())
                .vehicle(request.getVehicle())
                .resourceType(request.getResourceType())
                .borrowedVehicleId(request.getBorrowedVehicleId())
                .passengers(request.getPassengers())
                .build());
        List<TourProcessingDraftAssignment> current = tour.getAssignments();
        while (current.size() > requested.size()) {
            current.remove(current.size() - 1);
        }
        for (int index = 0; index < requested.size(); index++) {
            TourProcessingDraftAssignment target = index < current.size()
                    ? current.get(index)
                    : tour.addAssignment(new TourProcessingDraftAssignment());
            applyAssignment(target, requested.get(index));
            linker.link(target);
        }
    }

    private void applyAssignment(TourProcessingDraftAssignment assignment, TourProcessingDraftAssignmentDto requested) {
        ResourceType type = requested.getResourceType() == null ? ResourceType.INTERNAL : requested.getResourceType();
        assignment.setResourceType(type);
        assignment.setDriver(trimToNull(requested.getDriver()));
        assignment.setVehicle(type == ResourceType.BORROWED_VEHICLE ? null : trimToNull(requested.getVehicle()));
        assignment.setBorrowedVehicle(type == ResourceType.BORROWED_VEHICLE ? resolveBorrowedVehicle(requested.getBorrowedVehicleId()) : null);
        assignment.setPassengers(requested.getPassengers());
    }

    private TourProcessingDraftResponseDto toResponse(TourProcessingDraft draft) {
        return TourProcessingDraftResponseDto.builder()
                .id(draft.getId())
                .fileName(draft.getFileName())
                .fileType(draft.getFileType())
                .status(draft.getStatus())
                .importedBy(draft.getImportedBy())
                .importedAt(draft.getImportedAt())
                .totalRows(draft.getTotalRows())
                .duplicateRows(draft.getDuplicateRows())
                .conflictingRows(draft.getConflictingRows())
                .blocksDetected(draft.getBlocksDetected())
                .year(draft.getYear())
                .month(draft.getMonth())
                .businessDays(draft.getBusinessDays())
                .minDepartureDate(draft.getMinDepartureDate())
                .maxReturnDate(draft.getMaxReturnDate())
                .registeredAt(draft.getRegisteredAt())
                .build();
    }

    private List<TourProcessingDraftRowResponseDto> toRowResponses(UUID draftId, List<TourProcessingDraftTour> tours) {
        if (!tours.isEmpty()) {
            tourRepository.findAllByIdIn(tours.stream().map(TourProcessingDraftTour::getId).filter(Objects::nonNull).toList());
        }
        List<String> duplicatedNumbers = tours.stream()
                .filter(tour -> !tour.isPrimary())
                .map(TourProcessingDraftService::numberKey)
                .distinct()
                .toList();
        Map<String, TourProcessingDraftTour> primaryByNumber = new HashMap<>();
        if (!duplicatedNumbers.isEmpty()) {
            tourRepository.findPrimariesByNumbers(draftId, duplicatedNumbers)
                    .forEach(primary -> primaryByNumber.put(numberKey(primary), primary));
        }

        List<UUID> assignmentIds = new ArrayList<>();
        tours.forEach(tour -> tour.getAssignments().forEach(assignment -> assignmentIds.add(assignment.getId())));
        primaryByNumber.values().forEach(primary -> primary.getAssignments().forEach(assignment -> assignmentIds.add(assignment.getId())));
        Map<UUID, List<TourProcessingDraftDay>> daysByAssignmentId = new HashMap<>();
        List<UUID> knownIds = assignmentIds.stream().filter(Objects::nonNull).distinct().toList();
        if (!knownIds.isEmpty()) {
            dayRepository.findByAssignmentIdIn(knownIds).stream()
                    .sorted(Comparator.comparing(TourProcessingDraftDay::getDate))
                    .forEach(day -> daysByAssignmentId.computeIfAbsent(day.getAssignment().getId(), id -> new ArrayList<>()).add(day));
        }

        Map<UUID, Vehicle> vehiclesById = DriverHoursRules.vehiclesById(vehicleRepository.findAll());
        List<String> keywords = observationKeywords(tours);
        return tours.stream()
                .map(tour -> toRowResponse(tour, tour.isPrimary() ? tour : primaryByNumber.get(numberKey(tour)), daysByAssignmentId, vehiclesById, keywords))
                .toList();
    }

    private List<String> observationKeywords(List<TourProcessingDraftTour> tours) {
        if (tours.isEmpty()) {
            return List.of();
        }
        return observationKeywordRepository.findAll().stream()
                .map(ObservationKeyword::getText)
                .filter(text -> text != null && !text.isBlank())
                .toList();
    }

    private TourProcessingDraftRowResponseDto toRowResponse(
            TourProcessingDraftTour tour,
            TourProcessingDraftTour hoursSource,
            Map<UUID, List<TourProcessingDraftDay>> daysByAssignmentId,
            Map<UUID, Vehicle> vehiclesById,
            List<String> keywords
    ) {
        TourTrips tripSummary = TourTrips.of(tour, vehiclesById);
        List<TourProcessingDraftAssignment> parts = tour.getAssignments();
        TourProcessingDraftAssignment main = tour.primaryAssignment();
        boolean pendingContracting = parts.stream()
                .anyMatch(part -> part.getResourceType() == ResourceType.EXTERNAL
                        && (trimToNull(part.getDriver()) == null || trimToNull(part.getVehicle()) == null));
        return TourProcessingDraftRowResponseDto.builder()
                .id(tour.getId())
                .rowNumber(tour.getRowNumber())
                .driver(main == null ? null : main.getDriver())
                .number(tour.getNumber())
                .vehicle(main == null ? null : main.getVehicle())
                .resourceType(main == null ? ResourceType.INTERNAL : main.getResourceType())
                .borrowedVehicleId(main == null || main.getBorrowedVehicle() == null ? null : main.getBorrowedVehicle().getId())
                .borrowedVehiclePlate(main == null || main.getBorrowedVehicle() == null ? null : main.getBorrowedVehicle().getPlate())
                .pendingContracting(pendingContracting)
                .approvalStatus(tour.getApprovalStatus())
                .passengers(tour.getPassengers())
                .assignments(parts.size() > 1
                        ? parts.stream().map(part -> toAssignmentResponse(part, daysOf(part, hoursSource, daysByAssignmentId))).toList()
                        : null)
                .trips(tripSummary.trips())
                .totalCapacity(tripSummary.totalCapacity())
                .assignedPassengers(tripSummary.assignedPassengers())
                .executingUnit(tour.getExecutingUnit())
                .responsible(tour.getResponsible())
                .destination(tour.getDestination())
                .durationDays(tour.getDurationDays())
                .priority(tour.getPriority())
                .modality(tour.getModality())
                .departureTime(tour.getDepartureTime())
                .returnTime(tour.getReturnTime())
                .departureDate(tour.getDepartureDate())
                .returnDate(tour.getReturnDate())
                .observations(tour.getObservations())
                .observationReviewed(tour.isObservationReviewed())
                .matchedObservationKeywords(ObservationKeywordMatcher.matching(tour.getObservations(), keywords))
                .duplicateGroupSize(tour.getDuplicateGroupSize())
                .duplicateRank(tour.getDuplicateRank())
                .conflicting(tour.isConflicting())
                .days(main == null ? List.of() : daysOf(main, hoursSource, daysByAssignmentId))
                .build();
    }

    private List<TourProcessingDraftRowDayResponseDto> daysOf(
            TourProcessingDraftAssignment assignment,
            TourProcessingDraftTour hoursSource,
            Map<UUID, List<TourProcessingDraftDay>> daysByAssignmentId
    ) {
        TourProcessingDraftAssignment source = assignment;
        if (hoursSource != null && hoursSource != assignment.getTour()) {
            source = hoursSource.getAssignments().stream()
                    .filter(part -> part.getPart().equals(assignment.getPart()))
                    .findFirst()
                    .orElse(null);
        }
        if (source == null || source.getId() == null || hoursSource == null) {
            return List.of();
        }
        return daysByAssignmentId.getOrDefault(source.getId(), List.of()).stream().map(this::toDayResponse).toList();
    }

    private TourProcessingDraftAssignmentDto toAssignmentResponse(TourProcessingDraftAssignment assignment, List<TourProcessingDraftRowDayResponseDto> days) {
        return TourProcessingDraftAssignmentDto.builder()
                .rowId(assignment.getId())
                .driver(assignment.getDriver())
                .vehicle(assignment.getVehicle())
                .resourceType(assignment.getResourceType())
                .borrowedVehicleId(assignment.getBorrowedVehicle() == null ? null : assignment.getBorrowedVehicle().getId())
                .borrowedVehiclePlate(assignment.getBorrowedVehicle() == null ? null : assignment.getBorrowedVehicle().getPlate())
                .passengers(assignment.getPassengers())
                .days(days)
                .build();
    }

    private TourProcessingDraftRowDayResponseDto toDayResponse(TourProcessingDraftDay day) {
        return TourProcessingDraftRowDayResponseDto.builder()
                .id(day.getId())
                .date(day.getDate())
                .dayOfWeek(day.getDate().getDayOfWeek().getDisplayName(TextStyle.FULL, SPANISH))
                .startTime(day.getStartTime())
                .endTime(day.getEndTime())
                .ordinaryHours(day.getOrdinaryHours())
                .surplusHours(day.getSurplusHours())
                .overtimeHours(day.getOvertimeHours())
                .manuallyEdited(day.isManuallyEdited())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
