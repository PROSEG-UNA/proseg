package com.proseg.msvc_document_processor.config;

import com.proseg.msvc_document_processor.security.Privileges;
import com.proseg.msvc_document_processor.service.DocumentExportService;
import com.proseg.msvc_document_processor.service.DocumentImportService;
import com.proseg.msvc_document_processor.export.ExportFormat;
import com.proseg.msvc_document_processor.export.ExportResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        com.proseg.msvc_document_processor.controller.ImportController.class,
        com.proseg.msvc_document_processor.controller.ExportController.class
}, properties = "spring.cloud.config.enabled=false")
@Import(SecurityConfig.class)
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;
    @MockBean
    private DocumentImportService importService;
    @MockBean
    private DocumentExportService exportService;

    @Test
    void protects_import_routes_and_allows_only_import_authority() throws Exception {
        mockMvc.perform(get("/api/v1/document-processor/imports/assets/template"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/document-processor/imports/assets/template")
                        .with(jwt().authorities(() -> "LEER_ACTIVOS")))
                .andExpect(status().isForbidden());

        when(importService.generateTemplate("assets")).thenReturn(new byte[]{1});
        mockMvc.perform(get("/api/v1/document-processor/imports/assets/template")
                        .with(jwt().authorities(() -> Privileges.Activos.IMPORTAR)))
                .andExpect(status().isOk());

        var file = new org.springframework.mock.web.MockMultipartFile("file", "a.xlsx", "application/octet-stream", new byte[]{1});
        mockMvc.perform(multipart("/api/v1/document-processor/imports/assets/preview")
                        .file(file)
                        .with(jwt().authorities(() -> Privileges.Activos.EXPORTAR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void applies_distinct_export_privileges_per_document_type() throws Exception {
        when(exportService.export(any(), any(), any())).thenReturn(new ExportResult(ExportFormat.CSV, new byte[0]));

        mockMvc.perform(get("/api/v1/document-processor/exports/assets")
                        .with(jwt().authorities(() -> Privileges.Activos.IMPORTAR)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/document-processor/exports/assets")
                        .with(jwt().authorities(() -> Privileges.Activos.EXPORTAR)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/document-processor/exports/tickets")
                        .with(jwt().authorities(() -> Privileges.TicketsMantenimiento.LEER)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/document-processor/exports/tickets")
                        .with(jwt().authorities(() -> Privileges.SolicitudesMantenimiento.LEER)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/document-processor/exports/maintenance-requests")
                        .with(jwt().authorities(() -> Privileges.SolicitudesMantenimiento.LEER)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/document-processor/exports/maintenance-requests")
                        .with(jwt().authorities(() -> Privileges.TicketsMantenimiento.LEER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void permits_open_api_and_requires_authentication_for_other_routes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().is5xxServerError());
        mockMvc.perform(post("/api/v1/document-processor/imports/assets/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rows\":[]}"))
                .andExpect(status().isUnauthorized());
    }
}
