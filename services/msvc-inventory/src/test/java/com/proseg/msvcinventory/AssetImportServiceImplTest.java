package com.proseg.msvcinventory;

import com.proseg.common.api.exception.BaseException;
import com.proseg.msvcinventory.dto.request.AssetImportRequestDto;
import com.proseg.msvcinventory.dto.request.AssetImportRowDto;
import com.proseg.msvcinventory.dto.request.ImportConfirmRequestDto;
import com.proseg.msvcinventory.dto.response.PendingCreationDto;
import com.proseg.msvcinventory.dto.response.AssetImportResponseDto;
import com.proseg.msvcinventory.dto.response.ImportPreviewResponseDto;
import com.proseg.msvcinventory.service.impl.AssetImportServiceImpl;
import com.proseg.msvcinventory.service.impl.AssetImportRowProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetImportServiceImplTest {

    @Mock
    private AssetImportRowProcessor rowProcessor;

    @InjectMocks
    private AssetImportServiceImpl service;

    @Test
    @DisplayName("preview: muchas filas lanza excepción")
    void preview_muchasFilas_lanzaExcepcion() {
        AssetImportRequestDto req = new AssetImportRequestDto();
        var rows = new java.util.ArrayList<AssetImportRowDto>();
        for (int i = 0; i < 2001; i++) rows.add(new AssetImportRowDto());
        req.setRows(rows);

        assertThatThrownBy(() -> service.preview(req)).hasMessageContaining("La importación supera el máximo");
    }

    @Test
    @DisplayName("preview: cuando hay errores de fila los reporta y no planifica creaciones")
    void preview_erroresEnFilas_reportaErrores() {
        AssetImportRequestDto req = new AssetImportRequestDto();
        AssetImportRowDto r = AssetImportRowDto.builder().rowNumber(1).build();
        req.setRows(List.of(r));

        doThrow(new BaseException(org.springframework.http.HttpStatus.BAD_REQUEST, "ERR", "error"))
                .when(rowProcessor).validateRow(any(), any(), any(), any(), any(), any());

        ImportPreviewResponseDto res = service.preview(req);
        assertThat(res.getErrors()).hasSize(1);
        assertThat(res.getPendingCreations()).isEmpty();
    }

    @Test
    @DisplayName("confirm: si no se aprueban todas las creaciones cancela importación")
    void confirm_noAprobadas_devuelveCancelado() {
        ImportConfirmRequestDto req = new ImportConfirmRequestDto();
        AssetImportRowDto r = AssetImportRowDto.builder().rowNumber(1).build();
        req.setRows(List.of(r));
        when(rowProcessor.planCreations(any())).thenReturn(List.of(PendingCreationDto.builder().key("X").build()));

        AssetImportResponseDto res = service.confirm(req);
        assertThat(res.isCancelled()).isTrue();
        assertThat(res.getCreated()).isEqualTo(0);
    }

    @Test
    @DisplayName("confirm: si todo aprobado persiste filas")
    void confirm_todoAprobado_persisteFilas() {
        ImportConfirmRequestDto req = new ImportConfirmRequestDto();
        AssetImportRowDto r = AssetImportRowDto.builder().rowNumber(1).build();
        req.setRows(List.of(r));
        req.setApprovedKeys(List.of("K"));

        when(rowProcessor.planCreations(any())).thenReturn(List.of(PendingCreationDto.builder().key("K").build()));

        AssetImportResponseDto res = service.confirm(req);
        // When there are no errors and all approved, created == received
        assertThat(res.isCancelled()).isFalse();
        assertThat(res.getCreated()).isEqualTo(req.getRows().size());
    }
}
