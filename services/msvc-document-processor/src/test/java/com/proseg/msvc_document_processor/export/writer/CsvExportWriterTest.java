package com.proseg.msvc_document_processor.export.writer;

import com.proseg.msvc_document_processor.export.ExportColumnDefinition;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExportWriterTest {

    @Test
    void writes_utf8_bom_headers_rows_and_escapes_special_values() {
        var columns = List.of(
                new ExportColumnDefinition("name", "Nombre, completo", row -> row.get("name")),
                new ExportColumnDefinition("note", "Nota", row -> row.get("note")),
                new ExportColumnDefinition("null", "Vacío", row -> null));
        byte[] output = new CsvExportWriter().write(columns, List.of(
                Map.of("name", "Ana", "note", "dijo \"sí\"\nsegunda línea")));
        String csv = new String(output, StandardCharsets.UTF_8);

        assertThat(csv).startsWith("\uFEFF\"Nombre, completo\",Nota,Vacío\r\n");
        assertThat(csv).contains("Ana,\"dijo \"\"sí\"\"\nsegunda línea\",\r\n");
        assertThat(csv).endsWith("\r\n");
    }

    @Test
    void writes_header_when_no_rows_or_columns() {
        String headerOnly = new String(new CsvExportWriter().write(
                List.of(new ExportColumnDefinition("id", "ID", row -> row.get("id"))),
                List.of()), StandardCharsets.UTF_8);
        String noColumns = new String(new CsvExportWriter().write(List.of(), List.of(Map.of())), StandardCharsets.UTF_8);

        assertThat(headerOnly).isEqualTo("\uFEFFID\r\n");
        assertThat(noColumns).isEqualTo("\uFEFF\r\n\r\n");
    }

    @Test
    void quotes_commas_quotes_line_feeds_and_carriage_returns() {
        var columns = List.of(
                new ExportColumnDefinition("comma", "Comma", row -> "a,b"),
                new ExportColumnDefinition("quote", "Quote", row -> "a\"b"),
                new ExportColumnDefinition("lf", "LF", row -> "a\nb"),
                new ExportColumnDefinition("cr", "CR", row -> "a\rb"),
                new ExportColumnDefinition("plain", null, row -> null));
        String csv = new String(new CsvExportWriter().write(columns, List.of(Map.of())), StandardCharsets.UTF_8);

        assertThat(csv).contains("\"a,b\",\"a\"\"b\",\"a\nb\",\"a\rb\",");
        assertThat(csv).contains("Comma,Quote,LF,CR,");
    }
}
