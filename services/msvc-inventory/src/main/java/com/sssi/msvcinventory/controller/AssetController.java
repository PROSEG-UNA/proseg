package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.common.specification.FilterConstants;
import com.sssi.msvcinventory.dto.request.AssetImportRequestDto;
import com.sssi.msvcinventory.dto.request.AssetRequestDto;
import com.sssi.msvcinventory.dto.request.ImportConfirmRequestDto;
import com.sssi.msvcinventory.dto.response.AssetArchiveResponseDto;
import com.sssi.msvcinventory.dto.response.AssetImportResponseDto;
import com.sssi.msvcinventory.dto.response.AssetSchemaDto;
import com.sssi.msvcinventory.dto.response.ImportPreviewResponseDto;
import com.sssi.msvcinventory.dto.response.AssetResponseDto;
import com.sssi.msvcinventory.dto.response.NetworkInterfaceResponseDto;
import com.sssi.msvcinventory.service.AssetArchiveService;
import com.sssi.msvcinventory.service.AssetImportService;
import com.sssi.msvcinventory.service.AssetService;
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
@RequestMapping("${routes.assets:/api/v1/inventory/assets}")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final AssetArchiveService assetArchiveService;
    private final AssetImportService assetImportService;

    @PostMapping
    public ResponseEntity<ApiResponse<AssetResponseDto>> create(@Valid @RequestBody AssetRequestDto request) {
        return ApiResponseBuilder.created(assetService.create(request), "Activo creado correctamente");
    }

    @PostMapping("/import/preview")
    public ResponseEntity<ApiResponse<ImportPreviewResponseDto>> previewImport(
            @Valid @RequestBody AssetImportRequestDto request) {
        return ApiResponseBuilder.ok(assetImportService.preview(request), "Previsualización de importación");
    }

    @PostMapping("/import/confirm")
    public ResponseEntity<ApiResponse<AssetImportResponseDto>> confirmImport(
            @Valid @RequestBody ImportConfirmRequestDto request) {
        return ApiResponseBuilder.ok(assetImportService.confirm(request), "Importación procesada");
    }

    @GetMapping("/schema")
    public ResponseEntity<ApiResponse<AssetSchemaDto>> getAssetSchema() {
        return ApiResponseBuilder.ok(assetImportService.getAssetSchema(), "Esquema de importación");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(assetService.findById(id), "Activo obtenido correctamente");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AssetResponseDto>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);

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

    @GetMapping("/campus/{campusId}")
    public ResponseEntity<ApiResponse<PageResponse<AssetResponseDto>>> findByCampusId(
            @PathVariable UUID campusId,
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);

        return ApiResponseBuilder.ok(
                PageMapper.from(assetService.findByCampusId(campusId, search, filters, pageable)),
                "Activos por campus"
        );
    }

    @GetMapping("/building/{buildingId}")
    public ResponseEntity<ApiResponse<PageResponse<AssetResponseDto>>> findByBuildingId(
            @PathVariable UUID buildingId,
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);

        return ApiResponseBuilder.ok(
                PageMapper.from(assetService.findByBuildingId(buildingId, search, filters, pageable)),
                "Activos por edificio"
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
                "Última IP y MAC conocida"
        );
    }

    @GetMapping("/check-asset-number")
    public ResponseEntity<ApiResponse<Boolean>> checkAssetNumber(
            @RequestParam String value,
            @RequestParam(required = false) UUID excludeId) {
        return ApiResponseBuilder.ok(
                assetService.existsByAssetNumber(value, excludeId),
                "Verificación completada"
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
