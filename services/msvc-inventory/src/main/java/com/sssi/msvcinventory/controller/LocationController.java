package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.common.specification.FilterConstants;
import com.sssi.msvcinventory.dto.request.LocationRequestDto;
import com.sssi.msvcinventory.dto.response.LocationResponseDto;
import com.sssi.msvcinventory.service.LocationService;
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
@RequestMapping("${routes.locations:/api/v1/inventory/locations}")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    public ResponseEntity<ApiResponse<LocationResponseDto>> create(@Valid @RequestBody LocationRequestDto request) {
        return ApiResponseBuilder.created(
                locationService.create(request),
                "Location creada correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                locationService.findById(id),
                "Location obtenida correctamente"
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LocationResponseDto>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);

        return ApiResponseBuilder.ok(
                PageMapper.from(locationService.findAll(search, filters, pageable)),
                "Lista de locations"
        );
    }

    @GetMapping("/site/{siteId}")
    public ResponseEntity<ApiResponse<PageResponse<LocationResponseDto>>> findBySiteId(
            @PathVariable UUID siteId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        return ApiResponseBuilder.ok(
                PageMapper.from(locationService.findBySiteId(siteId, pageable)),
                "Locations por site"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody LocationRequestDto request) {

        return ApiResponseBuilder.ok(
                locationService.update(id, request),
                "Location actualizada correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        locationService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Location eliminada correctamente"
        );
    }
}