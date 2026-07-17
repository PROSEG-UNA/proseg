package com.sssi.msvc_transport.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.msvc_transport.dto.cleaning.CleaningFinalizeRequestDto;
import com.sssi.msvc_transport.dto.cleaning.CleaningFinalizeResponseDto;
import com.sssi.msvc_transport.dto.cleaning.CleaningPreviewResponseDto;
import com.sssi.msvc_transport.dto.cleaning.CleaningRegisterRequestDto;
import com.sssi.msvc_transport.dto.cleaning.CleaningRegisterResponseDto;
import com.sssi.msvc_transport.service.CleaningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${routes.cleaning:/api/v1/transport/cleaning}")
@RequiredArgsConstructor
public class CleaningController {

    private final CleaningService cleaningService;

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<CleaningPreviewResponseDto>> preview(@RequestParam("file") MultipartFile file) {
        return ApiResponseBuilder.ok(cleaningService.preview(file), "Vista previa generada correctamente");
    }

    @PostMapping("/finalize")
    public ResponseEntity<ApiResponse<CleaningFinalizeResponseDto>> finalizeCleaning(@Valid @RequestBody CleaningFinalizeRequestDto request) {
        return ApiResponseBuilder.ok(cleaningService.finalizeCleaning(request), "Depuración finalizada correctamente");
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CleaningRegisterResponseDto>> register(@Valid @RequestBody CleaningRegisterRequestDto request) {
        return ApiResponseBuilder.ok(cleaningService.registerRows(request), "Giras importadas correctamente");
    }
}
