package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.msvcinventory.dto.request.ImageRequestDto;
import com.sssi.msvcinventory.dto.response.ImageResponseDto;
import com.sssi.msvcinventory.service.ImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${routes.images:/api/v1/inventory/images}")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping
    public ResponseEntity<ApiResponse<ImageResponseDto>> create(@Valid @RequestBody ImageRequestDto request) {
        return ApiResponseBuilder.created(
                imageService.create(request),
                "Imagen del activo creada correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ImageResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                imageService.findById(id),
                "Imagen del activo obtenida correctamente"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ImageResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ImageRequestDto request) {
        return ApiResponseBuilder.ok(
                imageService.update(id, request),
                "Imagen del activo actualizada correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        imageService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Imagen del activo eliminada correctamente"
        );
    }
}
