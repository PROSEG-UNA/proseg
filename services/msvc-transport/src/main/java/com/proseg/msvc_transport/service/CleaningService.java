package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.cleaning.CleaningRegisterResponseDto;
import com.proseg.msvc_transport.entity.CleaningDraft;
import com.proseg.msvc_transport.entity.CleaningDraftRow;
import com.proseg.msvc_transport.entity.CleaningDraftStatus;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.Tour;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.CleaningDraftRepository;
import com.proseg.msvc_transport.repository.CleaningDraftRowRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.TourRepository;
import com.proseg.msvc_transport.repository.VehicleRepository;
import com.proseg.msvc_transport.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleaningService {

    private static final String IMPORTED_STATUS = "ACTIVE";
    private static final String IMPORTED_BRAND = "IMPORTADO";
    private static final String IMPORTED_MODEL = "GENERICO";
    private static final String DRAFT_RESOURCE = "Borrador de depuración";
    private static final int MAX_CONFLICTS_IN_MESSAGE = 10;
    private static final DateTimeFormatter TOUR_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final TourRepository tourRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final CleaningDraftRepository draftRepository;
    private final CleaningDraftRowRepository draftRowRepository;
    private final CleaningHistoryService cleaningHistoryService;
    private final CurrentUserResolver currentUserResolver;

    @Transactional
    public CleaningRegisterResponseDto registerDraft(UUID draftId, boolean replaceExistingInRange) {
        CleaningDraft draft = draftRepository.findByIdForUpdate(draftId)
                .orElseThrow(() -> TransportException.notFound(DRAFT_RESOURCE, draftId.toString()));
        if (draft.getStatus() != CleaningDraftStatus.DRAFT) {
            throw TransportException.conflict("CLEANING_DRAFT_ALREADY_REGISTERED", "El borrador ya fue registrado");
        }

        List<CleaningDraftRow> rows = draftRowRepository.findAllByDraftIdOrderByRowNumberAsc(draftId);
        ensureNoConflicts(rows);

        List<CleaningDraftRow> primaryRows = rows.stream()
                .filter(row -> row.getDuplicateRank() == 1)
                .toList();
        LocalDateTime processStartedAt = LocalDateTime.now();
        String executedBy = currentUserResolver.resolve();
        CleaningHistoryService.CleaningAuditData audit = new CleaningHistoryService.CleaningAuditData(
                draft.getFileName(),
                draft.getFileType(),
                draft.getTotalRows(),
                draft.getTotalRows(),
                0,
                draft.getDuplicateRows(),
                primaryRows.size(),
                replaceExistingInRange
        );
        List<CleaningHistoryService.CleaningDetailDraft> detailDrafts = new ArrayList<>();
        appendSkippedDuplicates(rows, detailDrafts);

        try {
            RegisterComputationResult result = registerRowsInternal(primaryRows, replaceExistingInRange, detailDrafts);
            LocalDateTime processFinishedAt = LocalDateTime.now();
            UUID executionId = cleaningHistoryService.recordSuccess(
                    audit,
                    result.response(),
                    executedBy,
                    processStartedAt,
                    processFinishedAt,
                    result.replacedRangeStart(),
                    result.replacedRangeEnd(),
                    detailDrafts
            );
            result.response().setCleaningExecutionId(executionId);
            draft.setStatus(CleaningDraftStatus.REGISTERED);
            draft.setRegisteredAt(processFinishedAt);
            draft.setCleaningExecutionId(executionId);
            return result.response();
        } catch (RuntimeException ex) {
            try {
                cleaningHistoryService.recordFailure(
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

    private void ensureNoConflicts(List<CleaningDraftRow> rows) {
        Set<String> conflictingNumbers = rows.stream()
                .filter(CleaningDraftRow::isConflicting)
                .map(this::numberKey)
                .collect(Collectors.toSet());
        if (conflictingNumbers.isEmpty()) {
            return;
        }

        Map<String, List<Integer>> rowNumbersByNumber = new LinkedHashMap<>();
        for (CleaningDraftRow row : rows) {
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
                "CLEANING_DRAFT_CONFLICTS",
                "Hay números de gira repetidos con datos distintos: " + detail + remaining
        );
    }

    private void appendSkippedDuplicates(
            List<CleaningDraftRow> rows,
            List<CleaningHistoryService.CleaningDetailDraft> detailDrafts
    ) {
        Map<String, Integer> firstRowByNumber = new HashMap<>();
        for (CleaningDraftRow row : rows) {
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

    private RegisterComputationResult registerRowsInternal(
            List<CleaningDraftRow> rows,
            boolean replaceExistingInRange,
            List<CleaningHistoryService.CleaningDetailDraft> detailDrafts
    ) {
        if (rows.isEmpty()) {
            throw TransportException.badRequest("CLEANING_REGISTER_EMPTY", "No hay filas para registrar");
        }

        Map<String, Driver> driverCache = new HashMap<>();
        Map<String, Vehicle> vehicleCache = new HashMap<>();
        Set<String> createdDrivers = new HashSet<>();
        Set<String> updatedDrivers = new HashSet<>();
        Set<String> createdVehicles = new HashSet<>();
        Set<String> updatedVehicles = new HashSet<>();

        List<Tour> tours = new ArrayList<>();
        for (CleaningDraftRow row : rows) {
            Map<String, String> values = toDetailValues(row);

            UpsertResult<Driver> driverResult = resolveOrCreateDriver(row.getDriver(), driverCache, createdDrivers, updatedDrivers);
            if (driverResult.action() != UpsertAction.SKIPPED) {
                detailDrafts.add(buildDetail(
                        row.getRowNumber(),
                        values,
                        values,
                        "DRIVER",
                        mapDriverAction(driverResult.action()),
                        "SUCCESS",
                        null,
                        null,
                        null
                ));
            }

            UpsertResult<Vehicle> vehicleResult = looksLikePlate(row.getVehicle())
                    ? resolveOrCreateVehicle(row.getVehicle(), row.getPassengers(), vehicleCache, createdVehicles, updatedVehicles)
                    : new UpsertResult<>(null, UpsertAction.SKIPPED);
            if (vehicleResult.action() != UpsertAction.SKIPPED) {
                detailDrafts.add(buildDetail(
                        row.getRowNumber(),
                        values,
                        values,
                        "VEHICLE",
                        mapVehicleAction(vehicleResult.action()),
                        "SUCCESS",
                        null,
                        null,
                        null
                ));
            }

            tours.add(mapToTour(row, driverResult.entity(), vehicleResult.entity()));
            detailDrafts.add(buildDetail(
                    row.getRowNumber(),
                    values,
                    values,
                    "TOUR",
                    "CREATED_TOUR",
                    "SUCCESS",
                    null,
                    null,
                    null
            ));
        }

        int replaced = 0;
        LocalDateTime replacedRangeStart = null;
        LocalDateTime replacedRangeEnd = null;
        if (replaceExistingInRange) {
            replacedRangeStart = tours.stream().map(Tour::getStartDate).min(LocalDateTime::compareTo).orElse(null);
            replacedRangeEnd = tours.stream().map(Tour::getEndDate).max(LocalDateTime::compareTo).orElse(null);
            if (replacedRangeStart != null && replacedRangeEnd != null) {
                List<Tour> existing = tourRepository.findAllByStartDateBetween(replacedRangeStart, replacedRangeEnd);
                replaced = existing.size();
                if (!existing.isEmpty()) {
                    tourRepository.deleteAll(existing);
                }
            }
        }

        tourRepository.saveAll(tours);
        CleaningRegisterResponseDto response = CleaningRegisterResponseDto.builder()
                .requestedRows(rows.size())
                .importedRows(tours.size())
                .replacedRows(replaced)
                .createdDrivers(createdDrivers.size())
                .updatedDrivers(updatedDrivers.size())
                .createdVehicles(createdVehicles.size())
                .updatedVehicles(updatedVehicles.size())
                .build();
        return new RegisterComputationResult(response, replacedRangeStart, replacedRangeEnd);
    }

    private Tour mapToTour(CleaningDraftRow row, Driver importedDriver, Vehicle importedVehicle) {
        Tour tour = new Tour();
        tour.setName("Gira " + row.getNumber());
        tour.setExternalNumber(row.getNumber());
        tour.setOrigin(row.getExecutingUnit());
        tour.setDestination(row.getDestination());
        tour.setStartDate(LocalDateTime.of(row.getDepartureDate(), row.getDepartureTime()));
        tour.setEndDate(LocalDateTime.of(row.getReturnDate(), row.getReturnTime()));
        tour.setStatus("PLANNED");
        tour.setPriority(row.getPriority());
        tour.setPassengers(row.getPassengers());
        tour.setRequestedVehicle(importedVehicle != null ? importedVehicle.getPlate() : null);
        tour.setRequestedVehicleType(importedVehicle != null ? importedVehicle.getType() : row.getVehicle());
        tour.setRequestedDriver(importedDriver != null
                ? buildDriverFullName(importedDriver.getFirstName(), importedDriver.getLastName())
                : null);
        tour.setResponsible(row.getResponsible());
        tour.setExecutingUnit(row.getExecutingUnit());
        tour.setModality(row.getModality());
        tour.setDurationDays(row.getDurationDays());
        tour.setDepartureTime(row.getDepartureTime().format(TOUR_TIME_FORMAT));
        tour.setReturnTime(row.getReturnTime().format(TOUR_TIME_FORMAT));
        tour.setObservations(row.getObservations());
        return tour;
    }

    private boolean looksLikePlate(String vehicle) {
        return vehicle != null && vehicle.chars().anyMatch(Character::isDigit);
    }

    private Map<String, String> toDetailValues(CleaningDraftRow row) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("number", text(row.getNumber()));
        values.put("driver", text(row.getDriver()));
        values.put("vehicle", text(row.getVehicle()));
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

    private String numberKey(CleaningDraftRow row) {
        return row.getNumber().toLowerCase(Locale.ROOT);
    }

    private UpsertResult<Driver> resolveOrCreateDriver(
            String rawDriverName,
            Map<String, Driver> cache,
            Set<String> createdKeys,
            Set<String> updatedKeys
    ) {
        String normalizedKey = normalize(rawDriverName);
        if (normalizedKey.isBlank()) {
            return new UpsertResult<>(null, UpsertAction.SKIPPED);
        }
        if (cache.containsKey(normalizedKey)) {
            return new UpsertResult<>(cache.get(normalizedKey), UpsertAction.REUSED);
        }

        String[] names = splitDriverName(rawDriverName);
        String firstName = names[0];
        String lastName = names[1];
        String generatedDocumentId = buildImportedDriverDocumentId(firstName, lastName);

        Optional<Driver> existingByName = driverRepository.findFirstByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName, lastName);
        Optional<Driver> existingByDocument = driverRepository.findByDocumentIdIgnoreCase(generatedDocumentId);
        Driver driver = existingByName.orElseGet(() -> existingByDocument.orElse(null));

        if (driver == null) {
            Driver created = new Driver();
            created.setFirstName(firstName);
            created.setLastName(lastName);
            created.setDocumentId(generatedDocumentId);
            created.setLicenseNumber(buildImportedDriverLicense(generatedDocumentId));
            created.setStatus(IMPORTED_STATUS);
            created.setAvailability(true);
            created.setRestrictions(null);
            Driver saved = driverRepository.save(created);
            cache.put(normalizedKey, saved);
            createdKeys.add(normalizedKey);
            return new UpsertResult<>(saved, UpsertAction.CREATED);
        }

        boolean changed = false;
        if (driver.getFirstName() == null || driver.getFirstName().isBlank()) {
            driver.setFirstName(firstName);
            changed = true;
        }
        if (driver.getLastName() == null || driver.getLastName().isBlank()) {
            driver.setLastName(lastName);
            changed = true;
        }
        if (driver.getDocumentId() == null || driver.getDocumentId().isBlank()) {
            driver.setDocumentId(generatedDocumentId);
            changed = true;
        }
        if (driver.getLicenseNumber() == null || driver.getLicenseNumber().isBlank()) {
            driver.setLicenseNumber(buildImportedDriverLicense(generatedDocumentId));
            changed = true;
        }

        Driver persisted = changed ? driverRepository.save(driver) : driver;
        if (changed) {
            updatedKeys.add(normalizedKey);
        }
        cache.put(normalizedKey, persisted);
        return new UpsertResult<>(persisted, changed ? UpsertAction.UPDATED : UpsertAction.REUSED);
    }

    private UpsertResult<Vehicle> resolveOrCreateVehicle(
            String rawPlate,
            int passengers,
            Map<String, Vehicle> cache,
            Set<String> createdKeys,
            Set<String> updatedKeys
    ) {
        String plate = normalizePlate(rawPlate);
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
            created.setBrand(IMPORTED_BRAND);
            created.setModel(IMPORTED_MODEL);
            created.setYear(LocalDate.now().getYear());
            created.setCapacity(Math.max(1, passengers));
            created.setStatus(IMPORTED_STATUS);
            created.setAvailable(true);
            created.setUnderMaintenance(false);
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
        if (vehicle.getBrand() == null || vehicle.getBrand().isBlank()) {
            vehicle.setBrand(IMPORTED_BRAND);
            changed = true;
        }
        if (vehicle.getModel() == null || vehicle.getModel().isBlank()) {
            vehicle.setModel(IMPORTED_MODEL);
            changed = true;
        }
        if (vehicle.getYear() == null) {
            vehicle.setYear(LocalDate.now().getYear());
            changed = true;
        }

        Vehicle persisted = changed ? vehicleRepository.save(vehicle) : vehicle;
        if (changed) {
            updatedKeys.add(plate);
        }
        cache.put(plate, persisted);
        return new UpsertResult<>(persisted, changed ? UpsertAction.UPDATED : UpsertAction.REUSED);
    }

    private String[] splitDriverName(String rawDriverName) {
        String safeName = safe(rawDriverName).replaceAll("\\s+", " ").trim();
        if (safeName.isBlank()) {
            return new String[]{"Chofer", "Importado"};
        }
        String[] parts = safeName.split(" ");
        if (parts.length == 1) {
            return new String[]{truncate(parts[0], 100), "Importado"};
        }
        String firstName = truncate(parts[0], 100);
        String lastName = truncate(String.join(" ", Arrays.copyOfRange(parts, 1, parts.length)), 100);
        if (lastName.isBlank()) {
            lastName = "Importado";
        }
        return new String[]{firstName, lastName};
    }

    private String buildImportedDriverDocumentId(String firstName, String lastName) {
        String token = normalize(firstName + lastName).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (token.isBlank()) {
            token = "CHOFER";
        }
        String hash = Integer.toHexString((firstName + "|" + lastName).toLowerCase(Locale.ROOT).hashCode()).toUpperCase(Locale.ROOT);
        return truncate("IMP-" + token + "-" + hash, 50);
    }

    private String buildImportedDriverLicense(String documentId) {
        return truncate("LIC-" + safe(documentId), 50);
    }

    private String buildDriverFullName(String firstName, String lastName) {
        String fullName = (safe(firstName) + " " + safe(lastName)).trim();
        return fullName.isBlank() ? "Chofer importado" : fullName;
    }

    private String normalizePlate(String value) {
        String plate = safe(value).toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        return truncate(plate, 20);
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

    private CleaningHistoryService.CleaningDetailDraft buildDetail(
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
        return CleaningHistoryService.CleaningDetailDraft.builder()
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
            CleaningRegisterResponseDto response,
            LocalDateTime replacedRangeStart,
            LocalDateTime replacedRangeEnd
    ) {}
}
