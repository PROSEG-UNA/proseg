package com.proseg.msvc_document_processor.excel;

import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import com.proseg.msvc_document_processor.testutil.ExcelFixtures;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenericExcelParserTest {

    private final GenericExcelParser parser = new GenericExcelParser();

    @Test
    void parses_headers_rows_and_skips_blank_rows() {
        byte[] file = ExcelFixtures.workbook(
                new String[]{"  NOMBRE ", "Fecha", "Monto", "Cantidad", "Desconocida"},
                new Object[][]{
                        {"Ana", "2026-03-14", "12,50", 2, "dato"},
                        {null, null, null, null, null},
                        {"Luis", "14/03/2026", 20.25, 3, "dato"}
                });

        var result = parser.parse(new ByteArrayInputStream(file), ExcelFixtures.schema());

        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getRows()).hasSize(2);
        assertThat(result.getRows().get(0))
                .containsEntry("rowNumber", 2)
                .containsEntry("name", "Ana")
                .containsEntry("date", java.time.LocalDate.of(2026, 3, 14))
                .containsEntry("amount", new java.math.BigDecimal("12.50"))
                .containsEntry("count", new java.math.BigDecimal("2.0"));
        assertThat(result.getErrors()).singleElement()
                .extracting("reason")
                .asString()
                .contains("Desconocida");
    }

    @Test
    void returns_row_errors_without_losing_other_rows_and_uses_schema_aliases() {
        byte[] file = ExcelFixtures.workbook(
                new String[]{"Name", "Fecha", "Monto", "Cantidad"},
                new Object[][]{
                        {"Ana", "fecha inválida", "1.5", 1},
                        {"Luis", "2026-01-02", "2.5", 2}
                });

        var result = parser.parse(new ByteArrayInputStream(file), ExcelFixtures.schema());

        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getRows()).singleElement().satisfies(parsedRow ->
                assertThat(parsedRow).containsEntry("name", "Luis"));
        assertThat(result.getErrors()).singleElement().satisfies(issue -> {
            assertThat(issue.getRow()).isEqualTo(2);
            assertThat(issue.getReason()).contains("fecha");
        });
    }

    @Test
    void fails_when_workbook_is_corrupt_or_contains_no_sheet() {
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(new byte[]{1, 2, 3}), ExcelFixtures.schema()))
                .isInstanceOf(DocumentProcessorException.class)
                .hasMessageContaining("leer");
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(new byte[0]), ExcelFixtures.schema()))
                .isInstanceOf(DocumentProcessorException.class);
    }

    @Test
    void fails_when_first_row_is_missing() throws Exception {
        byte[] file;
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             var out = new java.io.ByteArrayOutputStream()) {
            workbook.createSheet("vacía");
            workbook.write(out);
            file = out.toByteArray();
        }

        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(file), ExcelFixtures.schema()))
                .isInstanceOf(DocumentProcessorException.class);
    }

    @Test
    void handles_empty_schema_and_unmapped_headers() {
        byte[] file = ExcelFixtures.workbook(new String[]{"Sin mapeo"}, new Object[][]{{"dato"}});

        var result = parser.parse(new ByteArrayInputStream(file),
                com.proseg.msvc_document_processor.dto.response.SchemaDto.builder().columns(null).build());

        assertThat(result.getTotalRows()).isZero();
        assertThat(result.getRows()).isEmpty();
        assertThat(result.getErrors()).hasSize(1);
    }

    @Test
    void handles_null_schema_empty_headers_and_columns_without_names() {
        byte[] file = ExcelFixtures.workbook(new String[]{"", "Campo"}, new Object[][]{{null, "valor"}});
        var withoutSchema = parser.parse(new ByteArrayInputStream(file), null);
        assertThat(withoutSchema.getErrors()).hasSize(1);
        assertThat(withoutSchema.getTotalRows()).isZero();

        var schemaWithoutNames = com.proseg.msvc_document_processor.dto.response.SchemaDto.builder()
                .columns(java.util.List.of(com.proseg.msvc_document_processor.dto.response.ColumnSchemaDto.builder()
                        .attribute("ignored").names(null).type("TEXT").build()))
                .build();
        var result = parser.parse(new ByteArrayInputStream(file), schemaWithoutNames);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getRows()).isEmpty();
    }

    @Test
    void parses_duplicate_headers_in_column_order_and_omits_blank_values() {
        var schema = com.proseg.msvc_document_processor.dto.response.SchemaDto.builder()
                .columns(java.util.List.of(com.proseg.msvc_document_processor.dto.response.ColumnSchemaDto.builder()
                        .attribute("name").names(java.util.List.of("Nombre", "NOMBRE")).type("TEXT").build()))
                .build();
        byte[] file = ExcelFixtures.workbook(
                new String[]{"Nombre", "NOMBRE"},
                new Object[][]{{"Ana", "Otra"}, {"Solo nombre", null}});

        var result = parser.parse(new ByteArrayInputStream(file), schema);

        assertThat(result.getRows()).hasSize(2);
        assertThat(result.getRows().get(0)).containsEntry("name", "Otra");
        assertThat(result.getRows().get(1)).containsEntry("name", "Solo nombre");
    }
}
