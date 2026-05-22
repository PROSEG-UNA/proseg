package com.sssi.msvc_maintenance.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.common.specification.FilterConstants;
import com.sssi.msvc_maintenance.dto.request.UserCompanyRequestDto;
import com.sssi.msvc_maintenance.dto.response.UserCompanyResponseDto;
import com.sssi.msvc_maintenance.service.UserCompanyService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${routes.user-companies:/api/v1/maintenance/user-companies}")
@RequiredArgsConstructor
public class UserCompanyController {

    private final UserCompanyService userCompanyService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserCompanyResponseDto>> create(@Valid @RequestBody UserCompanyRequestDto request) {
        return ApiResponseBuilder.created(
                userCompanyService.create(request),
                "Relación usuario-compañía creada correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserCompanyResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                userCompanyService.findById(id),
                "Relación usuario-compañía obtenida correctamente"
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserCompanyResponseDto>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);

        return ApiResponseBuilder.ok(
                PageMapper.from(userCompanyService.findAll(search, filters, pageable)),
                "Lista de relaciones usuario-compañía"
        );
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<ApiResponse<PageResponse<UserCompanyResponseDto>>> findByCompanyId(
            @PathVariable UUID companyId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        return ApiResponseBuilder.ok(
                PageMapper.from(userCompanyService.findByCompanyId(companyId, pageable)),
                "Relaciones por compañía"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserCompanyResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserCompanyRequestDto request) {

        return ApiResponseBuilder.ok(
                userCompanyService.update(id, request),
                "Relación usuario-compañía actualizada correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        userCompanyService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Relación usuario-compañía eliminada correctamente"
        );
    }
}

