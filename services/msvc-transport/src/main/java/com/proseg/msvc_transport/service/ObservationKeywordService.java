package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.ObservationKeywordRequestDto;
import com.proseg.msvc_transport.dto.response.ObservationKeywordResponseDto;
import com.proseg.msvc_transport.entity.ObservationKeyword;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.ObservationKeywordRepository;
import com.proseg.msvc_transport.specification.GenericSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObservationKeywordService {

    private static final String RESOURCE = "Palabra clave";

    private final ObservationKeywordRepository observationKeywordRepository;

    @Transactional
    public ObservationKeywordResponseDto create(ObservationKeywordRequestDto request) {
        String text = displayText(request.getText());
        String normalized = normalizedText(text);
        if (observationKeywordRepository.existsByNormalizedText(normalized)) {
            throw duplicate();
        }
        if (observationKeywordRepository.restoreDeleted(text, normalized) > 0) {
            return toResponse(observationKeywordRepository.findByNormalizedText(normalized)
                    .orElseThrow(() -> TransportException.notFound(RESOURCE, normalized)));
        }
        ObservationKeyword entity = new ObservationKeyword();
        entity.setText(text);
        entity.setNormalizedText(normalized);
        return toResponse(observationKeywordRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public ObservationKeywordResponseDto findById(UUID id) {
        return observationKeywordRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> TransportException.notFound(RESOURCE, id.toString()));
    }

    @Transactional(readOnly = true)
    public Page<ObservationKeywordResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<ObservationKeyword> spec = Specification.where(GenericSpecifications.withSearch(ObservationKeyword.class, search))
                .and(GenericSpecifications.withColumnFilters(ObservationKeyword.class, filters));
        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), GenericSpecifications.sanitizeSort(ObservationKeyword.class, pageable.getSort()));
        return observationKeywordRepository.findAll(spec, sanitized).map(this::toResponse);
    }

    @Transactional
    public ObservationKeywordResponseDto update(UUID id, ObservationKeywordRequestDto request) {
        ObservationKeyword entity = observationKeywordRepository.findById(id)
                .orElseThrow(() -> TransportException.notFound(RESOURCE, id.toString()));
        String text = displayText(request.getText());
        String normalized = normalizedText(text);
        if (observationKeywordRepository.existsByNormalizedTextAndIdNot(normalized, id)) {
            throw duplicate();
        }
        observationKeywordRepository.deleteDeletedByNormalizedText(normalized, id);
        entity.setText(text);
        entity.setNormalizedText(normalized);
        return toResponse(observationKeywordRepository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        ObservationKeyword entity = observationKeywordRepository.findById(id)
                .orElseThrow(() -> TransportException.notFound(RESOURCE, id.toString()));
        observationKeywordRepository.delete(entity);
    }

    private String displayText(String text) {
        return text == null ? "" : text.trim();
    }

    private String normalizedText(String text) {
        String normalized = ObservationKeywordMatcher.normalize(text);
        if (normalized.isEmpty()) {
            throw TransportException.badRequest("OBSERVATION_KEYWORD_EMPTY", "La palabra clave no puede estar vacía");
        }
        return normalized;
    }

    private TransportException duplicate() {
        return TransportException.conflict("OBSERVATION_KEYWORD_DUPLICATE", "Ya existe una palabra clave con ese texto");
    }

    private ObservationKeywordResponseDto toResponse(ObservationKeyword entity) {
        return ObservationKeywordResponseDto.builder()
                .id(entity.getId())
                .text(entity.getText())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
