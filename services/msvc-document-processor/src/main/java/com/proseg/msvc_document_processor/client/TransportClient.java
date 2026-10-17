package com.proseg.msvc_document_processor.client;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.msvc_document_processor.config.FeignConfig;
import com.proseg.msvc_document_processor.dto.request.TourDraftRequestDto;
import com.proseg.msvc_document_processor.dto.response.OvertimeRequestReportDto;
import com.proseg.msvc_document_processor.dto.response.SchemaDto;
import com.proseg.msvc_document_processor.dto.response.TourDraftDto;
import com.proseg.msvc_document_processor.dto.response.TourDraftResultDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.UUID;

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

    @GetMapping("/v1/transport/cleaning/drafts/{id}")
    ApiResponse<TourDraftDto> getCleaningDraft(@PathVariable("id") UUID id);

    @GetMapping("/v1/transport/cleaning/drafts/{id}/rows")
    ApiResponse<PageResponse<Map<String, Object>>> getCleaningDraftRows(
            @PathVariable("id") UUID id,
            @RequestParam MultiValueMap<String, String> queryParams
    );

    @GetMapping("/v1/transport/drivers/months/{year}/{month}/overtime-request")
    ApiResponse<OvertimeRequestReportDto> getOvertimeRequestReport(
            @PathVariable("year") int year,
            @PathVariable("month") int month
    );
}
