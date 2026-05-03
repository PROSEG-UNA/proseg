package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.msvcinventory.dto.request.AssetModelRequestDto;
import com.sssi.msvcinventory.dto.response.AssetModelResponseDto;
import com.sssi.msvcinventory.service.AssetModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${routes.asset-models:/api/v1/inventory/asset-models}")
@RequiredArgsConstructor
public class AssetModelController {

    private final AssetModelService assetModelService;

    @PostMapping
    public ResponseEntity<ApiResponse<AssetModelResponseDto>> create(@Valid @RequestBody AssetModelRequestDto request) {
        return ApiResponseBuilder.created(
                assetModelService.create(request),
                "Modelo de activo creado correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetModelResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                assetModelService.findById(id),
                "Modelo de activo obtenido correctamente"
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AssetModelResponseDto>>> findAll(
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ApiResponseBuilder.ok(
                PageMapper.from(assetModelService.findAll(pageable)),
                "Lista de modelos de activo"
        );
    }

    @GetMapping("/brand/{brandId}")
    public ResponseEntity<ApiResponse<PageResponse<AssetModelResponseDto>>> findByBrandId(
            @PathVariable UUID brandId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ApiResponseBuilder.ok(
                PageMapper.from(assetModelService.findByBrandId(brandId, pageable)),
                "Modelos por marca"
        );
    }

    @GetMapping("/type/{assetTypeId}")
    public ResponseEntity<ApiResponse<PageResponse<AssetModelResponseDto>>> findByAssetTypeId(
            @PathVariable UUID assetTypeId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ApiResponseBuilder.ok(
                PageMapper.from(assetModelService.findByAssetTypeId(assetTypeId, pageable)),
                "Modelos por tipo de activo"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetModelResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AssetModelRequestDto request) {
        return ApiResponseBuilder.ok(
                assetModelService.update(id, request),
                "Modelo de activo actualizado correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        assetModelService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Modelo de activo eliminado correctamente"
        );
    }
}
