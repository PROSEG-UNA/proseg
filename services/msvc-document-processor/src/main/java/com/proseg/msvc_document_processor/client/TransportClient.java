package com.proseg.msvc_document_processor.client;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_document_processor.config.FeignConfig;
import com.proseg.msvc_document_processor.dto.request.TourDraftRequestDto;
import com.proseg.msvc_document_processor.dto.response.SchemaDto;
import com.proseg.msvc_document_processor.dto.response.TourDraftResultDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "msvc-transport",
        url = "${GATEWAY_BASE_URL:http://localhost:8081}/api",
        configuration = FeignConfig.class
)
public interface TransportClient {

    @GetMapping("/v1/transport/cleaning/imports/schema")
    ApiResponse<SchemaDto> getCleaningImportSchema();

    @PostMapping("/v1/transport/cleaning/drafts")
    ApiResponse<TourDraftResultDto> createCleaningDraft(@RequestBody TourDraftRequestDto request);
}
