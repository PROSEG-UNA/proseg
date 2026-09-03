package com.proseg.msvc_transport.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.common.api.util.PageMapper;
import com.proseg.common.specification.FilterConstants;
import com.proseg.msvc_transport.dto.cleaning.CleaningExecutionDetailViewResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningExecutionSummaryResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningFinalizeRequestDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningFinalizeResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningPreviewResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningRegisterRequestDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningRegisterResponseDto;
import com.proseg.msvc_transport.service.CleaningHistoryService;
import com.proseg.msvc_transport.service.CleaningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${routes.cleaning:/api/v1/transport/cleaning}")
@RequiredArgsConstructor
public class CleaningController {

    private final CleaningService cleaningService;
    private final CleaningHistoryService cleaningHistoryService;

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

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResponse<CleaningExecutionSummaryResponseDto>>> findHistory(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);
        return ApiResponseBuilder.ok(
                PageMapper.from(cleaningHistoryService.findAll(search, filters, pageable)),
                "Historial de depuraciones"
        );
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<ApiResponse<CleaningExecutionDetailViewResponseDto>> findHistoryById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(cleaningHistoryService.findById(id), "Detalle de depuración");
    }
}
