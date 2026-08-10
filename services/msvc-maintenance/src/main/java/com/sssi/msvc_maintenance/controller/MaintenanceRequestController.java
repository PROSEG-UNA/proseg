package com.sssi.msvc_maintenance.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.common.specification.FilterConstants;
import com.sssi.msvc_maintenance.dto.response.MaintenanceAssetOptionDto;
import com.sssi.msvc_maintenance.dto.request.MaintenanceRequestCancelRequestDto;
import com.sssi.msvc_maintenance.dto.request.MaintenanceRequestRequestDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceRequestResponseDto;
import com.sssi.msvc_maintenance.service.MaintenanceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("${routes.requests:/api/v1/maintenance/requests}")
@RequiredArgsConstructor
public class MaintenanceRequestController {

    private final MaintenanceRequestService maintenanceRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<MaintenanceRequestResponseDto>> create(
            @Valid @RequestBody MaintenanceRequestRequestDto request) {

        return ApiResponseBuilder.created(
                maintenanceRequestService.create(request),
                "Solicitud de mantenimiento creada correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceRequestResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                maintenanceRequestService.findById(id),
                "Solicitud de mantenimiento obtenida correctamente"
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MaintenanceRequestResponseDto>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceRequestService.findAll(search, filters, pageable)),
                "Lista de solicitudes de mantenimiento"
        );
    }

    @GetMapping("/assets")
    public ResponseEntity<ApiResponse<PageResponse<MaintenanceAssetOptionDto>>> findAvailableAssets(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceRequestService.findAvailableAssets(search, pageable)),
                "Lista de activos disponibles"
        );
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<ApiResponse<PageResponse<MaintenanceRequestResponseDto>>> findByCompanyId(
            @PathVariable UUID companyId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceRequestService.findByCompanyId(companyId, pageable)),
                "Solicitudes por empresa"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceRequestResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody MaintenanceRequestRequestDto request) {

        return ApiResponseBuilder.ok(
                maintenanceRequestService.update(id, request),
                "Solicitud de mantenimiento actualizada correctamente"
        );
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<MaintenanceRequestResponseDto>> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) MaintenanceRequestCancelRequestDto request) {

        String reason = request != null ? request.getReason() : null;
        return ApiResponseBuilder.ok(
                maintenanceRequestService.cancel(id, reason),
                "Solicitud de mantenimiento cancelada correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        maintenanceRequestService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Solicitud de mantenimiento eliminada correctamente"
        );
    }
}

