package com.proseg.msvc_transport.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.common.api.util.PageMapper;
import com.proseg.common.specification.FilterConstants;
import com.proseg.msvc_transport.dto.request.ExternalVehicleRequestDto;
import com.proseg.msvc_transport.dto.response.ExternalVehicleResponseDto;
import com.proseg.msvc_transport.service.ExternalVehicleService;
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
@RequestMapping("${routes.external-vehicles:/api/v1/transport/external-vehicles}")
@RequiredArgsConstructor
public class ExternalVehicleController {

    private final ExternalVehicleService service;

    @PostMapping
    public ResponseEntity<ApiResponse<ExternalVehicleResponseDto>> create(@Valid @RequestBody ExternalVehicleRequestDto request) {
        return ApiResponseBuilder.created(service.create(request), "Vehículo de contratación externa creado correctamente");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExternalVehicleResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(service.findById(id), "Vehículo de contratación externa obtenido correctamente");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExternalVehicleResponseDto>>> findAll(@RequestParam(required = false) String search, @RequestParam Map<String, String> allParams, @PageableDefault(size = 10, page = 0) Pageable pageable) {
        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);
        return ApiResponseBuilder.ok(PageMapper.from(service.findAll(search, filters, pageable)), "Lista de vehículos de contratación externa");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExternalVehicleResponseDto>> update(@PathVariable UUID id, @Valid @RequestBody ExternalVehicleRequestDto request) {
        return ApiResponseBuilder.ok(service.update(id, request), "Vehículo de contratación externa actualizado correctamente");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponseBuilder.ok(null, "Vehículo de contratación externa eliminado correctamente");
    }
}
