package com.proseg.msvc_archive.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.ArchiveUploadInitResponseDto;
import com.proseg.common.api.response.ArchiveUploadPartResponseDto;
import com.proseg.common.api.response.ArchiveUploadResponseDto;
import com.proseg.common.api.response.PresignedUrlResponseDto;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.msvc_archive.service.ArchiveDownload;
import com.proseg.msvc_archive.service.ArchiveService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Paths;

@RestController
@RequestMapping("${routes.files:/api/v1/archive/files}")
public class ArchiveController {

    private final ArchiveService archiveService;

    @Value("${archive.max-file-size-mb:25}")
    private long maxFileSizeMb;

    @Value("${archive.max-chunk-size-mb:5}")
    private long maxChunkSizeMb;

    public ArchiveController(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    private long getMaxFileSizeBytes() {
        return maxFileSizeMb * 1024 * 1024;
    }

    private long getMaxChunkSizeBytes() {
        return maxChunkSizeMb * 1024 * 1024;
    }

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<ArchiveUploadInitResponseDto>> initiateMultipartUpload(
            @RequestParam(value = "filename", required = false) String filename,
            @RequestParam(value = "contentType", required = false) String contentType,
            @RequestParam(value = "objectName", required = false) String objectName,
            @RequestParam(value = "folder", required = false) String folder,
            @RequestParam(value = "totalSize", required = false) Long totalSize) {

        if (totalSize != null && totalSize > getMaxFileSizeBytes()) {
            throw new IllegalArgumentException(
                    "El archivo supera el tamaño máximo permitido de " + maxFileSizeMb + " MB.");
        }

        ArchiveUploadInitResponseDto response = archiveService.initiateMultipartUpload(
                filename,
                objectName,
                folder,
                contentType);

        return ApiResponseBuilder.created(response, "Carga por partes iniciada");
    }

    @PostMapping(value = "/part", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ArchiveUploadPartResponseDto>> uploadPart(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("objectName") String objectName,
            @RequestParam("partNumber") int chunkNumber,
            @RequestPart("file") MultipartFile chunk) {
        if (chunk.getSize() > getMaxChunkSizeBytes()) {
            throw new IllegalArgumentException(
                    "La parte supera el tamaño máximo permitido de " + maxChunkSizeMb + " MB.");
        }

        ArchiveUploadPartResponseDto response = archiveService.uploadPart(
                uploadId,
                objectName,
                chunkNumber,
                chunk);

        return ApiResponseBuilder.ok(response, "Parte subida correctamente");
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<ArchiveUploadResponseDto>> completeMultipartUpload(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("objectName") String objectName,
            @RequestParam("totalSize") long totalSize) {
        if (totalSize > getMaxFileSizeBytes()) {
            throw new IllegalArgumentException(
                    "El archivo supera el tamaño máximo permitido de " + maxFileSizeMb + " MB.");
        }

        ArchiveUploadResponseDto response = archiveService.completeMultipartUpload(
                uploadId,
                objectName,
                totalSize);

        return ApiResponseBuilder.ok(response, "Carga por partes completada");
    }

    @GetMapping("/presigned")
    public ResponseEntity<ApiResponse<PresignedUrlResponseDto>> getPresignedUrl(
            @RequestParam("objectName") String objectName) {
        PresignedUrlResponseDto response = new PresignedUrlResponseDto();
        response.setUrl(archiveService.getPresignedGetUrl(objectName));
        return ApiResponseBuilder.ok(response, "URL firmada generada");
    }

    @GetMapping("/**")
    public ResponseEntity<InputStreamResource> download(
            HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatch = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String objectName = new AntPathMatcher().extractPathWithinPattern(bestMatch, path);

        ArchiveDownload download = archiveService.download(objectName);
        String filename = Paths.get(download.objectName()).getFileName().toString();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(new InputStreamResource(download.stream()));
    }
}
