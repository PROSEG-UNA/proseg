package com.proseg.msvc_transport.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.msvc_transport.dto.request.TourSplitKeywordRequestDto;
import com.proseg.msvc_transport.dto.request.TourSplitSettingsRequestDto;
import com.proseg.msvc_transport.dto.response.TourSplitKeywordResponseDto;
import com.proseg.msvc_transport.dto.response.TourSplitSettingsResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourSplitMode;
import com.proseg.msvc_transport.service.TourSplitSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${routes.tour-split:/api/v1/transport/tour-split}")
@RequiredArgsConstructor
public class TourSplitSettingsController {

    private final TourSplitSettingsService service;

    @GetMapping("/keywords")
    public ResponseEntity<ApiResponse<List<TourSplitKeywordResponseDto>>> findKeywords(@RequestParam TourSplitMode mode) {
        return ApiResponseBuilder.ok(service.findKeywords(mode), "Lista de palabras de destino");
    }

    @GetMapping("/keywords/{id}")
    public ResponseEntity<ApiResponse<TourSplitKeywordResponseDto>> findKeyword(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(service.findKeyword(id), "Palabra de destino obtenida correctamente");
    }

    @PostMapping("/keywords")
    public ResponseEntity<ApiResponse<TourSplitKeywordResponseDto>> createKeyword(@Valid @RequestBody TourSplitKeywordRequestDto request) {
        return ApiResponseBuilder.created(service.createKeyword(request), "Palabra de destino creada correctamente");
    }

    @PutMapping("/keywords/{id}")
    public ResponseEntity<ApiResponse<TourSplitKeywordResponseDto>> updateKeyword(@PathVariable UUID id, @Valid @RequestBody TourSplitKeywordRequestDto request) {
        return ApiResponseBuilder.ok(service.updateKeyword(id, request), "Palabra de destino actualizada correctamente");
    }

    @DeleteMapping("/keywords/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteKeyword(@PathVariable UUID id) {
        service.deleteKeyword(id);
        return ApiResponseBuilder.ok(null, "Palabra de destino eliminada correctamente");
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<TourSplitSettingsResponseDto>> findSettings() {
        return ApiResponseBuilder.ok(service.findSettings(), "Configuración de división de giras");
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<TourSplitSettingsResponseDto>> updateSettings(@Valid @RequestBody TourSplitSettingsRequestDto request) {
        return ApiResponseBuilder.ok(service.updateSettings(request), "Configuración de división de giras actualizada");
    }
}
