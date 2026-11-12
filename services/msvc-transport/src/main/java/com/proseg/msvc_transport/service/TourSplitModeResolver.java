package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.tourprocessing.TourSplitMode;
import com.proseg.msvc_transport.entity.TourSplitKeyword;
import com.proseg.msvc_transport.repository.TourSplitKeywordRepository;
import com.proseg.msvc_transport.repository.TourSplitSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class TourSplitModeResolver {

    public static final TourSplitMode INITIAL_DEFAULT_MODE = TourSplitMode.DISTINCT_VEHICLES;

    private final TourSplitKeywordRepository keywordRepository;
    private final TourSplitSettingRepository settingRepository;

    public TourSplitMode defaultMode() {
        return settingRepository.findAll().stream()
                .findFirst()
                .map(setting -> setting.getDefaultMode())
                .orElse(INITIAL_DEFAULT_MODE);
    }

    public Function<String, TourSplitMode> byDestination() {
        List<TourSplitKeyword> keywords = keywordRepository.findAll();
        TourSplitMode fallback = defaultMode();
        return destination -> modeOf(destination, keywords, fallback);
    }

    static TourSplitMode modeOf(String destination, List<TourSplitKeyword> keywords, TourSplitMode fallback) {
        return keywords.stream()
                .filter(keyword -> ObservationKeywordMatcher.matches(destination, keyword.getText()))
                .max(Comparator.comparingInt((TourSplitKeyword keyword) -> keyword.getNormalizedText().length())
                        .thenComparing(keyword -> keyword.getMode() == TourSplitMode.DISTINCT_VEHICLES))
                .map(TourSplitKeyword::getMode)
                .orElse(fallback);
    }
}
