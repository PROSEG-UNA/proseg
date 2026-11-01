package com.proseg.msvc_document_processor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.msvc_document_processor.dto.request.ConfirmRequestDto;
import com.proseg.msvc_document_processor.dto.response.PreviewSummaryDto;
import com.proseg.msvc_document_processor.dto.response.SummaryDto;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import com.proseg.msvc_document_processor.exception.GlobalExceptionHandler;
import com.proseg.msvc_document_processor.export.ExportFormat;
import com.proseg.msvc_document_processor.export.ExportResult;
import com.proseg.msvc_document_processor.service.DocumentExportService;
import com.proseg.msvc_document_processor.service.DocumentImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DocumentProcessorControllerTest {

    private static final String IMPORTS = "/api/v1/document-processor/imports";
    private static final String EXPORTS = "/api/v1/document-processor/exports";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private DocumentImportService importService;
    private DocumentExportService exportService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        importService = mock(DocumentImportService.class);
        exportService = mock(DocumentExportService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ImportController(importService),
                        new ExportController(exportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void preview_endpoint_returns_api_contract_and_propagates_file() throws Exception {
        var summary = PreviewSummaryDto.builder().totalRows(2).rows(List.of(Map.of("name", "Ana"))).build();
        when(importService.preview(eq("assets"), any())).thenReturn(summary);
        var file = new MockMultipartFile("file", "assets.xlsx", "application/vnd.ms-excel", new byte[]{1, 2});

        mockMvc.perform(multipart(IMPORTS + "/assets/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Previsualización procesada"))
                .andExpect(jsonPath("$.data.totalRows").value(2))
                .andExpect(jsonPath("$.data.rows[0].name").value("Ana"));
        verify(importService).preview(eq("assets"), any());
    }

    @Test
    void confirm_endpoint_deserializes_request_and_returns_summary() throws Exception {
        var summary = SummaryDto.builder().totalRows(1).created(1).cancelled(false).build();
        when(importService.confirm(eq("assets"), any())).thenReturn(summary);
        var request = ConfirmRequestDto.builder()
                .rows(List.of(Map.of("rowNumber", 2, "assetNumber", "A-1")))
                .approvedKeys(List.of("brand"))
                .build();

        mockMvc.perform(post(IMPORTS + "/assets/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Importación procesada"))
                .andExpect(jsonPath("$.data.created").value(1));
        verify(importService).confirm(eq("assets"), argThat(dto -> dto.getRows().size() == 1
                && dto.getApprovedKeys().contains("brand")));
    }

    @Test
    void template_endpoint_returns_xlsx_content_and_attachment_header() throws Exception {
        when(importService.generateTemplate("assets")).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get(IMPORTS + "/assets/template"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla-assets.xlsx\""))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void export_endpoint_uses_xlsx_default_filters_filename_and_body() throws Exception {
        when(exportService.export(eq("assets"), eq("xlsx"), any())).thenReturn(
                new ExportResult(ExportFormat.XLSX, new byte[]{4, 5}));

        mockMvc.perform(get(EXPORTS + "/assets")
                        .param("status", "ACTIVE")
                        .param("status", "INACTIVE")
                        .param("format", "xlsx")
                        .param("filename", "inventario.xlsx"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{4, 5}))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inventario.xlsx")));
        verify(exportService).export(eq("assets"), eq("xlsx"), argThat(params ->
                params.containsKey("status") && !params.containsKey("format") && !params.containsKey("filename")));
    }

    @Test
    void export_endpoint_sanitizes_custom_names_adds_extensions_and_defaults_name() throws Exception {
        when(exportService.export(eq("assets"), eq("csv"), any())).thenReturn(new ExportResult(ExportFormat.CSV, new byte[0]));
        mockMvc.perform(get(EXPORTS + "/assets").param("format", "csv").param("filename", "  a/b?.CSV  "))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("a_b_.CSV")));

        when(exportService.export(eq("tickets"), eq("xlsx"), any())).thenReturn(new ExportResult(ExportFormat.XLSX, new byte[0]));
        mockMvc.perform(get(EXPORTS + "/tickets"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, matchesPattern(
                        ".*export-tickets-\\d{8}-\\d{6}\\.xlsx.*")));
    }

    @Test
    void maps_domain_errors_and_invalid_json_to_http_responses() throws Exception {
        when(importService.generateTemplate("invalid")).thenThrow(DocumentProcessorException.unsupportedDocumentType("invalid"));
        mockMvc.perform(get(IMPORTS + "/invalid/template"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").value("UNSUPPORTED_DOCUMENT_TYPE"));

        mockMvc.perform(post(IMPORTS + "/assets/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void default_filename_and_filter_handling_accept_blank_custom_name() throws Exception {
        when(exportService.export(eq("assets"), eq("xlsx"), any())).thenReturn(new ExportResult(ExportFormat.XLSX, new byte[0]));
        mockMvc.perform(get(EXPORTS + "/assets").param("filename", "   "))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, matchesPattern(
                        ".*export-assets-\\d{8}-\\d{6}\\.xlsx.*")));
    }
}
