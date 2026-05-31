package com.sssi.msvc_email.notificacion.client;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.msvc_email.config.FeignConfig;
import com.sssi.msvc_email.notificacion.dto.CompanyResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "msvc-maintenance",
        url = "${GATEWAY_BASE_URL:http://localhost:8081}/api",
        configuration = FeignConfig.class
)
public interface MaintenanceClient {

    @GetMapping("/v1/maintenance/companies/{id}")
    ApiResponse<CompanyResponseDto> getCompanyById(@PathVariable UUID id);
}