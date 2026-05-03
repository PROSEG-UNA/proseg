package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.msvcinventory.dto.request.NetworkInterfaceRequestDto;
import com.sssi.msvcinventory.dto.response.NetworkInterfaceResponseDto;
import com.sssi.msvcinventory.service.NetworkInterfaceService;
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
                "Interfaz de red creada correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NetworkInterfaceResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                networkInterfaceService.findById(id),
                "Interfaz de red obtenida correctamente"
        );
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<ApiResponse<NetworkInterfaceResponseDto>> findByAssetId(@PathVariable UUID assetId) {
        return ApiResponseBuilder.ok(
                networkInterfaceService.findByAssetId(assetId),
                "Interfaz de red del activo obtenida correctamente"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NetworkInterfaceResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody NetworkInterfaceRequestDto request) {
        return ApiResponseBuilder.ok(
                networkInterfaceService.update(id, request),
                "Interfaz de red actualizada correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        networkInterfaceService.delete(id);
        return ApiResponseBuilder.ok(null, "Interfaz de red eliminada correctamente");
    }
}