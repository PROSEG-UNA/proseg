package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.msvcinventory.dto.request.AssetArchiveRequestDto;
import com.sssi.msvcinventory.dto.response.AssetArchiveResponseDto;
import com.sssi.msvcinventory.service.AssetArchiveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${routes.asset-archives:/api/v1/inventory/asset-archives}")
@RequiredArgsConstructor
public class AssetArchiveController {

    private final AssetArchiveService assetArchiveService;

    @PostMapping
    public ResponseEntity<ApiResponse<AssetArchiveResponseDto>> create(@Valid @RequestBody AssetArchiveRequestDto request) {
        return ApiResponseBuilder.created(
                assetArchiveService.create(request),
                "Archivo del activo creado correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetArchiveResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                assetArchiveService.findById(id),
                "Archivo del activo obtenido correctamente"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetArchiveResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AssetArchiveRequestDto request) {
        return ApiResponseBuilder.ok(
                assetArchiveService.update(id, request),
                "Archivo del activo actualizado correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        assetArchiveService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Archivo del activo eliminado correctamente"
        );
    }
}
