package com.proseg.msvc_archive.controller;

import com.proseg.common.api.response.ArchiveUploadInitResponseDto;
import com.proseg.common.api.response.ArchiveUploadPartResponseDto;
import com.proseg.common.api.response.ArchiveUploadResponseDto;
import com.proseg.msvc_archive.exception.ArchiveException;
import com.proseg.msvc_archive.exception.GlobalExceptionHandler;
import com.proseg.msvc_archive.service.ArchiveDownload;
import com.proseg.msvc_archive.service.ArchiveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArchiveControllerTest {

    private static final String BASE = "/api/v1/archive/files";

    private ArchiveService archiveService;
    private ArchiveController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        archiveService = mock(ArchiveService.class);
        controller = new ArchiveController(archiveService);
        ReflectionTestUtils.setField(controller, "maxFileSizeMb", 25L);
        ReflectionTestUtils.setField(controller, "maxChunkSizeMb", 5L);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addPlaceholderValue("routes.files", BASE)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /initiate: devuelve 201 con datos de inicio")
    void initiate_devuelveCreated() throws Exception {
        ArchiveUploadInitResponseDto dto = new ArchiveUploadInitResponseDto();
        dto.setUploadId(UUID.randomUUID().toString());
        dto.setObjectName("docs/foto.png");
        dto.setContentType("image/png");

        when(archiveService.initiateMultipartUpload(eq("foto.png"), eq("docs/foto.png"), eq("docs"), eq("image/png")))
                .thenReturn(dto);

        mockMvc.perform(post(BASE + "/initiate")
                        .param("filename", "foto.png")
                        .param("objectName", "docs/foto.png")
                        .param("folder", "docs")
                        .param("contentType", "image/png"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.objectName").value("docs/foto.png"))
                .andExpect(jsonPath("$.data.uploadId").value(dto.getUploadId()));
    }

    @Test
    @DisplayName("POST /part: sube multipart y responde 200")
    void uploadPart_devuelveOk() throws Exception {
        String uploadId = UUID.randomUUID().toString();
        ArchiveUploadPartResponseDto dto = new ArchiveUploadPartResponseDto();
        dto.setUploadId(uploadId);
        dto.setObjectName("docs/archivo.bin");
        dto.setPartNumber(1);
        dto.setSize(4L);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "part1.bin",
                "application/octet-stream",
                new byte[]{1, 2, 3, 4}
        );

        when(archiveService.uploadPart(eq(uploadId), eq("docs/archivo.bin"), eq(1), any()))
                .thenReturn(dto);

        mockMvc.perform(multipart(BASE + "/part")
                        .file(file)
                        .param("uploadId", uploadId)
                        .param("objectName", "docs/archivo.bin")
                        .param("partNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partNumber").value(1))
                .andExpect(jsonPath("$.data.size").value(4));
    }

    @Test
    @DisplayName("POST /complete: completa carga y responde 200")
    void complete_devuelveOk() throws Exception {
        String uploadId = UUID.randomUUID().toString();
        ArchiveUploadResponseDto dto = new ArchiveUploadResponseDto();
        dto.setObjectName("docs/archivo.bin");
        dto.setBucket("test-bucket");
        dto.setSize(100L);

        when(archiveService.completeMultipartUpload(uploadId, "docs/archivo.bin", 100L))
                .thenReturn(dto);

        mockMvc.perform(post(BASE + "/complete")
                        .param("uploadId", uploadId)
                        .param("objectName", "docs/archivo.bin")
                        .param("totalSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bucket").value("test-bucket"))
                .andExpect(jsonPath("$.data.size").value(100));
    }

    @Test
    @DisplayName("GET /presigned: devuelve URL firmada")
    void presigned_devuelveOk() throws Exception {
        when(archiveService.getPresignedGetUrl("docs/archivo.bin"))
                .thenReturn("https://minio.example/signed");

        mockMvc.perform(get(BASE + "/presigned")
                        .param("objectName", "docs/archivo.bin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("https://minio.example/signed"));
    }

    @Test
    @DisplayName("GET /**: descarga archivo con headers")
    void download_devuelveStream() throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[]{9, 8});
        ArchiveDownload download = new ArchiveDownload(
                "docs/archivo.pdf",
                "application/pdf",
                2L,
                stream
        );

        when(archiveService.download("docs/archivo.pdf")).thenReturn(download);

        mockMvc.perform(get(BASE + "/docs/archivo.pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"archivo.pdf\""))
                .andExpect(header().string("Cache-Control", "private, max-age=3600"));
    }

    @Test
    @DisplayName("ArchiveException en servicio: responde status y errorCode del dominio")
    void initiate_archiveException_devuelveBadRequest() throws Exception {
        when(archiveService.initiateMultipartUpload(anyString(), isNull(), isNull(), isNull()))
                .thenThrow(ArchiveException.invalidObjectName());

        mockMvc.perform(post(BASE + "/initiate")
                        .param("filename", "x.png"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("ARCHIVE_INVALID_OBJECT_NAME"));
    }
}
