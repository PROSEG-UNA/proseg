package com.proseg.msvc_document_processor.client;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.msvc_document_processor.config.FeignConfig;
import com.proseg.msvc_document_processor.dto.request.ConfirmRequestDto;
import com.proseg.msvc_document_processor.dto.response.ImportPreviewDto;
import com.proseg.msvc_document_processor.dto.request.RowsRequestDto;
import com.proseg.msvc_document_processor.dto.response.SchemaDto;
import com.proseg.msvc_document_processor.dto.response.ImportConfirmDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.util.MultiValueMap;

import java.util.Map;

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

    @GetMapping("/v1/inventory/assets")
    ApiResponse<PageResponse<Map<String, Object>>> getAssets(@RequestParam MultiValueMap<String, String> queryParams);
}
