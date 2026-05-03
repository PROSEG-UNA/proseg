package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.msvcinventory.dto.request.AssetImageRequestDto;
import com.sssi.msvcinventory.dto.response.AssetImageResponseDto;
import com.sssi.msvcinventory.service.AssetImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${routes.asset-images:/api/v1/inventory/asset-images}")
@RequiredArgsConstructor
public class AssetImageController {

    private final AssetImageService assetImageService;

    @PostMapping
    public ResponseEntity<ApiResponse<AssetImageResponseDto>> create(@Valid @RequestBody AssetImageRequestDto request) {
        return ApiResponseBuilder.created(
                assetImageService.create(request),
                "Imagen del activo creada correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetImageResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                assetImageService.findById(id),
                "Imagen del activo obtenida correctamente"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetImageResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AssetImageRequestDto request) {
        return ApiResponseBuilder.ok(
                assetImageService.update(id, request),
                "Imagen del activo actualizada correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        assetImageService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Imagen del activo eliminada correctamente"
        );
    }
}
