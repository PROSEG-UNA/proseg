package com.proseg.msvc_transport.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.common.api.util.PageMapper;
import com.proseg.common.specification.FilterConstants;
import com.proseg.msvc_transport.dto.request.AssignmentRequestDto;
import com.proseg.msvc_transport.dto.response.AssignmentGenerateResponseDto;
import com.proseg.msvc_transport.dto.response.AssignmentIntegrityCleanupResponseDto;
import com.proseg.msvc_transport.dto.response.AssignmentIntegrityReportDto;
import com.proseg.msvc_transport.dto.response.AssignmentResponseDto;
import com.proseg.msvc_transport.service.AssignmentService;
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
@RequestMapping("${routes.assignments:/api/v1/transport/assignments}")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService service;

    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentResponseDto>> create(@Valid @RequestBody AssignmentRequestDto request) {
        return ApiResponseBuilder.created(service.create(request), "Asignación creada correctamente");
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<AssignmentGenerateResponseDto>> generate() {
        return ApiResponseBuilder.ok(service.generateAssignments(), "Asignaciones generadas correctamente");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(service.findById(id), "Asignación obtenida correctamente");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AssignmentResponseDto>>> findAll(@RequestParam(required = false) String search, @RequestParam Map<String, String> allParams, @PageableDefault(size = 10, page = 0) Pageable pageable) {
        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);
        return ApiResponseBuilder.ok(PageMapper.from(service.findAll(search, filters, pageable)), "Lista de asignaciones");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponseDto>> update(@PathVariable UUID id, @Valid @RequestBody AssignmentRequestDto request) {
        return ApiResponseBuilder.ok(service.update(id, request), "Asignación actualizada correctamente");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponseBuilder.ok(null, "Asignación eliminada correctamente");
    }

    @GetMapping("/integrity/orphans")
    public ResponseEntity<ApiResponse<AssignmentIntegrityReportDto>> getIntegrityOrphans() {
        return ApiResponseBuilder.ok(service.getIntegrityReport(), "Reporte de referencias huérfanas generado correctamente");
    }

    @PostMapping("/integrity/orphans/cleanup")
    public ResponseEntity<ApiResponse<AssignmentIntegrityCleanupResponseDto>> cleanupIntegrityOrphans() {
        return ApiResponseBuilder.ok(service.cleanupOrphanReferences(), "Limpieza de referencias huérfanas ejecutada correctamente");
    }
}
