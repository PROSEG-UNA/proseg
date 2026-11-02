package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingRegisterResponseDto;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourApprovalStatus;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.VehicleRepository;
import com.proseg.msvc_transport.security.CurrentUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TourProcessingServiceTest {

    private static final UUID DRAFT_ID = UUID.randomUUID();
    private static final UUID EXECUTION_ID = UUID.randomUUID();

    private DriverRepository driverRepository;
    private VehicleRepository vehicleRepository;
    private TourProcessingDraftRepository draftRepository;
    private DraftTourStore rowRepository;
    private TourProcessingHistoryService historyService;
    private TourProcessingService service;
    private TourProcessingDraft draft;

    @BeforeEach
    void setUp() {
        driverRepository = mock(DriverRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        draftRepository = mock(TourProcessingDraftRepository.class);
        rowRepository = mock(DraftTourStore.class);
        historyService = mock(TourProcessingHistoryService.class);
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);

        when(currentUserResolver.resolve()).thenReturn("aleja");
        when(driverRepository.findFirstByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(vehicleRepository.findByPlateIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyService.recordSuccess(any(), any(), anyString(), any(), any(), any(), any(), anyList())).thenReturn(EXECUTION_ID);

        draft = TourProcessingDraft.builder()
                .id(DRAFT_ID)
                .fileName("1-mayo.xls")
                .fileType("HTML")
                .status(TourProcessingDraftStatus.DRAFT)
                .totalRows(3)
                .duplicateRows(1)
                .conflictingRows(0)
                .build();
        when(draftRepository.findByIdForUpdate(DRAFT_ID)).thenReturn(Optional.of(draft));

        service = new TourProcessingService(
                driverRepository,
                vehicleRepository,
                draftRepository,
                rowRepository,
                historyService,
                mock(DraftHoursService.class),
                currentUserResolver
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void registersOneTourPerNumberAndMarksDraftAsRegistered() {
        when(rowRepository.findAllByDraftIdOrderByRowNumberAsc(DRAFT_ID)).thenReturn(List.of(
                row(8, "03625", "301-295", 1, 2, false),
                row(12, "03625", "301-295", 2, 2, false),
                row(16, "03082", "Todo Terreno", 1, 1, false)
        ));

        TourProcessingRegisterResponseDto response = service.registerDraft(DRAFT_ID, false);

        assertThat(response.getImportedRows()).isEqualTo(2);
        assertThat(response.getCreatedVehicles()).isEqualTo(1);
        assertThat(response.getTourProcessingExecutionId()).isEqualTo(EXECUTION_ID);
        assertThat(draft.getStatus()).isEqualTo(TourProcessingDraftStatus.REGISTERED);
        assertThat(draft.getTourProcessingExecutionId()).isEqualTo(EXECUTION_ID);
        assertThat(draft.getRegisteredAt()).isNotNull();

        ArgumentCaptor<Vehicle> vehicleCaptor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(vehicleCaptor.capture());
        assertThat(vehicleCaptor.getValue().getPlate()).isEqualTo("301-295");
        assertThat(vehicleCaptor.getValue().getType()).isNull();

        ArgumentCaptor<Driver> driverCaptor = ArgumentCaptor.forClass(Driver.class);
        verify(driverRepository).save(driverCaptor.capture());
        assertThat(driverCaptor.getValue().getName()).isEqualTo("Daniel Zuniga");
        assertThat(response.getCreatedDrivers()).isEqualTo(1);
        assertThat(response.getUpdatedDrivers()).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void leavesNotApprovedToursOutOfTheRegistration() {
        TourProcessingDraftTour approved = row(8, "03625", "301-295", 1, 1, false);
        TourProcessingDraftTour notApproved = row(16, "03082", "301-300", 1, 1, false);
        notApproved.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        when(rowRepository.findAllByDraftIdOrderByRowNumberAsc(DRAFT_ID)).thenReturn(List.of(approved, notApproved));

        TourProcessingRegisterResponseDto response = service.registerDraft(DRAFT_ID, false);

        assertThat(response.getRequestedRows()).isEqualTo(1);
        assertThat(response.getImportedRows()).isEqualTo(1);

        ArgumentCaptor<List<TourProcessingHistoryService.TourProcessingDetailDraft>> detailsCaptor = ArgumentCaptor.forClass(List.class);
        verify(historyService).recordSuccess(any(), any(), anyString(), any(), any(), any(), any(), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue())
                .filteredOn(detail -> "IGNORED_NOT_APPROVED".equals(detail.getActionPerformed()))
                .extracting(TourProcessingHistoryService.TourProcessingDetailDraft::getOriginalRowNumber,
                        TourProcessingHistoryService.TourProcessingDetailDraft::getProcessingResult)
                .containsExactly(tuple(16, "SKIPPED"));
    }

    @Test
    void rejectsRegistrationWhenEveryTourIsNotApproved() {
        TourProcessingDraftTour notApproved = row(8, "03625", "301-295", 1, 1, false);
        notApproved.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        when(rowRepository.findAllByDraftIdOrderByRowNumberAsc(DRAFT_ID)).thenReturn(List.of(notApproved));

        assertThatThrownBy(() -> service.registerDraft(DRAFT_ID, false))
                .isInstanceOf(TransportException.class)
                .hasMessage("No hay filas para registrar");
        assertThat(draft.getStatus()).isEqualTo(TourProcessingDraftStatus.DRAFT);
    }

    @Test
    void doesNotBlockRegistrationForNotApprovedExternalToursPendingContracting() {
        TourProcessingDraftTour approved = row(8, "03625", "301-295", 1, 1, false);
        TourProcessingDraftTour pendingExternal = row(16, "03082", null, 1, 1, false);
        pendingExternal.primaryAssignment().setResourceType(ResourceType.EXTERNAL);
        pendingExternal.primaryAssignment().setDriver(null);
        pendingExternal.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        when(rowRepository.findAllByDraftIdOrderByRowNumberAsc(DRAFT_ID)).thenReturn(List.of(approved, pendingExternal));

        TourProcessingRegisterResponseDto response = service.registerDraft(DRAFT_ID, false);

        assertThat(response.getImportedRows()).isEqualTo(1);
        assertThat(draft.getStatus()).isEqualTo(TourProcessingDraftStatus.REGISTERED);
    }

    @Test
    void blocksRegistrationWhenARepeatedNumberHasDifferentData() {
        when(rowRepository.findAllByDraftIdOrderByRowNumberAsc(DRAFT_ID)).thenReturn(List.of(
                row(8, "03625", "301-295", 1, 2, false),
                row(12, "03625", "301-295", 2, 2, true)
        ));

        assertThatThrownBy(() -> service.registerDraft(DRAFT_ID, false))
                .isInstanceOf(TransportException.class)
                .hasMessage("Hay números de gira repetidos con datos distintos: 03625 (filas 8, 12)");
        assertThat(draft.getStatus()).isEqualTo(TourProcessingDraftStatus.DRAFT);
    }

    @Test
    @SuppressWarnings("unchecked")
    void registersDistributedRowsAsOneTourWithTwoAssignments() {
        TourProcessingDraftTour first = row(8, "03170", "301-569", 1, 1, false);
        first.setPassengers(70);
        first.primaryAssignment().setPassengers(40);
        TourProcessingDraftTour second = row(9, "03170", "301-590", 1, 1, false);
        second.primaryAssignment().setPassengers(30);
        second.primaryAssignment().setDriver("Marvin Quesada");
        first.addAssignment(second.primaryAssignment());
        when(rowRepository.findAllByDraftIdOrderByRowNumberAsc(DRAFT_ID)).thenReturn(List.of(first));

        TourProcessingRegisterResponseDto response = service.registerDraft(DRAFT_ID, false);

        assertThat(response.getImportedRows()).isEqualTo(1);
        assertThat(response.getRequestedRows()).isEqualTo(1);
    }

    @Test
    void rejectsRegistrationWhenDistributedPassengersDoNotMatchTheTourTotal() {
        TourProcessingDraftTour first = row(8, "03170", "301-569", 1, 1, false);
        first.setPassengers(70);
        first.primaryAssignment().setPassengers(40);
        TourProcessingDraftTour second = row(9, "03170", "301-590", 1, 1, false);
        second.primaryAssignment().setPassengers(29);
        first.addAssignment(second.primaryAssignment());
        when(rowRepository.findAllByDraftIdOrderByRowNumberAsc(DRAFT_ID)).thenReturn(List.of(first));

        assertThatThrownBy(() -> service.registerDraft(DRAFT_ID, false))
                .isInstanceOf(TransportException.class)
                .hasMessage("La distribución de pasajeros de una gira no coincide con su total");
    }

    @Test
    void rejectsADraftThatWasAlreadyRegistered() {
        draft.setStatus(TourProcessingDraftStatus.REGISTERED);

        assertThatThrownBy(() -> service.registerDraft(DRAFT_ID, false))
                .isInstanceOf(TransportException.class)
                .hasMessage("El borrador ya fue registrado");
    }

    private TourProcessingDraftTour row(int rowNumber, String number, String vehicle, int rank, int groupSize, boolean conflicting) {
        TourProcessingDraftTour tour = TourProcessingDraftTour.builder()
                .draft(draft)
                .rowNumber(rowNumber)
                .number(number)
                .passengers(3)
                .executingUnit("Division De Educacion Rural Cide")
                .responsible("Selvin Fallas")
                .destination("Tayutic")
                .durationDays(2)
                .priority(1)
                .modality("CTP")
                .departureTime(LocalTime.of(5, 0))
                .returnTime(LocalTime.of(12, 0))
                .departureDate(LocalDate.of(2026, 5, 2))
                .returnDate(LocalDate.of(2026, 5, 3))
                .duplicateRank(rank)
                .duplicateGroupSize(groupSize)
                .conflicting(conflicting)
                .build();
        tour.addAssignment(TourProcessingDraftAssignment.builder().driver("Daniel Zuniga").vehicle(vehicle).passengers(3).build());
        return tour;
    }
}
