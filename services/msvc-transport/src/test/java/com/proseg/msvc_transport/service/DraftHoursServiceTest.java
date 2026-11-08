package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.response.DriverMonthSurplusCapResponseDto;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.TourApprovalStatus;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
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

class DraftHoursServiceTest {
    private DriverRepository driverRepository;
    private TourProcessingDraftDayRepository dayRepository;
    private DraftHoursService service;

    @BeforeEach
    void setUp() {
        driverRepository = mock(DriverRepository.class);
        dayRepository = mock(TourProcessingDraftDayRepository.class);
        service = new DraftHoursService(driverRepository, dayRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void syncReplacesPeriodHoursWithTheDraftAssignments() {
        Driver laura = Driver.builder().id(UUID.nameUUIDFromBytes("Laura Mora".getBytes())).name("Laura Mora").build();
        Driver pedro = Driver.builder().id(UUID.nameUUIDFromBytes("Pedro Solis".getBytes())).name("Pedro Solis").build();
        when(driverRepository.findAll()).thenReturn(List.of(laura, pedro));
        TourProcessingDraft draft = TourProcessingDraft.builder().year(2026).month(5).businessDays(28).build();

        service.syncAll(draft, List.of(
                row("1", "Laura Mora", 1, "2026-05-05", "07:30", "2026-05-05", "19:30"),
                row("1", "Laura Mora", 2, "2026-05-05", "07:30", "2026-05-05", "19:30"),
                row("2", "Laura Mora", 1, "2026-05-09", "08:00", "2026-05-09", "12:00"),
                row("3", "Nadie", 1, "2026-05-12", "08:00", "2026-05-12", "12:00")
        ));
        ArgumentCaptor<List<TourProcessingDraftDay>> saved = ArgumentCaptor.forClass(List.class);
        verify(dayRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(2);
        assertThat(saved.getValue().stream().mapToDouble(TourProcessingDraftDay::getOrdinaryHours).sum()).isEqualTo(9);
        assertThat(saved.getValue().stream().mapToDouble(TourProcessingDraftDay::getSurplusHours).sum()).isEqualTo(3);
        assertThat(saved.getValue().stream().mapToDouble(TourProcessingDraftDay::getOvertimeHours).sum()).isEqualTo(4);
    }

    @Test
    @SuppressWarnings("unchecked")
    void syncIgnoresNotApprovedToursAndClearsTheirDays() {
        Driver laura = Driver.builder().id(UUID.nameUUIDFromBytes("Laura Mora".getBytes())).name("Laura Mora").build();
        when(driverRepository.findAll()).thenReturn(List.of(laura));
        TourProcessingDraft draft = TourProcessingDraft.builder().year(2026).month(5).businessDays(28).build();
        TourProcessingDraftAssignment approved = row("1", "Laura Mora", 1, "2026-05-05", "07:30", "2026-05-05", "19:30");
        TourProcessingDraftAssignment notApproved = row("2", "Laura Mora", 1, "2026-05-12", "07:30", "2026-05-12", "19:30");
        notApproved.getTour().setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        TourProcessingDraftDay staleDay = day(notApproved, "2026-05-12");
        when(dayRepository.findAllByDraftId(draft.getId())).thenReturn(List.of(staleDay));

        service.syncAll(draft, List.of(approved, notApproved));

        verify(dayRepository).deleteAllByIdInBatch(List.of(staleDay.getId()));
    }

    @Test
    void syncLoadsTheDaysOfEveryRowInASingleQuery() {
        Driver laura = Driver.builder().id(UUID.nameUUIDFromBytes("Laura Mora".getBytes())).name("Laura Mora").build();
        when(driverRepository.findAll()).thenReturn(List.of(laura));
        TourProcessingDraft draft = TourProcessingDraft.builder().year(2026).month(5).businessDays(28).build();

        service.syncAll(draft, List.of(
                row("1", "Laura Mora", 1, "2026-05-05", "07:30", "2026-05-05", "19:30"),
                row("1", "Laura Mora", 2, "2026-05-05", "07:30", "2026-05-05", "19:30"),
                row("2", "Laura Mora", 1, "2026-05-07", "07:30", "2026-05-08", "12:00")
        ));

        verify(dayRepository).findAllByDraftId(draft.getId());
        verify(dayRepository, never()).deleteAllByIdInBatch(anyCollection());
    }

    @Test
    @SuppressWarnings("unchecked")
    void syncKeepsManuallyEditedDaysAndUpdatesTheAutomaticOnesInPlace() {
        Driver laura = Driver.builder().id(UUID.nameUUIDFromBytes("Laura Mora".getBytes())).name("Laura Mora").build();
        when(driverRepository.findAll()).thenReturn(List.of(laura));
        TourProcessingDraft draft = TourProcessingDraft.builder().year(2026).month(5).businessDays(28).build();
        TourProcessingDraftAssignment tour = row("1", "Laura Mora", 1, "2026-05-05", "07:30", "2026-05-06", "12:00");
        TourProcessingDraftDay manual = day(tour, "2026-05-05");
        manual.setManuallyEdited(true);
        manual.setOrdinaryHours(1D);
        TourProcessingDraftDay automatic = day(tour, "2026-05-06");
        TourProcessingDraftDay outOfRange = day(tour, "2026-05-07");
        when(dayRepository.findAllByDraftId(draft.getId())).thenReturn(List.of(manual, automatic, outOfRange));

        service.syncAll(draft, List.of(tour));

        ArgumentCaptor<List<TourProcessingDraftDay>> saved = ArgumentCaptor.forClass(List.class);
        verify(dayRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).containsExactly(automatic);
        assertThat(manual.getOrdinaryHours()).isEqualTo(1D);
        assertThat(automatic.getOrdinaryHours()).isEqualTo(4.5);
        verify(dayRepository).deleteAllByIdInBatch(List.of(outOfRange.getId()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void syncChangedOnlyRecomputesTheDriversWhoseToursChanged() {
        Driver laura = Driver.builder().id(UUID.nameUUIDFromBytes("Laura Mora".getBytes())).name("Laura Mora").build();
        Driver pedro = Driver.builder().id(UUID.nameUUIDFromBytes("Pedro Solis".getBytes())).name("Pedro Solis").build();
        when(driverRepository.findAll()).thenReturn(List.of(laura, pedro));
        TourProcessingDraft draft = TourProcessingDraft.builder().year(2026).month(5).businessDays(28).build();
        TourProcessingDraftAssignment pedroTour = row("1", "Pedro Solis", 1, "2026-05-05", "07:30", "2026-05-05", "19:30");
        TourProcessingDraftAssignment edited = row("2", "Laura Mora", 1, "2026-05-07", "07:30", "2026-05-07", "12:00");
        Map<UUID, Set<DraftHoursService.WorkloadSlot>> before = DraftHoursService.workloadSlots(List.of(pedroTour, edited));
        edited.getTour().setReturnTime(LocalTime.parse("19:30"));

        service.syncChanged(draft, before, List.of(pedroTour, edited));
        ArgumentCaptor<List<TourProcessingDraftDay>> saved = ArgumentCaptor.forClass(List.class);
        verify(dayRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(1);
        assertThat(saved.getValue().get(0).getAssignment()).isSameAs(edited);
        assertThat(saved.getValue().get(0).getOrdinaryHours()).isEqualTo(9);
        assertThat(saved.getValue().get(0).getSurplusHours()).isEqualTo(3);
    }

    @Test
    void syncChangedDoesNothingWhenNoDriverWorkloadChanged() {
        TourProcessingDraft draft = TourProcessingDraft.builder().year(2026).month(5).businessDays(28).build();
        TourProcessingDraftAssignment tour = row("1", "Laura Mora", 1, "2026-05-05", "07:30", "2026-05-05", "19:30");
        Map<UUID, Set<DraftHoursService.WorkloadSlot>> before = DraftHoursService.workloadSlots(List.of(tour));
        tour.setPassengers(20);

        service.syncChanged(draft, before, List.of(tour));

        verify(driverRepository, never()).findAll();
        verify(dayRepository, never()).findAllByDraftId(draft.getId());
    }

    @Test
    void syncSkipsDraftsWithoutPeriod() {
        service.syncAll(TourProcessingDraft.builder().build(), List.of());
        org.mockito.Mockito.verifyNoInteractions(driverRepository, dayRepository);

    }

    private TourProcessingDraftDay day(TourProcessingDraftAssignment row, String date) {
        return TourProcessingDraftDay.builder()
                .id(UUID.randomUUID())
                .assignment(row)
                .date(LocalDate.parse(date))
                .startTime(LocalTime.parse("07:30"))
                .endTime(LocalTime.parse("12:00"))
                .ordinaryHours(0D)
                .surplusHours(0D)
                .overtimeHours(0D)
                .build();
    }

    private TourProcessingDraftAssignment row(String number, String driver, int duplicateRank,
                                 String departureDate, String departureTime, String returnDate, String returnTime) {
        var tour = com.proseg.msvc_transport.entity.TourProcessingDraftTour.builder()
                .id(UUID.randomUUID())
                .number(number)
                .duplicateRank(duplicateRank)
                .departureDate(LocalDate.parse(departureDate))
                .departureTime(LocalTime.parse(departureTime))
                .returnDate(LocalDate.parse(returnDate))
                .returnTime(LocalTime.parse(returnTime))
                .build();
        return tour.addAssignment(TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).driver(driver)
                .driverId(UUID.nameUUIDFromBytes(driver.getBytes(java.nio.charset.StandardCharsets.UTF_8))).build());
    }
}
