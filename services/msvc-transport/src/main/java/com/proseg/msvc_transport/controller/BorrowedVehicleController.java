package com.proseg.msvc_transport.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.common.api.util.PageMapper;
import com.proseg.common.specification.FilterConstants;
import com.proseg.msvc_transport.dto.request.BorrowedVehicleRequestDto;
import com.proseg.msvc_transport.dto.response.BorrowedVehicleResponseDto;
import com.proseg.msvc_transport.service.BorrowedVehicleService;
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
@RequestMapping("${routes.borrowed-vehicles:/api/v1/transport/borrowed-vehicles}")
@RequiredArgsConstructor
public class BorrowedVehicleController {

    private final BorrowedVehicleService service;

    @PostMapping
    public ResponseEntity<ApiResponse<BorrowedVehicleResponseDto>> create(@Valid @RequestBody BorrowedVehicleRequestDto request) {
        return ApiResponseBuilder.created(service.create(request), "Vehículo prestado creado correctamente");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BorrowedVehicleResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(service.findById(id), "Vehículo prestado obtenido correctamente");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BorrowedVehicleResponseDto>>> findAll(@RequestParam(required = false) String search, @RequestParam Map<String, String> allParams, @PageableDefault(size = 10, page = 0) Pageable pageable) {
        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);
        return ApiResponseBuilder.ok(PageMapper.from(service.findAll(search, filters, pageable)), "Lista de vehículos prestados");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BorrowedVehicleResponseDto>> update(@PathVariable UUID id, @Valid @RequestBody BorrowedVehicleRequestDto request) {
        return ApiResponseBuilder.ok(service.update(id, request), "Vehículo prestado actualizado correctamente");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponseBuilder.ok(null, "Vehículo prestado eliminado correctamente");
    }
}
