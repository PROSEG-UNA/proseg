package com.proseg.msvc_transport.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.common.api.util.PageMapper;
import com.proseg.common.specification.FilterConstants;
import com.proseg.msvc_transport.dto.request.VehicleRequestDto;
import com.proseg.msvc_transport.dto.response.VehicleResponseDto;
import com.proseg.msvc_transport.service.VehicleService;
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
@RequestMapping("${routes.vehicles:/api/v1/transport/vehicles}")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService service;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponseDto>> create(@Valid @RequestBody VehicleRequestDto request) {
        return ApiResponseBuilder.created(service.create(request), "Vehículo creado correctamente");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(service.findById(id), "Vehículo obtenido correctamente");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VehicleResponseDto>>> findAll(@RequestParam(required = false) String search, @RequestParam Map<String, String> allParams, @PageableDefault(size = 10, page = 0) Pageable pageable) {
        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);
        return ApiResponseBuilder.ok(PageMapper.from(service.findAll(search, filters, pageable)), "Lista de vehículos");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponseDto>> update(@PathVariable UUID id, @Valid @RequestBody VehicleRequestDto request) {
        return ApiResponseBuilder.ok(service.update(id, request), "Vehículo actualizado correctamente");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponseBuilder.ok(null, "Vehículo eliminado correctamente");
    }
}
