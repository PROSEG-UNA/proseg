package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.cleaning.CleaningDraftCreateRequestDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftImportResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftRowDayRequestDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftRowDayResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftRowIssueDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftRowRequestDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftRowResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDriverAutoAssignResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningImportColumnDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningImportSchemaDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningVehicleAutoAssignResponseDto;
import com.proseg.msvc_transport.dto.response.OvertimeRequestReportDto;
import com.proseg.msvc_transport.entity.BorrowedVehicle;
import com.proseg.msvc_transport.entity.CleaningDraft;
import com.proseg.msvc_transport.entity.CleaningDraftRow;
import com.proseg.msvc_transport.entity.CleaningDraftRowDay;
import com.proseg.msvc_transport.entity.CleaningDraftStatus;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.importer.TransportImportField;
import com.proseg.msvc_transport.repository.BorrowedVehicleRepository;
import com.proseg.msvc_transport.repository.CleaningDraftRepository;
import com.proseg.msvc_transport.repository.CleaningDraftRowDayRepository;
import com.proseg.msvc_transport.repository.CleaningDraftRowRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.VehicleRepository;
import com.proseg.msvc_transport.security.CurrentUserResolver;
import com.proseg.msvc_transport.service.driverhours.DriverAutoAssigner;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import com.proseg.msvc_transport.service.driverhours.VehicleAutoAssigner;
import com.proseg.msvc_transport.specification.GenericSpecifications;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CleaningDraftService {

    private static final int MAX_ROWS = 5000;
    private static final String DRAFT_RESOURCE = "Borrador de depuración";
    private static final String ROW_RESOURCE = "Fila del borrador";
    private static final String DAY_RESOURCE = "Día de la gira";
    private static final Locale SPANISH = Locale.forLanguageTag("es");
    private static final List<String> FIELD_ORDER = Arrays.stream(TransportImportField.values())
            .map(TransportImportField::getAttribute)
            .toList();

    private final CleaningDraftRepository draftRepository;
    private final CleaningDraftRowRepository rowRepository;
    private final CleaningDraftRowDayRepository dayRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final BorrowedVehicleRepository borrowedVehicleRepository;
    private final DriverMonthService driverMonthService;
    private final OvertimeRequestReportService overtimeRequestReportService;
    private final CurrentUserResolver currentUserResolver;
    private final Validator validator;

    public CleaningImportSchemaDto getImportSchema() {
        List<CleaningImportColumnDto> columns = Arrays.stream(TransportImportField.values())
                .map(field -> CleaningImportColumnDto.builder()
                        .attribute(field.getAttribute())
                        .names(field.getNames())
                        .type(field.getType().name())
                        .required(field.isRequired())
                        .build())
                .toList();
        return CleaningImportSchemaDto.builder().columns(columns).build();
    }

    @Transactional
    public CleaningDraftImportResponseDto create(CleaningDraftCreateRequestDto request) {
        ensureMonthIsAvailable(request.getYear(), request.getMonth());
        List<CleaningDraftRowRequestDto> rows = request.getRows() == null ? List.of() : request.getRows();
        if (rows.isEmpty()) {
            throw TransportException.badRequest("CLEANING_DRAFT_EMPTY", "No hay filas para importar");
        }
        if (rows.size() > MAX_ROWS) {
            throw TransportException.badRequest(
                    "CLEANING_TOO_MANY_ROWS",
                    "La importación supera el máximo de " + MAX_ROWS + " filas"
            );
        }

        List<CleaningDraftRowIssueDto> errors = validateRows(rows);
        if (!errors.isEmpty()) {
            return CleaningDraftImportResponseDto.builder()
                    .received(rows.size())
                    .errors(errors)
                    .build();
        }

        CleaningDraft draft = CleaningDraft.builder()
                .fileName(request.getFileName().trim())
                .fileType(request.getFileType().trim().toUpperCase(Locale.ROOT))
                .status(CleaningDraftStatus.DRAFT)
                .importedBy(currentUserResolver.resolve())
                .importedAt(LocalDateTime.now())
                .totalRows(rows.size())
                .blocksDetected(request.getBlocksDetected() == null ? 0 : request.getBlocksDetected())
                .year(request.getYear())
                .month(request.getMonth())
                .businessDays(request.getBusinessDays())
                .build();

        List<CleaningDraftRow> entities = rows.stream()
                .map(row -> toEntity(row, draft))
                .toList();
        markDuplicates(entities);
        summarize(draft, entities);

        CleaningDraft saved = draftRepository.save(draft);
        rowRepository.saveAll(entities);
        driverMonthService.syncWithDraft(saved, entities);

        return CleaningDraftImportResponseDto.builder()
                .draftId(saved.getId())
                .received(rows.size())
                .duplicateRows(saved.getDuplicateRows())
                .conflictingRows(saved.getConflictingRows())
                .errors(List.of())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<CleaningDraftResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<CleaningDraft> spec = Specification
                .where(GenericSpecifications.withSearch(CleaningDraft.class, search))
                .and(GenericSpecifications.withColumnFilters(CleaningDraft.class, filters));
        Sort sort = GenericSpecifications.sanitizeSort(CleaningDraft.class, pageable.getSort());
        Sort effectiveSort = sort.isUnsorted() ? Sort.by(Sort.Order.desc("importedAt")) : sort;
        return draftRepository.findAll(spec, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), effectiveSort))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CleaningDraftResponseDto findById(UUID id) {
        return toResponse(getDraft(id));
    }

    @Transactional(readOnly = true)
    public Page<CleaningDraftRowResponseDto> findRows(UUID draftId, String search, Map<String, String> filters, Pageable pageable) {
        getDraft(draftId);
        Specification<CleaningDraftRow> belongsToDraft = (root, query, cb) -> cb.equal(root.get("draft").get("id"), draftId);
        Specification<CleaningDraftRow> spec = Specification
                .where(belongsToDraft)
                .and(GenericSpecifications.withSearch(CleaningDraftRow.class, search))
                .and(GenericSpecifications.withColumnFilters(CleaningDraftRow.class, filters));
        Sort sort = GenericSpecifications.sanitizeSort(CleaningDraftRow.class, pageable.getSort());
        Sort effectiveSort = sort.isUnsorted() ? Sort.by(Sort.Order.asc("rowNumber")) : sort;
        return rowRepository.findAll(spec, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), effectiveSort))
                .map(this::toRowResponse);
    }

    @Transactional(readOnly = true)
    public OvertimeRequestReportDto getSchedule(UUID draftId) {
        return overtimeRequestReportService.buildSchedule(getDraft(draftId));
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
    public CleaningDraftRowResponseDto createRow(UUID draftId, CleaningDraftRowRequestDto request) {
        CleaningDraft draft = getEditableDraft(draftId);
        List<CleaningDraftRow> rows = new ArrayList<>(rowRepository.findAllByDraftIdOrderByRowNumberAsc(draftId));
        if (rows.size() >= MAX_ROWS) {
            throw TransportException.badRequest(
                    "CLEANING_TOO_MANY_ROWS",
                    "El borrador supera el máximo de " + MAX_ROWS + " filas"
            );
        }

        request.setRowNumber(rows.stream().mapToInt(CleaningDraftRow::getRowNumber).max().orElse(0) + 1);
        List<String> reasons = validateRow(request);
        if (!reasons.isEmpty()) {
            throw TransportException.unprocessable("CLEANING_DRAFT_ROW_INVALID", String.join(". ", reasons));
        }

        CleaningDraftRow row = toEntity(request, draft);
        rows.add(row);
        validateBorrowedVehicleAvailability(row, rows);
        markDuplicates(rows);
        summarize(draft, rows);
        draft.setTotalRows(rows.size());
        rowRepository.save(row);
        driverMonthService.syncWithDraft(draft, rows);
        return toRowResponse(row);
    }

    @Transactional
    public CleaningDraftRowResponseDto updateRow(UUID draftId, UUID rowId, CleaningDraftRowRequestDto request) {
        CleaningDraft draft = getEditableDraft(draftId);
        CleaningDraftRow row = rowRepository.findByIdAndDraftId(rowId, draftId)
                .orElseThrow(() -> TransportException.notFound(ROW_RESOURCE, rowId.toString()));

        List<String> reasons = validateRow(request);
        if (!reasons.isEmpty()) {
            throw TransportException.unprocessable("CLEANING_DRAFT_ROW_INVALID", String.join(". ", reasons));
        }

        List<CleaningDraftRow> rows = rowRepository.findAllByDraftIdOrderByRowNumberAsc(draftId);
        validateAssignedDriverCanAttend(draft, row, request, rows);

        applyValues(row, request);
        validateBorrowedVehicleAvailability(row, rows);
        markDuplicates(rows);
        summarize(draft, rows);
        driverMonthService.syncWithDraft(draft, rows);
        return toRowResponse(row);
    }

    private void validateAssignedDriverCanAttend(
            CleaningDraft draft,
            CleaningDraftRow row,
            CleaningDraftRowRequestDto request,
            List<CleaningDraftRow> rows
    ) {
        boolean durationIncreased = request.getDurationDays() > row.getDurationDays();
        boolean periodDefined = draft.getYear() != null && draft.getMonth() != null && draft.getBusinessDays() != null;
        boolean sameDriver = DriverHoursRules.driverKey(request.getDriver()).equals(DriverHoursRules.driverKey(row.getDriver()));
        if (!durationIncreased || !periodDefined || !sameDriver || !DriverHoursRules.isPrimary(row)) {
            return;
        }

        List<Driver> catalog = driverRepository.findAll();
        Driver driver = DriverHoursRules.catalogByKey(catalog).get(DriverHoursRules.driverKey(row.getDriver()));
        if (driver == null) {
            return;
        }

        List<CleaningDraftRow> otherRows = rows.stream()
                .filter(other -> !other.getId().equals(row.getId()))
                .toList();
        new DriverAutoAssigner(draft.getYear(), draft.getMonth(), draft.getBusinessDays(), catalog, otherRows)
                .rejectionFor(driver, toEntity(request, draft))
                .ifPresent(rejection -> {
                    throw TransportException.unprocessable(
                            "CLEANING_DRAFT_ROW_DRIVER_UNAVAILABLE",
                            "No se puede aumentar la duración de la gira porque el chofer asignado no puede atenderla.\n\n"
                                    + rejection.message()
                    );
                });
    }

    @Transactional
    public CleaningDraftRowDayResponseDto updateRowDay(UUID draftId, UUID rowId, UUID dayId, CleaningDraftRowDayRequestDto request) {
        CleaningDraft draft = getEditableDraft(draftId);
        CleaningDraftRow row = rowRepository.findByIdAndDraftId(rowId, draftId)
                .orElseThrow(() -> TransportException.notFound(ROW_RESOURCE, rowId.toString()));
        CleaningDraftRowDay day = dayRepository.findByIdAndRowId(dayId, rowId)
                .orElseThrow(() -> TransportException.notFound(DAY_RESOURCE, dayId.toString()));

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw TransportException.unprocessable(
                    "CLEANING_DRAFT_ROW_DAY_INVALID",
                    "La hora de fin debe ser posterior a la hora de inicio"
            );
        }
        if (request.getOrdinaryHours() > DriverHoursRules.MAX_ORDINARY_HOURS_PER_DAY + DriverHoursRules.HOURS_TOLERANCE) {
            throw TransportException.unprocessable(
                    "CLEANING_DRAFT_ROW_DAY_ORDINARY_LIMIT",
                    "Las horas ordinarias no pueden superar las " + (int) DriverHoursRules.MAX_ORDINARY_HOURS_PER_DAY + " horas por día"
            );
        }

        List<CleaningDraftRow> rows = rowRepository.findAllByDraftIdOrderByRowNumberAsc(draftId);
        validateDriverMonthlyCaps(draft, rows, row.getDriver(), dayId, request);

        day.setStartTime(request.getStartTime());
        day.setEndTime(request.getEndTime());
        day.setOrdinaryHours(request.getOrdinaryHours());
        day.setSurplusHours(request.getSurplusHours());
        day.setOvertimeHours(request.getOvertimeHours());
        day.setManuallyEdited(true);
        CleaningDraftRowDay saved = dayRepository.save(day);

        driverMonthService.recomputeDriverMonth(draft, rows, row.getDriver());

        return toDayResponse(saved);
    }

    private void validateDriverMonthlyCaps(
            CleaningDraft draft,
            List<CleaningDraftRow> rows,
            String driverName,
            UUID dayId,
            CleaningDraftRowDayRequestDto request
    ) {
        String key = DriverHoursRules.driverKey(driverName);
        List<UUID> driverRowIds = rows.stream()
                .filter(DriverHoursRules::isPrimary)
                .filter(candidate -> DriverHoursRules.driverKey(candidate.getDriver()).equals(key))
                .map(CleaningDraftRow::getId)
                .toList();
        double otherSurplus = 0;
        double otherOvertime = 0;
        for (CleaningDraftRowDay other : dayRepository.findByRowIdIn(driverRowIds)) {
            if (other.getId().equals(dayId)) continue;
            otherSurplus += other.getSurplusHours();
            otherOvertime += other.getOvertimeHours();
        }

        double surplusCap = DriverHoursRules.surplusCap(draft.getBusinessDays());
        double newSurplusTotal = otherSurplus + request.getSurplusHours();
        double newOvertimeTotal = otherOvertime + request.getOvertimeHours();

        if (newSurplusTotal > surplusCap + DriverHoursRules.HOURS_TOLERANCE) {
            throw TransportException.unprocessable(
                    "CLEANING_DRAFT_ROW_DAY_SURPLUS_CAP_EXCEEDED",
                    "Las horas sobresueldo acumuladas del chofer en el mes superarían el tope de " + surplusCap + " horas"
            );
        }
        if (newOvertimeTotal > DriverHoursRules.MONTHLY_OVERTIME_CAP + DriverHoursRules.HOURS_TOLERANCE) {
            throw TransportException.unprocessable(
                    "CLEANING_DRAFT_ROW_DAY_OVERTIME_CAP_EXCEEDED",
                    "Las horas extra acumuladas del chofer en el mes superarían el tope de " + (int) DriverHoursRules.MONTHLY_OVERTIME_CAP + " horas"
            );
        }

        boolean overtimeBelowSurplusThreshold = request.getOvertimeHours() > DriverHoursRules.HOURS_TOLERANCE
                && newSurplusTotal < surplusCap * DriverHoursRules.SURPLUS_CUTOFF_RATIO - DriverHoursRules.HOURS_TOLERANCE;
        if (overtimeBelowSurplusThreshold && !request.isConfirmOvertimeBelowSurplusThreshold()) {
            throw TransportException.conflict(
                    "CLEANING_DRAFT_ROW_DAY_OVERTIME_NEEDS_CONFIRMATION",
                    "El chofer todavía no alcanza el 95% de su tope de sobresueldo del mes"
            );
        }
    }

    @Transactional
    public void deleteRow(UUID draftId, UUID rowId) {
        CleaningDraft draft = getEditableDraft(draftId);
        CleaningDraftRow row = rowRepository.findByIdAndDraftId(rowId, draftId)
                .orElseThrow(() -> TransportException.notFound(ROW_RESOURCE, rowId.toString()));

        rowRepository.delete(row);
        rowRepository.flush();
        List<CleaningDraftRow> rows = rowRepository.findAllByDraftIdOrderByRowNumberAsc(draftId);
        markDuplicates(rows);
        summarize(draft, rows);
        draft.setTotalRows(rows.size());
        driverMonthService.syncWithDraft(draft, rows);
    }

    @Transactional
    public int deleteDuplicateRows(UUID draftId) {
        CleaningDraft draft = getEditableDraft(draftId);
        List<CleaningDraftRow> rows = rowRepository.findAllByDraftIdOrderByRowNumberAsc(draftId);
        if (rows.stream().anyMatch(CleaningDraftRow::isConflicting)) {
            throw TransportException.conflict(
                    "CLEANING_DRAFT_HAS_CONFLICTS",
                    "No se pueden eliminar las filas repetidas mientras haya un Numero repetido con datos distintos"
            );
        }

        List<CleaningDraftRow> duplicates = rows.stream()
                .filter(row -> row.getDuplicateRank() > 1)
                .toList();
        List<CleaningDraftRow> remaining = rows.stream()
                .filter(row -> row.getDuplicateRank() == 1)
                .toList();

        rowRepository.deleteAll(duplicates);
        markDuplicates(remaining);
        summarize(draft, remaining);
        draft.setTotalRows(remaining.size());
        driverMonthService.syncWithDraft(draft, remaining);
        return duplicates.size();
    }

    @Transactional
    public CleaningDriverAutoAssignResponseDto autoAssignDrivers(UUID draftId) {
        CleaningDraft draft = getEditableDraft(draftId);
        if (draft.getYear() == null || draft.getMonth() == null || draft.getBusinessDays() == null) {
            throw TransportException.unprocessable(
                    "CLEANING_DRAFT_WITHOUT_PERIOD",
                    "El borrador no tiene mes ni días hábiles definidos, no se pueden autoasignar choferes"
            );
        }

        List<CleaningDraftRow> rows = rowRepository.findAllByDraftIdOrderByRowNumberAsc(draftId);
        List<Driver> catalog = driverRepository.findAll();
        Map<String, Driver> catalogByKey = DriverHoursRules.catalogByKey(catalog);

        int clearedTours = 0;
        for (CleaningDraftRow row : rows) {
            if (row.getResourceType() == ResourceType.EXTERNAL) {
                continue;
            }
            String driverKey = DriverHoursRules.driverKey(row.getDriver());
            if (!driverKey.isEmpty() && !catalogByKey.containsKey(driverKey)) {
                row.setDriver(null);
                clearedTours += DriverHoursRules.isPrimary(row) ? 1 : 0;
            }
        }

        List<CleaningDraftRow> pendingTours = rows.stream()
                .filter(DriverHoursRules::isPrimary)
                .filter(row -> row.getResourceType() != ResourceType.EXTERNAL)
                .filter(row -> row.getDriver() == null)
                .toList();
        Map<CleaningDraftRow, Driver> assignments = new DriverAutoAssigner(
                draft.getYear(),
                draft.getMonth(),
                draft.getBusinessDays(),
                catalog,
                vehicleRepository.findAll(),
                rows
        ).assign(pendingTours);

        Map<String, String> driverNameByNumber = new HashMap<>();
        assignments.forEach((tour, driver) -> driverNameByNumber.put(numberKey(tour), driver == null ? null : driver.getName()));
        rows.stream()
                .filter(row -> row.getResourceType() != ResourceType.EXTERNAL)
                .filter(row -> driverNameByNumber.containsKey(numberKey(row)))
                .forEach(row -> row.setDriver(driverNameByNumber.get(numberKey(row))));

        markDuplicates(rows);
        summarize(draft, rows);
        driverMonthService.syncWithDraft(draft, rows);

        return CleaningDriverAutoAssignResponseDto.builder()
                .clearedTours(clearedTours)
                .assignedTours((int) assignments.values().stream().filter(java.util.Objects::nonNull).count())
                .unassignedTours((int) rows.stream()
                        .filter(DriverHoursRules::isPrimary)
                        .filter(row -> row.getResourceType() != ResourceType.EXTERNAL)
                        .filter(row -> row.getDriver() == null)
                        .count())
                .build();
    }

    @Transactional
    public CleaningVehicleAutoAssignResponseDto autoAssignVehicles(UUID draftId) {
        CleaningDraft draft = getEditableDraft(draftId);
        List<CleaningDraftRow> rows = rowRepository.findAllByDraftIdOrderByRowNumberAsc(draftId);
        List<Vehicle> vehicles = vehicleRepository.findAll();
        Map<String, Vehicle> vehicleCatalogByPlate = DriverHoursRules.vehicleCatalogByPlate(vehicles);

        int clearedTours = 0;
        for (CleaningDraftRow row : rows) {
            if (row.getResourceType() != ResourceType.INTERNAL) {
                continue;
            }
            if (row.getVehicle() != null && DriverHoursRules.catalogVehicle(row.getVehicle(), vehicleCatalogByPlate) == null) {
                row.setVehicle(null);
                clearedTours += DriverHoursRules.isPrimary(row) ? 1 : 0;
            }
        }

        List<CleaningDraftRow> pendingTours = rows.stream()
                .filter(DriverHoursRules::isPrimary)
                .filter(row -> row.getResourceType() == ResourceType.INTERNAL)
                .filter(row -> row.getVehicle() == null)
                .toList();
        Map<CleaningDraftRow, Vehicle> assignments = new VehicleAutoAssigner(
                vehicles,
                driverRepository.findAll(),
                rows
        ).assign(pendingTours);

        Map<String, String> plateByNumber = new HashMap<>();
        assignments.forEach((tour, vehicle) -> plateByNumber.put(numberKey(tour), vehicle.getPlate()));
        rows.stream()
                .filter(row -> row.getVehicle() == null)
                .forEach(row -> row.setVehicle(plateByNumber.get(numberKey(row))));

        markDuplicates(rows);
        summarize(draft, rows);

        return CleaningVehicleAutoAssignResponseDto.builder()
                .clearedTours(clearedTours)
                .assignedTours(assignments.size())
                .unassignedTours(pendingTours.size() - assignments.size())
                .build();
    }

    @Transactional
    public void delete(UUID id) {
        CleaningDraft draft = getDraft(id);
        if (draft.getStatus() != CleaningDraftStatus.DRAFT) {
            throw TransportException.conflict(
                    "CLEANING_DRAFT_ALREADY_REGISTERED",
                    "No se puede eliminar un borrador que ya fue registrado"
            );
        }
        draftRepository.delete(draft);
        driverMonthService.syncWithDraft(draft, List.of());
    }

    private CleaningDraft getDraft(UUID id) {
        return draftRepository.findById(id)
                .orElseThrow(() -> TransportException.notFound(DRAFT_RESOURCE, id.toString()));
    }

    private CleaningDraft getEditableDraft(UUID id) {
        CleaningDraft draft = draftRepository.findByIdForUpdate(id)
                .orElseThrow(() -> TransportException.notFound(DRAFT_RESOURCE, id.toString()));
        if (draft.getStatus() != CleaningDraftStatus.DRAFT) {
            throw TransportException.conflict(
                    "CLEANING_DRAFT_ALREADY_REGISTERED",
                    "No se puede editar un borrador que ya fue registrado"
            );
        }
        return draft;
    }

    private void ensureMonthIsAvailable(Integer year, Integer month) {
        if (draftRepository.existsByYearAndMonthAndStatus(year, month, CleaningDraftStatus.REGISTERED)) {
            throw TransportException.conflict(
                    "CLEANING_MONTH_ALREADY_REGISTERED",
                    "La depuración de " + monthLabel(year, month) + " ya fue registrada. No se puede importar otra del mismo mes"
            );
        }
        if (draftRepository.existsByYearAndMonthAndStatus(year, month, CleaningDraftStatus.DRAFT)) {
            throw TransportException.conflict(
                    "CLEANING_DRAFT_MONTH_EXISTS",
                    "Ya existe un borrador de depuración de " + monthLabel(year, month)
                            + " pendiente de registrar. Regístralo o elimínalo antes de importar otro del mismo mes"
            );
        }
    }

    private String monthLabel(Integer year, Integer month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, SPANISH) + " de " + year;
    }

    private List<CleaningDraftRowIssueDto> validateRows(List<CleaningDraftRowRequestDto> rows) {
        List<CleaningDraftRowIssueDto> errors = new ArrayList<>();
        for (CleaningDraftRowRequestDto row : rows) {
            List<String> reasons = validateRow(row);
            reasons.forEach(reason -> errors.add(CleaningDraftRowIssueDto.builder()
                    .row(row.getRowNumber())
                    .reason(reason)
                    .build()));
        }
        return errors;
    }

    private List<String> validateRow(CleaningDraftRowRequestDto row) {
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
        if (row.getResourceType() == ResourceType.BORROWED_VEHICLE) {
            if (row.getBorrowedVehicleId() == null) {
                return List.of("Selecciona el vehículo prestado para esta gira");
            }
            BorrowedVehicle borrowedVehicle = borrowedVehicleRepository.findById(row.getBorrowedVehicleId()).orElse(null);
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
        return borrowedVehicleRepository.findById(borrowedVehicleId)
                .orElseThrow(() -> TransportException.notFound("Vehículo prestado", borrowedVehicleId.toString()));
    }

    private void validateBorrowedVehicleAvailability(CleaningDraftRow row, List<CleaningDraftRow> rows) {
        if (row.getResourceType() != ResourceType.BORROWED_VEHICLE || row.getBorrowedVehicle() == null) {
            return;
        }
        boolean overlaps = rows.stream()
                .filter(other -> other != row)
                .filter(DriverHoursRules::isPrimary)
                .filter(other -> other.getResourceType() == ResourceType.BORROWED_VEHICLE)
                .filter(other -> other.getBorrowedVehicle() != null && other.getBorrowedVehicle().getId().equals(row.getBorrowedVehicle().getId()))
                .anyMatch(other -> DriverHoursRules.toursOverlap(other, row));
        if (overlaps) {
            throw TransportException.unprocessable(
                    "CLEANING_DRAFT_ROW_BORROWED_VEHICLE_OVERLAP",
                    "El vehículo prestado \"" + row.getBorrowedVehicle().getPlate() + "\" ya está asignado a otra gira en ese rango de fechas"
            );
        }
    }

    private int violationOrder(ConstraintViolation<CleaningDraftRowRequestDto> violation) {
        int index = FIELD_ORDER.indexOf(violation.getPropertyPath().toString());
        return index < 0 ? -1 : index;
    }

    private String numberKey(CleaningDraftRow row) {
        return row.getNumber().toLowerCase(Locale.ROOT);
    }

    private void markDuplicates(List<CleaningDraftRow> rows) {
        Map<String, List<CleaningDraftRow>> groups = new LinkedHashMap<>();
        for (CleaningDraftRow row : rows) {
            groups.computeIfAbsent(numberKey(row), key -> new ArrayList<>()).add(row);
        }
        for (List<CleaningDraftRow> group : groups.values()) {
            CleaningDraftRow first = group.get(0);
            for (int i = 0; i < group.size(); i++) {
                CleaningDraftRow row = group.get(i);
                row.setDuplicateRank(i + 1);
                row.setDuplicateGroupSize(group.size());
                row.setConflicting(i > 0 && !contentOf(row).equals(contentOf(first)));
            }
        }
    }

    private List<Object> contentOf(CleaningDraftRow row) {
        return Arrays.asList(
                row.getDriver(),
                row.getNumber(),
                row.getVehicle(),
                row.getResourceType(),
                row.getBorrowedVehicle() == null ? null : row.getBorrowedVehicle().getId(),
                row.getPassengers(),
                row.getExecutingUnit(),
                row.getResponsible(),
                row.getDestination(),
                row.getDurationDays(),
                row.getPriority(),
                row.getModality(),
                row.getDepartureTime(),
                row.getReturnTime(),
                row.getDepartureDate(),
                row.getReturnDate(),
                row.getObservations()
        );
    }

    private void summarize(CleaningDraft draft, List<CleaningDraftRow> rows) {
        draft.setDuplicateRows((int) rows.stream().filter(row -> row.getDuplicateRank() > 1).count());
        draft.setConflictingRows((int) rows.stream().filter(CleaningDraftRow::isConflicting).count());
        draft.setMinDepartureDate(rows.stream()
                .map(CleaningDraftRow::getDepartureDate)
                .min(LocalDate::compareTo)
                .orElse(null));
        draft.setMaxReturnDate(rows.stream()
                .map(CleaningDraftRow::getReturnDate)
                .max(LocalDate::compareTo)
                .orElse(null));
    }

    private CleaningDraftRow toEntity(CleaningDraftRowRequestDto request, CleaningDraft draft) {
        CleaningDraftRow row = CleaningDraftRow.builder()
                .draft(draft)
                .rowNumber(request.getRowNumber())
                .build();
        applyValues(row, request);
        return row;
    }

    private void applyValues(CleaningDraftRow row, CleaningDraftRowRequestDto request) {
        ResourceType resourceType = request.getResourceType() == null ? ResourceType.INTERNAL : request.getResourceType();
        row.setResourceType(resourceType);
        row.setDriver(trimToNull(request.getDriver()));
        row.setNumber(request.getNumber().trim());
        if (resourceType == ResourceType.BORROWED_VEHICLE) {
            row.setVehicle(null);
            row.setBorrowedVehicle(resolveBorrowedVehicle(request.getBorrowedVehicleId()));
        } else {
            row.setVehicle(trimToNull(request.getVehicle()));
            row.setBorrowedVehicle(null);
        }
        row.setPassengers(request.getPassengers());
        row.setExecutingUnit(request.getExecutingUnit().trim());
        row.setResponsible(request.getResponsible().trim());
        row.setDestination(request.getDestination().trim());
        row.setDurationDays(request.getDurationDays());
        row.setPriority(request.getPriority());
        row.setModality(trimToNull(request.getModality()));
        row.setDepartureTime(request.getDepartureTime());
        row.setReturnTime(request.getReturnTime());
        row.setDepartureDate(request.getDepartureDate());
        row.setReturnDate(request.getReturnDate());
        row.setObservations(trimToNull(request.getObservations()));
    }

    private CleaningDraftResponseDto toResponse(CleaningDraft draft) {
        return CleaningDraftResponseDto.builder()
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
                .cleaningExecutionId(draft.getCleaningExecutionId())
                .build();
    }

    private CleaningDraftRowResponseDto toRowResponse(CleaningDraftRow row) {
        boolean pendingContracting = row.getResourceType() == ResourceType.EXTERNAL
                && (trimToNull(row.getDriver()) == null || trimToNull(row.getVehicle()) == null);
        return CleaningDraftRowResponseDto.builder()
                .id(row.getId())
                .rowNumber(row.getRowNumber())
                .driver(row.getDriver())
                .number(row.getNumber())
                .vehicle(row.getVehicle())
                .resourceType(row.getResourceType())
                .borrowedVehicleId(row.getBorrowedVehicle() == null ? null : row.getBorrowedVehicle().getId())
                .borrowedVehiclePlate(row.getBorrowedVehicle() == null ? null : row.getBorrowedVehicle().getPlate())
                .pendingContracting(pendingContracting)
                .passengers(row.getPassengers())
                .executingUnit(row.getExecutingUnit())
                .responsible(row.getResponsible())
                .destination(row.getDestination())
                .durationDays(row.getDurationDays())
                .priority(row.getPriority())
                .modality(row.getModality())
                .departureTime(row.getDepartureTime())
                .returnTime(row.getReturnTime())
                .departureDate(row.getDepartureDate())
                .returnDate(row.getReturnDate())
                .observations(row.getObservations())
                .duplicateGroupSize(row.getDuplicateGroupSize())
                .duplicateRank(row.getDuplicateRank())
                .conflicting(row.isConflicting())
                .days(dayRepository.findByRowIdOrderByDateAsc(row.getId()).stream().map(this::toDayResponse).toList())
                .build();
    }

    private CleaningDraftRowDayResponseDto toDayResponse(CleaningDraftRowDay day) {
        return CleaningDraftRowDayResponseDto.builder()
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
