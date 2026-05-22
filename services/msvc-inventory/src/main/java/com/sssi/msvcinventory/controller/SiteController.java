package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.common.specification.FilterConstants;
import com.sssi.msvcinventory.dto.request.SiteRequestDto;
import com.sssi.msvcinventory.dto.response.SiteResponseDto;
import com.sssi.msvcinventory.service.SiteService;
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
@RequestMapping("${routes.sites:/api/v1/inventory/sites}")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @PostMapping
    public ResponseEntity<ApiResponse<SiteResponseDto>> create(@Valid @RequestBody SiteRequestDto request) {
        return ApiResponseBuilder.created(
                siteService.create(request),
                "Site creado correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SiteResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                siteService.findById(id),
                "Site obtenido correctamente"
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SiteResponseDto>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);

        return ApiResponseBuilder.ok(
                PageMapper.from(siteService.findAll(search, filters, pageable)),
                "Lista de sites"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SiteResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody SiteRequestDto request) {

        return ApiResponseBuilder.ok(
                siteService.update(id, request),
                "Site actualizado correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        siteService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Site eliminado correctamente"
        );
    }
}