package com.proseg.msvc_document_processor.client;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.msvc_document_processor.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(
        name = "msvc-maintenance",
        url = "${GATEWAY_BASE_URL:http://localhost:8081}/api",
        configuration = FeignConfig.class
)
public interface MaintenanceClient {

    @GetMapping("/v1/maintenance/tickets")
    ApiResponse<PageResponse<Map<String, Object>>> getTickets(@RequestParam MultiValueMap<String, String> queryParams);

    @GetMapping("/v1/maintenance/requests")
    ApiResponse<PageResponse<Map<String, Object>>> getRequests(@RequestParam MultiValueMap<String, String> queryParams);
}
