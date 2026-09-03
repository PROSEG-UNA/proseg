package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.cleaning.CleaningDuplicateGroupDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningFinalizeRequestDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningFinalizeResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningPreviewResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningRegisterRequestDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningRegisterResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningRowDto;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.Tour;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.TourRepository;
import com.proseg.msvc_transport.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleaningService {

    private static final List<String> ROW_KEYS = List.of(
            "date",
            "number",
            "vehicle",
            "vehicleType",
            "driver",
            "passengers",
            "executingUnit",
            "responsible",
            "destination",
            "durationDays",
            "priority",
            "modality",
            "departureTime",
            "returnTime",
            "departureDate",
            "returnDate",
            "observations"
    );
    private static final List<String> DUPLICATE_COLUMNS = List.of("departureDate", "departureTime", "returnTime", "destination", "passengers", "responsible", "executingUnit");
    private static final Pattern DATE_HEADER_PATTERN = Pattern.compile("(?i).*\\b(\\d{1,2})\\s+de\\s+([a-záéíóúñ]+)(?:\\s+de\\s+(\\d{4}))?.*");
    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("enero", 1), Map.entry("febrero", 2), Map.entry("marzo", 3), Map.entry("abril", 4),
            Map.entry("mayo", 5), Map.entry("junio", 6), Map.entry("julio", 7), Map.entry("agosto", 8),
            Map.entry("septiembre", 9), Map.entry("setiembre", 9), Map.entry("octubre", 10), Map.entry("noviembre", 11),
            Map.entry("diciembre", 12)
    );
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE
    );
    private static final List<DateTimeFormatter> TIME_FORMATS = List.of(
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("h:mm a")
    );
    private static final String IMPORTED_STATUS = "ACTIVE";
    private static final String IMPORTED_BRAND = "IMPORTADO";
    private static final String IMPORTED_MODEL = "GENERICO";

    private final TourRepository tourRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final CleaningHistoryService cleaningHistoryService;

    public CleaningPreviewResponseDto preview(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw TransportException.badRequest("CLEANING_FILE_REQUIRED", "Debe adjuntar un archivo Excel o CSV");
        }

        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        List<CleaningRowDto> rows;
        try {
            rows = fileName.endsWith(".csv") ? parseCsv(file) : parseExcel(file);
        } catch (IOException ex) {
            throw TransportException.badRequest("CLEANING_INVALID_FILE", "No se pudo leer el archivo");
        }

        if (rows.isEmpty()) {
            throw TransportException.badRequest("CLEANING_EMPTY_FILE", "No se encontraron filas válidas de gira");
        }

        Map<String, List<Integer>> duplicateMap = new HashMap<>();
        for (CleaningRowDto row : rows) {
            String key = buildDuplicateKey(row.getValues());
            if (!key.isBlank()) {
                duplicateMap.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row.getRowIndex());
            }
        }

        List<CleaningDuplicateGroupDto> duplicateGroups = new ArrayList<>();
        List<Integer> suggestedRemovals = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : duplicateMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicateGroups.add(CleaningDuplicateGroupDto.builder().key(entry.getKey()).rowIndexes(entry.getValue()).build());
                suggestedRemovals.addAll(entry.getValue().subList(1, entry.getValue().size()));
            }
        }

        return CleaningPreviewResponseDto.builder()
                .headers(ROW_KEYS)
                .rows(rows)
                .duplicateGroups(duplicateGroups)
                .suggestedRemovals(suggestedRemovals)
                .totalRowsRead(rows.size())
                .validRows(rows.size())
                .invalidRows(0)
                .duplicateRowsDetected(suggestedRemovals.size())
                .build();
    }

    public CleaningFinalizeResponseDto finalizeCleaning(CleaningFinalizeRequestDto request) {
        Set<Integer> removals = new HashSet<>();
        if (request.getSelectedForDeletion() != null) removals.addAll(request.getSelectedForDeletion());
        if (request.getManualRemovals() != null) removals.addAll(request.getManualRemovals());
        if (!request.isKeepDuplicates() && request.getSuggestedRemovals() != null) removals.addAll(request.getSuggestedRemovals());

        List<CleaningRowDto> cleaned = request.getRows().stream().filter(r -> !removals.contains(r.getRowIndex())).toList();
        return CleaningFinalizeResponseDto.builder()
                .cleanedRows(cleaned)
                .originalRows(request.getRows().size())
                .removedRows(request.getRows().size() - cleaned.size())
                .remainingRows(cleaned.size())
                .build();
    }

    @Transactional
    public CleaningRegisterResponseDto registerRows(CleaningRegisterRequestDto request) {
        LocalDateTime processStartedAt = resolveProcessStartedAt(request);
        String executedBy = resolveExecutedBy();
        List<CleaningHistoryService.CleaningDetailDraft> detailDrafts = new ArrayList<>();
        LocalDateTime processFinishedAt = null;
        appendFinalizeAuditDetails(request, detailDrafts);

        try {
            RegisterComputationResult result = registerRowsInternal(request, detailDrafts);
            processFinishedAt = LocalDateTime.now();
            UUID executionId = cleaningHistoryService.recordSuccess(
                    request,
                    result.response(),
                    executedBy,
                    processStartedAt,
                    processFinishedAt,
                    result.replacedRangeStart(),
                    result.replacedRangeEnd(),
                    detailDrafts
            );
            result.response().setCleaningExecutionId(executionId);
            return result.response();
        } catch (RuntimeException ex) {
            processFinishedAt = processFinishedAt == null ? LocalDateTime.now() : processFinishedAt;
            try {
                cleaningHistoryService.recordFailure(
                        request,
                        executedBy,
                        processStartedAt,
                        processFinishedAt,
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

    private RegisterComputationResult registerRowsInternal(
            CleaningRegisterRequestDto request,
            List<CleaningHistoryService.CleaningDetailDraft> detailDrafts
    ) {
        if (request.getRows() == null || request.getRows().isEmpty()) {
            throw TransportException.badRequest("CLEANING_REGISTER_EMPTY", "No hay filas para registrar");
        }

        Map<String, Driver> driverCache = new HashMap<>();
        Map<String, Vehicle> vehicleCache = new HashMap<>();
        Set<String> createdDrivers = new HashSet<>();
        Set<String> updatedDrivers = new HashSet<>();
        Set<String> createdVehicles = new HashSet<>();
        Set<String> updatedVehicles = new HashSet<>();

        List<Tour> tours = new ArrayList<>();
        for (CleaningRowDto row : request.getRows()) {
            Map<String, String> values = row.getValues() == null ? Map.of() : row.getValues();
            int passengers = Math.max(1, parseInteger(values.get("passengers"), 1));

            UpsertResult<Driver> driverResult = resolveOrCreateDriver(values.get("driver"), driverCache, createdDrivers, updatedDrivers);
            if (driverResult.action() != UpsertAction.SKIPPED) {
                detailDrafts.add(buildDetail(
                        row.getRowIndex(),
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

            UpsertResult<Vehicle> vehicleResult = resolveOrCreateVehicle(
                    values.get("vehicle"),
                    values.get("vehicleType"),
                    passengers,
                    vehicleCache,
                    createdVehicles,
                    updatedVehicles
            );
            if (vehicleResult.action() != UpsertAction.SKIPPED) {
                detailDrafts.add(buildDetail(
                        row.getRowIndex(),
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

            Tour mapped = mapToTour(row, driverResult.entity(), vehicleResult.entity());
            if (mapped != null) {
                tours.add(mapped);
                detailDrafts.add(buildDetail(
                        row.getRowIndex(),
                        values,
                        values,
                        "TOUR",
                        "CREATED_TOUR",
                        "SUCCESS",
                        null,
                        null,
                        null
                ));
            } else {
                detailDrafts.add(buildDetail(
                        row.getRowIndex(),
                        values,
                        values,
                        "ROW",
                        "INVALID_ROW",
                        "FAILED",
                        "No se pudo mapear la fila a una gira",
                        null,
                        "No se pudo transformar la fila"
                ));
            }
        }

        if (tours.isEmpty()) {
            throw TransportException.badRequest("CLEANING_REGISTER_EMPTY", "No se pudo convertir ninguna fila a gira");
        }

        int replaced = 0;
        LocalDateTime replacedRangeStart = null;
        LocalDateTime replacedRangeEnd = null;
        if (request.isReplaceExistingInRange()) {
            LocalDateTime min = tours.stream().map(Tour::getStartDate).min(LocalDateTime::compareTo).orElse(null);
            LocalDateTime max = tours.stream().map(Tour::getEndDate).max(LocalDateTime::compareTo).orElse(null);
            replacedRangeStart = min;
            replacedRangeEnd = max;
            if (min != null && max != null) {
                List<Tour> existing = tourRepository.findAllByStartDateBetween(min, max);
                replaced = existing.size();
                if (!existing.isEmpty()) {
                    tourRepository.deleteAll(existing);
                }
            }
        }

        tourRepository.saveAll(tours);
        CleaningRegisterResponseDto response = CleaningRegisterResponseDto.builder()
                .requestedRows(request.getRows().size())
                .importedRows(tours.size())
                .replacedRows(replaced)
                .createdDrivers(createdDrivers.size())
                .updatedDrivers(updatedDrivers.size())
                .createdVehicles(createdVehicles.size())
                .updatedVehicles(updatedVehicles.size())
                .build();
        return new RegisterComputationResult(response, replacedRangeStart, replacedRangeEnd);
    }

    private List<CleaningRowDto> parseExcel(MultipartFile file) throws IOException {
        List<CleaningRowDto> rows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) return rows;
            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> columnIndexes = new LinkedHashMap<>();
            String contextDate = null;

            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                List<String> values = new ArrayList<>();
                for (int c = 0; c < Math.max(row.getLastCellNum(), 17); c++) {
                    Cell cell = row.getCell(c);
                    values.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
                }

                if (isEmpty(values)) continue;
                String dateHeader = detectDateHeader(values);
                if (dateHeader != null) {
                    contextDate = dateHeader;
                    continue;
                }
                if (isColumnHeader(values)) {
                    columnIndexes = detectColumnIndexes(values);
                    continue;
                }
                if (columnIndexes.isEmpty()) {
                    columnIndexes = defaultColumnIndexes();
                }
                if (!looksLikeTourRow(values, columnIndexes)) continue;

                rows.add(CleaningRowDto.builder()
                        .rowIndex(r + 1)
                        .values(buildCanonicalValues(values, columnIndexes, contextDate))
                        .build());
            }
        }
        return rows;
    }

    private List<CleaningRowDto> parseCsv(MultipartFile file) throws IOException {
        List<CleaningRowDto> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            Map<String, Integer> columnIndexes = new LinkedHashMap<>();
            String contextDate = null;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                List<String> values = parseCsvLine(line);
                if (isEmpty(values)) continue;

                String dateHeader = detectDateHeader(values);
                if (dateHeader != null) {
                    contextDate = dateHeader;
                    continue;
                }
                if (isColumnHeader(values)) {
                    columnIndexes = detectColumnIndexes(values);
                    continue;
                }
                if (columnIndexes.isEmpty()) {
                    columnIndexes = defaultColumnIndexes();
                }
                if (!looksLikeTourRow(values, columnIndexes)) continue;

                rows.add(CleaningRowDto.builder()
                        .rowIndex(lineNumber)
                        .values(buildCanonicalValues(values, columnIndexes, contextDate))
                        .build());
            }
        }
        return rows;
    }

    private Map<String, String> buildCanonicalValues(List<String> values, Map<String, Integer> columnIndexes, String contextDate) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : ROW_KEYS) {
            if ("date".equals(key)) continue;
            Integer index = columnIndexes.get(key);
            map.put(key, index == null || index >= values.size() ? "" : safe(values.get(index)));
        }
        String departureDate = firstNonBlank(map.get("departureDate"), contextDate);
        map.put("date", safe(departureDate));
        if (map.get("departureDate").isBlank()) {
            map.put("departureDate", safe(contextDate));
        }
        return map;
    }

    private Map<String, Integer> detectColumnIndexes(List<String> values) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < values.size(); i++) {
            String normalized = normalize(values.get(i));
            if (normalized.isBlank()) continue;

            if (containsAny(normalized, "numero", "nro", "no")) map.put("number", i);
            else if (normalized.contains("tipovehiculo") || (normalized.contains("tipo") && normalized.contains("vehiculo"))) map.put("vehicleType", i);
            else if (normalized.contains("vehiculo") || normalized.contains("placa")) map.put("vehicle", i);
            else if (normalized.contains("chofer") || normalized.contains("conductor")) map.put("driver", i);
            else if (normalized.contains("pasaj")) map.put("passengers", i);
            else if (normalized.contains("unidadejecutora") || normalized.equals("unidad")) map.put("executingUnit", i);
            else if (normalized.contains("responsable")) map.put("responsible", i);
            else if (normalized.contains("destino")) map.put("destination", i);
            else if (normalized.contains("duracion")) map.put("durationDays", i);
            else if (normalized.contains("prioridad")) map.put("priority", i);
            else if (normalized.contains("modalidad")) map.put("modality", i);
            else if (normalized.contains("horasalida") || (normalized.contains("hora") && normalized.contains("salida"))) map.put("departureTime", i);
            else if (normalized.contains("horaregreso") || normalized.contains("horaretorno") || (normalized.contains("hora") && normalized.contains("regreso"))) map.put("returnTime", i);
            else if (normalized.contains("fechasalida") || (normalized.contains("fecha") && normalized.contains("salida"))) map.put("departureDate", i);
            else if (normalized.contains("fecharegreso") || normalized.contains("fecharetorno") || (normalized.contains("fecha") && normalized.contains("regreso"))) map.put("returnDate", i);
            else if (normalized.contains("observ")) map.put("observations", i);
        }
        return map;
    }

    private Map<String, Integer> defaultColumnIndexes() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("number", 0);
        map.put("vehicle", 1);
        map.put("vehicleType", 2);
        map.put("driver", 3);
        map.put("passengers", 4);
        map.put("executingUnit", 5);
        map.put("responsible", 6);
        map.put("destination", 7);
        map.put("durationDays", 8);
        map.put("priority", 9);
        map.put("modality", 10);
        map.put("departureTime", 11);
        map.put("returnTime", 12);
        map.put("departureDate", 13);
        map.put("returnDate", 14);
        map.put("observations", 15);
        return map;
    }

    private boolean isColumnHeader(List<String> values) {
        String joined = normalize(String.join(" ", values));
        return joined.contains("vehiculo") && joined.contains("chofer") && (joined.contains("destino") || joined.contains("prioridad"));
    }

    private boolean looksLikeTourRow(List<String> values, Map<String, Integer> indexes) {
        String destination = get(values, indexes.get("destination"));
        String responsible = get(values, indexes.get("responsible"));
        String executingUnit = get(values, indexes.get("executingUnit"));
        String passengers = get(values, indexes.get("passengers"));
        String priority = get(values, indexes.get("priority"));
        if (isHeaderWord(destination) || isHeaderWord(responsible) || isHeaderWord(executingUnit)) return false;
        return !destination.isBlank() || !responsible.isBlank() || isInteger(passengers) || isInteger(priority);
    }

    private String detectDateHeader(List<String> values) {
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            Matcher matcher = DATE_HEADER_PATTERN.matcher(value.trim());
            if (!matcher.matches()) continue;
            int day = Integer.parseInt(matcher.group(1));
            Integer month = MONTHS.get(normalize(matcher.group(2)));
            if (month == null) continue;
            int year = matcher.group(3) == null ? LocalDate.now().getYear() : Integer.parseInt(matcher.group(3));
            return LocalDate.of(year, month, day).toString();
        }
        return null;
    }

    private Tour mapToTour(CleaningRowDto row, Driver importedDriver, Vehicle importedVehicle) {
        Map<String, String> values = row.getValues() == null ? Map.of() : row.getValues();
        LocalDate departureDate = parseDate(firstNonBlank(values.get("departureDate"), values.get("date")));
        LocalDate returnDate = parseDate(firstNonBlank(values.get("returnDate"), values.get("departureDate"), values.get("date")));
        if (departureDate == null) departureDate = LocalDate.now();
        if (returnDate == null) returnDate = departureDate;

        int durationDays = parseInteger(values.get("durationDays"), 1);
        if (durationDays > 1 && returnDate.isBefore(departureDate)) {
            returnDate = departureDate.plusDays(durationDays - 1L);
        }

        LocalTime departureTime = parseTime(values.get("departureTime"), LocalTime.of(7, 30));
        LocalTime returnTime = parseTime(values.get("returnTime"), LocalTime.of(16, 30));
        LocalDateTime startDate = LocalDateTime.of(departureDate, departureTime);
        LocalDateTime endDate = LocalDateTime.of(returnDate, returnTime);
        if (endDate.isBefore(startDate)) {
            endDate = startDate.plusHours(1);
        }

        String destination = safe(values.get("destination"));
        String executingUnit = safe(values.get("executingUnit"));
        String number = safe(values.get("number"));
        String name = !number.isBlank() ? "Gira " + number : (!destination.isBlank() ? "Gira " + destination : "Gira importada");
        int priority = Math.max(1, parseInteger(values.get("priority"), 1));
        int passengers = Math.max(1, parseInteger(values.get("passengers"), 1));

        Tour tour = new Tour();
        tour.setName(name);
        tour.setExternalNumber(number);
        tour.setOrigin(executingUnit.isBlank() ? "Sin origen" : executingUnit);
        tour.setDestination(destination.isBlank() ? "Sin destino" : destination);
        tour.setStartDate(startDate);
        tour.setEndDate(endDate);
        tour.setStatus("PLANNED");
        tour.setPriority(priority);
        tour.setPassengers(passengers);
        tour.setRequestedVehicle(importedVehicle != null ? importedVehicle.getPlate() : safe(values.get("vehicle")));
        tour.setRequestedVehicleType(importedVehicle != null ? safe(importedVehicle.getType()) : safe(values.get("vehicleType")));
        tour.setRequestedDriver(importedDriver != null ? buildDriverFullName(importedDriver.getFirstName(), importedDriver.getLastName()) : safe(values.get("driver")));
        tour.setResponsible(safe(values.get("responsible")));
        tour.setExecutingUnit(executingUnit);
        tour.setModality(safe(values.get("modality")));
        tour.setDurationDays(durationDays);
        tour.setDepartureTime(safe(values.get("departureTime")));
        tour.setReturnTime(safe(values.get("returnTime")));
        tour.setObservations(safe(values.get("observations")));
        return tour;
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
            String rawVehicleType,
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
        String importedType = safe(rawVehicleType);

        if (vehicle == null) {
            Vehicle created = new Vehicle();
            created.setPlate(plate);
            created.setBrand(IMPORTED_BRAND);
            created.setModel(IMPORTED_MODEL);
            created.setYear(LocalDate.now().getYear());
            created.setCapacity(Math.max(1, passengers));
            created.setStatus(IMPORTED_STATUS);
            created.setType(importedType);
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
        if ((vehicle.getType() == null || vehicle.getType().isBlank()) && !importedType.isBlank()) {
            vehicle.setType(importedType);
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
        String lastName = truncate(String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length)), 100);
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

    private String buildDuplicateKey(Map<String, String> values) {
        if (values == null || values.isEmpty()) return "";
        List<String> normalized = new ArrayList<>(DUPLICATE_COLUMNS.size());
        for (String key : DUPLICATE_COLUMNS) {
            normalized.add(normalize(values.get(key)));
        }
        return String.join("|", normalized);
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

    private void appendFinalizeAuditDetails(
            CleaningRegisterRequestDto request,
            List<CleaningHistoryService.CleaningDetailDraft> detailDrafts
    ) {
        if (request.getAudit() == null) {
            return;
        }

        List<Integer> selectedRows = request.getAudit().getSelectedForDeletion() == null
                ? List.of()
                : request.getAudit().getSelectedForDeletion();
        Set<Integer> suggested = request.getAudit().getSuggestedRemovals() == null
                ? Set.of()
                : new HashSet<>(request.getAudit().getSuggestedRemovals());

        for (Integer rowNumber : selectedRows) {
            if (rowNumber == null || rowNumber <= 0) {
                continue;
            }
            String reason = suggested.contains(rowNumber)
                    ? "Fila removida por sugerencia de duplicado"
                    : "Fila removida manualmente durante la depuración";
            detailDrafts.add(buildDetail(
                    rowNumber,
                    Map.of(),
                    Map.of(),
                    "ROW",
                    "IGNORED_DUPLICATE",
                    "SKIPPED",
                    reason,
                    null,
                    null
            ));
        }

        int invalidRows = request.getAudit().getInvalidRows() == null ? 0 : Math.max(0, request.getAudit().getInvalidRows());
        if (invalidRows > 0) {
            detailDrafts.add(buildDetail(
                    null,
                    Map.of(),
                    Map.of(),
                    "ROW",
                    "INVALID_ROW",
                    "FAILED",
                    "Se detectaron filas inválidas en la fase de vista previa",
                    "Cantidad de filas inválidas: " + invalidRows,
                    null
            ));
        }
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

    private LocalDateTime resolveProcessStartedAt(CleaningRegisterRequestDto request) {
        if (request.getAudit() != null && request.getAudit().getProcessStartedAt() != null) {
            return request.getAudit().getProcessStartedAt();
        }
        return LocalDateTime.now();
    }

    private String resolveExecutedBy() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "usuario_desconocido";
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Jwt jwt = jwtAuthenticationToken.getToken();
            String preferredUsername = safe(jwt.getClaimAsString("preferred_username"));
            if (!preferredUsername.isBlank()) return preferredUsername;

            String displayName = safe(jwt.getClaimAsString("name"));
            if (!displayName.isBlank()) return displayName;

            String givenName = safe(jwt.getClaimAsString("given_name"));
            String familyName = safe(jwt.getClaimAsString("family_name"));
            String fullName = (givenName + " " + familyName).trim();
            if (!fullName.isBlank()) return fullName;

            String email = safe(jwt.getClaimAsString("email"));
            if (!email.isBlank()) return email;
        }

        String fallback = safe(authentication.getName());
        if (!fallback.isBlank() && !looksLikeIdentifier(fallback)) {
            return fallback;
        }
        return "usuario_desconocido";
    }

    private boolean looksLikeIdentifier(String value) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return true;
        }
        try {
            UUID.fromString(trimmed);
            return true;
        } catch (IllegalArgumentException ignored) {
        }
        return trimmed.matches("^[0-9a-fA-F]{32}$")
                || trimmed.matches("^\\d{8,}$");
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        if (line == null) return result;
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
                continue;
            }
            if (ch == ',' && !quoted) {
                result.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        result.add(current.toString().trim());
        return result;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        Matcher matcher = DATE_HEADER_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            int day = Integer.parseInt(matcher.group(1));
            Integer month = MONTHS.get(normalize(matcher.group(2)));
            int year = matcher.group(3) == null ? LocalDate.now().getYear() : Integer.parseInt(matcher.group(3));
            if (month != null) {
                return LocalDate.of(year, month, day);
            }
        }
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private LocalTime parseTime(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) return fallback;
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('.', ':');
        for (DateTimeFormatter formatter : TIME_FORMATS) {
            try {
                return LocalTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return fallback;
    }

    private boolean isHeaderWord(String value) {
        String normalized = normalize(value);
        return normalized.contains("destino") || normalized.contains("responsable") || normalized.contains("unidad");
    }

    private boolean isInteger(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            Integer.parseInt(value.replaceAll("[^0-9-]", ""));
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private int parseInteger(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private boolean isEmpty(List<String> values) {
        return values.stream().allMatch(v -> v == null || v.isBlank());
    }

    private String get(List<String> values, Integer index) {
        if (index == null || index < 0 || index >= values.size()) return "";
        return safe(values.get(index));
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) return true;
        }
        return false;
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
