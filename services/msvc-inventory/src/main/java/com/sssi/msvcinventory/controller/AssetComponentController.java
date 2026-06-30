package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.msvcinventory.dto.request.AssetComponentRequestDto;
import com.sssi.msvcinventory.dto.response.AssetComponentResponseDto;
import com.sssi.msvcinventory.service.AssetComponentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AssetComponentController {

    private final AssetComponentService assetComponentService;

    @PostMapping("${routes.assets:/api/v1/inventory/assets}/{assetId}/components")
    public ResponseEntity<ApiResponse<AssetComponentResponseDto>> create(
            @PathVariable UUID assetId,
            @Valid @RequestBody AssetComponentRequestDto request) {
        return ApiResponseBuilder.created(
                assetComponentService.create(assetId, request),
                "Componente registrado correctamente"
        );
    }

    @GetMapping("${routes.assets:/api/v1/inventory/assets}/{assetId}/components")
    public ResponseEntity<ApiResponse<List<AssetComponentResponseDto>>> findByAsset(
            @PathVariable UUID assetId) {
        return ApiResponseBuilder.ok(
                assetComponentService.findByAssetId(assetId),
                "Componentes del activo obtenidos correctamente"
        );
    }

    @PutMapping("${routes.asset-components:/api/v1/inventory/components}/{id}")
    public ResponseEntity<ApiResponse<AssetComponentResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AssetComponentRequestDto request) {
        return ApiResponseBuilder.ok(
                assetComponentService.update(id, request),
                "Componente actualizado correctamente"
        );
    }

    @DeleteMapping("${routes.asset-components:/api/v1/inventory/components}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        assetComponentService.delete(id);
        return ApiResponseBuilder.ok(null, "Componente eliminado correctamente");
    }
}

