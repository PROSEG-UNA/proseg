package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.cleaning.CleaningDraftCreateRequestDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftImportResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftRowIssueDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftRowRequestDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftRowResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningImportColumnDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningImportSchemaDto;
import com.proseg.msvc_transport.entity.CleaningDraft;
import com.proseg.msvc_transport.entity.CleaningDraftRow;
import com.proseg.msvc_transport.entity.CleaningDraftStatus;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.importer.TransportImportField;
import com.proseg.msvc_transport.repository.CleaningDraftRepository;
import com.proseg.msvc_transport.repository.CleaningDraftRowRepository;
import com.proseg.msvc_transport.security.CurrentUserResolver;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
    private static final List<String> FIELD_ORDER = Arrays.stream(TransportImportField.values())
            .map(TransportImportField::getAttribute)
            .toList();

    private final CleaningDraftRepository draftRepository;
    private final CleaningDraftRowRepository rowRepository;
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
                .build();

        List<CleaningDraftRow> entities = rows.stream()
                .map(row -> toEntity(row, draft))
                .toList();
        markDuplicates(entities);
        summarize(draft, entities);

        CleaningDraft saved = draftRepository.save(draft);
        rowRepository.saveAll(entities);

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
        markDuplicates(rows);
        summarize(draft, rows);
        draft.setTotalRows(rows.size());
        rowRepository.save(row);
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

        applyValues(row, request);
        List<CleaningDraftRow> rows = rowRepository.findAllByDraftIdOrderByRowNumberAsc(draftId);
        markDuplicates(rows);
        summarize(draft, rows);
        return toRowResponse(row);
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
        return duplicates.size();
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
        return List.of();
    }

    private int violationOrder(ConstraintViolation<CleaningDraftRowRequestDto> violation) {
        int index = FIELD_ORDER.indexOf(violation.getPropertyPath().toString());
        return index < 0 ? -1 : index;
    }

    private void markDuplicates(List<CleaningDraftRow> rows) {
        Map<String, List<CleaningDraftRow>> groups = new LinkedHashMap<>();
        for (CleaningDraftRow row : rows) {
            groups.computeIfAbsent(row.getNumber().toLowerCase(Locale.ROOT), key -> new ArrayList<>()).add(row);
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
        row.setDriver(trimToNull(request.getDriver()));
        row.setNumber(request.getNumber().trim());
        row.setVehicle(trimToNull(request.getVehicle()));
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
                .minDepartureDate(draft.getMinDepartureDate())
                .maxReturnDate(draft.getMaxReturnDate())
                .registeredAt(draft.getRegisteredAt())
                .cleaningExecutionId(draft.getCleaningExecutionId())
                .build();
    }

    private CleaningDraftRowResponseDto toRowResponse(CleaningDraftRow row) {
        return CleaningDraftRowResponseDto.builder()
                .id(row.getId())
                .rowNumber(row.getRowNumber())
                .driver(row.getDriver())
                .number(row.getNumber())
                .vehicle(row.getVehicle())
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
