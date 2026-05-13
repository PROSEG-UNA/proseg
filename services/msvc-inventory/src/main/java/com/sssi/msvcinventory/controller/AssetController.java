package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.msvcinventory.dto.request.AssetRequestDto;
import com.sssi.msvcinventory.dto.response.AssetArchiveResponseDto;
import com.sssi.msvcinventory.dto.response.AssetResponseDto;
import com.sssi.msvcinventory.dto.response.NetworkInterfaceResponseDto;
import com.sssi.msvcinventory.service.AssetArchiveService;
import com.sssi.msvcinventory.service.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("${routes.assets:/api/v1/inventory/assets}")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final AssetArchiveService assetArchiveService;

    @PostMapping
    public ResponseEntity<ApiResponse<AssetResponseDto>> create(@Valid @RequestBody AssetRequestDto request) {
        return ApiResponseBuilder.created(assetService.create(request), "Activo creado correctamente");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(assetService.findById(id), "Activo obtenido correctamente");
    }

    private static final Set<String> RESERVED_PARAMS = Set.of("search", "sort", "page", "size");

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AssetResponseDto>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Map<String, String> filters = new HashMap<>(allParams);
        RESERVED_PARAMS.forEach(filters::remove);

        return ApiResponseBuilder.ok(
                PageMapper.from(assetService.findAll(search, filters, pageable)),
                "Lista de activos"
        );
    }

    @GetMapping("/location/{locationId}")
    public ResponseEntity<ApiResponse<PageResponse<AssetResponseDto>>> findByLocationId(
            @PathVariable UUID locationId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ApiResponseBuilder.ok(
                PageMapper.from(assetService.findByLocationId(locationId, pageable)),
                "Activos por ubicación"
        );
    }

    @GetMapping("/site/{siteId}")
    public ResponseEntity<ApiResponse<PageResponse<AssetResponseDto>>> findBySiteId(
            @PathVariable UUID siteId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ApiResponseBuilder.ok(
                PageMapper.from(assetService.findBySiteId(siteId, pageable)),
                "Activos por sitio"
        );
    }

    @GetMapping("/type/{typeId}")
    public ResponseEntity<ApiResponse<PageResponse<AssetResponseDto>>> findByTypeId(
            @PathVariable UUID typeId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ApiResponseBuilder.ok(
                PageMapper.from(assetService.findByTypeId(typeId, pageable)),
                "Activos por tipo"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AssetRequestDto request) {
        return ApiResponseBuilder.ok(assetService.update(id, request), "Activo actualizado correctamente");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        assetService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Activo eliminado correctamente"
        );
    }

    @GetMapping("/{id}/network-interface/last-known")
    public ResponseEntity<ApiResponse<NetworkInterfaceResponseDto>> findLastKnownNetworkInterface(
            @PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                assetService.findLastKnownNetworkInterface(id),
                "Última interfaz de red conocida"
        );
    }

    @GetMapping("/{assetId}/asset-archives")
    public ResponseEntity<ApiResponse<PageResponse<AssetArchiveResponseDto>>> findAssetArchives(
            @PathVariable UUID assetId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ApiResponseBuilder.ok(
                PageMapper.from(assetArchiveService.findByAssetId(assetId, pageable)),
                "Archivos del activo"
        );
    }
}
