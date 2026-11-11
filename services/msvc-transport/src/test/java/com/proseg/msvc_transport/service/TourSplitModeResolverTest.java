package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.tourprocessing.TourSplitMode;
import com.proseg.msvc_transport.entity.TourSplitKeyword;
import com.proseg.msvc_transport.entity.TourSplitSetting;
import com.proseg.msvc_transport.repository.TourSplitKeywordRepository;
import com.proseg.msvc_transport.repository.TourSplitSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TourSplitModeResolverTest {

    private TourSplitKeywordRepository keywords;
    private TourSplitSettingRepository settings;
    private TourSplitModeResolver resolver;

    @BeforeEach
    void setUp() {
        keywords = mock(TourSplitKeywordRepository.class);
        settings = mock(TourSplitSettingRepository.class);
        resolver = new TourSplitModeResolver(keywords, settings);
    }

    @Test
    void usesDistinctDriversByDefaultWhenNothingIsConfigured() {
        when(keywords.findAll()).thenReturn(List.of());
        when(settings.findAll()).thenReturn(List.of());

        assertThat(resolver.byDestination().apply("Limón")).isEqualTo(TourSplitMode.DISTINCT_VEHICLES);
    }

    @Test
    void usesTheModeOfTheBankWhoseWordIsInsideTheDestinationIgnoringCaseAndAccents() {
        when(keywords.findAll()).thenReturn(List.of(keyword("san jose", TourSplitMode.SAME_DRIVER_AND_VEHICLE)));
        when(settings.findAll()).thenReturn(List.of(setting(TourSplitMode.DISTINCT_VEHICLES)));

        assertThat(resolver.byDestination().apply("Congreso en San José")).isEqualTo(TourSplitMode.SAME_DRIVER_AND_VEHICLE);
    }

    @Test
    void fallsBackToTheActiveSwitchWhenNoWordMatches() {
        when(keywords.findAll()).thenReturn(List.of(keyword("cartago", TourSplitMode.DISTINCT_VEHICLES)));
        when(settings.findAll()).thenReturn(List.of(setting(TourSplitMode.SAME_DRIVER_AND_VEHICLE)));

        assertThat(resolver.byDestination().apply("Limón")).isEqualTo(TourSplitMode.SAME_DRIVER_AND_VEHICLE);
        assertThat(resolver.byDestination().apply("Cartago")).isEqualTo(TourSplitMode.DISTINCT_VEHICLES);
    }

    @Test
    void prefersTheLongestMatchingWordWhenBothBanksMatch() {
        List<TourSplitKeyword> both = List.of(
                keyword("san jose", TourSplitMode.DISTINCT_VEHICLES),
                keyword("san jose centro", TourSplitMode.SAME_DRIVER_AND_VEHICLE)
        );

        assertThat(TourSplitModeResolver.modeOf("Gira San José Centro", both, TourSplitMode.DISTINCT_VEHICLES))
                .isEqualTo(TourSplitMode.SAME_DRIVER_AND_VEHICLE);
    }

    private TourSplitKeyword keyword(String text, TourSplitMode mode) {
        return TourSplitKeyword.builder().text(text).normalizedText(ObservationKeywordMatcher.normalize(text)).mode(mode).build();
    }

    private TourSplitSetting setting(TourSplitMode mode) {
        return TourSplitSetting.builder().defaultMode(mode).build();
    }
}
