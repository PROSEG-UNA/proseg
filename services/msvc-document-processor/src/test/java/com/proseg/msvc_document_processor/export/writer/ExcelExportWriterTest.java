package com.proseg.msvc_document_processor.export.writer;

import com.proseg.msvc_document_processor.export.ExportColumnDefinition;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelExportWriterTest {

    @Test
    void writes_header_and_rows_as_text_with_nulls_empty() throws Exception {
        byte[] result = new ExcelExportWriter().write(List.of(
                        new ExportColumnDefinition("id", "ID", row -> row.get("id")),
                        new ExportColumnDefinition("empty", "Vacío", row -> null)),
                List.of(Map.of("id", 42)));

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            var sheet = workbook.getSheet("Export");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("ID");
            assertThat(sheet.getRow(0).getCell(0).getCellStyle().getFont().getBold()).isTrue();
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("42");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEmpty();
        }
    }

    @Test
    void writes_empty_workbook_when_no_columns_or_rows() throws Exception {
        byte[] result = new ExcelExportWriter().write(List.of(), List.of());

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertThat(workbook.getSheetAt(0).getRow(0).getLastCellNum()).isEqualTo((short) -1);
        }
    }
}
