package com.proseg.msvcinventory.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.msvcinventory.dto.request.NetworkInterfaceRequestDto;
import com.proseg.msvcinventory.dto.response.NetworkInterfaceResponseDto;
import com.proseg.msvcinventory.service.NetworkInterfaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${routes.network-interfaces:/api/v1/inventory/network-interfaces}")
@RequiredArgsConstructor
public class NetworkInterfaceController {

    private final NetworkInterfaceService networkInterfaceService;

    @PostMapping
    public ResponseEntity<ApiResponse<NetworkInterfaceResponseDto>> create(@Valid @RequestBody NetworkInterfaceRequestDto request) {
        return ApiResponseBuilder.created(
                networkInterfaceService.create(request),
                "IP y MAC creada correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NetworkInterfaceResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                networkInterfaceService.findById(id),
                "IP y MAC obtenida correctamente"
        );
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<ApiResponse<NetworkInterfaceResponseDto>> findByAssetId(@PathVariable UUID assetId) {
        return ApiResponseBuilder.ok(
                networkInterfaceService.findByAssetId(assetId),
                "IP y MAC del activo obtenida correctamente"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NetworkInterfaceResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody NetworkInterfaceRequestDto request) {
        return ApiResponseBuilder.ok(
                networkInterfaceService.update(id, request),
                "IP y MAC actualizada correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        networkInterfaceService.delete(id);
        return ApiResponseBuilder.ok(null, "IP y MAC eliminada correctamente");
    }
}