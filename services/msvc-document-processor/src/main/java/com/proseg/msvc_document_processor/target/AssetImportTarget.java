package com.proseg.msvc_document_processor.target;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_document_processor.client.InventoryClient;
import com.proseg.msvc_document_processor.client.InventoryFeignExceptionTranslator;
import com.proseg.msvc_document_processor.dto.request.ConfirmRequestDto;
import com.proseg.msvc_document_processor.dto.response.ImportPreviewDto;
import com.proseg.msvc_document_processor.dto.request.RowsRequestDto;
import com.proseg.msvc_document_processor.dto.response.SchemaDto;
import com.proseg.msvc_document_processor.dto.response.ImportConfirmDto;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssetImportTarget implements ImportTarget {

    private final InventoryClient inventoryClient;
    private final InventoryFeignExceptionTranslator inventoryErrorTranslator;

    @Override
    public String documentType() {
        return "assets";
    }

    @Override
    public SchemaDto fetchSchema() {
        try {
            ApiResponse<SchemaDto> response = inventoryClient.getAssetSchema();
            if (response == null || response.getData() == null) {
                throw DocumentProcessorException.inventoryUnavailable();
            }
            return response.getData();
        } catch (FeignException e) {
            throw inventoryErrorTranslator.translate(e);
        }
    }

    @Override
    public ImportPreviewDto preview(RowsRequestDto request) {
        try {
            ApiResponse<ImportPreviewDto> response = inventoryClient.previewAssets(request);
            if (response == null || response.getData() == null) {
                throw DocumentProcessorException.inventoryUnavailable();
            }
            return response.getData();
        } catch (FeignException e) {
            throw inventoryErrorTranslator.translate(e);
        }
    }

    @Override
    public ImportConfirmDto confirm(ConfirmRequestDto request) {
        try {
            ApiResponse<ImportConfirmDto> response = inventoryClient.confirmAssets(request);
            if (response == null || response.getData() == null) {
                throw DocumentProcessorException.inventoryUnavailable();
            }
            return response.getData();
        } catch (FeignException e) {
            throw inventoryErrorTranslator.translate(e);
        }
    }
}
