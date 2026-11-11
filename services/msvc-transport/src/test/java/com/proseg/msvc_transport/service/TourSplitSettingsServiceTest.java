package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.TourSplitKeywordRequestDto;
import com.proseg.msvc_transport.dto.request.TourSplitSettingsRequestDto;
import com.proseg.msvc_transport.dto.response.TourSplitKeywordResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourSplitMode;
import com.proseg.msvc_transport.entity.TourSplitKeyword;
import com.proseg.msvc_transport.entity.TourSplitSetting;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.TourSplitKeywordRepository;
import com.proseg.msvc_transport.repository.TourSplitSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TourSplitSettingsServiceTest {

    private TourSplitKeywordRepository keywords;
    private TourSplitSettingRepository settings;
    private TourSplitSettingsService service;

    @BeforeEach
    void setUp() {
        keywords = mock(TourSplitKeywordRepository.class);
        settings = mock(TourSplitSettingRepository.class);
        when(keywords.save(any(TourSplitKeyword.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(settings.save(any(TourSplitSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new TourSplitSettingsService(keywords, settings, new TourSplitModeResolver(keywords, settings));
    }

    @Test
    void createsAWordInTheRequestedBankWithItsNormalizedText() {
        TourSplitKeywordResponseDto response = service.createKeyword(request(" San José ", TourSplitMode.SAME_DRIVER_AND_VEHICLE));

        ArgumentCaptor<TourSplitKeyword> saved = ArgumentCaptor.forClass(TourSplitKeyword.class);
        verify(keywords).save(saved.capture());
        assertThat(response.getText()).isEqualTo("San José");
        assertThat(saved.getValue().getNormalizedText()).isEqualTo("san jose");
        assertThat(saved.getValue().getMode()).isEqualTo(TourSplitMode.SAME_DRIVER_AND_VEHICLE);
    }

    @Test
    void rejectsAWordThatAlreadyExistsInEitherBank() {
        when(keywords.existsByNormalizedText("san jose")).thenReturn(true);

        assertThatThrownBy(() -> service.createKeyword(request("SAN JOSÉ", TourSplitMode.DISTINCT_VEHICLES)))
                .isInstanceOf(TransportException.class)
                .extracting(error -> ((TransportException) error).getHttpStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(keywords, never()).save(any());
    }

    @Test
    void reportsDistinctDriversAsTheDefaultUntilASwitchIsSaved() {
        when(settings.findAll()).thenReturn(List.of());

        assertThat(service.findSettings().getDefaultMode()).isEqualTo(TourSplitMode.DISTINCT_VEHICLES);
    }

    @Test
    void savesTheActiveSwitchReusingTheExistingSettingRow() {
        TourSplitSetting existing = TourSplitSetting.builder().defaultMode(TourSplitMode.DISTINCT_VEHICLES).build();
        when(settings.findAll()).thenReturn(List.of(existing));

        var response = service.updateSettings(TourSplitSettingsRequestDto.builder().defaultMode(TourSplitMode.SAME_DRIVER_AND_VEHICLE).build());

        assertThat(response.getDefaultMode()).isEqualTo(TourSplitMode.SAME_DRIVER_AND_VEHICLE);
        assertThat(existing.getDefaultMode()).isEqualTo(TourSplitMode.SAME_DRIVER_AND_VEHICLE);
        verify(settings).save(existing);
    }

    private TourSplitKeywordRequestDto request(String text, TourSplitMode mode) {
        return TourSplitKeywordRequestDto.builder().text(text).mode(mode).build();
    }
}
