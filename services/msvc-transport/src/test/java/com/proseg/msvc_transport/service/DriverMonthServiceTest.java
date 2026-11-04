package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.response.DriverMonthSurplusCapResponseDto;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.TourApprovalStatus;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DriverMonthServiceTest {

    private DriverRepository driverRepository;
    private TourProcessingDraftRepository draftRepository;
    private TourProcessingDraftDayRepository dayRepository;
    private DriverMonthService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        driverRepository = mock(DriverRepository.class);
        draftRepository = mock(TourProcessingDraftRepository.class);
        dayRepository = mock(TourProcessingDraftDayRepository.class);
        when(dayRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new DriverMonthService(driverRepository, draftRepository, dayRepository);
    }

    @Test
    void surplusCapComesFromTheDraftBusinessDays() {
        TourProcessingDraft draft = TourProcessingDraft.builder().year(2026).month(5).businessDays(28).build();
        when(draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(2026, 5)).thenReturn(Optional.of(draft));

        DriverMonthSurplusCapResponseDto cap = service.findSurplusCap(2026, 5);

        assertThat(cap.getBusinessDays()).isEqualTo(28);
        assertThat(cap.getSurplusHoursPerDriver()).isEqualTo(37.5);
    }

    @Test
    void surplusCapIsEmptyWithoutDraftOrBusinessDays() {
        when(draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(2026, 6)).thenReturn(Optional.empty());
        when(draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(2026, 7))
                .thenReturn(Optional.of(TourProcessingDraft.builder().year(2026).month(7).build()));

        assertThat(service.findSurplusCap(2026, 6)).extracting(
                DriverMonthSurplusCapResponseDto::getBusinessDays,
                DriverMonthSurplusCapResponseDto::getSurplusHoursPerDriver).containsOnlyNulls();
        assertThat(service.findSurplusCap(2026, 7)).extracting(
                DriverMonthSurplusCapResponseDto::getBusinessDays,
                DriverMonthSurplusCapResponseDto::getSurplusHoursPerDriver).containsOnlyNulls();
    }

}
