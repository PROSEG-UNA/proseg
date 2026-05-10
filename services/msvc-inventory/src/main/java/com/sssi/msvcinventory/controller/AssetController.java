package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.msvcinventory.dto.request.AssetRequestDto;
import com.sssi.msvcinventory.dto.response.AssetArchiveResponseDto;
import com.sssi.msvcinventory.dto.response.AssetResponseDto;
import com.sssi.msvcinventory.service.AssetArchiveService;
import com.sssi.msvcinventory.service.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.UUID;

@RestController
@RequestMapping("${routes.assets:/api/v1/inventory/assets}")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final AssetArchiveService assetArchiveService;

    @PostMapping
    public ResponseEntity<ApiResponse<AssetResponseDto>> create(@Valid @RequestBody AssetRequestDto request) {
                AssetResponseDto dto = assetService.create(request);
                List<AssetArchiveResponseDto> archives = assetArchiveService.getAssetArchivesByAssetId(dto.getId());
                dto.setArchiveUrls(archives.stream().map(AssetArchiveResponseDto::getImageUrl).filter(Objects::nonNull).collect(Collectors.toList()));

                return ApiResponseBuilder.created(dto, "Activo creado correctamente");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetResponseDto>> findById(@PathVariable UUID id) {
                AssetResponseDto dto = assetService.findById(id);
                List<AssetArchiveResponseDto> archives = assetArchiveService.getAssetArchivesByAssetId(id);
                dto.setArchiveUrls(archives.stream().map(AssetArchiveResponseDto::getImageUrl).filter(Objects::nonNull).collect(Collectors.toList()));

                return ApiResponseBuilder.ok(dto, "Activo obtenido correctamente");
    }

    @GetMapping
        public ResponseEntity<ApiResponse<PageResponse<AssetResponseDto>>> findAll(
                        @PageableDefault(size = 10, page = 0) Pageable pageable) {
                Page<AssetResponseDto> page = assetService.findAll(pageable);
                page.forEach(dto -> {
                        List<AssetArchiveResponseDto> archives = assetArchiveService.getAssetArchivesByAssetId(dto.getId());
                        dto.setArchiveUrls(archives.stream().map(AssetArchiveResponseDto::getImageUrl).filter(Objects::nonNull).collect(Collectors.toList()));
                });

                return ApiResponseBuilder.ok(
                                PageMapper.from(page),
                                "Lista de activos"
                );
        }

    @GetMapping("/location/{locationId}")
    public ResponseEntity<ApiResponse<PageResponse<AssetResponseDto>>> findByLocationId(
            @PathVariable UUID locationId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        Page<AssetResponseDto> page = assetService.findByLocationId(locationId, pageable);
        page.forEach(dto -> {
            List<AssetArchiveResponseDto> archives = assetArchiveService.getAssetArchivesByAssetId(dto.getId());
            dto.setArchiveUrls(archives.stream().map(AssetArchiveResponseDto::getImageUrl).filter(Objects::nonNull).collect(Collectors.toList()));
        });

        return ApiResponseBuilder.ok(
                PageMapper.from(page),
                "Activos por ubicación"
        );
    }

    @GetMapping("/site/{siteId}")
    public ResponseEntity<ApiResponse<PageResponse<AssetResponseDto>>> findBySiteId(
            @PathVariable UUID siteId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        Page<AssetResponseDto> page = assetService.findBySiteId(siteId, pageable);
        page.forEach(dto -> {
            List<AssetArchiveResponseDto> archives = assetArchiveService.getAssetArchivesByAssetId(dto.getId());
            dto.setArchiveUrls(archives.stream().map(AssetArchiveResponseDto::getImageUrl).filter(Objects::nonNull).collect(Collectors.toList()));
        });

        return ApiResponseBuilder.ok(
                PageMapper.from(page),
                "Activos por sitio"
        );
    }

    @GetMapping("/type/{typeId}")
    public ResponseEntity<ApiResponse<PageResponse<AssetResponseDto>>> findByTypeId(
            @PathVariable UUID typeId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        Page<AssetResponseDto> page = assetService.findByTypeId(typeId, pageable);
        page.forEach(dto -> {
            List<AssetArchiveResponseDto> archives = assetArchiveService.getAssetArchivesByAssetId(dto.getId());
            dto.setArchiveUrls(archives.stream().map(AssetArchiveResponseDto::getImageUrl).filter(Objects::nonNull).collect(Collectors.toList()));
        });

        return ApiResponseBuilder.ok(
                PageMapper.from(page),
                "Activos por tipo"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AssetRequestDto request) {
        AssetResponseDto dto = assetService.update(id, request);
        List<AssetArchiveResponseDto> archives = assetArchiveService.getAssetArchivesByAssetId(dto.getId());
        dto.setArchiveUrls(archives.stream().map(AssetArchiveResponseDto::getImageUrl).filter(Objects::nonNull).collect(Collectors.toList()));

        return ApiResponseBuilder.ok(dto, "Activo actualizado correctamente");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        assetService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Activo eliminado correctamente"
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
