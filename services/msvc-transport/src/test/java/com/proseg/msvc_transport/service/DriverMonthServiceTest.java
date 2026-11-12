package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.response.DriverMonthResponseDto;
import com.proseg.msvc_transport.dto.response.DriverMonthSurplusCapResponseDto;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DriverMonthServiceTest {

    private final UUID draftId = UUID.randomUUID();
    private final Driver ana = Driver.builder().id(UUID.randomUUID()).name("Ana").status("ACTIVE").build();
    private final Driver beto = Driver.builder().id(UUID.randomUUID()).name("Beto").status("ACTIVE").build();

    private DriverRepository driverRepository;
    private TourProcessingDraftRepository draftRepository;
    private TourProcessingDraftDayRepository dayRepository;
    private DraftHoursService draftHoursService;
    private DraftTourStore tourStore;
    private InMemoryBusinessDays businessDays;
    private DriverMonthService service;

    @BeforeEach
    void setUp() {
        driverRepository = mock(DriverRepository.class);
        draftRepository = mock(TourProcessingDraftRepository.class);
        dayRepository = mock(TourProcessingDraftDayRepository.class);
        draftHoursService = mock(DraftHoursService.class);
        tourStore = mock(DraftTourStore.class);
        businessDays = new InMemoryBusinessDays();
        when(driverRepository.findAll()).thenReturn(List.of(ana, beto));
        when(driverRepository.findById(ana.getId())).thenReturn(Optional.of(ana));
        service = new DriverMonthService(driverRepository, draftRepository, dayRepository, businessDays.service(), draftHoursService, tourStore);
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

    @Test
    void everyDriverStartsWithTheBusinessDaysOfTheMonth() {
        stubDraft(TourProcessingDraftStatus.DRAFT, 28);

        List<DriverMonthResponseDto> months = service.findAllForPeriod(2026, 5);

        assertThat(months).extracting(DriverMonthResponseDto::getBusinessDays).containsExactly(28, 28);
        assertThat(months).extracting(DriverMonthResponseDto::getSurplusCap).containsExactly(37.5, 37.5);
        assertThat(months).extracting(DriverMonthResponseDto::isBusinessDaysCustomized).containsExactly(false, false);
    }

    @Test
    void aDriverWithFewerBusinessDaysHasASmallerMaxAndTheOthersKeepTheMonthValue() {
        stubDraft(TourProcessingDraftStatus.DRAFT, 28);
        businessDays.customize(ana.getId(), 2026, 5, 21);

        List<DriverMonthResponseDto> months = service.findAllForPeriod(2026, 5);

        DriverMonthResponseDto anaMonth = months.stream().filter(month -> ana.getId().equals(month.getDriverId())).findFirst().orElseThrow();
        DriverMonthResponseDto betoMonth = months.stream().filter(month -> beto.getId().equals(month.getDriverId())).findFirst().orElseThrow();
        assertThat(anaMonth.getBusinessDays()).isEqualTo(21);
        assertThat(anaMonth.getSurplusCap()).isEqualTo(28.0);
        assertThat(anaMonth.isBusinessDaysCustomized()).isTrue();
        assertThat(betoMonth.getBusinessDays()).isEqualTo(28);
        assertThat(betoMonth.getSurplusCap()).isEqualTo(37.5);
        assertThat(betoMonth.isBusinessDaysCustomized()).isFalse();
    }

    @Test
    void monthsWithoutDraftHaveNoBusinessDaysNorMax() {
        when(draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(2026, 6)).thenReturn(Optional.empty());

        List<DriverMonthResponseDto> months = service.findAllForPeriod(2026, 6);

        assertThat(months).extracting(DriverMonthResponseDto::getBusinessDays).containsOnlyNulls();
        assertThat(months).extracting(DriverMonthResponseDto::getSurplusCap).containsOnlyNulls();
    }

    @Test
    void editingStoresTheValueAndRecalculatesOnlyThatDriversHours() {
        TourProcessingDraft draft = stubDraft(TourProcessingDraftStatus.DRAFT, 28);
        TourProcessingDraftAssignment assignment = assignmentOf(ana);
        TourProcessingDraftTour tour = assignment.getTour();
        when(tourStore.findAllByDraftIdOrderByRowNumberAsc(draftId)).thenReturn(List.of(tour));

        DriverMonthResponseDto response = service.updateBusinessDays(ana.getId(), 2026, 5, 21);

        assertThat(businessDays.storedValue(ana.getId(), 2026, 5)).isEqualTo(21);
        verify(draftHoursService).syncDriver(draft, List.of(assignment), ana.getId());
        assertThat(response.getDriverId()).isEqualTo(ana.getId());
        assertThat(response.getBusinessDays()).isEqualTo(21);
        assertThat(response.getSurplusCap()).isEqualTo(28.0);
        assertThat(response.isBusinessDaysCustomized()).isTrue();
    }

    @Test
    void editingNeverRerunsTheAutoAssignmentNorTouchesTheTours() {
        stubDraft(TourProcessingDraftStatus.DRAFT, 28);
        TourProcessingDraftAssignment assignment = assignmentOf(ana);
        when(tourStore.findAllByDraftIdOrderByRowNumberAsc(draftId)).thenReturn(List.of(assignment.getTour()));

        service.updateBusinessDays(ana.getId(), 2026, 5, 3);

        assertThat(assignment.getDriverId()).isEqualTo(ana.getId());
        assertThat(assignment.getDriver()).isEqualTo("Ana");
        verify(tourStore).findAllByDraftIdOrderByRowNumberAsc(draftId);
        verifyNoMoreInteractions(tourStore);
    }

    @Test
    void anotherValueForTheMonthDefaultKeepsTheEditedDriverAndMovesTheRest() {
        TourProcessingDraft draft = stubDraft(TourProcessingDraftStatus.DRAFT, 28);
        when(tourStore.findAllByDraftIdOrderByRowNumberAsc(draftId)).thenReturn(List.of());
        service.updateBusinessDays(ana.getId(), 2026, 5, 21);

        draft.setBusinessDays(22);
        List<DriverMonthResponseDto> months = service.findAllForPeriod(2026, 5);

        assertThat(months).extracting(DriverMonthResponseDto::getDriverName, DriverMonthResponseDto::getBusinessDays)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Ana", 21), org.assertj.core.groups.Tuple.tuple("Beto", 22));
    }

    @Test
    void nullRestoresTheMonthValueForThatDriver() {
        stubDraft(TourProcessingDraftStatus.DRAFT, 28);
        when(tourStore.findAllByDraftIdOrderByRowNumberAsc(draftId)).thenReturn(List.of());
        service.updateBusinessDays(ana.getId(), 2026, 5, 21);

        DriverMonthResponseDto response = service.updateBusinessDays(ana.getId(), 2026, 5, null);

        assertThat(businessDays.storedRecords()).isZero();
        assertThat(response.getBusinessDays()).isEqualTo(28);
        assertThat(response.isBusinessDaysCustomized()).isFalse();
    }

    @Test
    void acceptsZeroAndTheLastDayOfTheMonth() {
        stubDraft(TourProcessingDraftStatus.DRAFT, 28);
        when(tourStore.findAllByDraftIdOrderByRowNumberAsc(draftId)).thenReturn(List.of());

        assertThat(service.updateBusinessDays(ana.getId(), 2026, 5, 0).getSurplusCap()).isZero();
        assertThat(service.updateBusinessDays(ana.getId(), 2026, 5, 31).getBusinessDays()).isEqualTo(31);
    }

    @Test
    void rejectsValuesOutsideTheDaysOfTheMonth() {
        stubDraft(TourProcessingDraftStatus.DRAFT, 28);

        assertThatThrownBy(() -> service.updateBusinessDays(ana.getId(), 2026, 5, -1))
                .isInstanceOf(TransportException.class).hasMessage("Los días hábiles deben estar entre 0 y 31");
        assertThatThrownBy(() -> service.updateBusinessDays(ana.getId(), 2026, 5, 32))
                .isInstanceOf(TransportException.class).hasMessage("Los días hábiles deben estar entre 0 y 31");
        assertThat(businessDays.storedRecords()).isZero();
        verify(draftHoursService, never()).syncDriver(any(), anyList(), any());
    }

    @Test
    void rejectsEditingAClosedOrRegisteredMonth() {
        stubDraft(TourProcessingDraftStatus.CLOSED, 28);
        assertThatThrownBy(() -> service.updateBusinessDays(ana.getId(), 2026, 5, 21))
                .isInstanceOf(TransportException.class)
                .hasMessage("No se puede editar una depuración terminada. Devuélvela a borrador");

        stubDraft(TourProcessingDraftStatus.REGISTERED, 28);
        assertThatThrownBy(() -> service.updateBusinessDays(ana.getId(), 2026, 5, 21))
                .isInstanceOf(TransportException.class)
                .hasMessage("No se puede editar un borrador que ya fue registrado");
        assertThat(businessDays.storedRecords()).isZero();
        verify(draftHoursService, never()).syncDriver(any(), anyList(), any());
    }

    @Test
    void rejectsEditingAMonthWithoutDraft() {
        when(draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(2026, 6)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateBusinessDays(ana.getId(), 2026, 6, 21))
                .isInstanceOf(TransportException.class)
                .hasMessage("Importa el archivo del mes antes de cambiar los días hábiles de un chofer");
        assertThat(businessDays.storedRecords()).isZero();
    }

    @Test
    void rejectsEditingAnUnknownDriverOrAnInvalidMonth() {
        assertThatThrownBy(() -> service.updateBusinessDays(UUID.randomUUID(), 2026, 5, 21))
                .isInstanceOf(TransportException.class);
        assertThatThrownBy(() -> service.updateBusinessDays(ana.getId(), 2026, 13, 21))
                .isInstanceOf(TransportException.class).hasMessage("El mes debe estar entre 1 y 12");
        assertThat(businessDays.storedRecords()).isZero();
    }

    private TourProcessingDraft stubDraft(TourProcessingDraftStatus status, int defaultBusinessDays) {
        TourProcessingDraft draft = TourProcessingDraft.builder().id(draftId).status(status)
                .year(2026).month(5).businessDays(defaultBusinessDays).build();
        when(draftRepository.findFirstByYearAndMonthOrderByImportedAtDesc(eq(2026), eq(5))).thenReturn(Optional.of(draft));
        when(draftRepository.findByIdForUpdate(draftId)).thenReturn(Optional.of(draft));
        return draft;
    }

    private TourProcessingDraftAssignment assignmentOf(Driver driver) {
        TourProcessingDraftTour tour = TourProcessingDraftTour.builder()
                .id(UUID.randomUUID()).number("1").duplicateRank(1)
                .departureDate(LocalDate.of(2026, 5, 5)).departureTime(LocalTime.of(8, 0))
                .returnDate(LocalDate.of(2026, 5, 5)).returnTime(LocalTime.of(18, 0))
                .build();
        return tour.addAssignment(TourProcessingDraftAssignment.builder().id(UUID.randomUUID())
                .driver(driver.getName()).driverId(driver.getId()).build());
    }
}
