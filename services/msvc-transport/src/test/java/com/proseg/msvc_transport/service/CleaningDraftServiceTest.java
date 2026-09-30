package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.cleaning.CleaningDraftCreateRequestDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftImportResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftRowIssueDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftRowRequestDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningImportColumnDto;
import com.proseg.msvc_transport.entity.CleaningDraft;
import com.proseg.msvc_transport.entity.CleaningDraftRow;
import com.proseg.msvc_transport.entity.CleaningDraftStatus;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.CleaningDraftRepository;
import com.proseg.msvc_transport.repository.CleaningDraftRowRepository;
import com.proseg.msvc_transport.security.CurrentUserResolver;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
    private CleaningDraftService service;

    @BeforeEach
    void setUp() {
        draftRepository = mock(CleaningDraftRepository.class);
        rowRepository = mock(CleaningDraftRowRepository.class);
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

    private CleaningDraft draftWithStatus(CleaningDraftStatus status) {
        return CleaningDraft.builder()
                .id(UUID.randomUUID())
                .status(status)
                .build();
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
