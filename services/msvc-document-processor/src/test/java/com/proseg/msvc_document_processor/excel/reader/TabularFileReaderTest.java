package com.proseg.msvc_document_processor.excel.reader;

import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TabularFileReaderTest {

    private final TabularFileReader reader = new TabularFileReader();

    @Test
    void readsHtmlDisguisedAsXlsWithMixedEncoding() {
        byte[] html = concat(
                "\t\t\t\t<html><head><meta http-equiv=Content-Type content=\"text/html; charset=us-ascii\"></head><body><table>"
                        .getBytes(StandardCharsets.US_ASCII),
                "<tr><td>Ana Zu".getBytes(StandardCharsets.US_ASCII),
                "ñ".getBytes(Charset.forName("windows-1252")),
                "iga</td><td colspan=2>acad".getBytes(StandardCharsets.US_ASCII),
                "émicos".getBytes(StandardCharsets.UTF_8),
                "</td><td>x&nbsp;y</td><tr><td>segunda</td></table></body></html>".getBytes(StandardCharsets.US_ASCII)
        );

        TabularFile file = reader.read("1-mayo.xls", html);

        assertThat(file.format()).isEqualTo(SpreadsheetFormat.HTML);
        assertThat(file.rows()).hasSize(2);
        SheetRow first = file.rows().get(0);
        assertThat(first.rowNumber()).isEqualTo(1);
        assertThat(first.cell(0).text()).isEqualTo("Ana Zuñiga");
        assertThat(first.cell(1).text()).isEqualTo("académicos");
        assertThat(first.cell(2).isBlank()).isTrue();
        assertThat(first.cell(3).text()).isEqualTo("x y");
        assertThat(file.rows().get(1).rowNumber()).isEqualTo(2);
    }

    @Test
    void readsXlsxKeepingFormattedTextAndDates() throws IOException {
        byte[] content;
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            content = workbookWithSampleRow(workbook);
        }

        TabularFile file = reader.read("giras.xlsx", content);

        assertThat(file.format()).isEqualTo(SpreadsheetFormat.XLSX);
        assertSampleRow(file.rows());
    }

    @Test
    void readsLegacyXls() throws IOException {
        byte[] content;
        try (HSSFWorkbook workbook = new HSSFWorkbook()) {
            content = workbookWithSampleRow(workbook);
        }

        TabularFile file = reader.read("giras.xls", content);

        assertThat(file.format()).isEqualTo(SpreadsheetFormat.XLS);
        assertSampleRow(file.rows());
    }

    @Test
    void readsCsvWithSemicolonMultilineFieldsAndWindows1252() {
        String csv = "Numero;Chofer;Observaciones\r\n04504;Erick Ramírez;\"línea uno\r\nlínea dos\"\r\n\r\n01900;\"Con ; punto y coma\";\r\n";
        byte[] content = csv.getBytes(Charset.forName("windows-1252"));

        TabularFile file = reader.read("giras.csv", content);

        assertThat(file.format()).isEqualTo(SpreadsheetFormat.CSV);
        assertThat(file.rows()).hasSize(4);
        assertThat(file.rows().get(1).cell(0).text()).isEqualTo("04504");
        assertThat(file.rows().get(1).cell(1).text()).isEqualTo("Erick Ramírez");
        assertThat(file.rows().get(1).cell(2).text()).isEqualTo("línea uno línea dos");
        assertThat(file.rows().get(2).isBlank()).isTrue();
        assertThat(file.rows().get(3).rowNumber()).isEqualTo(4);
        assertThat(file.rows().get(3).cell(1).text()).isEqualTo("Con ; punto y coma");
    }

    @Test
    void rejectsUnknownBinaryContent() {
        byte[] content = {0x00, 0x01, 0x02, 0x03};

        assertThatThrownBy(() -> reader.read("giras.xls", content))
                .isInstanceOf(DocumentProcessorException.class)
                .hasMessage("El contenido del archivo no corresponde a un Excel, un CSV ni una tabla HTML");
    }

    @Test
    void rejectsUnsupportedExtension() {
        assertThatThrownBy(() -> reader.read("giras.pdf", new byte[]{1}))
                .isInstanceOf(DocumentProcessorException.class)
                .hasMessage("El archivo debe tener extensión .xlsx, .xls, .xlsm o .csv");
    }

    private byte[] workbookWithSampleRow(Workbook workbook) throws IOException {
        Sheet sheet = workbook.createSheet("Prueba");
        Row row = sheet.createRow(2);
        CellStyle zeroPadded = workbook.createCellStyle();
        zeroPadded.setDataFormat(workbook.createDataFormat().getFormat("00000"));
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(workbook.createDataFormat().getFormat("dd-mm-yyyy"));
        row.createCell(0).setCellValue(4504);
        row.getCell(0).setCellStyle(zeroPadded);
        row.createCell(1).setCellValue(LocalDate.of(2026, 5, 1));
        row.getCell(1).setCellStyle(dateStyle);
        row.createCell(2).setCellValue("Merced");
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void assertSampleRow(List<SheetRow> rows) {
        assertThat(rows).hasSize(1);
        SheetRow row = rows.get(0);
        assertThat(row.rowNumber()).isEqualTo(3);
        assertThat(row.cell(0).text()).isEqualTo("04504");
        assertThat(row.cell(0).numericValue()).isEqualTo(4504d);
        assertThat(row.cell(1).dateValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(row.cell(2).text()).isEqualTo("Merced");
    }

    private byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }
}
