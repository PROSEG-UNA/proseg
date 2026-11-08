package com.proseg.msvc_document_processor.service;

import com.proseg.msvc_document_processor.dto.request.ConfirmRequestDto;
import com.proseg.msvc_document_processor.dto.request.RowsRequestDto;
import com.proseg.msvc_document_processor.dto.response.ImportConfirmDto;
import com.proseg.msvc_document_processor.dto.response.ImportPreviewDto;
import com.proseg.msvc_document_processor.dto.response.PendingCreationDto;
import com.proseg.msvc_document_processor.dto.response.RowIssueDto;
import com.proseg.msvc_document_processor.dto.response.SchemaDto;
import com.proseg.msvc_document_processor.excel.ExcelTemplateGenerator;
import com.proseg.msvc_document_processor.excel.GenericExcelParser;
import com.proseg.msvc_document_processor.excel.ParseResult;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import com.proseg.msvc_document_processor.target.ImportTarget;
import com.proseg.msvc_document_processor.testutil.ExcelFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentImportServiceTest {

    @Mock
    private GenericExcelParser parser;
    @Mock
    private ExcelTemplateGenerator templateGenerator;
    @Mock
    private ImportTarget target;

    private DocumentImportService service;

    @BeforeEach
    void setUp() {
        when(target.documentType()).thenReturn("assets");
        service = new DocumentImportService(parser, templateGenerator, List.of(target));
    }

    @Test
    void generates_template_from_target_schema() {
        SchemaDto schema = ExcelFixtures.schema();
        byte[] expected = {1, 2, 3};
        when(target.fetchSchema()).thenReturn(schema);
        when(templateGenerator.generate(schema)).thenReturn(expected);

        assertThat(service.generateTemplate("assets")).isEqualTo(expected);
        verify(target).fetchSchema();
        verify(templateGenerator).generate(schema);
    }

    @Test
    void rejects_unknown_document_type_for_all_operations() {
        assertThatThrownBy(() -> service.generateTemplate("unknown"))
                .isInstanceOf(DocumentProcessorException.class)
                .hasMessageContaining("unknown");
        assertThatThrownBy(() -> service.preview("unknown", new MockMultipartFile("file", "a.xlsx", "x", new byte[]{1})))
                .isInstanceOf(DocumentProcessorException.class);
        assertThatThrownBy(() -> service.confirm("unknown", ConfirmRequestDto.builder().build()))
                .isInstanceOf(DocumentProcessorException.class);
        verifyNoInteractions(parser, templateGenerator);
        verify(target, never()).fetchSchema();
        verify(target, never()).preview(any());
    }

    @Test
    void rejects_empty_missing_name_and_unsupported_files() {
        assertThatThrownBy(() -> service.preview("assets", null))
                .isInstanceOf(DocumentProcessorException.class).hasMessageContaining("vacío");
        assertThatThrownBy(() -> service.preview("assets", new MockMultipartFile("file", "empty.xlsx", "x", new byte[0])))
                .isInstanceOf(DocumentProcessorException.class).hasMessageContaining("vacío");
        assertThatThrownBy(() -> service.preview("assets", new MockMultipartFile("file", null, "x", new byte[]{1})))
                .isInstanceOf(DocumentProcessorException.class).hasMessageContaining("extensión");
        assertThatThrownBy(() -> service.preview("assets", new MockMultipartFile("file", "data.csv", "text/csv", new byte[]{1})))
                .isInstanceOf(DocumentProcessorException.class).hasMessageContaining("extensión");
        verifyNoInteractions(parser);
        verify(target, never()).fetchSchema();
        verify(target, never()).preview(any());
    }

    @Test
    void previews_valid_excel_and_sends_parsed_rows_to_target() {
        SchemaDto schema = ExcelFixtures.schema();
        List<Map<String, Object>> rows = List.of(Map.of("rowNumber", 2, "name", "Ana"));
        when(target.fetchSchema()).thenReturn(schema);
        when(parser.parse(any(), eq(schema))).thenReturn(ParseResult.builder().totalRows(1).rows(rows).errors(List.of()).build());
        when(target.preview(any())).thenReturn(ImportPreviewDto.builder()
                .pendingCreations(List.of(PendingCreationDto.builder().key("type").build()))
                .errors(List.of())
                .build());
        var file = new MockMultipartFile("file", "datos.XLSX", "application/octet-stream", new byte[]{1});

        var result = service.preview("assets", file);

        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getRows()).isEqualTo(rows);
        assertThat(result.getPendingCreations()).hasSize(1);
        ArgumentCaptor<RowsRequestDto> request = ArgumentCaptor.forClass(RowsRequestDto.class);
        verify(target).preview(request.capture());
        assertThat(request.getValue().getRows()).isEqualTo(rows);
    }

    @Test
    void parser_errors_short_circuit_preview_and_are_sorted_with_blank_rows_first() {
        List<RowIssueDto> errors = new java.util.ArrayList<>(List.of(
                RowIssueDto.builder().row(4).reason("four").build(),
                RowIssueDto.builder().reason("header").build(),
                RowIssueDto.builder().row(2).reason("two").build()));
        when(target.fetchSchema()).thenReturn(ExcelFixtures.schema());
        when(parser.parse(any(), any())).thenReturn(ParseResult.builder().totalRows(2).rows(List.of()).errors(errors).build());

        var result = service.preview("assets", new MockMultipartFile("file", "data.xls", "x", new byte[]{1}));

        assertThat(result.getErrors()).extracting(RowIssueDto::getRow).containsExactly(null, 2, 4);
        assertThat(result.getRows()).isEmpty();
        assertThat(result.getPendingCreations()).isEmpty();
        verify(target, never()).preview(any());
    }

    @Test
    void remote_preview_errors_hide_parsed_rows_and_null_optional_lists_are_empty() {
        var rows = List.<Map<String, Object>>of(Map.of("rowNumber", 2));
        when(target.fetchSchema()).thenReturn(ExcelFixtures.schema());
        when(parser.parse(any(), any())).thenReturn(ParseResult.builder().totalRows(1).rows(rows).errors(List.of()).build());
        when(target.preview(any())).thenReturn(ImportPreviewDto.builder()
                .errors(List.of(RowIssueDto.builder().row(2).reason("duplicado").build()))
                .pendingCreations(null)
                .build());

        var result = service.preview("assets", new MockMultipartFile("file", "data.xlsx", "x", new byte[]{1}));

        assertThat(result.getRows()).isEmpty();
        assertThat(result.getErrors()).singleElement().extracting(RowIssueDto::getReason).isEqualTo("duplicado");
        assertThat(result.getPendingCreations()).isEmpty();
    }

    @Test
    void accepts_legacy_xls_and_handles_remote_preview_with_null_error_list() {
        var rows = List.<Map<String, Object>>of(Map.of("rowNumber", 2));
        when(target.fetchSchema()).thenReturn(ExcelFixtures.schema());
        when(parser.parse(any(), any())).thenReturn(ParseResult.builder().totalRows(1).rows(rows).errors(List.of()).build());
        when(target.preview(any())).thenReturn(ImportPreviewDto.builder().errors(null).pendingCreations(null).build());

        var result = service.preview("assets", new MockMultipartFile("file", "legacy.XLS", "application/octet-stream", new byte[]{1}));

        assertThat(result.getRows()).isEqualTo(rows);
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getPendingCreations()).isEmpty();
    }

    @Test
    void maps_input_stream_io_failure_to_unreadable_workbook() throws Exception {
        var file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("data.xlsx");
        when(file.getInputStream()).thenThrow(new IOException("read failed"));

        assertThatThrownBy(() -> service.preview("assets", file))
                .isInstanceOf(DocumentProcessorException.class)
                .hasMessageContaining("leer");
    }

    @Test
    void confirms_rows_and_sorts_errors_with_null_rows_counted_as_zero() {
        ConfirmRequestDto request = ConfirmRequestDto.builder().rows(List.of(Map.of("rowNumber", 1))).build();
        when(target.confirm(request)).thenReturn(ImportConfirmDto.builder()
                .created(1)
                .cancelled(true)
                .errors(List.of(
                        RowIssueDto.builder().row(3).reason("tres").build(),
                        RowIssueDto.builder().row(1).reason("uno").build()))
                .build());

        var result = service.confirm("assets", request);

        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getCreated()).isEqualTo(1);
        assertThat(result.isCancelled()).isTrue();
        assertThat(result.getErrors()).extracting(RowIssueDto::getRow).containsExactly(1, 3);

        ConfirmRequestDto emptyRequest = ConfirmRequestDto.builder().build();
        when(target.confirm(emptyRequest)).thenReturn(ImportConfirmDto.builder().errors(null).build());
        when(target.confirm(isNull())).thenReturn(ImportConfirmDto.builder().build());
        assertThat(service.confirm("assets", emptyRequest).getErrors()).isEmpty();
        assertThatThrownBy(() -> service.confirm("assets", null)).isInstanceOf(NullPointerException.class);
    }
}
