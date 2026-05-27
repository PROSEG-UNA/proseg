package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.common.specification.FilterConstants;
import com.sssi.msvcinventory.dto.request.BuildingRequestDto;
import com.sssi.msvcinventory.dto.response.BuildingResponseDto;
import com.sssi.msvcinventory.service.BuildingService;
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
@RequestMapping("${routes.buildings:/api/v1/inventory/buildings}")
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingService buildingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BuildingResponseDto>> create(@Valid @RequestBody BuildingRequestDto request) {
        return ApiResponseBuilder.created(
                buildingService.create(request),
                "Edificio creado correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BuildingResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                buildingService.findById(id),
                "Edificio obtenido correctamente"
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BuildingResponseDto>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);

        return ApiResponseBuilder.ok(
                PageMapper.from(buildingService.findAll(search, filters, pageable)),
                "Lista de edificios"
        );
    }

    @GetMapping("/campus/{campusId}")
    public ResponseEntity<ApiResponse<PageResponse<BuildingResponseDto>>> findByCampusId(
            @PathVariable UUID campusId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        return ApiResponseBuilder.ok(
                PageMapper.from(buildingService.findByCampusId(campusId, pageable)),
                "Edificios por campus"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BuildingResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody BuildingRequestDto request) {

        return ApiResponseBuilder.ok(
                buildingService.update(id, request),
                "Edificio actualizado correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        buildingService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Edificio eliminado correctamente"
        );
    }
}
