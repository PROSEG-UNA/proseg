package com.sssi.msvc_transport.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.common.specification.FilterConstants;
import com.sssi.msvc_transport.dto.request.DriverRequestDto;
import com.sssi.msvc_transport.dto.response.DriverResponseDto;
import com.sssi.msvc_transport.service.DriverService;
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
@RequestMapping("${routes.drivers:/api/v1/transport/drivers}")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService service;

    @PostMapping
    public ResponseEntity<ApiResponse<DriverResponseDto>> create(@Valid @RequestBody DriverRequestDto request) {
        return ApiResponseBuilder.created(service.create(request), "Chofer creado correctamente");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(service.findById(id), "Chofer obtenido correctamente");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DriverResponseDto>>> findAll(@RequestParam(required = false) String search, @RequestParam Map<String, String> allParams, @PageableDefault(size = 10, page = 0) Pageable pageable) {
        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);
        return ApiResponseBuilder.ok(PageMapper.from(service.findAll(search, filters, pageable)), "Lista de choferes");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponseDto>> update(@PathVariable UUID id, @Valid @RequestBody DriverRequestDto request) {
        return ApiResponseBuilder.ok(service.update(id, request), "Chofer actualizado correctamente");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponseBuilder.ok(null, "Chofer eliminado correctamente");
    }
}
