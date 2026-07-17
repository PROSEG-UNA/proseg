package com.sssi.msvc_transport.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.common.specification.FilterConstants;
import com.sssi.msvc_transport.dto.request.TourRequestDto;
import com.sssi.msvc_transport.dto.response.TourResponseDto;
import com.sssi.msvc_transport.service.TourService;
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
@RequestMapping("${routes.tours:/api/v1/transport/tours}")
@RequiredArgsConstructor
public class TourController {

    private final TourService service;

    @PostMapping
    public ResponseEntity<ApiResponse<TourResponseDto>> create(@Valid @RequestBody TourRequestDto request) {
        return ApiResponseBuilder.created(service.create(request), "Gira creada correctamente");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TourResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(service.findById(id), "Gira obtenida correctamente");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TourResponseDto>>> findAll(@RequestParam(required = false) String search, @RequestParam Map<String, String> allParams, @PageableDefault(size = 10, page = 0) Pageable pageable) {
        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);
        return ApiResponseBuilder.ok(PageMapper.from(service.findAll(search, filters, pageable)), "Lista de giras");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TourResponseDto>> update(@PathVariable UUID id, @Valid @RequestBody TourRequestDto request) {
        return ApiResponseBuilder.ok(service.update(id, request), "Gira actualizada correctamente");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponseBuilder.ok(null, "Gira eliminada correctamente");
    }
}
