package com.proseg.msvc_document_processor.service;

import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import com.proseg.msvc_document_processor.export.*;
import com.proseg.msvc_document_processor.export.writer.CsvExportWriter;
import com.proseg.msvc_document_processor.export.writer.ExcelExportWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DocumentExportServiceTest {

    @Mock
    private DocumentExportTarget target;
    @Mock
    private CsvExportWriter csvWriter;
    @Mock
    private ExcelExportWriter excelWriter;

    private ExportProperties properties;
    private DocumentExportService service;

    @BeforeEach
    void setUp() {
        properties = new ExportProperties();
        properties.setPageSize(10);
        properties.setMaxRows(5);
        lenient().when(target.documentType()).thenReturn("assets");
        lenient().when(target.columns()).thenReturn(List.of());
        service = new DocumentExportService(List.of(target), csvWriter, excelWriter, properties);
    }

    @Test
    void exports_all_pages_as_csv_and_preserves_filters() {
        var row1 = Map.<String, Object>of("id", 1);
        var row2 = Map.<String, Object>of("id", 2);
        when(target.fetchPage(any())).thenReturn(new ExportPage(List.of(row1), false, 2))
                .thenReturn(new ExportPage(List.of(row2), true, 2));
        when(csvWriter.write(any(), any())).thenReturn(new byte[]{1});
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("size", "1");
        params.add("status", "A");
        params.add("status", "B");

        ExportResult result = service.export(" ASSETS ", " CSV ", params);

        assertThat(result.format()).isEqualTo(ExportFormat.CSV);
        assertThat(result.content()).containsExactly((byte) 1);
        ArgumentCaptor<MultiValueMap<String, String>> pages = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(target, times(2)).fetchPage(pages.capture());
        assertThat(pages.getAllValues().get(0).getFirst("page")).isEqualTo("0");
        assertThat(pages.getAllValues().get(1).getFirst("page")).isEqualTo("1");
        assertThat(pages.getAllValues().get(0).get("status")).containsExactly("A", "B");
        verify(csvWriter).write(eq(List.of()), eq(List.of(row1, row2)));
        verifyNoInteractions(excelWriter);
    }

    @Test
    void uses_default_xlsx_format_and_fallback_for_invalid_page_sizes() {
        when(target.fetchPage(any())).thenReturn(new ExportPage(List.of(), true, 0));
        when(excelWriter.write(any(), any())).thenReturn(new byte[]{2});
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("size", "bad");

        assertThat(service.export("assets", null, params).format()).isEqualTo(ExportFormat.XLSX);
        ArgumentCaptor<MultiValueMap<String, String>> page = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(target).fetchPage(page.capture());
        assertThat(page.getValue().getFirst("size")).isEqualTo("10");

        params.set("size", "0");
        service.export("assets", null, params);
        params.set("size", "-2");
        service.export("assets", null, params);
        params.put("size", List.of());
        service.export("assets", null, params);
        params.remove("size");
        properties.setPageSize(0);
        service.export("assets", null, params);
        verify(excelWriter, times(5)).write(any(), any());
    }

    @Test
    void stops_on_empty_page_and_enforces_minimum_maximum_row_limit() {
        when(target.fetchPage(any()))
                .thenReturn(new ExportPage(List.of(Map.of("id", 1)), false, 2))
                .thenReturn(new ExportPage(null, false, 2));
        when(csvWriter.write(any(), any())).thenReturn(new byte[0]);
        service.export("assets", "csv", new LinkedMultiValueMap<>());
        verify(target, times(2)).fetchPage(any());

        reset(target);
        when(target.documentType()).thenReturn("assets");
        when(target.fetchPage(any())).thenReturn(new ExportPage(
                List.of(Map.of("id", 1), Map.of("id", 2)), true, 2));
        clearInvocations(csvWriter);
        properties.setMaxRows(0);
        assertThatThrownBy(() -> service.export("assets", "csv", new LinkedMultiValueMap<>()))
                .isInstanceOf(DocumentProcessorException.class)
                .hasMessageContaining("máximo");
        verifyNoInteractions(csvWriter);
    }

    @Test
    void rejects_unknown_target_and_format_without_fetching_pages() {
        assertThatThrownBy(() -> service.export("missing", "csv", new LinkedMultiValueMap<>()))
                .isInstanceOf(DocumentProcessorException.class);
        assertThatThrownBy(() -> service.export(null, "csv", new LinkedMultiValueMap<>()))
                .isInstanceOf(DocumentProcessorException.class);
        assertThatThrownBy(() -> service.export("assets", "pdf", new LinkedMultiValueMap<>()))
                .isInstanceOf(DocumentProcessorException.class);
        verify(target, never()).fetchPage(any());
    }
}
