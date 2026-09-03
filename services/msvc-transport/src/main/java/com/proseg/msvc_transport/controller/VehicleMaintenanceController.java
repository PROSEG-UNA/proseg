package com.proseg.msvc_transport.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.common.api.util.PageMapper;
import com.proseg.common.specification.FilterConstants;
import com.proseg.msvc_transport.dto.request.VehicleMaintenanceRequestDto;
import com.proseg.msvc_transport.dto.response.VehicleMaintenanceResponseDto;
import com.proseg.msvc_transport.service.VehicleMaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${routes.vehicle-maintenance:/api/v1/transport/maintenance}")
@RequiredArgsConstructor
public class VehicleMaintenanceController {

    private final VehicleMaintenanceService service;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleMaintenanceResponseDto>> create(@Valid @RequestBody VehicleMaintenanceRequestDto request) {
        return ApiResponseBuilder.created(service.create(request), "Mantenimiento creado correctamente");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleMaintenanceResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(service.findById(id), "Mantenimiento obtenido correctamente");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VehicleMaintenanceResponseDto>>> findAll(@RequestParam(required = false) String search, @RequestParam Map<String, String> allParams, @PageableDefault(size = 10, page = 0) Pageable pageable) {
        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);
        return ApiResponseBuilder.ok(PageMapper.from(service.findAll(search, filters, pageable)), "Lista de mantenimientos");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleMaintenanceResponseDto>> update(@PathVariable UUID id, @Valid @RequestBody VehicleMaintenanceRequestDto request) {
        return ApiResponseBuilder.ok(service.update(id, request), "Mantenimiento actualizado correctamente");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponseBuilder.ok(null, "Mantenimiento eliminado correctamente");
    }
}
