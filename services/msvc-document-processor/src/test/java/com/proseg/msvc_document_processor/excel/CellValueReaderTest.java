package com.proseg.msvc_document_processor.excel;

import com.proseg.msvc_document_processor.excel.exception.InvalidCellValueException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CellValueReaderTest {

    @Test
    void reads_and_normalizes_supported_string_values() throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var row = workbook.createSheet().createRow(0);
            var text = row.createCell(0);
            text.setCellValue("  hola  ");
            var integer = row.createCell(1);
            integer.setCellValue(12);
            var decimal = row.createCell(2);
            decimal.setCellValue(12.5);
            var bool = row.createCell(3);
            bool.setCellValue(true);
            var blank = row.createCell(4);
            blank.setCellValue(" ");
            var formula = row.createCell(5);
            formula.setCellFormula("1+1");

            assertThat(CellValueReader.readString(text)).isEqualTo("hola");
            assertThat(CellValueReader.readString(integer)).isEqualTo("12");
            assertThat(CellValueReader.readString(decimal)).isEqualTo("12.5");
            assertThat(CellValueReader.readString(bool)).isEqualTo("true");
            assertThat(CellValueReader.readString(blank)).isNull();
            assertThat(CellValueReader.readString(null)).isNull();
            assertThat(CellValueReader.readString(formula)).isEqualTo("0");
        }
    }

    @Test
    void reads_dates_from_formatted_cells_and_supported_text_formats() throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var row = workbook.createSheet().createRow(0);
            var numericDate = row.createCell(0);
            numericDate.setCellValue(java.sql.Date.valueOf("2026-03-14"));
            numericDate.getCellStyle().setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));
            var formulaDate = row.createCell(1);
            formulaDate.setCellFormula("A1");
            formulaDate.getCellStyle().setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));
            var textDate = row.createCell(2);
            textDate.setCellValue("14/03/2026");
            var shortTextDate = row.createCell(3);
            shortTextDate.setCellValue("4/3/2026");
            var isoDate = row.createCell(4);
            isoDate.setCellValue("2026-03-14");
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();

            assertThat(CellValueReader.readDate(numericDate, "Fecha")).isEqualTo(LocalDate.of(2026, 3, 14));
            assertThat(CellValueReader.readDate(formulaDate, "Fecha")).isEqualTo(LocalDate.of(2026, 3, 14));
            assertThat(CellValueReader.readDate(textDate, "Fecha")).isEqualTo(LocalDate.of(2026, 3, 14));
            assertThat(CellValueReader.readDate(shortTextDate, "Fecha")).isEqualTo(LocalDate.of(2026, 3, 4));
            assertThat(CellValueReader.readDate(isoDate, "Fecha")).isEqualTo(LocalDate.of(2026, 3, 14));
            assertThat(CellValueReader.readDate(null, "Fecha")).isNull();
            assertThat(CellValueReader.readDate(row.createCell(3), "Fecha")).isNull();
        }
    }

    @Test
    void rejects_unformatted_numeric_and_unrecognized_dates() throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var row = workbook.createSheet().createRow(0);
            var numeric = row.createCell(0);
            numeric.setCellValue(12);
            var text = row.createCell(1);
            text.setCellValue("31-12-2026");

            assertThatThrownBy(() -> CellValueReader.readDate(numeric, "Fecha"))
                    .isInstanceOf(InvalidCellValueException.class)
                    .hasMessageContaining("fecha inválido");
            assertThatThrownBy(() -> CellValueReader.readDate(text, "Fecha"))
                    .isInstanceOf(InvalidCellValueException.class)
                    .hasMessageContaining("no reconocido");
        }
    }

    @Test
    void reads_numeric_values_and_parses_localized_decimal_text() throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var row = workbook.createSheet().createRow(0);
            var numeric = row.createCell(0);
            numeric.setCellValue(19.25);
            var text = row.createCell(1);
            text.setCellValue("19,25");
            var blank = row.createCell(2);
            blank.setCellType(CellType.BLANK);

            assertThat(CellValueReader.readBigDecimal(numeric, "Monto")).isEqualByComparingTo("19.25");
            assertThat(CellValueReader.readBigDecimal(text, "Monto")).isEqualByComparingTo("19.25");
            assertThat(CellValueReader.readBigDecimal(null, "Monto")).isNull();
            assertThat(CellValueReader.readBigDecimal(blank, "Monto")).isNull();
        }
    }

    @Test
    void rejects_invalid_decimal_text() throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var cell = workbook.createSheet().createRow(0).createCell(0);
            cell.setCellValue("no-numérico");

            assertThatThrownBy(() -> CellValueReader.readBigDecimal(cell, "Monto"))
                    .isInstanceOf(InvalidCellValueException.class)
                    .hasMessageContaining("numérico inválido");
        }
    }
}
