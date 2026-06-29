package com.sssi.msvc_document_processor.target;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sssi.common.api.response.ApiResponse;
import com.sssi.msvc_document_processor.client.InventoryClient;
import com.sssi.msvc_document_processor.dto.request.ConfirmRequestDto;
import com.sssi.msvc_document_processor.dto.response.ImportPreviewDto;
import com.sssi.msvc_document_processor.dto.request.RowsRequestDto;
import com.sssi.msvc_document_processor.dto.response.SchemaDto;
import com.sssi.msvc_document_processor.dto.response.ImportConfirmDto;
import com.sssi.msvc_document_processor.exception.DocumentProcessorException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AssetImportTarget implements ImportTarget {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final InventoryClient inventoryClient;

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
            throw translateInventoryError(e);
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
            throw translateInventoryError(e);
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
            throw translateInventoryError(e);
        }
    }

    private DocumentProcessorException translateInventoryError(FeignException e) {
        Optional<ByteBuffer> body = e.responseBody();
        if (body.isEmpty()) {
            return DocumentProcessorException.inventoryUnavailable();
        }
        try {
            String json = StandardCharsets.UTF_8.decode(body.get()).toString();
            JsonNode node = OBJECT_MAPPER.readTree(json);

            String message = node.hasNonNull("message") ? node.get("message").asText() : null;
            if (message == null || message.isBlank()) {
                return DocumentProcessorException.inventoryUnavailable();
            }

            String errorCode = "INVENTORY_ERROR";
            JsonNode errors = node.get("errors");
            if (errors != null && errors.isArray() && !errors.isEmpty() && errors.get(0).isTextual()) {
                errorCode = errors.get(0).asText();
            }

            HttpStatus status = resolveStatus(node, e.status());

            return new DocumentProcessorException(status, errorCode, message);
        } catch (Exception parseError) {
            return DocumentProcessorException.inventoryUnavailable();
        }
    }

    private HttpStatus resolveStatus(JsonNode node, int fallbackStatus) {
        int statusValue = node.hasNonNull("status") ? node.get("status").asInt(fallbackStatus) : fallbackStatus;
        HttpStatus resolved = HttpStatus.resolve(statusValue);
        return resolved != null ? resolved : HttpStatus.BAD_GATEWAY;
    }
}
