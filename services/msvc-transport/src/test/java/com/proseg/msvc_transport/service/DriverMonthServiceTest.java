package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.response.DriverMonthSurplusCapResponseDto;
import com.proseg.msvc_transport.entity.CleaningDraft;
import com.proseg.msvc_transport.entity.CleaningDraftRow;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.DriverMonth;
import com.proseg.msvc_transport.repository.CleaningDraftRepository;
import com.proseg.msvc_transport.repository.CleaningDraftRowDayRepository;
import com.proseg.msvc_transport.repository.DriverMonthRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DriverMonthServiceTest {

    private DriverMonthRepository driverMonthRepository;
    private DriverRepository driverRepository;
    private CleaningDraftRepository draftRepository;
    private CleaningDraftRowDayRepository dayRepository;
    private DriverMonthService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        driverMonthRepository = mock(DriverMonthRepository.class);
        driverRepository = mock(DriverRepository.class);
        draftRepository = mock(CleaningDraftRepository.class);
        dayRepository = mock(CleaningDraftRowDayRepository.class);
        when(dayRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new DriverMonthService(driverMonthRepository, driverRepository, draftRepository, dayRepository);
    }

    @Test
    void surplusCapComesFromTheDraftBusinessDays() {
        CleaningDraft draft = CleaningDraft.builder().year(2026).month(5).businessDays(28).build();
        when(draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(2026, 5)).thenReturn(Optional.of(draft));

        DriverMonthSurplusCapResponseDto cap = service.findSurplusCap(2026, 5);

        assertThat(cap.getBusinessDays()).isEqualTo(28);
        assertThat(cap.getSurplusHoursPerDriver()).isEqualTo(37.5);
    }

    @Test
    void surplusCapIsEmptyWithoutDraftOrBusinessDays() {
        when(draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(2026, 6)).thenReturn(Optional.empty());
        when(draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(2026, 7))
                .thenReturn(Optional.of(CleaningDraft.builder().year(2026).month(7).build()));

        assertThat(service.findSurplusCap(2026, 6)).extracting(
                DriverMonthSurplusCapResponseDto::getBusinessDays,
                DriverMonthSurplusCapResponseDto::getSurplusHoursPerDriver).containsOnlyNulls();
        assertThat(service.findSurplusCap(2026, 7)).extracting(
                DriverMonthSurplusCapResponseDto::getBusinessDays,
                DriverMonthSurplusCapResponseDto::getSurplusHoursPerDriver).containsOnlyNulls();
    }

    @Test
    @SuppressWarnings("unchecked")
    void syncReplacesPeriodHoursWithTheDraftAssignments() {
        Driver laura = Driver.builder().id(UUID.randomUUID()).name("Laura Mora").build();
        Driver pedro = Driver.builder().id(UUID.randomUUID()).name("Pedro Solis").build();
        DriverMonth stalePedro = DriverMonth.builder()
                .driver(pedro).year(2026).month(5)
                .ordinaryHoursAccrued(10D).surplusHoursAccrued(5D).overtimeHoursAccrued(4D).weekendsWorked(1)
                .build();
        when(driverRepository.findAll()).thenReturn(List.of(laura, pedro));
        when(driverMonthRepository.findByYearAndMonth(2026, 5)).thenReturn(List.of(stalePedro));
        CleaningDraft draft = CleaningDraft.builder().year(2026).month(5).businessDays(28).build();

        service.syncWithDraft(draft, List.of(
                row("1", "laura  mora", 1, "2026-05-05", "07:30", "2026-05-05", "19:30"),
                row("1", "laura  mora", 2, "2026-05-05", "07:30", "2026-05-05", "19:30"),
                row("2", "Laura Mora", 1, "2026-05-09", "08:00", "2026-05-09", "12:00"),
                row("3", "Nadie", 1, "2026-05-12", "08:00", "2026-05-12", "12:00")
        ));

        ArgumentCaptor<List<DriverMonth>> saved = ArgumentCaptor.forClass(List.class);
        verify(driverMonthRepository).saveAll(saved.capture());
        assertThat(saved.getValue())
                .extracting(dm -> dm.getDriver().getName(), DriverMonth::getOrdinaryHoursAccrued,
                        DriverMonth::getSurplusHoursAccrued, DriverMonth::getOvertimeHoursAccrued, DriverMonth::getWeekendsWorked)
                .containsExactly(
                        tuple("Laura Mora", 9.0, 3.0, 4.0, 1),
                        tuple("Pedro Solis", 0.0, 0.0, 0.0, 0)
                );
    }

    @Test
    void syncSkipsDraftsWithoutPeriod() {
        service.syncWithDraft(CleaningDraft.builder().build(), List.of());

        verify(driverMonthRepository, never()).findByYearAndMonth(anyInt(), anyInt());
        verify(driverMonthRepository, never()).saveAll(anyList());
    }

    private CleaningDraftRow row(String number, String driver, int duplicateRank,
                                 String departureDate, String departureTime, String returnDate, String returnTime) {
        return CleaningDraftRow.builder()
                .id(UUID.randomUUID())
                .number(number)
                .driver(driver)
                .duplicateRank(duplicateRank)
                .departureDate(LocalDate.parse(departureDate))
                .departureTime(LocalTime.parse(departureTime))
                .returnDate(LocalDate.parse(returnDate))
                .returnTime(LocalTime.parse(returnTime))
                .build();
    }
}
