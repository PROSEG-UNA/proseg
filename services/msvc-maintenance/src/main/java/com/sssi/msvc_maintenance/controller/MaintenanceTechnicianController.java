package com.sssi.msvc_maintenance.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.common.specification.FilterConstants;
import com.sssi.msvc_maintenance.dto.request.MaintenanceTechnicianRequestDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceTechnicianResponseDto;
import com.sssi.msvc_maintenance.service.MaintenanceTechnicianService;
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
@RequestMapping("${routes.technicians:/api/v1/maintenance/technicians}")
@RequiredArgsConstructor
public class MaintenanceTechnicianController {

    private final MaintenanceTechnicianService maintenanceTechnicianService;

    @PostMapping
    public ResponseEntity<ApiResponse<MaintenanceTechnicianResponseDto>> create(
            @RequestParam UUID maintenanceRequestId,
            @Valid @RequestBody MaintenanceTechnicianRequestDto request) {

        return ApiResponseBuilder.created(
                maintenanceTechnicianService.create(maintenanceRequestId, request),
                "Técnico de mantenimiento creado correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceTechnicianResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                maintenanceTechnicianService.findById(id),
                "Técnico de mantenimiento obtenido correctamente"
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MaintenanceTechnicianResponseDto>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceTechnicianService.findAll(search, filters, pageable)),
                "Lista de técnicos de mantenimiento"
        );
    }

    @GetMapping("/maintenance-request/{maintenanceRequestId}")
    public ResponseEntity<ApiResponse<PageResponse<MaintenanceTechnicianResponseDto>>> findByMaintenanceRequestId(
            @PathVariable UUID maintenanceRequestId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceTechnicianService.findByMaintenanceRequestId(maintenanceRequestId, pageable)),
                "Técnicos por solicitud de mantenimiento"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceTechnicianResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody MaintenanceTechnicianRequestDto request) {

        return ApiResponseBuilder.ok(
                maintenanceTechnicianService.update(id, request),
                "Técnico de mantenimiento actualizado correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        maintenanceTechnicianService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Técnico de mantenimiento eliminado correctamente"
        );
    }
}

