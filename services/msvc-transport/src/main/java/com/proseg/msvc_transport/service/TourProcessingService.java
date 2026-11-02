package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingRegisterResponseDto;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftTourRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.VehicleRepository;
import com.proseg.msvc_transport.security.CurrentUserResolver;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TourProcessingService {

    private static final String IMPORTED_STATUS = "ACTIVE";
    private static final String DRAFT_RESOURCE = "Borrador de depuración";
    private static final int MAX_CONFLICTS_IN_MESSAGE = 10;
    private static final DateTimeFormatter TOUR_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final TourProcessingDraftRepository draftRepository;
    private final DraftTourStore draftTourRepository;
    private final TourProcessingHistoryService tourProcessingHistoryService;
    private final DraftHoursService draftHoursService;
    private final CurrentUserResolver currentUserResolver;

    @Transactional
    public TourProcessingRegisterResponseDto registerDraft(UUID draftId, boolean replaceExistingInRange) {
        TourProcessingDraft draft = draftRepository.findByIdForUpdate(draftId)
                .orElseThrow(() -> TransportException.notFound(DRAFT_RESOURCE, draftId.toString()));
        if (draft.getStatus() != TourProcessingDraftStatus.DRAFT) {
            throw TransportException.conflict("TOUR_PROCESSING_DRAFT_ALREADY_REGISTERED", "El borrador ya fue registrado");
        }

        List<TourProcessingDraftTour> rows = draftTourRepository.findAllByDraftIdOrderByRowNumberAsc(draftId);
        List<TourProcessingDraftTour> approvedRows = rows.stream()
                .filter(TourProcessingDraftTour::isApproved)
                .toList();
        ensureNoConflicts(rows);
        ensureNoPendingContracting(approvedRows);
        ensureValidDistributions(approvedRows);

        List<TourProcessingDraftTour> primaryRows = approvedRows.stream()
                .filter(row -> row.getDuplicateRank() == 1)
                .toList();
        LocalDateTime processStartedAt = LocalDateTime.now();
        String executedBy = currentUserResolver.resolve();
        TourProcessingHistoryService.TourProcessingAuditData audit = new TourProcessingHistoryService.TourProcessingAuditData(
                draft.getFileName(),
                draft.getFileType(),
                draft.getTotalRows(),
                draft.getTotalRows(),
                0,
                draft.getDuplicateRows(),
                (int) primaryRows.stream().filter(this::isLogicalRoot).count(),
                false
        );
        List<TourProcessingHistoryService.TourProcessingDetailDraft> detailDrafts = new ArrayList<>();
        appendSkippedDuplicates(rows, detailDrafts);
        appendSkippedNotApproved(rows, detailDrafts);

        try {
            RegisterComputationResult result = registerRowsInternal(primaryRows, replaceExistingInRange, detailDrafts);
            LocalDateTime processFinishedAt = LocalDateTime.now();
            UUID executionId = tourProcessingHistoryService.recordSuccess(
                    audit,
                    result.response(),
                    executedBy,
                    processStartedAt,
                    processFinishedAt,
                    result.replacedRangeStart(),
                    result.replacedRangeEnd(),
                    detailDrafts
            );
            result.response().setTourProcessingExecutionId(executionId);
            draft.setStatus(TourProcessingDraftStatus.REGISTERED);
            draft.setRegisteredAt(processFinishedAt);
            draft.setTourProcessingExecutionId(executionId);
            draftHoursService.syncAll(draft, rows.stream().flatMap(row -> row.getAssignments().stream()).toList());
            return result.response();
        } catch (RuntimeException ex) {
            try {
                tourProcessingHistoryService.recordFailure(
                        audit,
                        executedBy,
                        processStartedAt,
                        LocalDateTime.now(),
                        ex,
                        detailDrafts
                );
            } catch (RuntimeException historyEx) {
                ex.addSuppressed(historyEx);
                log.error("No se pudo registrar el historial de depuración fallida", historyEx);
            }
            throw ex;
        }
    }

    private void ensureNoConflicts(List<TourProcessingDraftTour> rows) {
        Set<String> conflictingNumbers = rows.stream()
                .filter(TourProcessingDraftTour::isConflicting)
                .map(this::numberKey)
                .collect(Collectors.toSet());
        if (conflictingNumbers.isEmpty()) {
            return;
        }

        Map<String, List<Integer>> rowNumbersByNumber = new LinkedHashMap<>();
        for (TourProcessingDraftTour row : rows) {
            if (conflictingNumbers.contains(numberKey(row))) {
                rowNumbersByNumber.computeIfAbsent(row.getNumber(), key -> new ArrayList<>()).add(row.getRowNumber());
            }
        }
        String detail = rowNumbersByNumber.entrySet().stream()
                .limit(MAX_CONFLICTS_IN_MESSAGE)
                .map(entry -> entry.getKey() + " (filas " + entry.getValue().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", ")) + ")")
                .collect(Collectors.joining("; "));
        String remaining = rowNumbersByNumber.size() > MAX_CONFLICTS_IN_MESSAGE
                ? " y " + (rowNumbersByNumber.size() - MAX_CONFLICTS_IN_MESSAGE) + " más"
                : "";
        throw TransportException.unprocessable(
                "TOUR_PROCESSING_DRAFT_CONFLICTS",
                "Hay números de gira repetidos con datos distintos: " + detail + remaining
        );
    }

    private void ensureNoPendingContracting(List<TourProcessingDraftTour> rows) {
        boolean pending = rows.stream().filter(TourProcessingDraftTour::isPrimary)
                .flatMap(row -> row.getAssignments().stream())
                .anyMatch(part -> part.getResourceType() == ResourceType.EXTERNAL
                        && (isBlank(part.getDriver()) || isBlank(part.getVehicle())));
        if (pending) {
            throw TransportException.unprocessable("TOUR_PROCESSING_DRAFT_PENDING_CONTRACTING",
                    "Hay giras de contratación externa pendientes de contratar (falta el chofer/empresa o la placa)");
        }
    }

    private void ensureValidDistributions(List<TourProcessingDraftTour> rows) {
        for (TourProcessingDraftTour row : rows) {
            if (row.getAssignments().isEmpty()
                    || row.getAssignments().stream().anyMatch(part -> part.getPassengers() == null || part.getPassengers() < 1)
                    || row.getAssignments().stream().mapToLong(TourProcessingDraftAssignment::getPassengers).sum() != row.getPassengers()) {
                throw TransportException.unprocessable("TOUR_PROCESSING_DISTRIBUTION_TOTAL",
                        "La distribución de pasajeros de una gira no coincide con su total");
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void appendSkippedDuplicates(
            List<TourProcessingDraftTour> rows,
            List<TourProcessingHistoryService.TourProcessingDetailDraft> detailDrafts
    ) {
        Map<String, Integer> firstRowByNumber = new HashMap<>();
        for (TourProcessingDraftTour row : rows) {
            Integer firstRow = firstRowByNumber.computeIfAbsent(numberKey(row), key -> row.getRowNumber());
            if (row.getDuplicateRank() <= 1) {
                continue;
            }
            Map<String, String> values = toDetailValues(row);
            detailDrafts.add(buildDetail(
                    row.getRowNumber(),
                    values,
                    values,
                    "ROW",
                    "IGNORED_DUPLICATE",
                    "SKIPPED",
                    "Fila repetida del Numero " + row.getNumber() + " (primera aparición en la fila " + firstRow + ")",
                    null,
                    null
            ));
        }
    }

    private void appendSkippedNotApproved(
            List<TourProcessingDraftTour> rows,
            List<TourProcessingHistoryService.TourProcessingDetailDraft> detailDrafts
    ) {
        for (TourProcessingDraftTour row : rows) {
            if (!isLogicalRoot(row) || row.getDuplicateRank() > 1 || row.isApproved()) {
                continue;
            }
            Map<String, String> values = toDetailValues(row);
            detailDrafts.add(buildDetail(
                    row.getRowNumber(),
                    values,
                    values,
                    "ROW",
                    "IGNORED_NOT_APPROVED",
                    "SKIPPED",
                    "Gira " + row.getNumber() + " no aprobada",
                    null,
                    null
            ));
        }
    }

    private RegisterComputationResult registerRowsInternal(
            List<TourProcessingDraftTour> rows,
            boolean replaceExistingInRange,
            List<TourProcessingHistoryService.TourProcessingDetailDraft> detailDrafts
    ) {
        if (rows.isEmpty()) {
            throw TransportException.badRequest("TOUR_PROCESSING_REGISTER_EMPTY", "No hay filas para registrar");
        }

        Map<String, Driver> driverCache = new HashMap<>();
        Map<String, Vehicle> vehicleCache = new HashMap<>();
        Set<String> createdDrivers = new HashSet<>();
        Set<String> createdVehicles = new HashSet<>();
        Set<String> updatedVehicles = new HashSet<>();

        for (TourProcessingDraftTour row : rows) {
            Map<String, String> values = toDetailValues(row);
            for (TourProcessingDraftAssignment part : row.getAssignments()) {
                if (part.getResourceType() == ResourceType.EXTERNAL) continue;
                UpsertResult<Driver> driverResult = resolveOrCreateDriver(part.getDriver(), driverCache, createdDrivers);
                if (driverResult.entity() != null) {
                    part.setDriverId(driverResult.entity().getId());
                    part.setDriver(driverResult.entity().getName());
                }
                if (part.getResourceType() == ResourceType.INTERNAL && DriverHoursRules.looksLikePlate(part.getVehicle())) {
                    UpsertResult<Vehicle> vehicleResult = resolveOrCreateVehicle(part.getVehicle(), part.getPassengers(),
                            vehicleCache, createdVehicles, updatedVehicles);
                    part.setVehicleId(vehicleResult.entity().getId());
                    part.setVehicle(vehicleResult.entity().getPlate());
                }
            }
            detailDrafts.add(buildDetail(row.getRowNumber(), values, values, "TOUR", "REGISTERED_TOUR",
                    "SUCCESS", null, null, null));
        }

        TourProcessingRegisterResponseDto response = TourProcessingRegisterResponseDto.builder()
                .requestedRows(rows.size())
                .importedRows(rows.size())
                .replacedRows(0)
                .createdDrivers(createdDrivers.size())
                .updatedDrivers(0)
                .createdVehicles(createdVehicles.size())
                .updatedVehicles(updatedVehicles.size())
                .build();
        return new RegisterComputationResult(response, null, null);
    }

    private Map<String, String> toDetailValues(TourProcessingDraftTour row) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("number", text(row.getNumber()));
        values.put("driver", text(row.primaryAssignment() == null ? null : row.primaryAssignment().getDriver()));
        values.put("vehicle", text(row.primaryAssignment() == null ? null : row.primaryAssignment().getVehicle()));
        values.put("passengers", text(row.getPassengers()));
        values.put("executingUnit", text(row.getExecutingUnit()));
        values.put("responsible", text(row.getResponsible()));
        values.put("destination", text(row.getDestination()));
        values.put("durationDays", text(row.getDurationDays()));
        values.put("priority", text(row.getPriority()));
        values.put("modality", text(row.getModality()));
        values.put("departureTime", text(row.getDepartureTime()));
        values.put("returnTime", text(row.getReturnTime()));
        values.put("departureDate", text(row.getDepartureDate()));
        values.put("returnDate", text(row.getReturnDate()));
        values.put("observations", text(row.getObservations()));
        return values;
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private String numberKey(TourProcessingDraftTour row) {
        return row.getNumber().toLowerCase(Locale.ROOT);
    }

    private boolean isLogicalRoot(TourProcessingDraftTour row) {
        return true;
    }

    private UpsertResult<Driver> resolveOrCreateDriver(
            String rawDriverName,
            Map<String, Driver> cache,
            Set<String> createdKeys
    ) {
        String normalizedKey = normalize(rawDriverName);
        if (normalizedKey.isBlank()) {
            return new UpsertResult<>(null, UpsertAction.SKIPPED);
        }
        if (cache.containsKey(normalizedKey)) {
            return new UpsertResult<>(cache.get(normalizedKey), UpsertAction.REUSED);
        }

        String name = truncate(safe(rawDriverName).replaceAll("\\s+", " "), 200);

        Driver existing = driverRepository.findFirstByNameIgnoreCase(name).orElse(null);
        if (existing != null) {
            cache.put(normalizedKey, existing);
            return new UpsertResult<>(existing, UpsertAction.REUSED);
        }

        Driver created = new Driver();
        created.setName(name);
        created.setStatus(IMPORTED_STATUS);
        Driver saved = driverRepository.save(created);
        cache.put(normalizedKey, saved);
        createdKeys.add(normalizedKey);
        return new UpsertResult<>(saved, UpsertAction.CREATED);
    }

    private UpsertResult<Vehicle> resolveOrCreateVehicle(
            String rawPlate,
            int passengers,
            Map<String, Vehicle> cache,
            Set<String> createdKeys,
            Set<String> updatedKeys
    ) {
        String plate = DriverHoursRules.plateKey(rawPlate);
        if (plate.isBlank()) {
            return new UpsertResult<>(null, UpsertAction.SKIPPED);
        }
        if (cache.containsKey(plate)) {
            return new UpsertResult<>(cache.get(plate), UpsertAction.REUSED);
        }

        Vehicle vehicle = vehicleRepository.findByPlateIgnoreCase(plate).orElse(null);

        if (vehicle == null) {
            Vehicle created = new Vehicle();
            created.setPlate(plate);
            created.setCapacity(Math.max(1, passengers));
            created.setAvailable(true);
            Vehicle saved = vehicleRepository.save(created);
            cache.put(plate, saved);
            createdKeys.add(plate);
            return new UpsertResult<>(saved, UpsertAction.CREATED);
        }

        boolean changed = false;
        if (vehicle.getCapacity() == null) {
            vehicle.setCapacity(Math.max(1, passengers));
            changed = true;
        }

        Vehicle persisted = changed ? vehicleRepository.save(vehicle) : vehicle;
        if (changed) {
            updatedKeys.add(plate);
        }
        cache.put(plate, persisted);
        return new UpsertResult<>(persisted, changed ? UpsertAction.UPDATED : UpsertAction.REUSED);
    }

    private String truncate(String value, int max) {
        String current = safe(value);
        if (current.length() <= max) {
            return current;
        }
        return current.substring(0, max);
    }

    private String mapDriverAction(UpsertAction action) {
        return switch (action) {
            case CREATED -> "CREATED_DRIVER";
            case UPDATED -> "UPDATED_DRIVER";
            case REUSED -> "REUSED_DRIVER";
            case SKIPPED -> "INVALID_ROW";
        };
    }

    private String mapVehicleAction(UpsertAction action) {
        return switch (action) {
            case CREATED -> "CREATED_VEHICLE";
            case UPDATED -> "UPDATED_VEHICLE";
            case REUSED -> "REUSED_VEHICLE";
            case SKIPPED -> "INVALID_ROW";
        };
    }

    private TourProcessingHistoryService.TourProcessingDetailDraft buildDetail(
            Integer originalRowNumber,
            Map<String, String> originalData,
            Map<String, String> normalizedData,
            String recordType,
            String actionPerformed,
            String processingResult,
            String rejectionReason,
            String observations,
            String validationErrors
    ) {
        return TourProcessingHistoryService.TourProcessingDetailDraft.builder()
                .originalRowNumber(originalRowNumber)
                .originalData(originalData == null ? Map.of() : new LinkedHashMap<>(originalData))
                .normalizedData(normalizedData == null ? Map.of() : new LinkedHashMap<>(normalizedData))
                .recordType(recordType)
                .actionPerformed(actionPerformed)
                .processingResult(processingResult)
                .rejectionReason(rejectionReason)
                .observations(observations)
                .validationErrors(validationErrors)
                .build();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value
                .toLowerCase(Locale.ROOT)
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("ñ", "n")
                .replace(" ", "")
                .replace("_", "")
                .trim();
    }

    private enum UpsertAction {
        CREATED,
        UPDATED,
        REUSED,
        SKIPPED
    }

    private record UpsertResult<T>(T entity, UpsertAction action) {}

    private record RegisterComputationResult(
            TourProcessingRegisterResponseDto response,
            LocalDateTime replacedRangeStart,
            LocalDateTime replacedRangeEnd
    ) {}
}
