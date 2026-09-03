package com.proseg.msvc_maintenance.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.msvc_maintenance.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${routes.userCompanies:/api/v1/maintenance/user-companies}")
@RequiredArgsConstructor
public class UserCompanyController {

    private final CompanyService companyService;

    @GetMapping("/{keycloakUserId}/has-company")
    public ResponseEntity<ApiResponse<Boolean>> hasCompany(@PathVariable String keycloakUserId) {
        return ApiResponseBuilder.ok(
                companyService.hasCompany(keycloakUserId),
                "Verificación de empresa asociada"
        );
    }
}
