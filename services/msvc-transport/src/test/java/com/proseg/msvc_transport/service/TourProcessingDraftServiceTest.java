package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftCreateRequestDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftAssignmentDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftImportResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowDayRequestDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowIssueDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowRequestDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDriverAutoAssignResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingVehicleAutoAssignResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingImportColumnDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourSplitMode;
import com.proseg.msvc_transport.dto.response.OvertimeRequestReportDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowDayResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowResponseDto;
import com.proseg.msvc_transport.entity.DriverMonthBusinessDays;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.repository.TourProcessingDraftAssignmentRepository;
import com.proseg.msvc_transport.service.driverhours.ManualDays;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.entity.BorrowedVehicle;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.ObservationKeyword;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourApprovalStatus;
import com.proseg.msvc_transport.entity.UlloaShift;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.entity.VehicleType;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.BorrowedVehicleRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.ObservationKeywordRepository;
import com.proseg.msvc_transport.repository.VehicleRepository;
import com.proseg.msvc_transport.security.CurrentUserResolver;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TourProcessingDraftServiceTest {

    private TourProcessingDraftRepository draftRepository;
    private DraftTourStore rowRepository;
    private TourProcessingDraftAssignmentRepository assignmentRepository;
    private TourProcessingDraftDayRepository dayRepository;
    private DriverRepository driverRepository;
    private VehicleRepository vehicleRepository;
    private BorrowedVehicleRepository borrowedVehicleRepository;
    private DraftHoursService draftHoursService;
    private OvertimeRequestReportService overtimeRequestReportService;
    private InMemoryBusinessDays businessDays;
    private ObservationKeywordRepository observationKeywordRepository;
    private TourProcessingDraftService service;

    @BeforeEach
    void setUp() {
        businessDays = new InMemoryBusinessDays();
        draftRepository = mock(TourProcessingDraftRepository.class);
        rowRepository = mock(DraftTourStore.class);
        assignmentRepository = mock(TourProcessingDraftAssignmentRepository.class);
        dayRepository = mock(TourProcessingDraftDayRepository.class);
        driverRepository = mock(DriverRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        borrowedVehicleRepository = mock(BorrowedVehicleRepository.class);
        draftHoursService = mock(DraftHoursService.class);
        when(draftHoursService.manualDays(any())).thenReturn(ManualDays.NONE);
        overtimeRequestReportService = mock(OvertimeRequestReportService.class);
        observationKeywordRepository = mock(ObservationKeywordRepository.class);
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
        when(currentUserResolver.resolve()).thenReturn("aleja");
        when(draftRepository.save(any(TourProcessingDraft.class))).thenAnswer(invocation -> {
            TourProcessingDraft draft = invocation.getArgument(0);
            draft.setId(UUID.randomUUID());
            return draft;
        });
        service = new TourProcessingDraftService(
                draftRepository,
                rowRepository,
                assignmentRepository,
                dayRepository,
                driverRepository,
                vehicleRepository,
                borrowedVehicleRepository,
                draftHoursService,
                businessDays.service(),
                overtimeRequestReportService,
                currentUserResolver,
                Validation.buildDefaultValidatorFactory().getValidator(),
                mock(JdbcTemplate.class),
                observationKeywordRepository
        );
    }

    @Test
    void exposesSchemaInDeclarationOrderWithRequiredFlags() {
        List<TourProcessingImportColumnDto> columns = service.getImportSchema().getColumns();

        assertThat(columns).hasSize(15);
        assertThat(columns.get(10).getAttribute()).isEqualTo("departureTime");
        assertThat(columns.get(12).getAttribute()).isEqualTo("departureDate");
        assertThat(columns)
                .extracting(TourProcessingImportColumnDto::getAttribute)
                .doesNotContain("perDay", "perMonth");
        assertThat(columns)
                .filteredOn(TourProcessingImportColumnDto::isRequired)
                .extracting(TourProcessingImportColumnDto::getAttribute)
                .containsExactly("number", "passengers", "executingUnit", "responsible", "destination",
                        "durationDays", "priority", "departureTime", "returnTime", "departureDate", "returnDate");
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsDraftKeepingAllRowsAndMarkingDuplicates() {
        TourProcessingDraftRowRequestDto first = row(8, "03625", "Tayutic");
        TourProcessingDraftRowRequestDto repeated = row(12, "03625", "Tayutic");
        TourProcessingDraftRowRequestDto conflicting = row(15, "03625", "Heredia");
        TourProcessingDraftRowRequestDto single = row(20, "04504", "Merced");

        TourProcessingDraftImportResponseDto response = service.create(request(List.of(first, repeated, conflicting, single)));

        ArgumentCaptor<List<TourProcessingDraftTour>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(rowRepository).saveAll(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue())
                .extracting(TourProcessingDraftTour::getRowNumber, TourProcessingDraftTour::getDuplicateRank,
                        TourProcessingDraftTour::getDuplicateGroupSize, TourProcessingDraftTour::isConflicting)
                .containsExactly(
                        tuple(8, 1, 3, false),
                        tuple(12, 2, 3, false),
                        tuple(15, 3, 3, true),
                        tuple(20, 1, 1, false)
                );
        assertThat(response.getDraftId()).isNotNull();
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getDuplicateRows()).isEqualTo(2);
        assertThat(response.getConflictingRows()).isEqualTo(1);

        ArgumentCaptor<TourProcessingDraft> draftCaptor = ArgumentCaptor.forClass(TourProcessingDraft.class);
        verify(draftRepository).save(draftCaptor.capture());
        TourProcessingDraft draft = draftCaptor.getValue();
        assertThat(draft.getStatus()).isEqualTo(TourProcessingDraftStatus.DRAFT);
        assertThat(draft.getTotalRows()).isEqualTo(4);
        assertThat(draft.getImportedBy()).isEqualTo("aleja");
        assertThat(draft.getFileType()).isEqualTo("HTML");
        assertThat(draft.getYear()).isEqualTo(2026);
        assertThat(draft.getMonth()).isEqualTo(5);
        assertThat(draft.getBusinessDays()).isEqualTo(21);
        assertThat(draft.getMinDepartureDate()).isEqualTo(LocalDate.of(2026, 5, 2));
        assertThat(draft.getMaxReturnDate()).isEqualTo(LocalDate.of(2026, 5, 3));
    }

    @Test
    void returnsRowErrorsWithoutSavingAnything() {
        TourProcessingDraftRowRequestDto missingPassengers = row(3, "04504", "Merced");
        missingPassengers.setPassengers(null);
        TourProcessingDraftRowRequestDto returnBeforeDeparture = row(4, "04505", "Merced");
        returnBeforeDeparture.setReturnDate(LocalDate.of(2026, 5, 1));
        TourProcessingDraftRowRequestDto sameDayBackwards = row(5, "04506", "Merced");
        sameDayBackwards.setReturnDate(sameDayBackwards.getDepartureDate());
        sameDayBackwards.setReturnTime(LocalTime.of(4, 0));
        TourProcessingDraftRowRequestDto tooLongNumber = row(6, "1".repeat(51), "Merced");

        TourProcessingDraftImportResponseDto response = service.create(request(List.of(
                missingPassengers, returnBeforeDeparture, sameDayBackwards, tooLongNumber)));

        assertThat(response.getDraftId()).isNull();
        assertThat(response.getErrors())
                .extracting(TourProcessingDraftRowIssueDto::getRow, TourProcessingDraftRowIssueDto::getReason)
                .containsExactly(
                        tuple(3, "El campo obligatorio 'Pasajeros' está vacío"),
                        tuple(4, "La fecha de regreso es anterior a la fecha de salida"),
                        tuple(5, "La hora de regreso es anterior a la hora de salida en una gira de un solo día"),
                        tuple(6, "El número no puede superar los 50 caracteres")
                );
        verify(draftRepository, never()).save(any());
        verify(rowRepository, never()).saveAll(anyList());
    }

    @Test
    void rejectsImportWhenAPendingDraftAlreadyExistsForTheMonth() {
        when(draftRepository.existsByYearAndMonthAndStatus(2026, 5, TourProcessingDraftStatus.DRAFT)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(List.of(row(8, "03625", "Tayutic")))))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("TOUR_PROCESSING_DRAFT_MONTH_EXISTS");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getMessage()).contains("mayo de 2026");
                });
        verify(draftRepository, never()).save(any());
        verify(rowRepository, never()).saveAll(anyList());
    }

    @Test
    void rejectsImportWhenTheMonthWasAlreadyRegistered() {
        when(draftRepository.existsByYearAndMonthAndStatus(2026, 5, TourProcessingDraftStatus.REGISTERED)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(List.of(row(8, "03625", "Tayutic")))))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("TOUR_PROCESSING_MONTH_ALREADY_REGISTERED");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getMessage()).contains("mayo de 2026");
                });
        verify(draftRepository, never()).save(any());
        verify(rowRepository, never()).saveAll(anyList());
    }

    @Test
    void rejectsImportWhenTheMonthWasAlreadyClosed() {
        when(draftRepository.existsByYearAndMonthAndStatus(2026, 5, TourProcessingDraftStatus.CLOSED)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(List.of(row(8, "03625", "Tayutic")))))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("TOUR_PROCESSING_MONTH_ALREADY_CLOSED");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
        verify(draftRepository, never()).save(any());
    }

    @Test
    void finishesAndReopensTheSameDraftWithoutChangingItsDays() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        draft.setTotalRows(0);
        draft.setDuplicateRows(0);
        draft.setConflictingRows(0);
        draft.setBlocksDetected(0);
        when(draftRepository.findByIdForUpdate(draft.getId())).thenReturn(Optional.of(draft));

        assertThat(service.finish(draft.getId()).getStatus()).isEqualTo(TourProcessingDraftStatus.CLOSED);
        assertThat(service.reopen(draft.getId()).getStatus()).isEqualTo(TourProcessingDraftStatus.DRAFT);
        verify(draftHoursService, never()).syncAll(any(), anyList());
    }

    @Test
    void rejectsEditingAClosedDraft() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.CLOSED);
        when(draftRepository.findByIdForUpdate(draft.getId())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.createRow(draft.getId(), row(9, "03626", "Tayutic")))
                .isInstanceOfSatisfying(TransportException.class, exception -> assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> service.finish(draft.getId()))
                .isInstanceOfSatisfying(TransportException.class, exception -> assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> service.delete(draft.getId()))
                .isInstanceOfSatisfying(TransportException.class, exception -> assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void reportsTheRegisteredMonthWhenItAlsoHasAPendingDraft() {
        when(draftRepository.existsByYearAndMonthAndStatus(2026, 5, TourProcessingDraftStatus.REGISTERED)).thenReturn(true);
        when(draftRepository.existsByYearAndMonthAndStatus(2026, 5, TourProcessingDraftStatus.DRAFT)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(List.of(row(8, "03625", "Tayutic")))))
                .isInstanceOfSatisfying(TransportException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("TOUR_PROCESSING_MONTH_ALREADY_REGISTERED"));
    }

    @Test
    void rejectsImportsAboveTheRowLimit() {
        List<TourProcessingDraftRowRequestDto> rows = new ArrayList<>(Collections.nCopies(5001, row(1, "1", "Merced")));

        assertThatThrownBy(() -> service.create(request(rows)))
                .isInstanceOf(TransportException.class)
                .hasMessage("La importación supera el máximo de 5000 filas");
    }

    @Test
    void createsRowAtTheEndAndRecalculatesDuplicatesAndDraftSummary() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour first = entity(draft, 8, "03625", "Tayutic");
        TourProcessingDraftTour single = entity(draft, 20, "04504", "Merced");
        stubDraft(draft, first, single);

        TourProcessingDraftRowRequestDto request = row(1, "03625", "Heredia");
        request.setRowNumber(null);
        request.setReturnDate(LocalDate.of(2026, 5, 9));
        var response = service.createRow(draft.getId(), request);

        ArgumentCaptor<TourProcessingDraftTour> rowCaptor = ArgumentCaptor.forClass(TourProcessingDraftTour.class);
        verify(rowRepository).save(rowCaptor.capture());
        TourProcessingDraftTour created = rowCaptor.getValue();
        assertThat(created.getDraft()).isSameAs(draft);
        assertThat(created.getRowNumber()).isEqualTo(21);
        assertThat(response.getRowNumber()).isEqualTo(21);
        assertThat(response.getDestination()).isEqualTo("Heredia");
        assertThat(List.of(first, single, created))
                .extracting(TourProcessingDraftTour::getRowNumber, TourProcessingDraftTour::getDuplicateRank,
                        TourProcessingDraftTour::getDuplicateGroupSize, TourProcessingDraftTour::isConflicting)
                .containsExactly(
                        tuple(8, 1, 2, false),
                        tuple(20, 1, 1, false),
                        tuple(21, 2, 2, true)
                );
        assertThat(draft.getTotalRows()).isEqualTo(3);
        assertThat(draft.getDuplicateRows()).isEqualTo(1);
        assertThat(draft.getConflictingRows()).isEqualTo(1);
        assertThat(draft.getMaxReturnDate()).isEqualTo(LocalDate.of(2026, 5, 9));
    }

    @Test
    void distributesPassengersAcrossAssignmentsWithoutCreatingDuplicateTours() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        stubDraft(draft);
        List<TourProcessingDraftTour> saved = new ArrayList<>();
        when(rowRepository.save(any(TourProcessingDraftTour.class))).thenAnswer(invocation -> {
            TourProcessingDraftTour candidate = invocation.getArgument(0);
            assertThat(candidate.getDuplicateGroupSize()).isNotNull();
            assertThat(candidate.getDuplicateRank()).isNotNull();
            if (candidate.getId() == null) candidate.setId(UUID.randomUUID());
            if (!saved.contains(candidate)) saved.add(candidate);
            return candidate;
        });

        TourProcessingDraftRowRequestDto request = row(1, "03170", "Barva");
        request.setPassengers(70);
        request.setAssignments(List.of(
                TourProcessingDraftAssignmentDto.builder().driver("Daniel Zuniga").vehicle("301-295")
                        .resourceType(ResourceType.INTERNAL).passengers(40).build(),
                TourProcessingDraftAssignmentDto.builder().driver("Marvin Quesada").vehicle("301-590")
                        .resourceType(ResourceType.INTERNAL).passengers(30).build()
        ));

        var response = service.createRow(draft.getId(), request);

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getAssignments()).extracting(TourProcessingDraftAssignment::getPassengers).containsExactly(40, 30);
        assertThat(saved).extracting(TourProcessingDraftTour::getDuplicateRank).containsOnly(1);
        assertThat(response.getPassengers()).isEqualTo(70);
        assertThat(response.getAssignments()).hasSize(2);
        assertThat(draft.getTotalRows()).isEqualTo(1);
        assertThat(draft.getDuplicateRows()).isZero();
    }

    @Test
    void acceptsDistributionWhenPassengerCountsDoNotMatch() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        stubDraft(draft);
        TourProcessingDraftRowRequestDto request = row(1, "03170", "Barva");
        request.setPassengers(70);
        request.setAssignments(List.of(
                TourProcessingDraftAssignmentDto.builder().passengers(40).build(),
                TourProcessingDraftAssignmentDto.builder().passengers(29).build()
        ));

        var response = service.createRow(draft.getId(), request);

        assertThat(response.getAssignments()).extracting(TourProcessingDraftAssignmentDto::getPassengers).containsExactly(40, 29);
        assertThat(response.getTrips()).isEqualTo(2);
        assertThat(response.getAssignedPassengers()).isEqualTo(69);
        verify(rowRepository).save(any());
    }

    @Test
    void createsFirstRowOfAnEmptyDraftWithRowNumberOne() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        stubDraft(draft);

        var response = service.createRow(draft.getId(), row(99, "04504", "Merced"));

        assertThat(response.getRowNumber()).isEqualTo(1);
        assertThat(draft.getTotalRows()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidRowCreationWithoutSavingAnything() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        stubDraft(draft, entity(draft, 8, "03625", "Tayutic"));
        TourProcessingDraftRowRequestDto invalid = row(1, "04504", "Merced");
        invalid.setReturnDate(LocalDate.of(2026, 5, 1));

        assertThatThrownBy(() -> service.createRow(draft.getId(), invalid))
                .isInstanceOf(TransportException.class)
                .hasMessage("La fecha de regreso es anterior a la fecha de salida");
        verify(rowRepository, never()).save(any());
    }

    private TourProcessingDraftRowRequestDto borrowedVehicleRequest(String driver) {
        UUID borrowedId = UUID.randomUUID();
        BorrowedVehicle borrowed = mock(BorrowedVehicle.class);
        when(borrowed.getPlate()).thenReturn("PRE-123");
        when(borrowed.getCapacity()).thenReturn(30);
        when(borrowedVehicleRepository.findByIdAndIsDeletedFalse(borrowedId)).thenReturn(Optional.of(borrowed));
        TourProcessingDraftRowRequestDto request = row(1, "04504", "Merced");
        request.setResourceType(ResourceType.BORROWED_VEHICLE);
        request.setBorrowedVehicleId(borrowedId);
        request.setVehicle(null);
        request.setDriver(driver);
        return request;
    }

    @Test
    void rejectsBorrowedVehicleRowWhenTheDriverIsNotInTheSystem() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        stubDraft(draft, entity(draft, 8, "03625", "Tayutic"));
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));
        TourProcessingDraftRowRequestDto request = borrowedVehicleRequest("Pedro Otra Unidad");

        assertThatThrownBy(() -> service.createRow(draft.getId(), request))
                .isInstanceOf(TransportException.class)
                .hasMessageContaining("\"Pedro Otra Unidad\" no existe en el sistema");
        verify(rowRepository, never()).save(any());
    }

    @Test
    void acceptsBorrowedVehicleRowWhenTheDriverIsInTheSystemOrBlank() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        stubDraft(draft, entity(draft, 8, "03625", "Tayutic"));
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));

        service.createRow(draft.getId(), borrowedVehicleRequest("Daniel Zuniga"));
        service.createRow(draft.getId(), borrowedVehicleRequest(null));

        verify(rowRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void rejectsRowCreationOnRegisteredDraft() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.REGISTERED);
        stubDraft(draft);

        assertThatThrownBy(() -> service.createRow(draft.getId(), row(1, "04504", "Merced")))
                .isInstanceOf(TransportException.class)
                .hasMessage("No se puede editar un borrador que ya fue registrado");
        verify(rowRepository, never()).save(any());
    }

    @Test
    void updatesRowAndRecalculatesDuplicatesAndDraftSummary() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour first = entity(draft, 8, "03625", "Tayutic");
        TourProcessingDraftTour conflicting = entity(draft, 15, "03625", "Heredia");
        TourProcessingDraftTour single = entity(draft, 20, "04504", "Merced");
        stubDraft(draft, first, conflicting, single);

        TourProcessingDraftRowRequestDto request = row(15, "03625", "Tayutic");
        request.setReturnDate(LocalDate.of(2026, 5, 9));
        var response = service.updateRow(draft.getId(), conflicting.getId(), request);

        assertThat(response.getDestination()).isEqualTo("Tayutic");
        assertThat(List.of(first, conflicting, single))
                .extracting(TourProcessingDraftTour::getRowNumber, TourProcessingDraftTour::getDuplicateRank,
                        TourProcessingDraftTour::getDuplicateGroupSize, TourProcessingDraftTour::isConflicting)
                .containsExactly(
                        tuple(8, 1, 2, false),
                        tuple(15, 2, 2, true),
                        tuple(20, 1, 1, false)
                );
        assertThat(draft.getDuplicateRows()).isEqualTo(1);
        assertThat(draft.getConflictingRows()).isEqualTo(1);
        assertThat(draft.getMaxReturnDate()).isEqualTo(LocalDate.of(2026, 5, 9));
    }

    @Test
    void updatingRowClearsConflictWhenContentMatchesFirstOccurrence() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour first = entity(draft, 8, "03625", "Tayutic");
        TourProcessingDraftTour conflicting = entity(draft, 15, "03625", "Heredia");
        stubDraft(draft, first, conflicting);

        service.updateRow(draft.getId(), conflicting.getId(), row(15, "03625", "Tayutic"));

        assertThat(conflicting.isConflicting()).isFalse();
        assertThat(draft.getConflictingRows()).isZero();
        assertThat(draft.getDuplicateRows()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidRowUpdateWithoutChangingTheRow() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour existing = entity(draft, 8, "03625", "Tayutic");
        stubDraft(draft, existing);
        TourProcessingDraftRowRequestDto invalid = row(8, "03625", "Heredia");
        invalid.setReturnDate(LocalDate.of(2026, 5, 1));

        assertThatThrownBy(() -> service.updateRow(draft.getId(), existing.getId(), invalid))
                .isInstanceOf(TransportException.class)
                .hasMessage("La fecha de regreso es anterior a la fecha de salida");
        assertThat(existing.getDestination()).isEqualTo("Tayutic");
    }

    @Test
    void rejectsIncreasingTheDurationWhenTheAssignedDriverAlreadyHasAnotherTourTheNextDay() {
        TourProcessingDraft draft = draftWithPeriod();
        TourProcessingDraftTour oneDayTour = oneDayTour(draft);
        TourProcessingDraftTour nextDayTour = entity(draft, 9, "200", "Heredia");
        nextDayTour.setDepartureDate(LocalDate.of(2026, 5, 5));
        nextDayTour.setDepartureTime(LocalTime.of(8, 0));
        nextDayTour.setReturnDate(LocalDate.of(2026, 5, 5));
        nextDayTour.setReturnTime(LocalTime.of(17, 0));
        nextDayTour.setDurationDays(1);
        stubDraft(draft, oneDayTour, nextDayTour);
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));

        assertThatThrownBy(() -> service.updateRow(draft.getId(), oneDayTour.getId(), twoDayWeekdayRequest()))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("TOUR_PROCESSING_DRAFT_ROW_DRIVER_UNAVAILABLE");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(exception.getMessage()).contains("Daniel Zuniga", "200");
                });
        assertThat(oneDayTour.getDurationDays()).isEqualTo(1);
        assertThat(oneDayTour.getReturnDate()).isEqualTo(LocalDate.of(2026, 5, 4));
        verify(draftHoursService, never()).syncChanged(any(), anyMap(), anyList());
    }

    @Test
    void doesNotBlockIncreasingTheDurationIntoAWeekendThatExceedsTheWeekendCap() {
        TourProcessingDraft draft = draftWithPeriod();
        TourProcessingDraftTour friday = oneDayTour(draft);
        friday.setDepartureDate(LocalDate.of(2026, 5, 22));
        friday.setReturnDate(LocalDate.of(2026, 5, 22));
        stubDraft(draft, friday,
                shortRow(draft, 9, "201", "2026-05-02", "08:00", "10:00", "Daniel Zuniga"),
                shortRow(draft, 10, "202", "2026-05-09", "08:00", "10:00", "Daniel Zuniga"),
                shortRow(draft, 11, "203", "2026-05-16", "08:00", "10:00", "Daniel Zuniga"),
                fullDayRow(draft, 12, "204", "2026-05-11", "Daniel Zuniga"),
                fullDayRow(draft, 13, "205", "2026-05-12", "Daniel Zuniga"));
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));
        TourProcessingDraftRowRequestDto request = row(8, "100", "Cartago");
        request.setDepartureDate(LocalDate.of(2026, 5, 22));
        request.setReturnDate(LocalDate.of(2026, 5, 23));

        service.updateRow(draft.getId(), friday.getId(), request);

        assertThat(friday.getReturnDate()).isEqualTo(LocalDate.of(2026, 5, 23));
    }

    @Test
    void allowsIncreasingTheDurationWhenTheAssignedDriverIsFree() {
        TourProcessingDraft draft = draftWithPeriod();
        TourProcessingDraftTour oneDayTour = oneDayTour(draft);
        TourProcessingDraftTour laterTour = entity(draft, 9, "200", "Heredia");
        laterTour.setDepartureDate(LocalDate.of(2026, 5, 12));
        laterTour.setReturnDate(LocalDate.of(2026, 5, 13));
        stubDraft(draft, oneDayTour, laterTour);
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));

        service.updateRow(draft.getId(), oneDayTour.getId(), twoDayWeekdayRequest());

        assertThat(oneDayTour.getDurationDays()).isEqualTo(2);
        assertThat(oneDayTour.getReturnDate()).isEqualTo(LocalDate.of(2026, 5, 5));
        verify(draftHoursService).syncChanged(eq(draft), anyMap(), anyList());
    }

    @Test
    void doesNotCheckTheDriverWhenTheDurationDoesNotIncrease() {
        TourProcessingDraft draft = draftWithPeriod();
        TourProcessingDraftTour twoDayTour = entity(draft, 8, "100", "Cartago");
        TourProcessingDraftTour overlappingTour = entity(draft, 9, "200", "Heredia");
        stubDraft(draft, twoDayTour, overlappingTour);
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));

        service.updateRow(draft.getId(), twoDayTour.getId(), row(8, "100", "Limón"));

        assertThat(twoDayTour.getDestination()).isEqualTo("Limón");
    }

    @Test
    void doesNotCheckTheDriverWhenTheTourHasNoCatalogDriver() {
        TourProcessingDraft draft = draftWithPeriod();
        TourProcessingDraftTour oneDayTour = oneDayTour(draft);
        oneDayTour.primaryAssignment().setDriver(null);
        TourProcessingDraftTour nextDayTour = entity(draft, 9, "200", "Heredia");
        nextDayTour.setDepartureDate(LocalDate.of(2026, 5, 5));
        stubDraft(draft, oneDayTour, nextDayTour);
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));
        TourProcessingDraftRowRequestDto request = twoDayWeekdayRequest();
        request.setDriver(null);

        service.updateRow(draft.getId(), oneDayTour.getId(), request);

        assertThat(oneDayTour.getDurationDays()).isEqualTo(2);
    }

    @Test
    void newRowsAreApprovedByDefault() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour existing = entity(draft, 8, "03625", "Tayutic");

        assertThat(existing.getApprovalStatus()).isEqualTo(TourApprovalStatus.APPROVED);
    }

    @Test
    void updateRowKeepsTheApprovalStatusWhenTheRequestDoesNotSendIt() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour existing = entity(draft, 8, "03625", "Tayutic");
        existing.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        stubDraft(draft, existing);

        var response = service.updateRow(draft.getId(), existing.getId(), row(8, "03625", "Heredia"));

        assertThat(existing.getApprovalStatus()).isEqualTo(TourApprovalStatus.NOT_APPROVED);
        assertThat(response.getApprovalStatus()).isEqualTo(TourApprovalStatus.NOT_APPROVED);
    }

    @Test
    void updateRowClearsObservationReviewedWhenTheObservationChanges() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour existing = entity(draft, 8, "03625", "Tayutic");
        existing.setObservationReviewed(true);
        existing.setObservations("Llevar urgente");
        stubDraft(draft, existing);
        TourProcessingDraftRowRequestDto request = row(8, "03625", "Tayutic");
        request.setObservations("Llevar urgente mañana");

        var response = service.updateRow(draft.getId(), existing.getId(), request);

        assertThat(existing.isObservationReviewed()).isFalse();
        assertThat(response.isObservationReviewed()).isFalse();
        assertThat(response.getObservations()).isEqualTo("Llevar urgente mañana");
    }

    @Test
    void updateRowKeepsObservationReviewedWhenTheObservationStaysTheSame() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour existing = entity(draft, 8, "03625", "Tayutic");
        existing.setObservationReviewed(true);
        existing.setObservations("Llevar urgente");
        stubDraft(draft, existing);
        TourProcessingDraftRowRequestDto request = row(8, "03625", "Tayutic");
        request.setObservations("  Llevar urgente  ");

        var response = service.updateRow(draft.getId(), existing.getId(), request);

        assertThat(existing.isObservationReviewed()).isTrue();
        assertThat(response.isObservationReviewed()).isTrue();
    }

    @Test
    void updateRowReportsKeywordsThatMatchTheObservation() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour existing = entity(draft, 8, "03625", "Tayutic");
        existing.setObservations("Revisar URGENTE hoy");
        stubDraft(draft, existing);
        ObservationKeyword keyword = new ObservationKeyword();
        keyword.setText("urgente");
        when(observationKeywordRepository.findAll()).thenReturn(List.of(keyword));
        TourProcessingDraftRowRequestDto request = row(8, "03625", "Tayutic");
        request.setObservations("Revisar URGENTE hoy");

        var response = service.updateRow(draft.getId(), existing.getId(), request);

        assertThat(response.getMatchedObservationKeywords()).containsExactly("urgente");
        assertThat(existing.isObservationReviewed()).isFalse();
    }

    @Test
    void updateRowAppliesTheApprovalStatusSentInTheRequest() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour existing = entity(draft, 8, "03625", "Tayutic");
        stubDraft(draft, existing);
        TourProcessingDraftRowRequestDto request = row(8, "03625", "Tayutic");
        request.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);

        var response = service.updateRow(draft.getId(), existing.getId(), request);

        assertThat(existing.getApprovalStatus()).isEqualTo(TourApprovalStatus.NOT_APPROVED);
        assertThat(response.getApprovalStatus()).isEqualTo(TourApprovalStatus.NOT_APPROVED);
    }

    @Test
    void updatesTheApprovalStatusOfARow() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour existing = entity(draft, 8, "03625", "Tayutic");
        stubDraft(draft, existing);

        var notApproved = service.updateRowApprovalStatus(draft.getId(), existing.getId(), TourApprovalStatus.NOT_APPROVED);
        assertThat(notApproved.getApprovalStatus()).isEqualTo(TourApprovalStatus.NOT_APPROVED);
        assertThat(existing.getApprovalStatus()).isEqualTo(TourApprovalStatus.NOT_APPROVED);

        var approved = service.updateRowApprovalStatus(draft.getId(), existing.getId(), TourApprovalStatus.APPROVED);
        assertThat(approved.getApprovalStatus()).isEqualTo(TourApprovalStatus.APPROVED);
        assertThat(existing.getApprovalStatus()).isEqualTo(TourApprovalStatus.APPROVED);
        verify(rowRepository, times(2)).save(existing);
        verify(rowRepository, never()).saveAll(anyList());
        verify(draftHoursService, times(2)).syncChanged(eq(draft), anyMap(), anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void approvalPersistsOnlyTheChangedTourAndSyncsHoursForAllAssignments() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour changed = entity(draft, 8, "100", "Cartago");
        TourProcessingDraftTour other = entity(draft, 9, "200", "Heredia");
        stubDraft(draft, changed, other);

        service.updateRowApprovalStatus(draft.getId(), changed.getId(), TourApprovalStatus.NOT_APPROVED);

        verify(rowRepository).save(changed);
        verify(rowRepository, never()).save(other);
        verify(rowRepository, never()).saveAll(anyList());
        ArgumentCaptor<List<TourProcessingDraftAssignment>> assignments = ArgumentCaptor.forClass(List.class);
        verify(draftHoursService).syncChanged(eq(draft), anyMap(), assignments.capture());
        assertThat(assignments.getValue()).containsExactlyInAnyOrder(changed.primaryAssignment(), other.primaryAssignment());
    }

    @Test
    void rejectsApprovingATourWhoseDriverGotAnOverlappingTourWhileItWasNotApproved() {
        TourProcessingDraft draft = draftWithPeriod();
        TourProcessingDraftTour notApproved = entity(draft, 8, "100", "Cartago");
        notApproved.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        TourProcessingDraftTour overlapping = entity(draft, 9, "200", "Heredia");
        overlapping.primaryAssignment().setVehicle("600-600");
        stubDraft(draft, notApproved, overlapping);
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));

        assertThatThrownBy(() -> service.updateRowApprovalStatus(draft.getId(), notApproved.getId(), TourApprovalStatus.APPROVED))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("TOUR_PROCESSING_DRAFT_ROW_DRIVER_UNAVAILABLE");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(exception.getMessage()).contains("Daniel Zuniga", "200");
                });
        assertThat(notApproved.getApprovalStatus()).isEqualTo(TourApprovalStatus.NOT_APPROVED);
        verify(draftHoursService, never()).syncChanged(any(), anyMap(), anyList());
    }

    @Test
    void rejectsApprovingATourWhoseVehicleGotAnOverlappingTourWhileItWasNotApproved() {
        TourProcessingDraft draft = draftWithPeriod();
        TourProcessingDraftTour notApproved = entity(draft, 8, "100", "Cartago");
        notApproved.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        TourProcessingDraftTour overlapping = entity(draft, 9, "200", "Heredia");
        overlapping.primaryAssignment().setDriver("Otro Chofer");
        stubDraft(draft, notApproved, overlapping);
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));
        when(vehicleRepository.findAll()).thenReturn(List.of(
                Vehicle.builder().id(UUID.randomUUID()).plate("301-295").type(VehicleType.SEDAN).capacity(30).build()
        ));

        assertThatThrownBy(() -> service.updateRowApprovalStatus(draft.getId(), notApproved.getId(), TourApprovalStatus.APPROVED))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("TOUR_PROCESSING_DRAFT_ROW_VEHICLE_OVERLAP");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(exception.getMessage()).contains("301-295", "200");
                });
        assertThat(notApproved.getApprovalStatus()).isEqualTo(TourApprovalStatus.NOT_APPROVED);
        verify(draftHoursService, never()).syncChanged(any(), anyMap(), anyList());
    }

    @Test
    void approvesAgainATourWhenItsDriverAndVehicleAreStillFree() {
        TourProcessingDraft draft = draftWithPeriod();
        TourProcessingDraftTour notApproved = entity(draft, 8, "100", "Cartago");
        notApproved.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        TourProcessingDraftTour laterTour = entity(draft, 9, "200", "Heredia");
        laterTour.setDepartureDate(LocalDate.of(2026, 5, 12));
        laterTour.setReturnDate(LocalDate.of(2026, 5, 13));
        stubDraft(draft, notApproved, laterTour);
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));
        when(vehicleRepository.findAll()).thenReturn(List.of(
                Vehicle.builder().id(UUID.randomUUID()).plate("301-295").type(VehicleType.SEDAN).capacity(30).build()
        ));

        service.updateRowApprovalStatus(draft.getId(), notApproved.getId(), TourApprovalStatus.APPROVED);

        assertThat(notApproved.getApprovalStatus()).isEqualTo(TourApprovalStatus.APPROVED);
        verify(draftHoursService).syncChanged(eq(draft), anyMap(), anyList());
    }

    @Test
    void ignoresOtherNotApprovedToursWhenApprovingATourAgain() {
        TourProcessingDraft draft = draftWithPeriod();
        TourProcessingDraftTour notApproved = entity(draft, 8, "100", "Cartago");
        notApproved.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        TourProcessingDraftTour otherNotApproved = entity(draft, 9, "200", "Heredia");
        otherNotApproved.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        stubDraft(draft, notApproved, otherNotApproved);
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));
        when(vehicleRepository.findAll()).thenReturn(List.of(
                Vehicle.builder().id(UUID.randomUUID()).plate("301-295").type(VehicleType.SEDAN).capacity(30).build()
        ));

        service.updateRowApprovalStatus(draft.getId(), notApproved.getId(), TourApprovalStatus.APPROVED);

        assertThat(notApproved.getApprovalStatus()).isEqualTo(TourApprovalStatus.APPROVED);
    }

    @Test
    void doesNotCheckConflictsWhenMarkingATourAsNotApproved() {
        TourProcessingDraft draft = draftWithPeriod();
        TourProcessingDraftTour approved = entity(draft, 8, "100", "Cartago");
        TourProcessingDraftTour overlapping = entity(draft, 9, "200", "Heredia");
        stubDraft(draft, approved, overlapping);
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));

        service.updateRowApprovalStatus(draft.getId(), approved.getId(), TourApprovalStatus.NOT_APPROVED);

        assertThat(approved.getApprovalStatus()).isEqualTo(TourApprovalStatus.NOT_APPROVED);
    }

    @Test
    void rejectsApprovingATourFromTheRowModalWhenItsDriverIsBusy() {
        TourProcessingDraft draft = draftWithPeriod();
        TourProcessingDraftTour notApproved = entity(draft, 8, "100", "Cartago");
        notApproved.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        TourProcessingDraftTour overlapping = entity(draft, 9, "200", "Heredia");
        stubDraft(draft, notApproved, overlapping);
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));
        TourProcessingDraftRowRequestDto request = row(8, "100", "Cartago");
        request.setApprovalStatus(TourApprovalStatus.APPROVED);

        assertThatThrownBy(() -> service.updateRow(draft.getId(), notApproved.getId(), request))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("TOUR_PROCESSING_DRAFT_ROW_DRIVER_UNAVAILABLE");
                    assertThat(exception.getMessage()).contains("Daniel Zuniga", "200");
                });
        verify(draftHoursService, never()).syncChanged(any(), anyMap(), anyList());
    }

    @Test
    void draftDetailCountsOnlyPrimaryToursWithoutLoadingRepeatedRows() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        draft.setTotalRows(3);
        draft.setDuplicateRows(1);
        draft.setConflictingRows(0);
        draft.setBlocksDetected(0);
        TourProcessingDraftTour oversized = entity(draft, 1, "100", "Cartago");
        oversized.setPassengers(5);
        TourProcessingDraftTour notApproved = entity(draft, 2, "200", "Heredia");
        notApproved.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(rowRepository.findPrimariesByDraftId(draft.getId())).thenReturn(List.of(oversized, notApproved));
        when(vehicleRepository.findAll()).thenReturn(List.of(Vehicle.builder()
                .id(UUID.randomUUID()).plate("301-295").capacity(4).available(true).build()));

        var response = service.findById(draft.getId());

        assertThat(response.getOversizedRows()).isEqualTo(1);
        verify(rowRepository, never()).findAllByDraftIdOrderByRowNumberAsc(draft.getId());
    }

    @Test
    void notApprovedToursAreSkippedByTheDriverAutoAssignment() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        draft.setYear(2026);
        draft.setMonth(5);
        draft.setBusinessDays(28);
        TourProcessingDraftTour notApproved = entity(draft, 8, "100", "Cartago");
        notApproved.primaryAssignment().setDriver(null);
        notApproved.setApprovalStatus(TourApprovalStatus.NOT_APPROVED);
        stubDraft(draft, notApproved);
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));

        var response = service.autoAssignDrivers(draft.getId());

        assertThat(notApproved.primaryAssignment().getDriver()).isNull();
        assertThat(response.getAssignedTours()).isZero();
        assertThat(response.getUnassignedTours()).isZero();
        assertThat(response.getUnassignedDetails()).isEmpty();
    }

    @Test
    void updatingTheApprovalStatusAppliesToEveryPartOfADistributedRow() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        UUID distributionId = UUID.randomUUID();
        TourProcessingDraftTour firstPart = entity(draft, 8, "03625", "Tayutic");
        TourProcessingDraftTour secondPart = entity(draft, 9, "03625", "Tayutic");
        firstPart.addAssignment(secondPart.primaryAssignment());
        stubDraft(draft, firstPart);


        service.updateRowApprovalStatus(draft.getId(), firstPart.getId(), TourApprovalStatus.NOT_APPROVED);

        assertThat(firstPart.getAssignments()).allSatisfy(part -> assertThat(part.isActive()).isFalse());
    }

    @Test
    void rejectsUpdatingTheApprovalStatusFromADistributionPartThatIsNotTheMainRow() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour secondPart = entity(draft, 9, "03625", "Tayutic");
        secondPart.primaryAssignment().setPart(2);
        stubDraft(draft);
        when(assignmentRepository.findByIdAndDraftId(secondPart.getId(), draft.getId())).thenReturn(Optional.of(secondPart.primaryAssignment()));

        assertThatThrownBy(() -> service.updateRowApprovalStatus(draft.getId(), secondPart.getId(), TourApprovalStatus.NOT_APPROVED))
                .isInstanceOfSatisfying(TransportException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo("TOUR_PROCESSING_DISTRIBUTION_PART"));
        assertThat(secondPart.getApprovalStatus()).isEqualTo(TourApprovalStatus.APPROVED);
    }

    @Test
    void rejectsUpdatingTheApprovalStatusOnRegisteredDraft() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.REGISTERED);
        TourProcessingDraftTour existing = entity(draft, 8, "03625", "Tayutic");
        stubDraft(draft, existing);

        assertThatThrownBy(() -> service.updateRowApprovalStatus(draft.getId(), existing.getId(), TourApprovalStatus.NOT_APPROVED))
                .isInstanceOf(TransportException.class)
                .hasMessage("No se puede editar un borrador que ya fue registrado");
        assertThat(existing.getApprovalStatus()).isEqualTo(TourApprovalStatus.APPROVED);
    }

    @Test
    void rejectsRowUpdateOnRegisteredDraft() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.REGISTERED);
        TourProcessingDraftTour existing = entity(draft, 8, "03625", "Tayutic");
        stubDraft(draft, existing);

        assertThatThrownBy(() -> service.updateRow(draft.getId(), existing.getId(), row(8, "03625", "Heredia")))
                .isInstanceOf(TransportException.class)
                .hasMessage("No se puede editar un borrador que ya fue registrado");
        assertThat(existing.getDestination()).isEqualTo("Tayutic");
    }

    @Test
    void rejectsRowUpdateWhenRowDoesNotBelongToDraft() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        UUID unknownRowId = UUID.randomUUID();
        when(draftRepository.findByIdForUpdate(draft.getId())).thenReturn(Optional.of(draft));
        when(rowRepository.findByIdAndDraftId(unknownRowId, draft.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRow(draft.getId(), unknownRowId, row(8, "03625", "Heredia")))
                .isInstanceOf(TransportException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deletesDuplicateRowsKeepingFirstOccurrenceAndRecalculatesDraftSummary() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour first = entity(draft, 8, "03625", "Tayutic");
        TourProcessingDraftTour repeated = entity(draft, 12, "03625", "Tayutic");
        TourProcessingDraftTour repeatedAgain = entity(draft, 16, "03625", "Tayutic");
        TourProcessingDraftTour single = entity(draft, 20, "04504", "Merced");
        first.setDuplicateGroupSize(3);
        repeated.setDuplicateRank(2);
        repeated.setDuplicateGroupSize(3);
        repeatedAgain.setDuplicateRank(3);
        repeatedAgain.setDuplicateGroupSize(3);
        stubDraft(draft, first, repeated, repeatedAgain, single);

        int deleted = service.deleteDuplicateRows(draft.getId());

        ArgumentCaptor<List<TourProcessingDraftTour>> deletedCaptor = ArgumentCaptor.forClass(List.class);
        verify(rowRepository).deleteAll(deletedCaptor.capture());
        assertThat(deletedCaptor.getValue()).containsExactly(repeated, repeatedAgain);
        assertThat(deleted).isEqualTo(2);
        assertThat(List.of(first, single))
                .extracting(TourProcessingDraftTour::getRowNumber, TourProcessingDraftTour::getDuplicateRank,
                        TourProcessingDraftTour::getDuplicateGroupSize, TourProcessingDraftTour::isConflicting)
                .containsExactly(
                        tuple(8, 1, 1, false),
                        tuple(20, 1, 1, false)
                );
        assertThat(draft.getTotalRows()).isEqualTo(2);
        assertThat(draft.getDuplicateRows()).isZero();
        assertThat(draft.getConflictingRows()).isZero();
    }

    @Test
    void deletingDuplicatesWithoutRepeatedRowsDeletesNothing() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        stubDraft(draft, entity(draft, 8, "03625", "Tayutic"), entity(draft, 20, "04504", "Merced"));

        int deleted = service.deleteDuplicateRows(draft.getId());

        assertThat(deleted).isZero();
        assertThat(draft.getTotalRows()).isEqualTo(2);
    }

    @Test
    void rejectsDeletingDuplicatesWhenThereAreConflictingRows() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour first = entity(draft, 8, "03625", "Tayutic");
        TourProcessingDraftTour conflicting = entity(draft, 15, "03625", "Heredia");
        conflicting.setDuplicateRank(2);
        conflicting.setConflicting(true);
        stubDraft(draft, first, conflicting);

        assertThatThrownBy(() -> service.deleteDuplicateRows(draft.getId()))
                .isInstanceOf(TransportException.class)
                .hasMessage("No se pueden eliminar las filas repetidas mientras haya un Numero repetido con datos distintos");
        verify(rowRepository, never()).deleteAll(anyList());
    }

    @Test
    void rejectsDeletingDuplicatesOnRegisteredDraft() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.REGISTERED);
        stubDraft(draft);

        assertThatThrownBy(() -> service.deleteDuplicateRows(draft.getId()))
                .isInstanceOf(TransportException.class)
                .hasMessage("No se puede editar un borrador que ya fue registrado");
        verify(rowRepository, never()).deleteAll(anyList());
    }

    @Test
    void splitsToursThatExceedTheirVehicleCapacityIntoEvenlyDistributedParts() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour oversized = entity(draft, 1, "100", "Cartago");
        oversized.setPassengers(70);
        TourProcessingDraftTour fits = entity(draft, 2, "200", "Heredia");
        fits.setPassengers(30);
        stubDraft(draft, oversized, fits);
        when(vehicleRepository.findAll()).thenReturn(List.of(
                Vehicle.builder().id(UUID.randomUUID()).plate("301-295").type(VehicleType.SEDAN).capacity(30).build()
        ));

        int split = service.splitOversizedRows(draft.getId());

        assertThat(split).isEqualTo(1);
        assertThat(oversized.getPassengers()).isEqualTo(70);
        assertThat(oversized.getAssignments()).extracting(TourProcessingDraftAssignment::getPart, TourProcessingDraftAssignment::getPassengers)
                .containsExactly(tuple(1, 24), tuple(2, 23), tuple(3, 23));
        assertThat(oversized.primaryAssignment().getDriver()).isEqualTo("Daniel Zuniga");
        assertThat(oversized.primaryAssignment().getVehicle()).isEqualTo("301-295");
        assertThat(fits.getAssignments()).hasSize(1);
        verify(draftHoursService).syncChanged(any(), anyMap(), anyList());
    }

    @Test
    void splitsToursKeepingTheSameDriverAndVehicleInEveryTripWhenAskedTo() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour congress = entity(draft, 1, "100", "San José");
        congress.setPassengers(150);
        congress.primaryAssignment().setPassengers(150);
        stubDraft(draft, congress);
        when(vehicleRepository.findAll()).thenReturn(List.of(
                Vehicle.builder().id(UUID.randomUUID()).plate("301-295").type(VehicleType.SEDAN).capacity(30).build()
        ));

        int split = service.splitOversizedRows(draft.getId(), TourSplitMode.SAME_DRIVER_AND_VEHICLE);

        assertThat(split).isEqualTo(1);
        assertThat(congress.getAssignments()).extracting(TourProcessingDraftAssignment::getPassengers).containsExactly(30, 30, 30, 30, 30);
        assertThat(congress.getAssignments()).extracting(TourProcessingDraftAssignment::getDriver, TourProcessingDraftAssignment::getVehicle)
                .containsOnly(tuple("Daniel Zuniga", "301-295"));
        assertThat(congress.getAssignments()).extracting(TourProcessingDraftAssignment::getDriverId)
                .containsOnly(congress.primaryAssignment().getDriverId());
    }

    @Test
    void splitsEveryPartIntoCapacityRespectingTripsWithoutExceedingTheVehicleSeats() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour fifty = entity(draft, 1, "100", "Cartago");
        fifty.setPassengers(50);
        fifty.primaryAssignment().setPassengers(50);
        stubDraft(draft, fifty);
        when(vehicleRepository.findAll()).thenReturn(List.of(
                Vehicle.builder().id(UUID.randomUUID()).plate("301-295").type(VehicleType.SEDAN).capacity(30).build()
        ));

        service.splitOversizedRows(draft.getId(), TourSplitMode.SAME_DRIVER_AND_VEHICLE);

        assertThat(fifty.getAssignments()).extracting(TourProcessingDraftAssignment::getPassengers).containsExactly(25, 25);
    }

    @Test
    void acceptsTheSameDriverAndVehicleInEveryTripOfATourAndExposesTripsAndCapacity() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        stubDraft(draft);
        when(vehicleRepository.findAll()).thenReturn(List.of(
                Vehicle.builder().id(UUID.randomUUID()).plate("301-295").type(VehicleType.SEDAN).capacity(30).build()
        ));
        TourProcessingDraftRowRequestDto request = row(1, "03170", "Barva");
        request.setPassengers(150);
        request.setAssignments(java.util.stream.IntStream.range(0, 5).mapToObj(index -> TourProcessingDraftAssignmentDto.builder()
                .driver("Daniel Zuniga").vehicle("301-295").resourceType(ResourceType.INTERNAL).passengers(30).build()).toList());

        var response = service.createRow(draft.getId(), request);

        assertThat(response.getAssignments()).hasSize(5);
        assertThat(response.getTrips()).isEqualTo(5);
        assertThat(response.getTotalCapacity()).isEqualTo(150);
        assertThat(response.getAssignedPassengers()).isEqualTo(150);
    }

    @Test
    void stillRejectsATripWithMorePassengersThanItsVehicleCapacity() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        stubDraft(draft);
        when(vehicleRepository.findAll()).thenReturn(List.of(
                Vehicle.builder().id(UUID.randomUUID()).plate("301-295").type(VehicleType.SEDAN).capacity(30).build()
        ));
        TourProcessingDraftRowRequestDto request = row(1, "03170", "Barva");
        request.setPassengers(60);
        request.setAssignments(List.of(
                TourProcessingDraftAssignmentDto.builder().driver("Daniel Zuniga").vehicle("301-295").resourceType(ResourceType.INTERNAL).passengers(31).build(),
                TourProcessingDraftAssignmentDto.builder().driver("Daniel Zuniga").vehicle("301-295").resourceType(ResourceType.INTERNAL).passengers(29).build()
        ));

        assertThatThrownBy(() -> service.createRow(draft.getId(), request))
                .isInstanceOf(TransportException.class)
                .hasMessage("El vehículo 301-295 no tiene capacidad para 31 pasajeros");
    }

    @Test
    void splitsToursWithoutVehicleUsingTheLargestAvailableCapacityAndSkipsDistributedAndExternalOnes() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour unassigned = entity(draft, 1, "100", "Cartago");
        unassigned.primaryAssignment().setVehicle(null);
        unassigned.setPassengers(41);
        TourProcessingDraftTour alreadyDistributed = entity(draft, 2, "200", "Heredia");
        alreadyDistributed.setPassengers(60);
        alreadyDistributed.addAssignment(TourProcessingDraftAssignment.builder().passengers(30).build());
        TourProcessingDraftTour external = entity(draft, 3, "300", "Limón");
        external.primaryAssignment().setResourceType(ResourceType.EXTERNAL);
        external.setPassengers(60);
        stubDraft(draft, unassigned, alreadyDistributed, external);
        when(vehicleRepository.findAll()).thenReturn(List.of(
                Vehicle.builder().id(UUID.randomUUID()).plate("301-295").type(VehicleType.SEDAN).capacity(5).build(),
                Vehicle.builder().id(UUID.randomUUID()).plate("600-600").type(VehicleType.SEDAN).capacity(20).build()
        ));

        int split = service.splitOversizedRows(draft.getId());

        assertThat(split).isEqualTo(1);
        assertThat(unassigned.primaryAssignment().getPassengers()).isEqualTo(14);
        assertThat(unassigned.getPassengers()).isEqualTo(41);
        assertThat(alreadyDistributed.getPassengers()).isEqualTo(60);
        assertThat(external.getAssignments()).hasSize(1);
        verify(rowRepository).flush();
    }

    @Test
    void splittingWithoutOversizedToursChangesNothing() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        stubDraft(draft, entity(draft, 1, "100", "Cartago"));
        when(vehicleRepository.findAll()).thenReturn(List.of(
                Vehicle.builder().id(UUID.randomUUID()).plate("301-295").type(VehicleType.SEDAN).capacity(5).build()
        ));

        assertThat(service.splitOversizedRows(draft.getId())).isZero();

        verify(rowRepository, never()).save(any(TourProcessingDraftTour.class));
        verify(draftHoursService, never()).syncChanged(any(), anyMap(), anyList());
    }

    @Test
    void rejectsSplittingOversizedToursOnRegisteredDraft() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.REGISTERED);
        stubDraft(draft);

        assertThatThrownBy(() -> service.splitOversizedRows(draft.getId()))
                .isInstanceOf(TransportException.class)
                .hasMessage("No se puede editar un borrador que ya fue registrado");
    }

    @Test
    void reportsHowManyToursExceedTheirVehicleCapacityOnlyForPendingDrafts() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        draft.setTotalRows(2);
        draft.setDuplicateRows(0);
        draft.setConflictingRows(0);
        draft.setBlocksDetected(0);
        TourProcessingDraftTour oversized = entity(draft, 1, "100", "Cartago");
        oversized.setPassengers(40);
        when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(rowRepository.findPrimariesByDraftId(draft.getId())).thenReturn(List.of(oversized, entity(draft, 2, "200", "Heredia")));
        when(vehicleRepository.findAll()).thenReturn(List.of(
                Vehicle.builder().id(UUID.randomUUID()).plate("301-295").type(VehicleType.SEDAN).capacity(30).build()
        ));

        assertThat(service.findById(draft.getId()).getOversizedRows()).isEqualTo(1);

        draft.setStatus(TourProcessingDraftStatus.REGISTERED);
        assertThat(service.findById(draft.getId()).getOversizedRows()).isZero();
    }

    @Test
    void autoAssignClearsUnknownDriversKeepsCatalogDriversAndFillsRepeatedRows() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        draft.setYear(2026);
        draft.setMonth(5);
        draft.setBusinessDays(28);
        TourProcessingDraftTour kept = entity(draft, 1, "100", "Cartago");
        kept.primaryAssignment().setDriver("daniel   ZUNIGA");
        kept.setDepartureDate(LocalDate.of(2026, 5, 6));
        kept.setReturnDate(LocalDate.of(2026, 5, 6));
        TourProcessingDraftTour unknown = entity(draft, 2, "200", "Heredia");
        unknown.primaryAssignment().setDriver("Chofer Fantasma");
        unknown.setDepartureDate(LocalDate.of(2026, 5, 4));
        unknown.setReturnDate(LocalDate.of(2026, 5, 5));
        TourProcessingDraftTour unknownRepeated = entity(draft, 3, "200", "Heredia");
        unknownRepeated.primaryAssignment().setDriver("Chofer Fantasma");
        unknownRepeated.setDuplicateRank(2);
        unknownRepeated.setDepartureDate(LocalDate.of(2026, 5, 4));
        unknownRepeated.setReturnDate(LocalDate.of(2026, 5, 5));
        stubDraft(draft, kept, unknown, unknownRepeated);
        when(driverRepository.findAll()).thenReturn(List.of(
                Driver.builder().id(UUID.randomUUID()).name("Daniel Zuniga").status("ACTIVE").build(),
                Driver.builder().id(UUID.randomUUID()).name("Laura Mora").status("ACTIVE").build()
        ));

        TourProcessingDriverAutoAssignResponseDto result = service.autoAssignDrivers(draft.getId());

        assertThat(kept.primaryAssignment().getDriver()).isEqualTo("Daniel Zuniga");
        assertThat(unknown.primaryAssignment().getDriver()).isEqualTo("Laura Mora");
        assertThat(unknownRepeated.primaryAssignment().getDriver()).isEqualTo("Laura Mora");
        assertThat(unknownRepeated.isConflicting()).isFalse();
        assertThat(result.getClearedTours()).isEqualTo(1);
        assertThat(result.getAssignedTours()).isEqualTo(1);
        assertThat(result.getUnassignedTours()).isZero();
        verify(draftHoursService).syncChanged(eq(draft), anyMap(), anyList());
    }

    @Test
    void autoAssignKeepsABorrowedDriverThatIsNotInTheCatalog() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        draft.setYear(2026);
        draft.setMonth(5);
        draft.setBusinessDays(28);
        TourProcessingDraftTour borrowed = shortRow(draft, 1, "100", "2026-05-06", "08:00", "12:00", "Pedro Externo");
        borrowed.primaryAssignment().setResourceType(ResourceType.BORROWED_VEHICLE);
        TourProcessingDraftTour pending = shortRow(draft, 2, "200", "2026-05-06", "13:00", "15:00", null);
        stubDraft(draft, borrowed, pending);
        when(driverRepository.findAll()).thenReturn(List.of(
                Driver.builder().id(UUID.randomUUID()).name("Laura Mora").status("ACTIVE").build()
        ));

        TourProcessingDriverAutoAssignResponseDto result = service.autoAssignDrivers(draft.getId());

        assertThat(borrowed.primaryAssignment().getDriver()).isEqualTo("Pedro Externo");
        assertThat(borrowed.primaryAssignment().getDriverId()).isNull();
        assertThat(pending.primaryAssignment().getDriver()).isEqualTo("Laura Mora");
        assertThat(result.getClearedTours()).isZero();
    }

    @Test
    void autoAssignLeavesToursEmptyWhenNoDriverQualifies() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        draft.setYear(2026);
        draft.setMonth(5);
        draft.setBusinessDays(28);
        TourProcessingDraftTour kept = entity(draft, 1, "100", "Cartago");
        TourProcessingDraftTour pending = entity(draft, 2, "200", "Heredia");
        pending.primaryAssignment().setDriver(null);
        stubDraft(draft, kept, pending);
        when(driverRepository.findAll()).thenReturn(List.of(
                Driver.builder().id(UUID.randomUUID()).name("Daniel Zuniga").status("ACTIVE").build()
        ));

        TourProcessingDriverAutoAssignResponseDto result = service.autoAssignDrivers(draft.getId());

        assertThat(pending.primaryAssignment().getDriver()).isNull();
        assertThat(kept.primaryAssignment().getDriver()).isEqualTo("Daniel Zuniga");
        assertThat(result.getAssignedTours()).isZero();
        assertThat(result.getUnassignedTours()).isEqualTo(1);
        assertThat(result.getUnassignedDetails()).hasSize(1);
        assertThat(result.getUnassignedDetails().get(0).rowId()).isEqualTo(pending.getId());
        assertThat(result.getUnassignedDetails().get(0).reasons()).isNotEmpty();
        assertThat(result.getSurplusTargetHours()).isEqualTo(35.625);
    }

    @Test
    void autoAssignPreservesExistingAssignmentsWhileFillingPendingTours() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        draft.setYear(2026);
        draft.setMonth(5);
        draft.setBusinessDays(28);
        Driver ana = Driver.builder().id(UUID.randomUUID()).name("Ana").status("ACTIVE").build();
        Driver beto = Driver.builder().id(UUID.randomUUID()).name("Beto").status("ACTIVE").build();

        TourProcessingDraftTour betoWeekday = fullDayRow(draft, 1, "1", "2026-05-04", "Beto");
        TourProcessingDraftTour betoWeekday2 = fullDayRow(draft, 2, "2", "2026-05-05", "Beto");
        TourProcessingDraftTour betoWeekday3 = fullDayRow(draft, 3, "3", "2026-05-06", "Beto");
        TourProcessingDraftTour betoWeekday4 = fullDayRow(draft, 4, "4", "2026-05-11", "Beto");
        TourProcessingDraftTour lowPriorityWeekend = shortRow(draft, 5, "9", "2026-05-02", "08:00", "10:00", null);
        lowPriorityWeekend.setPriority(5);
        stubDraft(draft, betoWeekday, betoWeekday2, betoWeekday3, betoWeekday4, lowPriorityWeekend);
        when(driverRepository.findAll()).thenReturn(List.of(ana, beto));

        TourProcessingDriverAutoAssignResponseDto result = service.autoAssignDrivers(draft.getId());

        assertThat(lowPriorityWeekend.primaryAssignment().getDriver()).isEqualTo("Beto");
        assertThat(betoWeekday.primaryAssignment().getDriver()).isEqualTo("Beto");
        assertThat(result.getAssignedTours()).isEqualTo(1);
        assertThat(result.getUnassignedTours()).isZero();
    }

    @Test
    void autoAssignGivesTheWeekendTourToTheDriverWhoReachedNinetyFivePercentOfTheirOwnMax() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        draft.setYear(2026);
        draft.setMonth(5);
        draft.setBusinessDays(28);
        Driver ana = Driver.builder().id(UUID.randomUUID()).name("Ana").status("ACTIVE").build();
        Driver beto = Driver.builder().id(UUID.randomUUID()).name("Beto").status("ACTIVE").build();
        businessDays.customize(ana.getId(), 2026, 5, 14);
        TourProcessingDraftTour weekend = shortRow(draft, 7, "9", "2026-05-09", "08:00", "12:00", null);
        stubDraft(draft,
                shortRow(draft, 1, "1", "2026-05-04", "16:30", "23:30", "Ana"),
                shortRow(draft, 2, "2", "2026-05-05", "16:30", "23:30", "Ana"),
                shortRow(draft, 3, "3", "2026-05-06", "16:30", "21:30", "Ana"),
                shortRow(draft, 4, "4", "2026-05-04", "16:30", "23:30", "Beto"),
                shortRow(draft, 5, "5", "2026-05-05", "16:30", "23:30", "Beto"),
                shortRow(draft, 6, "6", "2026-05-06", "16:30", "21:30", "Beto"),
                weekend);
        when(driverRepository.findAll()).thenReturn(List.of(ana, beto));

        TourProcessingDriverAutoAssignResponseDto result = service.autoAssignDrivers(draft.getId());

        assertThat(weekend.primaryAssignment().getDriver()).isEqualTo("Ana");
        assertThat(result.getAssignedTours()).isEqualTo(1);
        assertThat(result.getSurplusDeficits()).containsOnlyKeys("Beto");
        assertThat(result.getSurplusTargets()).containsOnlyKeys("Beto");
        assertThat(result.getSurplusTargets().get("Beto")).isEqualTo(35.625);
        assertThat(result.getSurplusTargetHours()).isEqualTo(35.625);
    }

    @Test
    void autoAssignLeavesTheSameWeekendTourPendingWhenEveryoneUsesTheMonthValue() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        draft.setYear(2026);
        draft.setMonth(5);
        draft.setBusinessDays(28);
        Driver ana = Driver.builder().id(UUID.randomUUID()).name("Ana").status("ACTIVE").build();
        Driver beto = Driver.builder().id(UUID.randomUUID()).name("Beto").status("ACTIVE").build();
        TourProcessingDraftTour weekend = shortRow(draft, 7, "9", "2026-05-09", "08:00", "12:00", null);
        weekend.setPassengers(5);
        stubDraft(draft,
                shortRow(draft, 1, "1", "2026-05-04", "16:30", "23:30", "Ana"),
                shortRow(draft, 2, "2", "2026-05-05", "16:30", "23:30", "Ana"),
                shortRow(draft, 3, "3", "2026-05-06", "16:30", "21:30", "Ana"),
                shortRow(draft, 4, "4", "2026-05-04", "16:30", "23:30", "Beto"),
                shortRow(draft, 5, "5", "2026-05-05", "16:30", "23:30", "Beto"),
                shortRow(draft, 6, "6", "2026-05-06", "16:30", "21:30", "Beto"),
                weekend);
        when(driverRepository.findAll()).thenReturn(List.of(ana, beto));

        TourProcessingDriverAutoAssignResponseDto result = service.autoAssignDrivers(draft.getId());

        assertThat(weekend.primaryAssignment().getDriver()).isNull();
        assertThat(result.getAssignedTours()).isZero();
        assertThat(result.getSurplusDeficits()).containsOnlyKeys("Ana", "Beto");
        assertThat(result.getMandatoryCoverage().total()).isEqualTo(6);
        assertThat(result.getMandatoryCoverage().fulfilled()).isEqualTo(6);
        assertThat(result.getMandatoryCoverage().coveringDrivers()).isEmpty();
    }

    @Test
    void autoAssignCoversAnObligatoryWeekendTourWithAWarningAndReportsTheCoverageSummary() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        draft.setYear(2026);
        draft.setMonth(5);
        draft.setBusinessDays(28);
        Driver ana = Driver.builder().id(UUID.randomUUID()).name("Ana").status("ACTIVE").build();
        Driver beto = Driver.builder().id(UUID.randomUUID()).name("Beto").status("ACTIVE").build();
        TourProcessingDraftTour weekend = shortRow(draft, 7, "9", "2026-05-09", "08:00", "12:00", null);
        TourProcessingDraftTour largeWeekend = shortRow(draft, 8, "10", "2026-05-16", "08:00", "12:00", null);
        largeWeekend.setPassengers(5);
        stubDraft(draft,
                shortRow(draft, 1, "1", "2026-05-04", "16:30", "23:30", "Ana"),
                shortRow(draft, 2, "2", "2026-05-05", "16:30", "23:30", "Ana"),
                shortRow(draft, 3, "3", "2026-05-06", "16:30", "21:30", "Ana"),
                shortRow(draft, 4, "4", "2026-05-04", "16:30", "23:30", "Beto"),
                shortRow(draft, 5, "5", "2026-05-05", "16:30", "23:30", "Beto"),
                shortRow(draft, 6, "6", "2026-05-06", "16:30", "21:30", "Beto"),
                weekend, largeWeekend);
        when(driverRepository.findAll()).thenReturn(List.of(ana, beto));

        TourProcessingDriverAutoAssignResponseDto result = service.autoAssignDrivers(draft.getId());

        assertThat(weekend.primaryAssignment().getDriver()).isNotNull();
        assertThat(largeWeekend.primaryAssignment().getDriver()).isNull();
        assertThat(result.getAssignedTours()).isEqualTo(1);
        assertThat(result.getUnassignedTours()).isEqualTo(1);
        assertThat(result.getPriorityOneUnassigned()).isEqualTo(1);
        TourProcessingDriverAutoAssignResponseDto.MandatoryCoverage coverage = result.getMandatoryCoverage();
        assertThat(coverage.total()).isEqualTo(7);
        assertThat(coverage.withWarning()).isEqualTo(1);
        assertThat(coverage.fulfilled()).isEqualTo(6);
        assertThat(coverage.unattended()).isZero();
        assertThat(coverage.warnings()).hasSize(1);
        assertThat(coverage.warnings().get(0).rowId()).isEqualTo(weekend.getId());
        assertThat(coverage.warnings().get(0).number()).isEqualTo("9");
        assertThat(coverage.warnings().get(0).reasons().get(0)).contains("con sobresueldo al");
        assertThat(coverage.coveringDrivers()).hasSize(1);
        assertThat(coverage.coveringDrivers().get(0).name()).isEqualTo(weekend.primaryAssignment().getDriver());
        assertThat(coverage.driversBelowTarget()).isEqualTo(2);
    }

    @Test
    void importingAndAutoAssigningNeverTouchTheCustomizedBusinessDays() {
        Driver ana = Driver.builder().id(UUID.randomUUID()).name("Ana").status("ACTIVE").build();
        businessDays.customize(ana.getId(), 2026, 5, 21);
        when(driverRepository.findAll()).thenReturn(List.of(ana));

        service.create(request(List.of(row(8, "03625", "Tayutic"), row(12, "04504", "Merced"))));

        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        draft.setYear(2026);
        draft.setMonth(5);
        draft.setBusinessDays(28);
        TourProcessingDraftTour pending = shortRow(draft, 1, "1", "2026-05-04", "08:00", "18:00", null);
        stubDraft(draft, pending);
        service.autoAssignDrivers(draft.getId());
        service.clearAssignments(draft.getId());

        assertThat(businessDays.storedValue(ana.getId(), 2026, 5)).isEqualTo(21);
        assertThat(businessDays.storedRecords()).isEqualTo(1);
        verify(businessDays.repository(), times(1)).save(any(DriverMonthBusinessDays.class));
        verify(businessDays.repository(), never()).delete(any(DriverMonthBusinessDays.class));
        verify(businessDays.repository(), never()).deleteAll();
    }

    @Test
    void autoAssignReportsManualExceptionsWithoutChangingThemOrExternalDrivers() {
        TourProcessingDraft draft = draftWithBusinessDays(28);
        draft.setYear(2026);
        draft.setMonth(5);
        Driver ana = Driver.builder().id(UUID.randomUUID()).name("Ana").status("ACTIVE").build();
        TourProcessingDraftTour weekend = shortRow(draft, 1, "1", "2026-05-09", "08:00", "12:00", "Ana");
        TourProcessingDraftTour repeated = shortRow(draft, 2, "1", "2026-05-09", "08:00", "12:00", "Ana");
        repeated.setDuplicateRank(2);
        TourProcessingDraftTour external = shortRow(draft, 3, "2", "2026-05-09", "08:00", "12:00", "Empresa externa");
        external.primaryAssignment().setResourceType(ResourceType.EXTERNAL);
        stubDraft(draft, weekend, repeated, external);
        when(driverRepository.findAll()).thenReturn(List.of(ana));

        TourProcessingDriverAutoAssignResponseDto result = service.autoAssignDrivers(draft.getId());

        assertThat(weekend.primaryAssignment().getDriver()).isEqualTo("Ana");
        assertThat(repeated.primaryAssignment().getDriver()).isEqualTo("Ana");
        assertThat(external.primaryAssignment().getDriver()).isEqualTo("Empresa externa");
        assertThat(result.getAssignedTours()).isZero();
        assertThat(result.getUnassignedTours()).isZero();
        assertThat(result.getPreservedAssignmentWarnings()).hasSize(1);
        assertThat(result.getPreservedAssignmentWarnings().get(0).reasons()).anyMatch(reason -> reason.contains("95%"));
        assertThat(result.getClearedTours()).isZero();
        verify(draftHoursService).syncChanged(eq(draft), anyMap(), anyList());
    }

    @Test
    void rejectsAutoAssignWhenDraftHasNoBusinessDays() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        stubDraft(draft);

        assertThatThrownBy(() -> service.autoAssignDrivers(draft.getId()))
                .isInstanceOf(TransportException.class)
                .hasMessage("El borrador no tiene mes ni días hábiles definidos, no se pueden autoasignar choferes");
        verify(draftHoursService, never()).syncChanged(any(), anyMap(), anyList());
    }

    @Test
    void rejectsAutoAssignOnRegisteredDraft() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.REGISTERED);
        stubDraft(draft);

        assertThatThrownBy(() -> service.autoAssignDrivers(draft.getId()))
                .isInstanceOf(TransportException.class)
                .hasMessage("No se puede editar un borrador que ya fue registrado");
    }

    @Test
    void autoAssignUlloaDriversFillsOnlyUlloaToursAndLeavesTheRestUntouched() {
        TourProcessingDraft draft = draftWithBusinessDays(28);
        draft.setYear(2026);
        draft.setMonth(5);
        Driver ricardo = Driver.builder().id(UUID.randomUUID()).name("Ricardo Jovane").status("ACTIVE")
                .ordinaryStart(LocalTime.of(12, 0)).ordinaryEnd(LocalTime.of(21, 0))
                .ulloaShifts(EnumSet.of(UlloaShift.AFTERNOON)).build();
        Driver laura = Driver.builder().id(UUID.randomUUID()).name("Laura Mora").status("ACTIVE").build();
        TourProcessingDraftTour afternoon = ulloaRow(draft, 1, "1", "2026-05-05", "12:00", "22:00", null);
        TourProcessingDraftTour unknownDriverMorning = ulloaRow(draft, 2, "2", "2026-05-05", "06:30", "17:30", "Chofer Fantasma");
        TourProcessingDraftTour regular = shortRow(draft, 3, "3", "2026-05-06", "08:00", "10:00", null);
        TourProcessingDraftTour unknownOutsideUlloa = shortRow(draft, 4, "4", "2026-05-07", "08:00", "10:00", "Otro Fantasma");
        stubDraft(draft, afternoon, unknownDriverMorning, regular, unknownOutsideUlloa);
        when(driverRepository.findAll()).thenReturn(List.of(ricardo, laura));

        TourProcessingDriverAutoAssignResponseDto result = service.autoAssignUlloaDrivers(draft.getId());

        assertThat(afternoon.primaryAssignment().getDriver()).isEqualTo("Ricardo Jovane");
        assertThat(unknownDriverMorning.primaryAssignment().getDriver()).isNull();
        assertThat(regular.primaryAssignment().getDriver()).isNull();
        assertThat(unknownOutsideUlloa.primaryAssignment().getDriver()).isEqualTo("Otro Fantasma");
        assertThat(result.getAssignedTours()).isEqualTo(1);
        assertThat(result.getClearedTours()).isEqualTo(1);
        assertThat(result.getUnassignedTours()).isEqualTo(1);
        verify(draftHoursService).syncChanged(eq(draft), anyMap(), anyList());
    }

    @Test
    void rejectsUlloaAutoAssignWhenDraftHasNoBusinessDays() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        stubDraft(draft);

        assertThatThrownBy(() -> service.autoAssignUlloaDrivers(draft.getId()))
                .isInstanceOf(TransportException.class)
                .hasMessage("El borrador no tiene mes ni días hábiles definidos, no se pueden autoasignar choferes");
        verify(draftHoursService, never()).syncChanged(any(), anyMap(), anyList());
    }

    @Test
    void autoAssignVehiclesClearsUnknownValuesKeepsCatalogPlatesAndFillsRepeatedRows() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour kept = entity(draft, 1, "100", "Cartago");
        kept.primaryAssignment().setVehicle("301-295 ");
        TourProcessingDraftTour vehicleTypeText = entity(draft, 2, "200", "Heredia");
        vehicleTypeText.primaryAssignment().setVehicle("Todo Terreno");
        TourProcessingDraftTour unknownPlate = entity(draft, 3, "300", "Limón");
        unknownPlate.primaryAssignment().setVehicle("999-999");
        TourProcessingDraftTour unknownPlateRepeated = entity(draft, 4, "300", "Limón");
        unknownPlateRepeated.primaryAssignment().setVehicle("999-999");
        unknownPlateRepeated.setDuplicateRank(2);
        stubDraft(draft, kept, vehicleTypeText, unknownPlate, unknownPlateRepeated);
        when(vehicleRepository.findAll()).thenReturn(List.of(
                Vehicle.builder().id(UUID.randomUUID()).plate("301-295").type(VehicleType.SEDAN).capacity(5).build(),
                Vehicle.builder().id(UUID.randomUUID()).plate("600-600").type(VehicleType.SEDAN).capacity(5).build(),
                Vehicle.builder().id(UUID.randomUUID()).plate("500-500").type(VehicleType.SEDAN).capacity(5).build()
        ));

        TourProcessingVehicleAutoAssignResponseDto result = service.autoAssignVehicles(draft.getId());

        assertThat(kept.primaryAssignment().getVehicle()).isEqualTo("301-295");
        assertThat(vehicleTypeText.primaryAssignment().getVehicle()).isEqualTo("500-500");
        assertThat(unknownPlate.primaryAssignment().getVehicle()).isEqualTo("600-600");
        assertThat(unknownPlateRepeated.primaryAssignment().getVehicle()).isEqualTo("600-600");
        assertThat(unknownPlateRepeated.isConflicting()).isFalse();
        assertThat(result.getClearedTours()).isEqualTo(2);
        assertThat(result.getAssignedTours()).isEqualTo(2);
        assertThat(result.getUnassignedTours()).isZero();
    }

    @Test
    void assignsVehicleToExistingDriverBeforeCompletingDistributedTour() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        draft.setYear(2026);
        draft.setMonth(5);
        draft.setBusinessDays(21);
        TourProcessingDraftTour withDriver = shortRow(draft, 1, "200", "2026-05-04", "08:00", "10:00", "Daniel Zuniga");
        withDriver.setPriority(2);
        withDriver.primaryAssignment().setVehicle(null);
        TourProcessingDraftTour distributed = shortRow(draft, 2, "100", "2026-05-04", "08:00", "10:00", null);
        distributed.setPassengers(6);
        distributed.primaryAssignment().setPassengers(3);
        distributed.primaryAssignment().setVehicle(null);
        distributed.addAssignment(TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).passengers(3).build());
        stubDraft(draft, withDriver, distributed);
        when(driverRepository.findAll()).thenReturn(List.of(
                Driver.builder().id(UUID.randomUUID()).name("Daniel Zuniga").build(),
                Driver.builder().id(UUID.randomUUID()).name("Ana").build(),
                Driver.builder().id(UUID.randomUUID()).name("Beto").build()
        ));
        when(vehicleRepository.findAll()).thenReturn(List.of(
                Vehicle.builder().id(UUID.randomUUID()).plate("100-001").capacity(3).build(),
                Vehicle.builder().id(UUID.randomUUID()).plate("200-002").capacity(3).build()
        ));

        TourProcessingVehicleAutoAssignResponseDto result = service.autoAssignVehicles(draft.getId());

        assertThat(withDriver.primaryAssignment().getVehicle()).isEqualTo("100-001");
        assertThat(distributed.getAssignments()).allSatisfy(part -> assertThat(part.getVehicle()).isNull());
        assertThat(result.getAssignedTours()).isEqualTo(1);
    }

    @Test
    void completesDistributedTourWithExistingDriverBeforeTourWithoutDriver() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        draft.setYear(2026);
        draft.setMonth(5);
        draft.setBusinessDays(21);
        TourProcessingDraftTour withDriver = shortRow(draft, 1, "200", "2026-05-04", "08:00", "10:00", "Ana");
        withDriver.setPriority(2);
        withDriver.setPassengers(6);
        withDriver.primaryAssignment().setPassengers(3);
        withDriver.primaryAssignment().setVehicle(null);
        withDriver.addAssignment(TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).passengers(3).build());
        TourProcessingDraftTour withoutDriver = shortRow(draft, 2, "100", "2026-05-04", "08:00", "10:00", null);
        withoutDriver.setPassengers(6);
        withoutDriver.primaryAssignment().setPassengers(3);
        withoutDriver.primaryAssignment().setVehicle(null);
        withoutDriver.addAssignment(TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).passengers(3).build());
        stubDraft(draft, withDriver, withoutDriver);
        when(driverRepository.findAll()).thenReturn(List.of(
                Driver.builder().id(UUID.randomUUID()).name("Ana").build(),
                Driver.builder().id(UUID.randomUUID()).name("Beto").build(),
                Driver.builder().id(UUID.randomUUID()).name("Caro").build()
        ));
        when(vehicleRepository.findAll()).thenReturn(List.of(
                Vehicle.builder().id(UUID.randomUUID()).plate("100-001").capacity(3).build(),
                Vehicle.builder().id(UUID.randomUUID()).plate("200-002").capacity(3).build()
        ));

        TourProcessingVehicleAutoAssignResponseDto result = service.autoAssignVehicles(draft.getId());

        assertThat(withDriver.getAssignments()).allSatisfy(part -> assertThat(part.getVehicle()).isNotNull());
        assertThat(withoutDriver.getAssignments()).allSatisfy(part -> assertThat(part.getVehicle()).isNull());
        assertThat(result.getAssignedTours()).isEqualTo(2);
        assertThat(result.getUnassignedDetails()).hasSize(2)
                .allSatisfy(issue -> assertThat(issue.reasons()).anyMatch(reason -> reason.contains("combinación completa")));
    }

    @Test
    void reportsWhyVehicleCouldNotBeAssignedAndFlagsExistingVehicleWithoutCapacity() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour pending = entity(draft, 1, "100", "Cartago");
        pending.primaryAssignment().setVehicle(null);
        pending.setPassengers(10);
        pending.primaryAssignment().setPassengers(10);
        TourProcessingDraftTour kept = entity(draft, 2, "200", "Heredia");
        kept.primaryAssignment().setVehicle("300-003");
        stubDraft(draft, pending, kept);
        when(vehicleRepository.findAll()).thenReturn(List.of(
                Vehicle.builder().id(UUID.randomUUID()).plate("300-003").build()
        ));

        TourProcessingVehicleAutoAssignResponseDto result = service.autoAssignVehicles(draft.getId());

        assertThat(result.getUnassignedDetails()).hasSize(1);
        assertThat(result.getUnassignedDetails().get(0).reasons()).anyMatch(reason -> reason.contains("capacidad registrada"));
        assertThat(result.getPreservedAssignmentWarnings()).hasSize(1);
        assertThat(result.getPreservedAssignmentWarnings().get(0).reasons()).anyMatch(reason -> reason.contains("no tiene capacidad"));
        assertThat(kept.primaryAssignment().getVehicle()).isEqualTo("300-003");
    }

    @Test
    void rejectsVehicleAutoAssignOnRegisteredDraft() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.REGISTERED);
        stubDraft(draft);

        assertThatThrownBy(() -> service.autoAssignVehicles(draft.getId()))
                .isInstanceOf(TransportException.class)
                .hasMessage("No se puede editar un borrador que ya fue registrado");
    }

    @Test
    void clearsEveryDriverAndVehicleAssignmentIncludingBorrowedAndDistributedRows() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour internal = entity(draft, 1, "100", "Cartago");
        TourProcessingDraftTour repeated = entity(draft, 2, "100", "Cartago");
        repeated.setDuplicateRank(2);
        TourProcessingDraftTour borrowed = entity(draft, 3, "200", "Heredia");
        borrowed.primaryAssignment().setResourceType(ResourceType.BORROWED_VEHICLE);
        borrowed.primaryAssignment().setVehicle(null);
        borrowed.primaryAssignment().setBorrowedVehicle(mock(BorrowedVehicle.class));
        TourProcessingDraftTour external = entity(draft, 4, "300", "Limón");
        external.primaryAssignment().setResourceType(ResourceType.EXTERNAL);
        TourProcessingDraftTour distributed = entity(draft, 5, "400", "San José");
        TourProcessingDraftTour distributedChild = entity(draft, 6, "400", "San José");
        distributed.addAssignment(distributedChild.primaryAssignment());
        List<TourProcessingDraftTour> rows = List.of(internal, repeated, borrowed, external, distributed);
        stubDraft(draft, rows.toArray(TourProcessingDraftTour[]::new));

        service.clearAssignments(draft.getId());

        assertThat(rows).allSatisfy(row -> {
            assertThat(row.primaryAssignment().getDriver()).isNull();
            assertThat(row.primaryAssignment().getVehicle()).isNull();
            assertThat(row.primaryAssignment().getBorrowedVehicle()).isNull();
        });
        assertThat(borrowed.primaryAssignment().getResourceType()).isEqualTo(ResourceType.BORROWED_VEHICLE);
        assertThat(external.primaryAssignment().getResourceType()).isEqualTo(ResourceType.EXTERNAL);
        assertThat(distributedChild.primaryAssignment().getPart()).isEqualTo(2);
        verify(draftHoursService).syncChanged(eq(draft), anyMap(), anyList());
    }

    @Test
    void countsBorrowedToursWithoutVehicleAsUnassigned() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour borrowed = entity(draft, 1, "200", "Heredia");
        borrowed.primaryAssignment().setResourceType(ResourceType.BORROWED_VEHICLE);
        borrowed.primaryAssignment().setVehicle(null);
        stubDraft(draft, borrowed);

        TourProcessingVehicleAutoAssignResponseDto result = service.autoAssignVehicles(draft.getId());

        assertThat(result.getUnassignedTours()).isEqualTo(1);
        assertThat(result.getUnassignedDetails().get(0).reasons()).containsExactly("Selecciona el vehículo prestado de esta gira.");
    }

    @Test
    void rejectsClearingAssignmentsOnRegisteredDraft() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.REGISTERED);
        stubDraft(draft);

        assertThatThrownBy(() -> service.clearAssignments(draft.getId()))
                .isInstanceOf(TransportException.class)
                .hasMessage("No se puede editar un borrador que ya fue registrado");
        verify(draftHoursService, never()).syncChanged(any(), anyMap(), anyList());
    }

    @Test
    void rejectsManualDayEditWhenOrdinaryHoursExceedNineHours() {
        TourProcessingDraft draft = draftWithBusinessDays(21);
        TourProcessingDraftTour row = entity(draft, 1, "100", "Cartago");
        stubDraft(draft, row);
        TourProcessingDraftDay day = dayEntity(row, LocalDate.of(2026, 5, 4), LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 0.0, 0.0);
        when(dayRepository.findByIdAndDraftId(day.getId(), draft.getId())).thenReturn(Optional.of(day));
        when(dayRepository.findByAssignmentIdIn(anyList())).thenReturn(List.of(day));

        TourProcessingDraftRowDayRequestDto request = dayRequest(LocalTime.of(7, 0), LocalTime.of(20, 0), 9.5, 0.0, 0.0, false);

        assertThatThrownBy(() -> service.updateRowDay(draft.getId(), row.getId(), day.getId(), request))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("TOUR_PROCESSING_DRAFT_ROW_DAY_ORDINARY_LIMIT");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                });
        verify(dayRepository, never()).save(any());
    }

    @Test
    void rejectsManualDayEditWhenSurplusCapForTheMonthIsExceeded() {
        TourProcessingDraft draft = draftWithBusinessDays(21);
        TourProcessingDraftTour otherRow = entity(draft, 1, "100", "Cartago");
        TourProcessingDraftTour row = entity(draft, 2, "200", "Heredia");
        stubDraft(draft, otherRow, row);
        TourProcessingDraftDay otherDay = dayEntity(otherRow, LocalDate.of(2026, 5, 4), LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 27.0, 0.0);
        TourProcessingDraftDay day = dayEntity(row, LocalDate.of(2026, 5, 11), LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 0.0, 0.0);
        when(dayRepository.findByIdAndDraftId(day.getId(), draft.getId())).thenReturn(Optional.of(day));
        row.primaryAssignment().setDriverId(UUID.nameUUIDFromBytes("Daniel Zuniga".getBytes()));
        otherRow.primaryAssignment().setDriverId(row.primaryAssignment().getDriverId());
        when(dayRepository.findAllByDraftId(draft.getId())).thenReturn(List.of(otherDay, day));

        TourProcessingDraftRowDayRequestDto request = dayRequest(LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 2.0, 0.0, false);

        assertThatThrownBy(() -> service.updateRowDay(draft.getId(), row.getId(), day.getId(), request))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("TOUR_PROCESSING_DRAFT_ROW_DAY_SURPLUS_CAP_EXCEEDED");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                });
        verify(dayRepository, never()).save(any());
    }

    @Test
    void rejectsManualDayEditWhenOvertimeCapForTheMonthIsExceeded() {
        TourProcessingDraft draft = draftWithBusinessDays(21);
        TourProcessingDraftTour otherRow = entity(draft, 1, "100", "Cartago");
        TourProcessingDraftTour row = entity(draft, 2, "200", "Heredia");
        stubDraft(draft, otherRow, row);
        TourProcessingDraftDay otherDay = dayEntity(otherRow, LocalDate.of(2026, 5, 4), LocalTime.of(7, 30), LocalTime.of(20, 30), 9.0, 4.0, 59.0);
        TourProcessingDraftDay day = dayEntity(row, LocalDate.of(2026, 5, 11), LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 0.0, 0.0);
        when(dayRepository.findByIdAndDraftId(day.getId(), draft.getId())).thenReturn(Optional.of(day));
        row.primaryAssignment().setDriverId(UUID.nameUUIDFromBytes("Daniel Zuniga".getBytes()));
        otherRow.primaryAssignment().setDriverId(row.primaryAssignment().getDriverId());
        when(dayRepository.findAllByDraftId(draft.getId())).thenReturn(List.of(otherDay, day));

        TourProcessingDraftRowDayRequestDto request = dayRequest(LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 0.0, 2.0, false);

        assertThatThrownBy(() -> service.updateRowDay(draft.getId(), row.getId(), day.getId(), request))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("TOUR_PROCESSING_DRAFT_ROW_DAY_OVERTIME_CAP_EXCEEDED");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                });
        verify(dayRepository, never()).save(any());
    }

    @Test
    void requiresConfirmationToAssignOvertimeBelowTheSurplusThreshold() {
        TourProcessingDraft draft = draftWithBusinessDays(21);
        TourProcessingDraftTour row = entity(draft, 1, "100", "Cartago");
        stubDraft(draft, row);
        TourProcessingDraftDay day = dayEntity(row, LocalDate.of(2026, 5, 4), LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 0.0, 0.0);
        when(dayRepository.findByIdAndDraftId(day.getId(), draft.getId())).thenReturn(Optional.of(day));
        when(dayRepository.findByAssignmentIdIn(anyList())).thenReturn(List.of(day));

        TourProcessingDraftRowDayRequestDto request = dayRequest(LocalTime.of(7, 30), LocalTime.of(17, 30), 9.0, 0.0, 1.0, false);

        assertThatThrownBy(() -> service.updateRowDay(draft.getId(), row.getId(), day.getId(), request))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("TOUR_PROCESSING_DRAFT_ROW_DAY_OVERTIME_NEEDS_CONFIRMATION");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
        verify(dayRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void savesOvertimeBelowTheSurplusThresholdWhenConfirmed() {
        TourProcessingDraft draft = draftWithBusinessDays(21);
        TourProcessingDraftTour row = entity(draft, 1, "100", "Cartago");
        stubDraft(draft, row);
        TourProcessingDraftDay day = dayEntity(row, LocalDate.of(2026, 5, 4), LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 0.0, 0.0);
        when(dayRepository.findByIdAndDraftId(day.getId(), draft.getId())).thenReturn(Optional.of(day));
        when(dayRepository.findByAssignmentIdIn(anyList())).thenReturn(List.of(day));
        when(dayRepository.save(any(TourProcessingDraftDay.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TourProcessingDraftRowDayRequestDto request = dayRequest(LocalTime.of(7, 30), LocalTime.of(17, 30), 9.0, 0.0, 1.0, true);

        var response = service.updateRowDay(draft.getId(), row.getId(), day.getId(), request);

        assertThat(response.getOvertimeHours()).isEqualTo(1.0);
        assertThat(response.isManuallyEdited()).isTrue();
        verify(dayRepository).save(day);
        verify(draftHoursService).syncDriver(draft, List.of(row.primaryAssignment()), row.primaryAssignment().getDriverId());
    }

    @Test
    void rejectsManualDayEditWhenEndTimeIsNotAfterStartTime() {
        TourProcessingDraft draft = draftWithBusinessDays(21);
        TourProcessingDraftTour row = entity(draft, 1, "100", "Cartago");
        stubDraft(draft, row);
        TourProcessingDraftDay day = dayEntity(row, LocalDate.of(2026, 5, 4), LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 0.0, 0.0);
        when(dayRepository.findByIdAndDraftId(day.getId(), draft.getId())).thenReturn(Optional.of(day));

        TourProcessingDraftRowDayRequestDto request = dayRequest(LocalTime.of(16, 0), LocalTime.of(8, 0), 9.0, 0.0, 0.0, false);

        assertThatThrownBy(() -> service.updateRowDay(draft.getId(), row.getId(), day.getId(), request))
                .isInstanceOfSatisfying(TransportException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo("TOUR_PROCESSING_DRAFT_ROW_DAY_INVALID"));
        verify(dayRepository, never()).save(any());
    }

    @Test
    void getScheduleDelegatesToTheOvertimeReportServiceWithTheResolvedDraft() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        OvertimeRequestReportDto expected = OvertimeRequestReportDto.builder().build();
        when(overtimeRequestReportService.buildSchedule(draft)).thenReturn(expected);

        OvertimeRequestReportDto result = service.getSchedule(draft.getId());

        assertThat(result).isSameAs(expected);
    }

    @Test
    void getScheduleFailsWhenTheDraftDoesNotExist() {
        UUID draftId = UUID.randomUUID();
        when(draftRepository.findById(draftId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSchedule(draftId))
                .isInstanceOf(TransportException.class);
        verify(overtimeRequestReportService, never()).buildSchedule(any());
    }

    @Test
    void findsOneRowWithItsDistributionAndDaysUsingASingleDaysQuery() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        TourProcessingDraftTour root = entity(draft, 1, "300", "Turrialba");
        TourProcessingDraftTour part = entity(draft, 2, "300", "Turrialba");
        part.primaryAssignment().setDriver("Laura Mora");
        root.setPassengers(6);
        root.addAssignment(part.primaryAssignment());
        TourProcessingDraftDay rootDay = dayEntity(root, LocalDate.of(2026, 5, 2), LocalTime.of(5, 0), LocalTime.of(18, 0), 9, 4, 0);
        TourProcessingDraftDay partDay = dayEntity(part, LocalDate.of(2026, 5, 2), LocalTime.of(5, 0), LocalTime.of(18, 0), 9, 2, 2);
        when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(assignmentRepository.findByIdAndDraftId(part.getId(), draft.getId())).thenReturn(Optional.of(part.primaryAssignment()));
        when(rowRepository.findByIdAndDraftId(root.getId(), draft.getId())).thenReturn(Optional.of(root));
        when(dayRepository.findByAssignmentIdIn(anyCollection())).thenReturn(List.of(partDay, rootDay));

        TourProcessingDraftRowResponseDto response = service.findRow(draft.getId(), part.getId());

        assertThat(response.getId()).isEqualTo(root.getId());
        assertThat(response.getPassengers()).isEqualTo(6);
        assertThat(response.getDays()).extracting(TourProcessingDraftRowDayResponseDto::getId).containsExactly(rootDay.getId());
        assertThat(response.getAssignments())
                .extracting(TourProcessingDraftAssignmentDto::getDriver, assignment -> assignment.getDays().get(0).getId())
                .containsExactly(tuple("Daniel Zuniga", rootDay.getId()), tuple("Laura Mora", partDay.getId()));
        verify(dayRepository).findByAssignmentIdIn(anyCollection());
    }

    @Test
    void rejectsFindingARowOfAnotherDraft() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        UUID rowId = UUID.randomUUID();
        when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(rowRepository.findByIdAndDraftId(rowId, draft.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findRow(draft.getId(), rowId))
                .isInstanceOf(TransportException.class);
        verify(dayRepository, never()).findByAssignmentIdIn(anyCollection());
    }

    private TourProcessingDraft draftWithBusinessDays(int businessDays) {
        return TourProcessingDraft.builder()
                .id(UUID.randomUUID())
                .status(TourProcessingDraftStatus.DRAFT)
                .businessDays(businessDays)
                .build();
    }

    private TourProcessingDraftDay dayEntity(TourProcessingDraftTour row, LocalDate date, LocalTime start, LocalTime end,
                                           double ordinary, double surplus, double overtime) {
        return TourProcessingDraftDay.builder()
                .id(UUID.randomUUID())
                .assignment(row.primaryAssignment())
                .date(date)
                .startTime(start)
                .endTime(end)
                .ordinaryHours(ordinary)
                .surplusHours(surplus)
                .overtimeHours(overtime)
                .manuallyEdited(false)
                .build();
    }

    private TourProcessingDraftRowDayRequestDto dayRequest(LocalTime start, LocalTime end, double ordinary, double surplus,
                                                       double overtime, boolean confirmOvertimeBelowSurplusThreshold) {
        return TourProcessingDraftRowDayRequestDto.builder()
                .startTime(start)
                .endTime(end)
                .ordinaryHours(ordinary)
                .surplusHours(surplus)
                .overtimeHours(overtime)
                .confirmOvertimeBelowSurplusThreshold(confirmOvertimeBelowSurplusThreshold)
                .build();
    }

    private TourProcessingDraft draftWithPeriod() {
        TourProcessingDraft draft = draftWithStatus(TourProcessingDraftStatus.DRAFT);
        draft.setYear(2026);
        draft.setMonth(5);
        draft.setBusinessDays(21);
        return draft;
    }

    private TourProcessingDraftTour oneDayTour(TourProcessingDraft draft) {
        TourProcessingDraftTour tour = entity(draft, 8, "100", "Cartago");
        tour.setDurationDays(1);
        tour.setDepartureDate(LocalDate.of(2026, 5, 4));
        tour.setReturnDate(LocalDate.of(2026, 5, 4));
        return tour;
    }

    private TourProcessingDraftRowRequestDto twoDayWeekdayRequest() {
        TourProcessingDraftRowRequestDto request = row(8, "100", "Cartago");
        request.setDepartureDate(LocalDate.of(2026, 5, 4));
        request.setReturnDate(LocalDate.of(2026, 5, 5));
        return request;
    }

    private Driver catalogDriver() {
        return Driver.builder().id(UUID.randomUUID()).name("Daniel Zuniga").status("ACTIVE").build();
    }

    private TourProcessingDraft draftWithStatus(TourProcessingDraftStatus status) {
        return TourProcessingDraft.builder()
                .id(UUID.randomUUID())
                .status(status)
                .build();
    }

    private TourProcessingDraftTour shortRow(TourProcessingDraft draft, int rowNumber, String number, String date,
                                       String startTime, String endTime, String driverName) {
        TourProcessingDraftTour row = entity(draft, rowNumber, number, "Destino");
        row.primaryAssignment().setDriver(driverName);
        row.setDurationDays(1);
        row.setDepartureDate(LocalDate.parse(date));
        row.setDepartureTime(LocalTime.parse(startTime));
        row.setReturnDate(LocalDate.parse(date));
        row.setReturnTime(LocalTime.parse(endTime));
        return row;
    }

    private TourProcessingDraftTour ulloaRow(TourProcessingDraft draft, int rowNumber, String number, String date,
                                      String startTime, String endTime, String driverName) {
        TourProcessingDraftTour row = shortRow(draft, rowNumber, number, date, startTime, endTime, driverName);
        row.setExecutingUnit("Programa De Servicios Generales");
        row.setDestination("Ulloa (barreal)");
        row.setPriority(6);
        row.setPassengers(30);
        return row;
    }

    private TourProcessingDraftTour fullDayRow(TourProcessingDraft draft, int rowNumber, String number, String date, String driverName) {
        return shortRow(draft, rowNumber, number, date, "00:00", "23:30", driverName);
    }

    private TourProcessingDraftTour entity(TourProcessingDraft draft, int rowNumber, String number, String destination) {
        TourProcessingDraftTour tour = TourProcessingDraftTour.builder()
                .id(UUID.randomUUID())
                .draft(draft)
                .rowNumber(rowNumber)
                .number(number)
                .passengers(3)
                .executingUnit("Division De Educacion Rural Cide")
                .responsible("Selvin Fallas")
                .destination(destination)
                .durationDays(2)
                .priority(1)
                .modality("CTP")
                .departureTime(LocalTime.of(5, 0))
                .returnTime(LocalTime.of(12, 0))
                .departureDate(LocalDate.of(2026, 5, 2))
                .returnDate(LocalDate.of(2026, 5, 3))
                .duplicateRank(1)
                .duplicateGroupSize(1)
                .build();
        tour.addAssignment(TourProcessingDraftAssignment.builder().id(tour.getId()).driver("Daniel Zuniga").vehicle("301-295").passengers(3).build());
        return tour;
    }

    private void stubDraft(TourProcessingDraft draft, TourProcessingDraftTour... rows) {
        when(draftRepository.findByIdForUpdate(draft.getId())).thenReturn(Optional.of(draft));
        for (TourProcessingDraftTour row : rows) {
            when(rowRepository.findByIdAndDraftId(row.getId(), draft.getId())).thenReturn(Optional.of(row));
        }
        when(rowRepository.findAllByDraftIdOrderByRowNumberAsc(draft.getId())).thenAnswer(invocation -> {
            DraftCatalogLinker linker = DraftCatalogLinker.of(driverRepository.findAll(), vehicleRepository.findAll());
            for (TourProcessingDraftTour row : rows) row.getAssignments().forEach(linker::link);
            return List.of(rows);
        });
    }

    private TourProcessingDraftCreateRequestDto request(List<TourProcessingDraftRowRequestDto> rows) {
        return TourProcessingDraftCreateRequestDto.builder()
                .fileName("1-mayo.xls")
                .fileType("html")
                .totalRowsRead(rows.size())
                .blocksDetected(2)
                .year(2026)
                .month(5)
                .businessDays(21)
                .rows(rows)
                .build();
    }

    private TourProcessingDraftRowRequestDto row(int rowNumber, String number, String destination) {
        return TourProcessingDraftRowRequestDto.builder()
                .rowNumber(rowNumber)
                .driver("Daniel Zuniga")
                .number(number)
                .vehicle("301-295")
                .passengers(3)
                .executingUnit("Division De Educacion Rural Cide")
                .responsible("Selvin Fallas")
                .destination(destination)
                .durationDays(2)
                .priority(1)
                .modality("CTP")
                .departureTime(LocalTime.of(5, 0))
                .returnTime(LocalTime.of(12, 0))
                .departureDate(LocalDate.of(2026, 5, 2))
                .returnDate(LocalDate.of(2026, 5, 3))
                .build();
    }
}
