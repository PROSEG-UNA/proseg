package com.proseg.msvc_document_processor.target;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_document_processor.client.InventoryClient;
import com.proseg.msvc_document_processor.client.InventoryFeignExceptionTranslator;
import com.proseg.msvc_document_processor.dto.request.ConfirmRequestDto;
import com.proseg.msvc_document_processor.dto.request.RowsRequestDto;
import com.proseg.msvc_document_processor.dto.response.ImportConfirmDto;
import com.proseg.msvc_document_processor.dto.response.ImportPreviewDto;
import com.proseg.msvc_document_processor.dto.response.SchemaDto;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AssetImportTargetTest {

    private InventoryClient client;
    private InventoryFeignExceptionTranslator translator;
    private AssetImportTarget target;

    @BeforeEach
    void setUp() {
        client = mock(InventoryClient.class);
        translator = mock(InventoryFeignExceptionTranslator.class);
        target = new AssetImportTarget(client, translator);
    }

    @Test
    void delegates_schema_preview_and_confirmation() {
        SchemaDto schema = SchemaDto.builder().build();
        RowsRequestDto rows = RowsRequestDto.builder().build();
        ImportPreviewDto preview = ImportPreviewDto.builder().build();
        ConfirmRequestDto request = ConfirmRequestDto.builder().build();
        ImportConfirmDto confirm = ImportConfirmDto.builder().build();
        when(client.getAssetSchema()).thenReturn(new ApiResponse<>("ok", schema, 200));
        when(client.previewAssets(rows)).thenReturn(new ApiResponse<>("ok", preview, 200));
        when(client.confirmAssets(request)).thenReturn(new ApiResponse<>("ok", confirm, 200));

        assertThat(target.documentType()).isEqualTo("assets");
        assertThat(target.fetchSchema()).isSameAs(schema);
        assertThat(target.preview(rows)).isSameAs(preview);
        assertThat(target.confirm(request)).isSameAs(confirm);
        verify(client).getAssetSchema();
        verify(client).previewAssets(rows);
        verify(client).confirmAssets(request);
    }

    @Test
    void maps_missing_responses_to_inventory_unavailable() {
        when(client.getAssetSchema()).thenReturn(null);
        when(client.previewAssets(any())).thenReturn(new ApiResponse<>("ok", null, 200));
        when(client.confirmAssets(any())).thenReturn(null);

        assertThatThrownBy(target::fetchSchema).isInstanceOf(DocumentProcessorException.class);
        assertThatThrownBy(() -> target.preview(null)).isInstanceOf(DocumentProcessorException.class);
        assertThatThrownBy(() -> target.confirm(null)).isInstanceOf(DocumentProcessorException.class);
    }

    @Test
    void translates_feign_failures_for_each_operation() {
        FeignException failure = mock(FeignException.class);
        DocumentProcessorException translated = DocumentProcessorException.inventoryUnavailable();
        when(client.getAssetSchema()).thenThrow(failure);
        when(client.previewAssets(any())).thenThrow(failure);
        when(client.confirmAssets(any())).thenThrow(failure);
        when(translator.translate(failure)).thenReturn(translated);

        assertThatThrownBy(target::fetchSchema).isSameAs(translated);
        assertThatThrownBy(() -> target.preview(null)).isSameAs(translated);
        assertThatThrownBy(() -> target.confirm(null)).isSameAs(translated);
        verify(translator, times(3)).translate(failure);
    }
}
