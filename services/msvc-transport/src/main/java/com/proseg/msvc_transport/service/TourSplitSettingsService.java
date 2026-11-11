package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.TourSplitKeywordRequestDto;
import com.proseg.msvc_transport.dto.request.TourSplitSettingsRequestDto;
import com.proseg.msvc_transport.dto.response.TourSplitKeywordResponseDto;
import com.proseg.msvc_transport.dto.response.TourSplitSettingsResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourSplitMode;
import com.proseg.msvc_transport.entity.TourSplitKeyword;
import com.proseg.msvc_transport.entity.TourSplitSetting;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.TourSplitKeywordRepository;
import com.proseg.msvc_transport.repository.TourSplitSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourSplitSettingsService {

    private static final String RESOURCE = "Palabra de destino";

    private final TourSplitKeywordRepository keywordRepository;
    private final TourSplitSettingRepository settingRepository;
    private final TourSplitModeResolver modeResolver;

    @Transactional(readOnly = true)
    public List<TourSplitKeywordResponseDto> findKeywords(TourSplitMode mode) {
        return keywordRepository.findAllByModeOrderByTextAsc(mode).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TourSplitKeywordResponseDto findKeyword(UUID id) {
        return toResponse(keywordOf(id));
    }

    @Transactional
    public TourSplitKeywordResponseDto createKeyword(TourSplitKeywordRequestDto request) {
        String text = displayText(request.getText());
        String normalized = normalizedText(text);
        if (keywordRepository.existsByNormalizedText(normalized)) {
            throw duplicate();
        }
        TourSplitKeyword entity = new TourSplitKeyword();
        entity.setText(text);
        entity.setNormalizedText(normalized);
        entity.setMode(request.getMode());
        return toResponse(keywordRepository.save(entity));
    }

    @Transactional
    public TourSplitKeywordResponseDto updateKeyword(UUID id, TourSplitKeywordRequestDto request) {
        TourSplitKeyword entity = keywordOf(id);
        String text = displayText(request.getText());
        String normalized = normalizedText(text);
        if (keywordRepository.existsByNormalizedTextAndIdNot(normalized, id)) {
            throw duplicate();
        }
        entity.setText(text);
        entity.setNormalizedText(normalized);
        return toResponse(keywordRepository.save(entity));
    }

    @Transactional
    public void deleteKeyword(UUID id) {
        keywordRepository.delete(keywordOf(id));
    }

    @Transactional(readOnly = true)
    public TourSplitSettingsResponseDto findSettings() {
        return TourSplitSettingsResponseDto.builder().defaultMode(modeResolver.defaultMode()).build();
    }

    @Transactional
    public TourSplitSettingsResponseDto updateSettings(TourSplitSettingsRequestDto request) {
        TourSplitSetting setting = settingRepository.findAll().stream().findFirst().orElseGet(TourSplitSetting::new);
        setting.setDefaultMode(request.getDefaultMode());
        settingRepository.save(setting);
        return TourSplitSettingsResponseDto.builder().defaultMode(setting.getDefaultMode()).build();
    }

    private TourSplitKeyword keywordOf(UUID id) {
        return keywordRepository.findById(id).orElseThrow(() -> TransportException.notFound(RESOURCE, id.toString()));
    }

    private String displayText(String text) {
        return text == null ? "" : text.trim();
    }

    private String normalizedText(String text) {
        String normalized = ObservationKeywordMatcher.normalize(text);
        if (normalized.isEmpty()) {
            throw TransportException.badRequest("TOUR_SPLIT_KEYWORD_EMPTY", "El destino no puede estar vacío");
        }
        return normalized;
    }

    private TransportException duplicate() {
        return TransportException.conflict("TOUR_SPLIT_KEYWORD_DUPLICATE", "Ese destino ya está en uno de los dos bancos");
    }

    private TourSplitKeywordResponseDto toResponse(TourSplitKeyword entity) {
        return TourSplitKeywordResponseDto.builder()
                .id(entity.getId())
                .text(entity.getText())
                .mode(entity.getMode())
                .build();
    }
}
