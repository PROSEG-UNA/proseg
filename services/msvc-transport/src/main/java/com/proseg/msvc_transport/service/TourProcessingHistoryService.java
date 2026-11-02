package com.proseg.msvc_transport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingExecutionDetailResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingExecutionDetailViewResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingExecutionSummaryResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingRegisterResponseDto;
import com.proseg.msvc_transport.entity.TourProcessingExecution;
import com.proseg.msvc_transport.entity.TourProcessingExecutionDetail;
import com.proseg.msvc_transport.entity.TourProcessingExecutionStatus;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.TourProcessingExecutionDetailRepository;
import com.proseg.msvc_transport.repository.TourProcessingExecutionRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourProcessingHistoryService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "executedAt",
            "executedBy",
            "fileName",
            "finalStatus",
            "durationMs",
            "totalReadRecords",
            "validRecords",
            "invalidRecords",
            "duplicatesDetected",
            "createdDrivers",
            "updatedDrivers",
            "createdVehicles",
            "updatedVehicles",
            "createdTours",
            "updatedTours"
    );

    private final TourProcessingExecutionRepository tourProcessingExecutionRepository;
    private final TourProcessingExecutionDetailRepository tourProcessingExecutionDetailRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public UUID recordSuccess(
            TourProcessingAuditData audit,
            TourProcessingRegisterResponseDto response,
            String executedBy,
            LocalDateTime processStartedAt,
            LocalDateTime processFinishedAt,
            LocalDateTime replacedRangeStart,
            LocalDateTime replacedRangeEnd,
            List<TourProcessingDetailDraft> details
    ) {
        TourProcessingExecutionStatus finalStatus = response.getImportedRows() < response.getRequestedRows()
                ? TourProcessingExecutionStatus.PARTIAL
                : TourProcessingExecutionStatus.SUCCESS;
        TourProcessingExecution execution = buildExecution(
                audit,
                response,
                executedBy,
                processStartedAt,
                processFinishedAt,
                finalStatus,
                null,
                replacedRangeStart,
                replacedRangeEnd
        );
        TourProcessingExecution saved = tourProcessingExecutionRepository.save(execution);
        saveDetails(saved, details);
        return saved.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID recordFailure(
            TourProcessingAuditData audit,
            String executedBy,
            LocalDateTime processStartedAt,
            LocalDateTime processFinishedAt,
            Exception exception,
            List<TourProcessingDetailDraft> details
    ) {
        TourProcessingRegisterResponseDto failed = TourProcessingRegisterResponseDto.builder()
                .requestedRows(audit.requestedRows())
                .importedRows(0)
                .replacedRows(0)
                .createdDrivers(0)
                .updatedDrivers(0)
                .createdVehicles(0)
                .updatedVehicles(0)
                .build();
        TourProcessingExecution execution = buildExecution(
                audit,
                failed,
                executedBy,
                processStartedAt,
                processFinishedAt,
                TourProcessingExecutionStatus.FAILED,
                exception.getMessage(),
                null,
                null
        );
        TourProcessingExecution saved = tourProcessingExecutionRepository.save(execution);
        saveDetails(saved, details);
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public Page<TourProcessingExecutionSummaryResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<TourProcessingExecution> spec = buildSpecification(search, filters);
        Pageable sanitized = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sanitizeSort(pageable.getSort())
        );
        return tourProcessingExecutionRepository.findAll(spec, sanitized).map(this::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public TourProcessingExecutionDetailViewResponseDto findById(UUID id) {
        TourProcessingExecution execution = tourProcessingExecutionRepository.findById(id)
                .orElseThrow(() -> TransportException.notFound("Historial de depuración", id.toString()));
        List<TourProcessingExecutionDetailResponseDto> details = tourProcessingExecutionDetailRepository
                .findAllByTourProcessingExecutionIdOrderByOriginalRowNumberAscIdAsc(id)
                .stream()
                .map(this::toDetailDto)
                .toList();
        return TourProcessingExecutionDetailViewResponseDto.builder()
                .id(execution.getId())
                .executedAt(execution.getExecutedAt())
                .executedBy(execution.getExecutedBy())
                .executedByDisplay(toDisplayUser(execution.getExecutedBy()))
                .fileName(execution.getFileName())
                .fileType(execution.getFileType())
                .processStartedAt(execution.getProcessStartedAt())
                .processFinishedAt(execution.getProcessFinishedAt())
                .durationMs(execution.getDurationMs())
                .totalReadRecords(execution.getTotalReadRecords())
                .validRecords(execution.getValidRecords())
                .invalidRecords(execution.getInvalidRecords())
                .duplicatesDetected(execution.getDuplicatesDetected())
                .createdDrivers(execution.getCreatedDrivers())
                .updatedDrivers(execution.getUpdatedDrivers())
                .createdVehicles(execution.getCreatedVehicles())
                .updatedVehicles(execution.getUpdatedVehicles())
                .createdTours(execution.getCreatedTours())
                .updatedTours(execution.getUpdatedTours())
                .replacedExistingTours(execution.isReplacedExistingTours())
                .replacedRangeStart(execution.getReplacedRangeStart())
                .replacedRangeEnd(execution.getReplacedRangeEnd())
                .finalStatus(execution.getFinalStatus())
                .errorMessage(execution.getErrorMessage())
                .details(details)
                .build();
    }

    private TourProcessingExecution buildExecution(
            TourProcessingAuditData audit,
            TourProcessingRegisterResponseDto response,
            String executedBy,
            LocalDateTime processStartedAt,
            LocalDateTime processFinishedAt,
            TourProcessingExecutionStatus status,
            String errorMessage,
            LocalDateTime replacedRangeStart,
            LocalDateTime replacedRangeEnd
    ) {
        int totalReadRecords = Math.max(0, audit.totalRowsRead());
        int validRecords = Math.max(0, audit.validRows());
        int invalidRecords = Math.max(0, audit.invalidRows());
        int duplicatesDetected = Math.max(0, audit.duplicatesDetected());
        String fileName = safe(audit.fileName(), "archivo_desconocido");
        String fileType = safe(audit.fileType(), "DESCONOCIDO").toUpperCase(Locale.ROOT);
        long duration = Math.max(0L, java.time.Duration.between(processStartedAt, processFinishedAt).toMillis());

        return TourProcessingExecution.builder()
                .executedAt(processFinishedAt)
                .executedBy(safe(executedBy, "usuario_desconocido"))
                .fileName(fileName)
                .fileType(fileType)
                .processStartedAt(processStartedAt)
                .processFinishedAt(processFinishedAt)
                .durationMs(duration)
                .totalReadRecords(totalReadRecords)
                .validRecords(validRecords)
                .invalidRecords(invalidRecords)
                .duplicatesDetected(duplicatesDetected)
                .createdDrivers(response.getCreatedDrivers())
                .updatedDrivers(response.getUpdatedDrivers())
                .createdVehicles(response.getCreatedVehicles())
                .updatedVehicles(response.getUpdatedVehicles())
                .createdTours(response.getImportedRows())
                .updatedTours(0)
                .replacedExistingTours(audit.replaceExistingInRange())
                .replacedRangeStart(replacedRangeStart)
                .replacedRangeEnd(replacedRangeEnd)
                .finalStatus(status)
                .errorMessage(errorMessage)
                .executionDate(processFinishedAt.toLocalDate())
                .build();
    }

    private void saveDetails(TourProcessingExecution execution, List<TourProcessingDetailDraft> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        List<TourProcessingExecutionDetail> entities = details.stream()
                .map(detail -> TourProcessingExecutionDetail.builder()
                        .tourProcessingExecution(execution)
                        .originalRowNumber(detail.getOriginalRowNumber())
                        .originalData(writeJson(detail.getOriginalData()))
                        .normalizedData(writeJson(detail.getNormalizedData()))
                        .recordType(detail.getRecordType())
                        .actionPerformed(detail.getActionPerformed())
                        .processingResult(detail.getProcessingResult())
                        .rejectionReason(detail.getRejectionReason())
                        .observations(detail.getObservations())
                        .validationErrors(detail.getValidationErrors())
                        .build())
                .toList();
        tourProcessingExecutionDetailRepository.saveAll(entities);
    }

    private TourProcessingExecutionSummaryResponseDto toSummaryDto(TourProcessingExecution entity) {
        return TourProcessingExecutionSummaryResponseDto.builder()
                .id(entity.getId())
                .executedAt(entity.getExecutedAt())
                .executedBy(entity.getExecutedBy())
                .executedByDisplay(toDisplayUser(entity.getExecutedBy()))
                .fileName(entity.getFileName())
                .fileType(entity.getFileType())
                .finalStatus(entity.getFinalStatus())
                .durationMs(entity.getDurationMs())
                .totalReadRecords(entity.getTotalReadRecords())
                .validRecords(entity.getValidRecords())
                .invalidRecords(entity.getInvalidRecords())
                .duplicatesDetected(entity.getDuplicatesDetected())
                .createdDrivers(entity.getCreatedDrivers())
                .updatedDrivers(entity.getUpdatedDrivers())
                .createdVehicles(entity.getCreatedVehicles())
                .updatedVehicles(entity.getUpdatedVehicles())
                .createdTours(entity.getCreatedTours())
                .updatedTours(entity.getUpdatedTours())
                .build();
    }

    private TourProcessingExecutionDetailResponseDto toDetailDto(TourProcessingExecutionDetail entity) {
        return TourProcessingExecutionDetailResponseDto.builder()
                .id(entity.getId())
                .originalRowNumber(entity.getOriginalRowNumber())
                .recordType(entity.getRecordType())
                .actionPerformed(entity.getActionPerformed())
                .processingResult(entity.getProcessingResult())
                .rejectionReason(entity.getRejectionReason())
                .observations(entity.getObservations())
                .validationErrors(entity.getValidationErrors())
                .originalData(readJsonMap(entity.getOriginalData()))
                .normalizedData(readJsonMap(entity.getNormalizedData()))
                .build();
    }

    private Specification<TourProcessingExecution> buildSpecification(String search, Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fileName")), pattern),
                        cb.like(cb.lower(root.get("executedBy")), pattern),
                        cb.like(cb.lower(root.get("fileType")), pattern),
                        cb.like(cb.lower(root.get("finalStatus").as(String.class)), pattern)
                ));
            }

            String executedBy = firstNonBlank(filters.get("executedBy"), filters.get("usuario"), filters.get("user"));
            if (!executedBy.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("executedBy")), "%" + executedBy.toLowerCase(Locale.ROOT) + "%"));
            }

            String fileName = firstNonBlank(filters.get("fileName"), filters.get("archivo"), filters.get("file"));
            if (!fileName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("fileName")), "%" + fileName.toLowerCase(Locale.ROOT) + "%"));
            }

            String status = firstNonBlank(filters.get("finalStatus"), filters.get("estado"), filters.get("status"));
            if (!status.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("finalStatus").as(String.class)),
                        "%" + status.toLowerCase(Locale.ROOT) + "%"
                ));
            }

            String dateValue = firstNonBlank(filters.get("executionDate"), filters.get("fecha"), filters.get("date"));
            if (!dateValue.isBlank()) {
                try {
                    LocalDate date = LocalDate.parse(dateValue);
                    predicates.add(cb.equal(root.get("executionDate"), date));
                } catch (Exception ex) {
                    predicates.add(cb.disjunction());
                }
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort sanitizeSort(Sort requestedSort) {
        if (requestedSort == null || requestedSort.isUnsorted()) {
            return Sort.by(Sort.Order.desc("executedAt"));
        }
        List<Sort.Order> valid = requestedSort.stream()
                .filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty()))
                .toList();
        if (valid.isEmpty()) {
            return Sort.by(Sort.Order.desc("executedAt"));
        }
        return Sort.by(valid);
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String toDisplayUser(String value) {
        String normalized = safe(value, "");
        if (normalized.isBlank() || looksLikeIdentifier(normalized)) {
            return "Usuario no disponible";
        }
        return normalized;
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

    private String writeJson(Map<String, String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? Map.of() : values);
        } catch (JsonProcessingException ex) {
            throw TransportException.badRequest("TOUR_PROCESSING_HISTORY_JSON_WRITE_ERROR", "No se pudo serializar el detalle de depuración");
        }
    }

    private Map<String, String> readJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            throw TransportException.badRequest("TOUR_PROCESSING_HISTORY_JSON_READ_ERROR", "No se pudo deserializar el detalle de depuración");
        }
    }

    public record TourProcessingAuditData(
            String fileName,
            String fileType,
            int totalRowsRead,
            int validRows,
            int invalidRows,
            int duplicatesDetected,
            int requestedRows,
            boolean replaceExistingInRange
    ) {}

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TourProcessingDetailDraft {
        private Integer originalRowNumber;
        @Builder.Default
        private Map<String, String> originalData = Map.of();
        @Builder.Default
        private Map<String, String> normalizedData = Map.of();
        private String recordType;
        private String actionPerformed;
        private String processingResult;
        private String rejectionReason;
        private String observations;
        private String validationErrors;
    }
}
