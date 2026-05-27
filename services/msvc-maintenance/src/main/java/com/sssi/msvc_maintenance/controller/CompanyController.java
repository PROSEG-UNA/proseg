package com.sssi.msvc_maintenance.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.common.specification.FilterConstants;
import com.sssi.msvc_maintenance.dto.request.CompanyRequestDto;
import com.sssi.msvc_maintenance.dto.request.CompanyUserRequestDto;
import com.sssi.msvc_maintenance.dto.response.CompanyResponseDto;
import com.sssi.msvc_maintenance.dto.response.KeycloakUserResponse;
import com.sssi.msvc_maintenance.dto.response.UserCompanyResponseDto;
import com.sssi.msvc_maintenance.mapper.UserCompanyMapper;
import com.sssi.msvc_maintenance.service.CompanyService;
import com.sssi.msvc_maintenance.service.impl.CompanyUserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${routes.companies:/api/v1/maintenance/companies}")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyUserManagementService companyUserManagementService;
    private final UserCompanyMapper userCompanyMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyResponseDto>> create(@Valid @RequestBody CompanyRequestDto request) {
        return ApiResponseBuilder.created(
                companyService.create(request),
                "Empresa creada correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                companyService.findById(id),
                "Empresa obtenida correctamente"
        );
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<ApiResponse<List<KeycloakUserResponse>>> findUsers(
            @PathVariable UUID id
    ) {
        return ApiResponseBuilder.ok(
                companyUserManagementService.findUsersByCompanyId(id),
                "Usuarios asociados a la empresa"
        );
    }

    @PostMapping("/{id}/users")
    public ResponseEntity<ApiResponse<UserCompanyResponseDto>> assignUser(
            @PathVariable UUID id,
            @Valid @RequestBody CompanyUserRequestDto request) {
        return ApiResponseBuilder.created(
                userCompanyMapper.toResponse(
                        companyUserManagementService.assignUserToCompany(id, request.getKeycloakUserId())
                ),
                "Usuario vinculado correctamente"
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CompanyResponseDto>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);

        return ApiResponseBuilder.ok(
                PageMapper.from(companyService.findAll(search, filters, pageable)),
                "Lista de empresas"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CompanyRequestDto request) {

        return ApiResponseBuilder.ok(
                companyService.update(id, request),
                "Empresa actualizada correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        companyService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Empresa eliminada correctamente"
        );
    }

    @DeleteMapping("/{id}/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> unassignUser(@PathVariable UUID id, @PathVariable String userId) {
        companyUserManagementService.removeUserFromCompany(id, userId);
        return ApiResponseBuilder.ok(
                null,
                "Usuario desasignado correctamente"
        );
    }
}

