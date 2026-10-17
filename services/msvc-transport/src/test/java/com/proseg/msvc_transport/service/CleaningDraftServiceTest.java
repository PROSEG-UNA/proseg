package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.cleaning.CleaningDraftCreateRequestDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftImportResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftRowDayRequestDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftRowIssueDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftRowRequestDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDriverAutoAssignResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningVehicleAutoAssignResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningImportColumnDto;
import com.proseg.msvc_transport.dto.response.OvertimeRequestReportDto;
import com.proseg.msvc_transport.entity.CleaningDraft;
import com.proseg.msvc_transport.entity.CleaningDraftRow;
import com.proseg.msvc_transport.entity.CleaningDraftRowDay;
import com.proseg.msvc_transport.entity.CleaningDraftStatus;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.entity.VehicleType;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.CleaningDraftRepository;
import com.proseg.msvc_transport.repository.CleaningDraftRowDayRepository;
import com.proseg.msvc_transport.repository.CleaningDraftRowRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.VehicleRepository;
import com.proseg.msvc_transport.security.CurrentUserResolver;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CleaningDraftServiceTest {

    private CleaningDraftRepository draftRepository;
    private CleaningDraftRowRepository rowRepository;
    private CleaningDraftRowDayRepository dayRepository;
    private DriverRepository driverRepository;
    private VehicleRepository vehicleRepository;
    private DriverMonthService driverMonthService;
    private OvertimeRequestReportService overtimeRequestReportService;
    private CleaningDraftService service;

    @BeforeEach
    void setUp() {
        draftRepository = mock(CleaningDraftRepository.class);
        rowRepository = mock(CleaningDraftRowRepository.class);
        dayRepository = mock(CleaningDraftRowDayRepository.class);
        driverRepository = mock(DriverRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        driverMonthService = mock(DriverMonthService.class);
        overtimeRequestReportService = mock(OvertimeRequestReportService.class);
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
        when(currentUserResolver.resolve()).thenReturn("aleja");
        when(draftRepository.save(any(CleaningDraft.class))).thenAnswer(invocation -> {
            CleaningDraft draft = invocation.getArgument(0);
            draft.setId(UUID.randomUUID());
            return draft;
        });
        service = new CleaningDraftService(
                draftRepository,
                rowRepository,
                dayRepository,
                driverRepository,
                vehicleRepository,
                driverMonthService,
                overtimeRequestReportService,
                currentUserResolver,
                Validation.buildDefaultValidatorFactory().getValidator()
        );
    }

    @Test
    void exposesSchemaInDeclarationOrderWithRequiredFlags() {
        List<CleaningImportColumnDto> columns = service.getImportSchema().getColumns();

        assertThat(columns).hasSize(15);
        assertThat(columns.get(10).getAttribute()).isEqualTo("departureTime");
        assertThat(columns.get(12).getAttribute()).isEqualTo("departureDate");
        assertThat(columns)
                .extracting(CleaningImportColumnDto::getAttribute)
                .doesNotContain("perDay", "perMonth");
        assertThat(columns)
                .filteredOn(CleaningImportColumnDto::isRequired)
                .extracting(CleaningImportColumnDto::getAttribute)
                .containsExactly("number", "passengers", "executingUnit", "responsible", "destination",
                        "durationDays", "priority", "departureTime", "returnTime", "departureDate", "returnDate");
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsDraftKeepingAllRowsAndMarkingDuplicates() {
        CleaningDraftRowRequestDto first = row(8, "03625", "Tayutic");
        CleaningDraftRowRequestDto repeated = row(12, "03625", "Tayutic");
        CleaningDraftRowRequestDto conflicting = row(15, "03625", "Heredia");
        CleaningDraftRowRequestDto single = row(20, "04504", "Merced");

        CleaningDraftImportResponseDto response = service.create(request(List.of(first, repeated, conflicting, single)));

        ArgumentCaptor<List<CleaningDraftRow>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(rowRepository).saveAll(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue())
                .extracting(CleaningDraftRow::getRowNumber, CleaningDraftRow::getDuplicateRank,
                        CleaningDraftRow::getDuplicateGroupSize, CleaningDraftRow::isConflicting)
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

        ArgumentCaptor<CleaningDraft> draftCaptor = ArgumentCaptor.forClass(CleaningDraft.class);
        verify(draftRepository).save(draftCaptor.capture());
        CleaningDraft draft = draftCaptor.getValue();
        assertThat(draft.getStatus()).isEqualTo(CleaningDraftStatus.DRAFT);
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
        CleaningDraftRowRequestDto missingPassengers = row(3, "04504", "Merced");
        missingPassengers.setPassengers(null);
        CleaningDraftRowRequestDto returnBeforeDeparture = row(4, "04505", "Merced");
        returnBeforeDeparture.setReturnDate(LocalDate.of(2026, 5, 1));
        CleaningDraftRowRequestDto sameDayBackwards = row(5, "04506", "Merced");
        sameDayBackwards.setReturnDate(sameDayBackwards.getDepartureDate());
        sameDayBackwards.setReturnTime(LocalTime.of(4, 0));
        CleaningDraftRowRequestDto tooLongNumber = row(6, "1".repeat(51), "Merced");

        CleaningDraftImportResponseDto response = service.create(request(List.of(
                missingPassengers, returnBeforeDeparture, sameDayBackwards, tooLongNumber)));

        assertThat(response.getDraftId()).isNull();
        assertThat(response.getErrors())
                .extracting(CleaningDraftRowIssueDto::getRow, CleaningDraftRowIssueDto::getReason)
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
        when(draftRepository.existsByYearAndMonthAndStatus(2026, 5, CleaningDraftStatus.DRAFT)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(List.of(row(8, "03625", "Tayutic")))))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("CLEANING_DRAFT_MONTH_EXISTS");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getMessage()).contains("mayo de 2026");
                });
        verify(draftRepository, never()).save(any());
        verify(rowRepository, never()).saveAll(anyList());
    }

    @Test
    void rejectsImportWhenTheMonthWasAlreadyRegistered() {
        when(draftRepository.existsByYearAndMonthAndStatus(2026, 5, CleaningDraftStatus.REGISTERED)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(List.of(row(8, "03625", "Tayutic")))))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("CLEANING_MONTH_ALREADY_REGISTERED");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getMessage()).contains("mayo de 2026");
                });
        verify(draftRepository, never()).save(any());
        verify(rowRepository, never()).saveAll(anyList());
    }

    @Test
    void reportsTheRegisteredMonthWhenItAlsoHasAPendingDraft() {
        when(draftRepository.existsByYearAndMonthAndStatus(2026, 5, CleaningDraftStatus.REGISTERED)).thenReturn(true);
        when(draftRepository.existsByYearAndMonthAndStatus(2026, 5, CleaningDraftStatus.DRAFT)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(List.of(row(8, "03625", "Tayutic")))))
                .isInstanceOfSatisfying(TransportException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("CLEANING_MONTH_ALREADY_REGISTERED"));
    }

    @Test
    void rejectsImportsAboveTheRowLimit() {
        List<CleaningDraftRowRequestDto> rows = new ArrayList<>(Collections.nCopies(5001, row(1, "1", "Merced")));

        assertThatThrownBy(() -> service.create(request(rows)))
                .isInstanceOf(TransportException.class)
                .hasMessage("La importación supera el máximo de 5000 filas");
    }

    @Test
    void createsRowAtTheEndAndRecalculatesDuplicatesAndDraftSummary() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.DRAFT);
        CleaningDraftRow first = entity(draft, 8, "03625", "Tayutic");
        CleaningDraftRow single = entity(draft, 20, "04504", "Merced");
        stubDraft(draft, first, single);

        CleaningDraftRowRequestDto request = row(1, "03625", "Heredia");
        request.setRowNumber(null);
        request.setReturnDate(LocalDate.of(2026, 5, 9));
        var response = service.createRow(draft.getId(), request);

        ArgumentCaptor<CleaningDraftRow> rowCaptor = ArgumentCaptor.forClass(CleaningDraftRow.class);
        verify(rowRepository).save(rowCaptor.capture());
        CleaningDraftRow created = rowCaptor.getValue();
        assertThat(created.getDraft()).isSameAs(draft);
        assertThat(created.getRowNumber()).isEqualTo(21);
        assertThat(response.getRowNumber()).isEqualTo(21);
        assertThat(response.getDestination()).isEqualTo("Heredia");
        assertThat(List.of(first, single, created))
                .extracting(CleaningDraftRow::getRowNumber, CleaningDraftRow::getDuplicateRank,
                        CleaningDraftRow::getDuplicateGroupSize, CleaningDraftRow::isConflicting)
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
    void createsFirstRowOfAnEmptyDraftWithRowNumberOne() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.DRAFT);
        stubDraft(draft);

        var response = service.createRow(draft.getId(), row(99, "04504", "Merced"));

        assertThat(response.getRowNumber()).isEqualTo(1);
        assertThat(draft.getTotalRows()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidRowCreationWithoutSavingAnything() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.DRAFT);
        stubDraft(draft, entity(draft, 8, "03625", "Tayutic"));
        CleaningDraftRowRequestDto invalid = row(1, "04504", "Merced");
        invalid.setReturnDate(LocalDate.of(2026, 5, 1));

        assertThatThrownBy(() -> service.createRow(draft.getId(), invalid))
                .isInstanceOf(TransportException.class)
                .hasMessage("La fecha de regreso es anterior a la fecha de salida");
        verify(rowRepository, never()).save(any());
    }

    @Test
    void rejectsRowCreationOnRegisteredDraft() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.REGISTERED);
        stubDraft(draft);

        assertThatThrownBy(() -> service.createRow(draft.getId(), row(1, "04504", "Merced")))
                .isInstanceOf(TransportException.class)
                .hasMessage("No se puede editar un borrador que ya fue registrado");
        verify(rowRepository, never()).save(any());
    }

    @Test
    void updatesRowAndRecalculatesDuplicatesAndDraftSummary() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.DRAFT);
        CleaningDraftRow first = entity(draft, 8, "03625", "Tayutic");
        CleaningDraftRow conflicting = entity(draft, 15, "03625", "Heredia");
        CleaningDraftRow single = entity(draft, 20, "04504", "Merced");
        stubDraft(draft, first, conflicting, single);

        CleaningDraftRowRequestDto request = row(15, "03625", "Tayutic");
        request.setReturnDate(LocalDate.of(2026, 5, 9));
        var response = service.updateRow(draft.getId(), conflicting.getId(), request);

        assertThat(response.getDestination()).isEqualTo("Tayutic");
        assertThat(List.of(first, conflicting, single))
                .extracting(CleaningDraftRow::getRowNumber, CleaningDraftRow::getDuplicateRank,
                        CleaningDraftRow::getDuplicateGroupSize, CleaningDraftRow::isConflicting)
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
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.DRAFT);
        CleaningDraftRow first = entity(draft, 8, "03625", "Tayutic");
        CleaningDraftRow conflicting = entity(draft, 15, "03625", "Heredia");
        stubDraft(draft, first, conflicting);

        service.updateRow(draft.getId(), conflicting.getId(), row(15, "03625", "Tayutic"));

        assertThat(conflicting.isConflicting()).isFalse();
        assertThat(draft.getConflictingRows()).isZero();
        assertThat(draft.getDuplicateRows()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidRowUpdateWithoutChangingTheRow() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.DRAFT);
        CleaningDraftRow existing = entity(draft, 8, "03625", "Tayutic");
        stubDraft(draft, existing);
        CleaningDraftRowRequestDto invalid = row(8, "03625", "Heredia");
        invalid.setReturnDate(LocalDate.of(2026, 5, 1));

        assertThatThrownBy(() -> service.updateRow(draft.getId(), existing.getId(), invalid))
                .isInstanceOf(TransportException.class)
                .hasMessage("La fecha de regreso es anterior a la fecha de salida");
        assertThat(existing.getDestination()).isEqualTo("Tayutic");
    }

    @Test
    void rejectsIncreasingTheDurationWhenTheAssignedDriverAlreadyHasAnotherTourTheNextDay() {
        CleaningDraft draft = draftWithPeriod();
        CleaningDraftRow oneDayTour = oneDayTour(draft);
        CleaningDraftRow nextDayTour = entity(draft, 9, "200", "Heredia");
        nextDayTour.setDepartureDate(LocalDate.of(2026, 5, 5));
        nextDayTour.setDepartureTime(LocalTime.of(8, 0));
        nextDayTour.setReturnDate(LocalDate.of(2026, 5, 5));
        nextDayTour.setReturnTime(LocalTime.of(17, 0));
        nextDayTour.setDurationDays(1);
        stubDraft(draft, oneDayTour, nextDayTour);
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));

        assertThatThrownBy(() -> service.updateRow(draft.getId(), oneDayTour.getId(), twoDayWeekdayRequest()))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("CLEANING_DRAFT_ROW_DRIVER_UNAVAILABLE");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(exception.getMessage()).contains("Daniel Zuniga", "200");
                });
        assertThat(oneDayTour.getDurationDays()).isEqualTo(1);
        assertThat(oneDayTour.getReturnDate()).isEqualTo(LocalDate.of(2026, 5, 4));
        verify(driverMonthService, never()).syncWithDraft(any(), anyList());
    }

    @Test
    void allowsIncreasingTheDurationWhenTheAssignedDriverIsFree() {
        CleaningDraft draft = draftWithPeriod();
        CleaningDraftRow oneDayTour = oneDayTour(draft);
        CleaningDraftRow laterTour = entity(draft, 9, "200", "Heredia");
        laterTour.setDepartureDate(LocalDate.of(2026, 5, 12));
        laterTour.setReturnDate(LocalDate.of(2026, 5, 13));
        stubDraft(draft, oneDayTour, laterTour);
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));

        service.updateRow(draft.getId(), oneDayTour.getId(), twoDayWeekdayRequest());

        assertThat(oneDayTour.getDurationDays()).isEqualTo(2);
        assertThat(oneDayTour.getReturnDate()).isEqualTo(LocalDate.of(2026, 5, 5));
        verify(driverMonthService).syncWithDraft(draft, List.of(oneDayTour, laterTour));
    }

    @Test
    void doesNotCheckTheDriverWhenTheDurationDoesNotIncrease() {
        CleaningDraft draft = draftWithPeriod();
        CleaningDraftRow twoDayTour = entity(draft, 8, "100", "Cartago");
        CleaningDraftRow overlappingTour = entity(draft, 9, "200", "Heredia");
        stubDraft(draft, twoDayTour, overlappingTour);
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));

        service.updateRow(draft.getId(), twoDayTour.getId(), row(8, "100", "Limón"));

        assertThat(twoDayTour.getDestination()).isEqualTo("Limón");
    }

    @Test
    void doesNotCheckTheDriverWhenTheTourHasNoCatalogDriver() {
        CleaningDraft draft = draftWithPeriod();
        CleaningDraftRow oneDayTour = oneDayTour(draft);
        oneDayTour.setDriver(null);
        CleaningDraftRow nextDayTour = entity(draft, 9, "200", "Heredia");
        nextDayTour.setDepartureDate(LocalDate.of(2026, 5, 5));
        stubDraft(draft, oneDayTour, nextDayTour);
        when(driverRepository.findAll()).thenReturn(List.of(catalogDriver()));
        CleaningDraftRowRequestDto request = twoDayWeekdayRequest();
        request.setDriver(null);

        service.updateRow(draft.getId(), oneDayTour.getId(), request);

        assertThat(oneDayTour.getDurationDays()).isEqualTo(2);
    }

    @Test
    void rejectsRowUpdateOnRegisteredDraft() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.REGISTERED);
        CleaningDraftRow existing = entity(draft, 8, "03625", "Tayutic");
        stubDraft(draft, existing);

        assertThatThrownBy(() -> service.updateRow(draft.getId(), existing.getId(), row(8, "03625", "Heredia")))
                .isInstanceOf(TransportException.class)
                .hasMessage("No se puede editar un borrador que ya fue registrado");
        assertThat(existing.getDestination()).isEqualTo("Tayutic");
    }

    @Test
    void rejectsRowUpdateWhenRowDoesNotBelongToDraft() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.DRAFT);
        UUID unknownRowId = UUID.randomUUID();
        when(draftRepository.findByIdForUpdate(draft.getId())).thenReturn(Optional.of(draft));
        when(rowRepository.findByIdAndDraftId(unknownRowId, draft.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRow(draft.getId(), unknownRowId, row(8, "03625", "Heredia")))
                .isInstanceOf(TransportException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deletesDuplicateRowsKeepingFirstOccurrenceAndRecalculatesDraftSummary() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.DRAFT);
        CleaningDraftRow first = entity(draft, 8, "03625", "Tayutic");
        CleaningDraftRow repeated = entity(draft, 12, "03625", "Tayutic");
        CleaningDraftRow repeatedAgain = entity(draft, 16, "03625", "Tayutic");
        CleaningDraftRow single = entity(draft, 20, "04504", "Merced");
        first.setDuplicateGroupSize(3);
        repeated.setDuplicateRank(2);
        repeated.setDuplicateGroupSize(3);
        repeatedAgain.setDuplicateRank(3);
        repeatedAgain.setDuplicateGroupSize(3);
        stubDraft(draft, first, repeated, repeatedAgain, single);

        int deleted = service.deleteDuplicateRows(draft.getId());

        ArgumentCaptor<List<CleaningDraftRow>> deletedCaptor = ArgumentCaptor.forClass(List.class);
        verify(rowRepository).deleteAll(deletedCaptor.capture());
        assertThat(deletedCaptor.getValue()).containsExactly(repeated, repeatedAgain);
        assertThat(deleted).isEqualTo(2);
        assertThat(List.of(first, single))
                .extracting(CleaningDraftRow::getRowNumber, CleaningDraftRow::getDuplicateRank,
                        CleaningDraftRow::getDuplicateGroupSize, CleaningDraftRow::isConflicting)
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
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.DRAFT);
        stubDraft(draft, entity(draft, 8, "03625", "Tayutic"), entity(draft, 20, "04504", "Merced"));

        int deleted = service.deleteDuplicateRows(draft.getId());

        assertThat(deleted).isZero();
        assertThat(draft.getTotalRows()).isEqualTo(2);
    }

    @Test
    void rejectsDeletingDuplicatesWhenThereAreConflictingRows() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.DRAFT);
        CleaningDraftRow first = entity(draft, 8, "03625", "Tayutic");
        CleaningDraftRow conflicting = entity(draft, 15, "03625", "Heredia");
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
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.REGISTERED);
        stubDraft(draft);

        assertThatThrownBy(() -> service.deleteDuplicateRows(draft.getId()))
                .isInstanceOf(TransportException.class)
                .hasMessage("No se puede editar un borrador que ya fue registrado");
        verify(rowRepository, never()).deleteAll(anyList());
    }

    @Test
    void autoAssignClearsUnknownDriversKeepsCatalogDriversAndFillsRepeatedRows() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.DRAFT);
        draft.setYear(2026);
        draft.setMonth(5);
        draft.setBusinessDays(28);
        CleaningDraftRow kept = entity(draft, 1, "100", "Cartago");
        kept.setDriver("daniel   ZUNIGA");
        CleaningDraftRow unknown = entity(draft, 2, "200", "Heredia");
        unknown.setDriver("Chofer Fantasma");
        unknown.setDepartureDate(LocalDate.of(2026, 5, 4));
        unknown.setReturnDate(LocalDate.of(2026, 5, 5));
        CleaningDraftRow unknownRepeated = entity(draft, 3, "200", "Heredia");
        unknownRepeated.setDriver("Chofer Fantasma");
        unknownRepeated.setDuplicateRank(2);
        unknownRepeated.setDepartureDate(LocalDate.of(2026, 5, 4));
        unknownRepeated.setReturnDate(LocalDate.of(2026, 5, 5));
        stubDraft(draft, kept, unknown, unknownRepeated);
        when(driverRepository.findAll()).thenReturn(List.of(
                Driver.builder().id(UUID.randomUUID()).name("Daniel Zuniga").status("ACTIVE").build(),
                Driver.builder().id(UUID.randomUUID()).name("Laura Mora").status("ACTIVE").build()
        ));

        CleaningDriverAutoAssignResponseDto result = service.autoAssignDrivers(draft.getId());

        assertThat(kept.getDriver()).isEqualTo("daniel   ZUNIGA");
        assertThat(unknown.getDriver()).isEqualTo("Laura Mora");
        assertThat(unknownRepeated.getDriver()).isEqualTo("Laura Mora");
        assertThat(unknownRepeated.isConflicting()).isFalse();
        assertThat(result.getClearedTours()).isEqualTo(1);
        assertThat(result.getAssignedTours()).isEqualTo(1);
        assertThat(result.getUnassignedTours()).isZero();
        verify(driverMonthService).syncWithDraft(draft, List.of(kept, unknown, unknownRepeated));
    }

    @Test
    void autoAssignLeavesToursEmptyWhenNoDriverQualifies() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.DRAFT);
        draft.setYear(2026);
        draft.setMonth(5);
        draft.setBusinessDays(28);
        CleaningDraftRow kept = entity(draft, 1, "100", "Cartago");
        CleaningDraftRow pending = entity(draft, 2, "200", "Heredia");
        pending.setDriver(null);
        stubDraft(draft, kept, pending);
        when(driverRepository.findAll()).thenReturn(List.of(
                Driver.builder().id(UUID.randomUUID()).name("Daniel Zuniga").status("ACTIVE").build()
        ));

        CleaningDriverAutoAssignResponseDto result = service.autoAssignDrivers(draft.getId());

        assertThat(pending.getDriver()).isNull();
        assertThat(result.getAssignedTours()).isZero();
        assertThat(result.getUnassignedTours()).isEqualTo(1);
    }

    @Test
    void autoAssignUpdatesAnAlreadyAssignedRowWhenTheSecondPassSwapsItsGira() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.DRAFT);
        draft.setYear(2026);
        draft.setMonth(5);
        draft.setBusinessDays(28);
        Driver ana = Driver.builder().id(UUID.randomUUID()).name("Ana").status("ACTIVE").build();
        Driver beto = Driver.builder().id(UUID.randomUUID()).name("Beto").status("ACTIVE").build();

        CleaningDraftRow betoWeekday = fullDayRow(draft, 1, "1", "2026-05-04", "Beto");
        CleaningDraftRow betoWeekday2 = fullDayRow(draft, 2, "2", "2026-05-05", "Beto");
        CleaningDraftRow betoWeekday3 = fullDayRow(draft, 3, "3", "2026-05-06", "Beto");
        CleaningDraftRow betoWeekday4 = fullDayRow(draft, 4, "4", "2026-05-11", "Beto");
        CleaningDraftRow lowPriorityWeekend = shortRow(draft, 5, "9", "2026-05-02", "08:00", "10:00", null);
        lowPriorityWeekend.setPriority(5);
        stubDraft(draft, betoWeekday, betoWeekday2, betoWeekday3, betoWeekday4, lowPriorityWeekend);
        when(driverRepository.findAll()).thenReturn(List.of(ana, beto));

        service.autoAssignDrivers(draft.getId());

        assertThat(lowPriorityWeekend.getDriver()).isEqualTo("Beto");
        assertThat(betoWeekday.getDriver()).isEqualTo("Ana");
    }

    @Test
    void rejectsAutoAssignWhenDraftHasNoBusinessDays() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.DRAFT);
        stubDraft(draft);

        assertThatThrownBy(() -> service.autoAssignDrivers(draft.getId()))
                .isInstanceOf(TransportException.class)
                .hasMessage("El borrador no tiene mes ni días hábiles definidos, no se pueden autoasignar choferes");
        verify(driverMonthService, never()).syncWithDraft(any(), anyList());
    }

    @Test
    void rejectsAutoAssignOnRegisteredDraft() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.REGISTERED);
        stubDraft(draft);

        assertThatThrownBy(() -> service.autoAssignDrivers(draft.getId()))
                .isInstanceOf(TransportException.class)
                .hasMessage("No se puede editar un borrador que ya fue registrado");
    }

    @Test
    void autoAssignVehiclesClearsUnknownValuesKeepsCatalogPlatesAndFillsRepeatedRows() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.DRAFT);
        CleaningDraftRow kept = entity(draft, 1, "100", "Cartago");
        kept.setVehicle("301-295 ");
        CleaningDraftRow vehicleTypeText = entity(draft, 2, "200", "Heredia");
        vehicleTypeText.setVehicle("Todo Terreno");
        CleaningDraftRow unknownPlate = entity(draft, 3, "300", "Limón");
        unknownPlate.setVehicle("999-999");
        CleaningDraftRow unknownPlateRepeated = entity(draft, 4, "300", "Limón");
        unknownPlateRepeated.setVehicle("999-999");
        unknownPlateRepeated.setDuplicateRank(2);
        stubDraft(draft, kept, vehicleTypeText, unknownPlate, unknownPlateRepeated);
        when(vehicleRepository.findAll()).thenReturn(List.of(
                Vehicle.builder().id(UUID.randomUUID()).plate("301-295").type(VehicleType.SEDAN).capacity(5).build(),
                Vehicle.builder().id(UUID.randomUUID()).plate("600-600").type(VehicleType.SEDAN).capacity(5).build(),
                Vehicle.builder().id(UUID.randomUUID()).plate("500-500").type(VehicleType.SEDAN).capacity(5).build()
        ));

        CleaningVehicleAutoAssignResponseDto result = service.autoAssignVehicles(draft.getId());

        assertThat(kept.getVehicle()).isEqualTo("301-295 ");
        assertThat(vehicleTypeText.getVehicle()).isEqualTo("500-500");
        assertThat(unknownPlate.getVehicle()).isEqualTo("600-600");
        assertThat(unknownPlateRepeated.getVehicle()).isEqualTo("600-600");
        assertThat(unknownPlateRepeated.isConflicting()).isFalse();
        assertThat(result.getClearedTours()).isEqualTo(2);
        assertThat(result.getAssignedTours()).isEqualTo(2);
        assertThat(result.getUnassignedTours()).isZero();
    }

    @Test
    void rejectsVehicleAutoAssignOnRegisteredDraft() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.REGISTERED);
        stubDraft(draft);

        assertThatThrownBy(() -> service.autoAssignVehicles(draft.getId()))
                .isInstanceOf(TransportException.class)
                .hasMessage("No se puede editar un borrador que ya fue registrado");
    }

    @Test
    void rejectsManualDayEditWhenOrdinaryHoursExceedNineHours() {
        CleaningDraft draft = draftWithBusinessDays(21);
        CleaningDraftRow row = entity(draft, 1, "100", "Cartago");
        stubDraft(draft, row);
        CleaningDraftRowDay day = dayEntity(row, LocalDate.of(2026, 5, 4), LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 0.0, 0.0);
        when(dayRepository.findByIdAndRowId(day.getId(), row.getId())).thenReturn(Optional.of(day));
        when(dayRepository.findByRowIdIn(anyList())).thenReturn(List.of(day));

        CleaningDraftRowDayRequestDto request = dayRequest(LocalTime.of(7, 0), LocalTime.of(20, 0), 9.5, 0.0, 0.0, false);

        assertThatThrownBy(() -> service.updateRowDay(draft.getId(), row.getId(), day.getId(), request))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("CLEANING_DRAFT_ROW_DAY_ORDINARY_LIMIT");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                });
        verify(dayRepository, never()).save(any());
    }

    @Test
    void rejectsManualDayEditWhenSurplusCapForTheMonthIsExceeded() {
        CleaningDraft draft = draftWithBusinessDays(21);
        CleaningDraftRow otherRow = entity(draft, 1, "100", "Cartago");
        CleaningDraftRow row = entity(draft, 2, "200", "Heredia");
        stubDraft(draft, otherRow, row);
        CleaningDraftRowDay otherDay = dayEntity(otherRow, LocalDate.of(2026, 5, 4), LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 27.0, 0.0);
        CleaningDraftRowDay day = dayEntity(row, LocalDate.of(2026, 5, 11), LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 0.0, 0.0);
        when(dayRepository.findByIdAndRowId(day.getId(), row.getId())).thenReturn(Optional.of(day));
        when(dayRepository.findByRowIdIn(anyList())).thenReturn(List.of(otherDay, day));

        CleaningDraftRowDayRequestDto request = dayRequest(LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 2.0, 0.0, false);

        assertThatThrownBy(() -> service.updateRowDay(draft.getId(), row.getId(), day.getId(), request))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("CLEANING_DRAFT_ROW_DAY_SURPLUS_CAP_EXCEEDED");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                });
        verify(dayRepository, never()).save(any());
    }

    @Test
    void rejectsManualDayEditWhenOvertimeCapForTheMonthIsExceeded() {
        CleaningDraft draft = draftWithBusinessDays(21);
        CleaningDraftRow otherRow = entity(draft, 1, "100", "Cartago");
        CleaningDraftRow row = entity(draft, 2, "200", "Heredia");
        stubDraft(draft, otherRow, row);
        CleaningDraftRowDay otherDay = dayEntity(otherRow, LocalDate.of(2026, 5, 4), LocalTime.of(7, 30), LocalTime.of(20, 30), 9.0, 4.0, 59.0);
        CleaningDraftRowDay day = dayEntity(row, LocalDate.of(2026, 5, 11), LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 0.0, 0.0);
        when(dayRepository.findByIdAndRowId(day.getId(), row.getId())).thenReturn(Optional.of(day));
        when(dayRepository.findByRowIdIn(anyList())).thenReturn(List.of(otherDay, day));

        CleaningDraftRowDayRequestDto request = dayRequest(LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 0.0, 2.0, false);

        assertThatThrownBy(() -> service.updateRowDay(draft.getId(), row.getId(), day.getId(), request))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("CLEANING_DRAFT_ROW_DAY_OVERTIME_CAP_EXCEEDED");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                });
        verify(dayRepository, never()).save(any());
    }

    @Test
    void requiresConfirmationToAssignOvertimeBelowTheSurplusThreshold() {
        CleaningDraft draft = draftWithBusinessDays(21);
        CleaningDraftRow row = entity(draft, 1, "100", "Cartago");
        stubDraft(draft, row);
        CleaningDraftRowDay day = dayEntity(row, LocalDate.of(2026, 5, 4), LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 0.0, 0.0);
        when(dayRepository.findByIdAndRowId(day.getId(), row.getId())).thenReturn(Optional.of(day));
        when(dayRepository.findByRowIdIn(anyList())).thenReturn(List.of(day));

        CleaningDraftRowDayRequestDto request = dayRequest(LocalTime.of(7, 30), LocalTime.of(17, 30), 9.0, 0.0, 1.0, false);

        assertThatThrownBy(() -> service.updateRowDay(draft.getId(), row.getId(), day.getId(), request))
                .isInstanceOfSatisfying(TransportException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("CLEANING_DRAFT_ROW_DAY_OVERTIME_NEEDS_CONFIRMATION");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
        verify(dayRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void savesOvertimeBelowTheSurplusThresholdWhenConfirmed() {
        CleaningDraft draft = draftWithBusinessDays(21);
        CleaningDraftRow row = entity(draft, 1, "100", "Cartago");
        stubDraft(draft, row);
        CleaningDraftRowDay day = dayEntity(row, LocalDate.of(2026, 5, 4), LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 0.0, 0.0);
        when(dayRepository.findByIdAndRowId(day.getId(), row.getId())).thenReturn(Optional.of(day));
        when(dayRepository.findByRowIdIn(anyList())).thenReturn(List.of(day));
        when(dayRepository.save(any(CleaningDraftRowDay.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CleaningDraftRowDayRequestDto request = dayRequest(LocalTime.of(7, 30), LocalTime.of(17, 30), 9.0, 0.0, 1.0, true);

        var response = service.updateRowDay(draft.getId(), row.getId(), day.getId(), request);

        assertThat(response.getOvertimeHours()).isEqualTo(1.0);
        assertThat(response.isManuallyEdited()).isTrue();
        verify(dayRepository).save(day);
        verify(driverMonthService).recomputeDriverMonth(draft, List.of(row), row.getDriver());
    }

    @Test
    void rejectsManualDayEditWhenEndTimeIsNotAfterStartTime() {
        CleaningDraft draft = draftWithBusinessDays(21);
        CleaningDraftRow row = entity(draft, 1, "100", "Cartago");
        stubDraft(draft, row);
        CleaningDraftRowDay day = dayEntity(row, LocalDate.of(2026, 5, 4), LocalTime.of(7, 30), LocalTime.of(16, 30), 9.0, 0.0, 0.0);
        when(dayRepository.findByIdAndRowId(day.getId(), row.getId())).thenReturn(Optional.of(day));

        CleaningDraftRowDayRequestDto request = dayRequest(LocalTime.of(16, 0), LocalTime.of(8, 0), 9.0, 0.0, 0.0, false);

        assertThatThrownBy(() -> service.updateRowDay(draft.getId(), row.getId(), day.getId(), request))
                .isInstanceOfSatisfying(TransportException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo("CLEANING_DRAFT_ROW_DAY_INVALID"));
        verify(dayRepository, never()).save(any());
    }

    @Test
    void getScheduleDelegatesToTheOvertimeReportServiceWithTheResolvedDraft() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.DRAFT);
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

    private CleaningDraft draftWithBusinessDays(int businessDays) {
        return CleaningDraft.builder()
                .id(UUID.randomUUID())
                .status(CleaningDraftStatus.DRAFT)
                .businessDays(businessDays)
                .build();
    }

    private CleaningDraftRowDay dayEntity(CleaningDraftRow row, LocalDate date, LocalTime start, LocalTime end,
                                           double ordinary, double surplus, double overtime) {
        return CleaningDraftRowDay.builder()
                .id(UUID.randomUUID())
                .row(row)
                .date(date)
                .startTime(start)
                .endTime(end)
                .ordinaryHours(ordinary)
                .surplusHours(surplus)
                .overtimeHours(overtime)
                .manuallyEdited(false)
                .build();
    }

    private CleaningDraftRowDayRequestDto dayRequest(LocalTime start, LocalTime end, double ordinary, double surplus,
                                                       double overtime, boolean confirmOvertimeBelowSurplusThreshold) {
        return CleaningDraftRowDayRequestDto.builder()
                .startTime(start)
                .endTime(end)
                .ordinaryHours(ordinary)
                .surplusHours(surplus)
                .overtimeHours(overtime)
                .confirmOvertimeBelowSurplusThreshold(confirmOvertimeBelowSurplusThreshold)
                .build();
    }

    private CleaningDraft draftWithPeriod() {
        CleaningDraft draft = draftWithStatus(CleaningDraftStatus.DRAFT);
        draft.setYear(2026);
        draft.setMonth(5);
        draft.setBusinessDays(21);
        return draft;
    }

    private CleaningDraftRow oneDayTour(CleaningDraft draft) {
        CleaningDraftRow tour = entity(draft, 8, "100", "Cartago");
        tour.setDurationDays(1);
        tour.setDepartureDate(LocalDate.of(2026, 5, 4));
        tour.setReturnDate(LocalDate.of(2026, 5, 4));
        return tour;
    }

    private CleaningDraftRowRequestDto twoDayWeekdayRequest() {
        CleaningDraftRowRequestDto request = row(8, "100", "Cartago");
        request.setDepartureDate(LocalDate.of(2026, 5, 4));
        request.setReturnDate(LocalDate.of(2026, 5, 5));
        return request;
    }

    private Driver catalogDriver() {
        return Driver.builder().id(UUID.randomUUID()).name("Daniel Zuniga").status("ACTIVE").build();
    }

    private CleaningDraft draftWithStatus(CleaningDraftStatus status) {
        return CleaningDraft.builder()
                .id(UUID.randomUUID())
                .status(status)
                .build();
    }

    private CleaningDraftRow shortRow(CleaningDraft draft, int rowNumber, String number, String date,
                                       String startTime, String endTime, String driverName) {
        CleaningDraftRow row = entity(draft, rowNumber, number, "Destino");
        row.setDriver(driverName);
        row.setDurationDays(1);
        row.setDepartureDate(LocalDate.parse(date));
        row.setDepartureTime(LocalTime.parse(startTime));
        row.setReturnDate(LocalDate.parse(date));
        row.setReturnTime(LocalTime.parse(endTime));
        return row;
    }

    private CleaningDraftRow fullDayRow(CleaningDraft draft, int rowNumber, String number, String date, String driverName) {
        return shortRow(draft, rowNumber, number, date, "00:00", "23:30", driverName);
    }

    private CleaningDraftRow entity(CleaningDraft draft, int rowNumber, String number, String destination) {
        return CleaningDraftRow.builder()
                .id(UUID.randomUUID())
                .draft(draft)
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
                .duplicateRank(1)
                .duplicateGroupSize(1)
                .build();
    }

    private void stubDraft(CleaningDraft draft, CleaningDraftRow... rows) {
        when(draftRepository.findByIdForUpdate(draft.getId())).thenReturn(Optional.of(draft));
        for (CleaningDraftRow row : rows) {
            when(rowRepository.findByIdAndDraftId(row.getId(), draft.getId())).thenReturn(Optional.of(row));
        }
        when(rowRepository.findAllByDraftIdOrderByRowNumberAsc(draft.getId())).thenReturn(List.of(rows));
    }

    private CleaningDraftCreateRequestDto request(List<CleaningDraftRowRequestDto> rows) {
        return CleaningDraftCreateRequestDto.builder()
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

    private CleaningDraftRowRequestDto row(int rowNumber, String number, String destination) {
        return CleaningDraftRowRequestDto.builder()
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
