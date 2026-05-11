package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.msvcinventory.dto.request.TypeRequestDto;
import com.sssi.msvcinventory.dto.response.TypeResponseDto;
import com.sssi.msvcinventory.service.TypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${routes.types:/api/v1/inventory/types}")
@RequiredArgsConstructor
public class TypeController {

    private final TypeService typeService;

    @PostMapping
    public ResponseEntity<ApiResponse<TypeResponseDto>> create(@Valid @RequestBody TypeRequestDto request) {
        return ApiResponseBuilder.created(
                typeService.create(request),
                "Tipo de activo creado correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TypeResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                typeService.findById(id),
                "Tipo de activo obtenido correctamente"
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TypeResponseDto>>> findAll(
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ApiResponseBuilder.ok(
                PageMapper.from(typeService.findAll(pageable)),
                "Lista de tipos de activo"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TypeResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TypeRequestDto request) {
        return ApiResponseBuilder.ok(
                typeService.update(id, request),
                "Tipo de activo actualizado correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        typeService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Tipo de activo eliminado correctamente"
        );
    }
}
