package com.proseg.msvc_document_processor.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.msvc_document_processor.dto.request.ConfirmRequestDto;
import com.proseg.msvc_document_processor.dto.response.PreviewSummaryDto;
import com.proseg.msvc_document_processor.dto.response.SummaryDto;
import com.proseg.msvc_document_processor.service.DocumentImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/document-processor/imports")
@RequiredArgsConstructor
public class ImportController {

    private final DocumentImportService documentImportService;

    @PostMapping(path = "/{documentType}/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PreviewSummaryDto>> previewDocument(
            @PathVariable String documentType,
            @RequestParam("file") MultipartFile file) {
        return ApiResponseBuilder.ok(
                documentImportService.preview(documentType, file),
                "Previsualización procesada"
        );
    }

    @PostMapping("/{documentType}/confirm")
    public ResponseEntity<ApiResponse<SummaryDto>> confirmDocument(
            @PathVariable String documentType,
            @RequestBody ConfirmRequestDto request) {
        return ApiResponseBuilder.ok(
                documentImportService.confirm(documentType, request),
                "Importación procesada"
        );
    }

    @GetMapping("/{documentType}/template")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable String documentType) {
        byte[] body = documentImportService.generateTemplate(documentType);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"plantilla-" + documentType + ".xlsx\"")
                .body(body);
    }
}
