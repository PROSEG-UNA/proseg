package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.ObservationKeywordRequestDto;
import com.proseg.msvc_transport.dto.response.ObservationKeywordResponseDto;
import com.proseg.msvc_transport.entity.ObservationKeyword;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.ObservationKeywordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObservationKeywordServiceTest {

    private ObservationKeywordRepository repository;
    private ObservationKeywordService service;

    @BeforeEach
    void setUp() {
        repository = mock(ObservationKeywordRepository.class);
        when(repository.save(any(ObservationKeyword.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new ObservationKeywordService(repository);
    }

    @Test
    void createsAKeywordWithItsNormalizedText() {
        ObservationKeywordResponseDto response = service.create(request("  Urgente "));

        ArgumentCaptor<ObservationKeyword> saved = ArgumentCaptor.forClass(ObservationKeyword.class);
        verify(repository).save(saved.capture());
        assertThat(response.getText()).isEqualTo("Urgente");
        assertThat(saved.getValue().getText()).isEqualTo("Urgente");
        assertThat(saved.getValue().getNormalizedText()).isEqualTo("urgente");
    }

    @Test
    void rejectsAnActiveKeywordWithTheSameNormalizedText() {
        when(repository.existsByNormalizedText("urgente")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("URGENTE")))
                .isInstanceOf(TransportException.class)
                .hasMessage("Ya existe una palabra clave con ese texto")
                .extracting(error -> ((TransportException) error).getHttpStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).restoreDeleted(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void restoresADeletedKeywordInsteadOfInsertingAnother() {
        ObservationKeyword deleted = new ObservationKeyword();
        deleted.setText("Urgente");
        deleted.setNormalizedText("urgente");
        deleted.markAsActive();
        when(repository.existsByNormalizedText("urgente")).thenReturn(false);
        when(repository.restoreDeleted("Urgente", "urgente")).thenReturn(1);
        when(repository.findByNormalizedText("urgente")).thenReturn(Optional.of(deleted));

        ObservationKeywordResponseDto response = service.create(request("Urgente"));

        assertThat(response.getText()).isEqualTo("Urgente");
        verify(repository).restoreDeleted("Urgente", "urgente");
        verify(repository, never()).save(any());
    }

    @Test
    void updatesTheTextWhenNoOtherActiveKeywordUsesIt() {
        UUID id = UUID.randomUUID();
        ObservationKeyword current = new ObservationKeyword();
        current.setId(id);
        current.setText("urgente");
        current.setNormalizedText("urgente");
        when(repository.findById(id)).thenReturn(Optional.of(current));
        when(repository.existsByNormalizedTextAndIdNot("prioridad", id)).thenReturn(false);

        ObservationKeywordResponseDto response = service.update(id, request(" Prioridad "));

        assertThat(response.getText()).isEqualTo("Prioridad");
        assertThat(current.getNormalizedText()).isEqualTo("prioridad");
        verify(repository).deleteDeletedByNormalizedText("prioridad", id);
    }

    @Test
    void rejectsAnUpdateThatCollidesWithAnotherActiveKeyword() {
        UUID id = UUID.randomUUID();
        ObservationKeyword current = new ObservationKeyword();
        current.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(current));
        when(repository.existsByNormalizedTextAndIdNot("urgente", id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, request("urgénte")))
                .isInstanceOf(TransportException.class)
                .hasMessage("Ya existe una palabra clave con ese texto")
                .extracting(error -> ((TransportException) error).getHttpStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).save(any());
    }

    @Test
    void deletesAnActiveKeyword() {
        UUID id = UUID.randomUUID();
        ObservationKeyword current = new ObservationKeyword();
        current.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(current));

        service.delete(id);

        verify(repository).delete(current);
    }

    private ObservationKeywordRequestDto request(String text) {
        return ObservationKeywordRequestDto.builder().text(text).build();
    }
}
