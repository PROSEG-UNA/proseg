package com.proseg.msvc_document_processor.controller;

import com.proseg.msvc_document_processor.export.DocumentFile;
import com.proseg.msvc_document_processor.export.ExportFormat;
import com.proseg.msvc_document_processor.service.OvertimeRequestExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/document-processor/exports/drivers")
@RequiredArgsConstructor
public class DriverExportController {

    private final OvertimeRequestExportService overtimeRequestExportService;

    @GetMapping("/overtime-request")
    public ResponseEntity<byte[]> exportOvertimeRequest(
            @RequestParam int year,
            @RequestParam int month
    ) {
        DocumentFile export = overtimeRequestExportService.export(year, month);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ExportFormat.XLSX.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(export.content());
    }
}
