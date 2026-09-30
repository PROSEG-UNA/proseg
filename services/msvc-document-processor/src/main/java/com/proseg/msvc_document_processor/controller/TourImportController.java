package com.proseg.msvc_document_processor.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.msvc_document_processor.dto.response.TourImportSummaryDto;
import com.proseg.msvc_document_processor.service.TourImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/document-processor/imports/tours")
@RequiredArgsConstructor
public class TourImportController {

    private final TourImportService tourImportService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TourImportSummaryDto>> importTours(@RequestParam("file") MultipartFile file) {
        return ApiResponseBuilder.ok(tourImportService.importFile(file), "Importación de giras procesada");
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla-giras.xlsx\"")
                .body(tourImportService.generateTemplate());
    }
}
