package com.proseg.msvcinventory.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.common.api.util.PageMapper;
import com.proseg.common.specification.FilterConstants;
import com.proseg.msvcinventory.dto.request.ModelRequestDto;
import com.proseg.msvcinventory.dto.response.ModelResponseDto;
import com.proseg.msvcinventory.service.ModelService;
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
@RequestMapping("${routes.models:/api/v1/inventory/models}")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    @PostMapping
    public ResponseEntity<ApiResponse<ModelResponseDto>> create(@Valid @RequestBody ModelRequestDto request) {
        return ApiResponseBuilder.created(
                modelService.create(request),
                "Modelo de activo creado correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ModelResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                modelService.findById(id),
                "Modelo de activo obtenido correctamente"
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ModelResponseDto>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);

        return ApiResponseBuilder.ok(
                PageMapper.from(modelService.findAll(search, filters, pageable)),
                "Lista de modelos de activo"
        );
    }

    @GetMapping("/brand/{brandId}")
    public ResponseEntity<ApiResponse<PageResponse<ModelResponseDto>>> findByBrandId(
            @PathVariable UUID brandId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ApiResponseBuilder.ok(
                PageMapper.from(modelService.findByBrandId(brandId, pageable)),
                "Modelos por marca"
        );
    }

    @GetMapping("/type/{typeId}")
    public ResponseEntity<ApiResponse<PageResponse<ModelResponseDto>>> findByTypeId(
            @PathVariable UUID typeId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ApiResponseBuilder.ok(
                PageMapper.from(modelService.findByTypeId(typeId, pageable)),
                "Modelos por tipo de activo"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ModelResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ModelRequestDto request) {
        return ApiResponseBuilder.ok(
                modelService.update(id, request),
                "Modelo de activo actualizado correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        modelService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Modelo de activo eliminado correctamente"
        );
    }
}
