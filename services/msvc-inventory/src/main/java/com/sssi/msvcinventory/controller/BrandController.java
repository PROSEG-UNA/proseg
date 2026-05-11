package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.msvcinventory.dto.request.BrandRequestDto;
import com.sssi.msvcinventory.dto.response.BrandResponseDto;
import com.sssi.msvcinventory.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${routes.brands:/api/v1/inventory/brands}")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @PostMapping
    public ResponseEntity<ApiResponse<BrandResponseDto>> create(@Valid @RequestBody BrandRequestDto request) {
        return ApiResponseBuilder.created(
                brandService.create(request),
                "Brand creada correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                brandService.findById(id),
                "Brand obtenida correctamente"
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BrandResponseDto>>> findAll(
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        return ApiResponseBuilder.ok(
                PageMapper.from(brandService.findAll(pageable)),
                "Lista de brands"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody BrandRequestDto request) {

        return ApiResponseBuilder.ok(
                brandService.update(id, request),
                "Brand actualizada correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        brandService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Brand eliminada correctamente"
        );
    }
}