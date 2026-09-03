package com.proseg.msvcinventory.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.msvcinventory.dto.request.AssetArchiveRequestDto;
import com.proseg.msvcinventory.dto.response.AssetArchiveResponseDto;
import com.proseg.msvcinventory.service.AssetArchiveService;
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
