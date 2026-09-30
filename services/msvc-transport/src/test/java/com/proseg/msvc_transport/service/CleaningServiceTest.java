package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.cleaning.CleaningRegisterResponseDto;
import com.proseg.msvc_transport.entity.CleaningDraft;
import com.proseg.msvc_transport.entity.CleaningDraftRow;
import com.proseg.msvc_transport.entity.CleaningDraftStatus;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.Tour;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.CleaningDraftRepository;
import com.proseg.msvc_transport.repository.CleaningDraftRowRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.TourRepository;
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

class CleaningServiceTest {

    private static final UUID DRAFT_ID = UUID.randomUUID();
    private static final UUID EXECUTION_ID = UUID.randomUUID();

    private TourRepository tourRepository;
    private VehicleRepository vehicleRepository;
    private CleaningDraftRepository draftRepository;
    private CleaningDraftRowRepository rowRepository;
    private CleaningHistoryService historyService;
    private CleaningService service;
    private CleaningDraft draft;

    @BeforeEach
    void setUp() {
        tourRepository = mock(TourRepository.class);
        DriverRepository driverRepository = mock(DriverRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        draftRepository = mock(CleaningDraftRepository.class);
        rowRepository = mock(CleaningDraftRowRepository.class);
        historyService = mock(CleaningHistoryService.class);
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);

        when(currentUserResolver.resolve()).thenReturn("aleja");
        when(driverRepository.findFirstByFirstNameIgnoreCaseAndLastNameIgnoreCase(anyString(), anyString())).thenReturn(Optional.empty());
        when(driverRepository.findByDocumentIdIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(vehicleRepository.findByPlateIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyService.recordSuccess(any(), any(), anyString(), any(), any(), any(), any(), anyList())).thenReturn(EXECUTION_ID);

        draft = CleaningDraft.builder()
                .id(DRAFT_ID)
                .fileName("1-mayo.xls")
                .fileType("HTML")
                .status(CleaningDraftStatus.DRAFT)
                .totalRows(3)
                .duplicateRows(1)
                .conflictingRows(0)
                .build();
        when(draftRepository.findByIdForUpdate(DRAFT_ID)).thenReturn(Optional.of(draft));

        service = new CleaningService(
                tourRepository,
                driverRepository,
                vehicleRepository,
                draftRepository,
                rowRepository,
                historyService,
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

        CleaningRegisterResponseDto response = service.registerDraft(DRAFT_ID, false);

        ArgumentCaptor<List<Tour>> toursCaptor = ArgumentCaptor.forClass(List.class);
        verify(tourRepository).saveAll(toursCaptor.capture());
        assertThat(toursCaptor.getValue())
                .extracting(Tour::getExternalNumber, Tour::getRequestedVehicle, Tour::getRequestedVehicleType,
                        Tour::getStartDate, Tour::getDepartureTime)
                .containsExactly(
                        tuple("03625", "301-295", null, LocalDateTime.of(2026, 5, 2, 5, 0), "05:00"),
                        tuple("03082", null, "Todo Terreno", LocalDateTime.of(2026, 5, 2, 5, 0), "05:00")
                );
        assertThat(response.getImportedRows()).isEqualTo(2);
        assertThat(response.getCreatedVehicles()).isEqualTo(1);
        assertThat(response.getCleaningExecutionId()).isEqualTo(EXECUTION_ID);
        assertThat(draft.getStatus()).isEqualTo(CleaningDraftStatus.REGISTERED);
        assertThat(draft.getCleaningExecutionId()).isEqualTo(EXECUTION_ID);
        assertThat(draft.getRegisteredAt()).isNotNull();

        ArgumentCaptor<Vehicle> vehicleCaptor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(vehicleCaptor.capture());
        assertThat(vehicleCaptor.getValue().getPlate()).isEqualTo("301-295");
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
        verify(tourRepository, never()).saveAll(anyList());
        assertThat(draft.getStatus()).isEqualTo(CleaningDraftStatus.DRAFT);
    }

    @Test
    void rejectsADraftThatWasAlreadyRegistered() {
        draft.setStatus(CleaningDraftStatus.REGISTERED);

        assertThatThrownBy(() -> service.registerDraft(DRAFT_ID, false))
                .isInstanceOf(TransportException.class)
                .hasMessage("El borrador ya fue registrado");
        verify(tourRepository, never()).saveAll(anyList());
    }

    private CleaningDraftRow row(int rowNumber, String number, String vehicle, int rank, int groupSize, boolean conflicting) {
        return CleaningDraftRow.builder()
                .draft(draft)
                .rowNumber(rowNumber)
                .driver("Daniel Zuniga")
                .number(number)
                .vehicle(vehicle)
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
    }
}
