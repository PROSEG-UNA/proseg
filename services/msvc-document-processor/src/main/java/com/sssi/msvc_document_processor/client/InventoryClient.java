package com.sssi.msvc_document_processor.client;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.msvc_document_processor.config.FeignConfig;
import com.sssi.msvc_document_processor.dto.request.ConfirmRequestDto;
import com.sssi.msvc_document_processor.dto.response.ImportPreviewDto;
import com.sssi.msvc_document_processor.dto.request.RowsRequestDto;
import com.sssi.msvc_document_processor.dto.response.SchemaDto;
import com.sssi.msvc_document_processor.dto.response.ImportConfirmDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "msvc-inventory",
        url = "${GATEWAY_BASE_URL:http://localhost:8081}/api",
        configuration = FeignConfig.class
)
public interface InventoryClient {

    @GetMapping("/v1/inventory/assets/schema")
    ApiResponse<SchemaDto> getAssetSchema();

    @PostMapping("/v1/inventory/assets/import/preview")
    ApiResponse<ImportPreviewDto> previewAssets(@RequestBody RowsRequestDto request);

    @PostMapping("/v1/inventory/assets/import/confirm")
    ApiResponse<ImportConfirmDto> confirmAssets(@RequestBody ConfirmRequestDto request);
}
