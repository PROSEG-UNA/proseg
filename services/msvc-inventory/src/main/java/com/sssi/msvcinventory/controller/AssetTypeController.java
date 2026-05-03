package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.msvcinventory.dto.request.AssetTypeRequestDto;
import com.sssi.msvcinventory.dto.response.AssetTypeResponseDto;
import com.sssi.msvcinventory.service.AssetTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${routes.asset-types:/api/v1/inventory/asset-types}")
@RequiredArgsConstructor
public class AssetTypeController {

    private final AssetTypeService assetTypeService;

    @PostMapping
    public ResponseEntity<ApiResponse<AssetTypeResponseDto>> create(@Valid @RequestBody AssetTypeRequestDto request) {
        return ApiResponseBuilder.created(
                assetTypeService.create(request),
                "Tipo de activo creado correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetTypeResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                assetTypeService.findById(id),
                "Tipo de activo obtenido correctamente"
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AssetTypeResponseDto>>> findAll(
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ApiResponseBuilder.ok(
                PageMapper.from(assetTypeService.findAll(pageable)),
                "Lista de tipos de activo"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetTypeResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AssetTypeRequestDto request) {
        return ApiResponseBuilder.ok(
                assetTypeService.update(id, request),
                "Tipo de activo actualizado correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        assetTypeService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Tipo de activo eliminado correctamente"
        );
    }
}
