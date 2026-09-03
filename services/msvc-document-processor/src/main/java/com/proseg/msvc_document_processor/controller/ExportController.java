package com.proseg.msvc_document_processor.controller;

import com.proseg.msvc_document_processor.export.ExportResult;
import com.proseg.msvc_document_processor.service.DocumentExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/document-processor/exports")
@RequiredArgsConstructor
public class ExportController {

    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DocumentExportService documentExportService;

    @GetMapping("/{documentType}")
    public ResponseEntity<byte[]> exportDocument(
            @PathVariable String documentType,
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) String filename,
            @RequestParam MultiValueMap<String, String> allParams) {

        MultiValueMap<String, String> exportFilters = new LinkedMultiValueMap<>(allParams);
        exportFilters.remove("format");
        exportFilters.remove("filename");

        ExportResult exportResult = documentExportService.export(documentType, format, exportFilters);

        String resolvedName = resolveFilename(documentType, filename, exportResult.format().getFileExtension());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(exportResult.format().getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(resolvedName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(exportResult.content());
    }

    private String resolveFilename(String documentType, String customName, String extension) {
        String baseName = customName != null && !customName.isBlank()
                ? customName.trim()
                : "export-" + documentType + "-" + FILE_DATE_FORMATTER.format(LocalDateTime.now());
        String safeName = baseName.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (safeName.toLowerCase().endsWith("." + extension)) {
            return safeName;
        }
        return safeName + "." + extension;
    }
}
