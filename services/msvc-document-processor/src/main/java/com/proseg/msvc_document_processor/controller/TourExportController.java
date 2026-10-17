package com.proseg.msvc_document_processor.controller;

import com.proseg.msvc_document_processor.export.ExportFormat;
import com.proseg.msvc_document_processor.export.TourDraftExport;
import com.proseg.msvc_document_processor.service.TourDraftExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/document-processor/exports/tours")
@RequiredArgsConstructor
public class TourExportController {

    private final TourDraftExportService tourDraftExportService;

    @GetMapping("/drafts/{draftId}")
    public ResponseEntity<byte[]> exportDraft(@PathVariable UUID draftId) {
        TourDraftExport export = tourDraftExportService.export(draftId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ExportFormat.XLSX.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(export.content());
    }
}
