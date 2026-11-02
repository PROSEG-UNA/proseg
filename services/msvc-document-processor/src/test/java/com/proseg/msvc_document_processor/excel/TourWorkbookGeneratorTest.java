package com.proseg.msvc_document_processor.excel;

import com.proseg.msvc_document_processor.dto.response.RowIssueDto;
import com.proseg.msvc_document_processor.excel.block.DailyBlockSheetParser;
import com.proseg.msvc_document_processor.excel.reader.TabularFile;
import com.proseg.msvc_document_processor.excel.reader.TabularFileReader;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TourWorkbookGeneratorTest {

    private final TourWorkbookGenerator generator = new TourWorkbookGenerator();

    @Test
    void generatesTheDailyBlockLayoutOfTheRealFile() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(generator.generateTemplate(TourSchemaFixture.schema())))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row titleRow = sheet.getRow(0);
            Row headerRow = sheet.getRow(1);

            assertThat(titleRow.getCell(0).getStringCellValue()).isEqualTo("{día} {número} de {mes} de {año}");
            assertThat(titleRow.getCell(12).getStringCellValue()).isEqualTo("Hora");
            assertThat(titleRow.getCell(14).getStringCellValue()).isEqualTo("Fecha");
            assertThat(headerValues(headerRow)).isEqualTo(TourSchemaFixture.REAL_FILE_HEADERS);
        }
    }

    @Test
    void appliesThePlainArialStyleOfTheRealFile() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(generator.generateTemplate(TourSchemaFixture.schema())))) {
            Sheet sheet = workbook.getSheetAt(0);
            CellStyle headerStyle = sheet.getRow(1).getCell(2).getCellStyle();
            int lastColumn = TourSchemaFixture.REAL_FILE_HEADERS.size() - 1;

            assertThat(workbook.getFontAt(headerStyle.getFontIndex()).getFontName()).isEqualTo("Arial");
            assertThat(workbook.getFontAt(headerStyle.getFontIndex()).getFontHeightInPoints()).isEqualTo((short) 10);
            assertThat(workbook.getFontAt(headerStyle.getFontIndex()).getBold()).isFalse();
            assertThat(headerStyle.getWrapText()).isTrue();
            assertThat(sheet.getDefaultRowHeightInPoints()).isEqualTo(15f);
            assertThat(sheet.getColumnWidth(0)).isGreaterThan(sheet.getColumnWidth(1));
            assertThat(sheet.getColumnWidth(1)).isEqualTo(sheet.getColumnWidth(lastColumn - 1));
            assertThat(sheet.getColumnWidth(lastColumn)).isGreaterThan(sheet.getColumnWidth(lastColumn - 1));
            assertThat(sheet.getColumnStyle(3).getDataFormatString()).isEqualTo("@");
            assertThat(sheet.getColumnStyle(12).getDataFormatString()).isEqualTo("hh:mm");
            assertThat(sheet.getColumnStyle(14).getDataFormatString()).isEqualTo("dd-mm-yyyy");
        }
    }

    @Test
    void generatedHeadersAreAcceptedByTheParser() {
        byte[] content = generator.generateTemplate(TourSchemaFixture.schema());

        TabularFile file = new TabularFileReader().read("plantilla-giras.xlsx", content);
        List<String> reasons = new DailyBlockSheetParser().parse(file.rows(), TourSchemaFixture.schema())
                .getErrors().stream()
                .map(RowIssueDto::getReason)
                .toList();

        assertThat(reasons).noneMatch(reason -> reason.startsWith("Columna") || reason.startsWith("Falta"));
    }

    @Test
    void writesEachBlockWithItsTitleHeadersAndTypedValues() throws IOException {
        List<TourSheetBlock> blocks = List.of(
                new TourSheetBlock("Sabado 2 de Mayo de 2026", List.of(
                        draftRow(3, null, "01900", "301-627", 32, "08:00:00", "17:00:00", "2026-05-02", "2026-05-02", null))),
                new TourSheetBlock("Domingo 3 de Mayo de 2026", List.of(
                        draftRow(9, "Jorge Francisco Mena", "03817", "Microbus", 10, "13:30:00", "18:00:00", "2026-05-03", "2026-05-04", "Viaje largo")))
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(generator.generate(TourSchemaFixture.schema(), blocks)))) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Sabado 2 de Mayo de 2026");
            assertThat(sheet.getRow(0).getCell(12).getStringCellValue()).isEqualTo("Hora");
            assertThat(sheet.getRow(0).getCell(14).getStringCellValue()).isEqualTo("Fecha");
            assertThat(headerValues(sheet.getRow(1))).isEqualTo(TourSchemaFixture.REAL_FILE_HEADERS);
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Domingo 3 de Mayo de 2026");
            assertThat(headerValues(sheet.getRow(4))).isEqualTo(TourSchemaFixture.REAL_FILE_HEADERS);
            assertThat(sheet.getLastRowNum()).isEqualTo(5);

            Row firstData = sheet.getRow(2);
            assertThat(firstData.getCell(2)).isNull();
            assertThat(firstData.getCell(3).getCellType()).isEqualTo(CellType.STRING);
            assertThat(firstData.getCell(3).getStringCellValue()).isEqualTo("01900");
            assertThat(firstData.getCell(5).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(firstData.getCell(5).getNumericCellValue()).isEqualTo(32d);
            assertThat(DateUtil.isCellDateFormatted(firstData.getCell(12))).isTrue();
            assertThat(formatter.formatCellValue(firstData.getCell(12))).isEqualTo("08:00");
            assertThat(DateUtil.isCellDateFormatted(firstData.getCell(14))).isTrue();
            assertThat(formatter.formatCellValue(firstData.getCell(14))).isEqualTo("02-05-2026");
            assertThat(firstData.getCell(16)).isNull();
            assertThat(firstData.getHeightInPoints()).isEqualTo(15f);
        }
    }

    @Test
    void exportedDraftIsReimportedWithTheSameValues() {
        List<TourSheetBlock> blocks = List.of(
                new TourSheetBlock("Miercoles 6 de Mayo de 2026", List.of(
                        draftRow(1, null, "01900", "Sedan", 22, "07:30:00", "22:15:00", "2026-05-06", "2026-05-06", null),
                        draftRow(2, "Juan Chacón", "04040", "301-627", 3, "05:00:00", "12:00:00", "2026-05-06", "2026-05-08", "Viajan académicos"))),
                new TourSheetBlock("Sabado 9 de Mayo de 2026", List.of(
                        draftRow(3, null, "04032", null, 16, "13:00:00", "17:45:00", "2026-05-09", "2026-05-09", null)))
        );

        TabularFile file = new TabularFileReader().read("mayo 2026_depurado.xlsx", generator.generate(TourSchemaFixture.schema(), blocks));
        ParseResult result = new DailyBlockSheetParser().parse(file.rows(), TourSchemaFixture.schema());

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getBlocksDetected()).isEqualTo(2);
        assertThat(result.getRows()).hasSize(3);
        Map<String, Object> second = result.getRows().get(1);
        assertThat(second.get("driver")).isEqualTo("Juan Chacón");
        assertThat(second.get("number")).isEqualTo("04040");
        assertThat(second.get("vehicle")).isEqualTo("301-627");
        assertThat(second.get("passengers")).isEqualTo(3);
        assertThat(second.get("executingUnit")).isEqualTo("Escuela De Ciencias Agrarias");
        assertThat(second.get("durationDays")).isEqualTo(2);
        assertThat(second.get("priority")).isEqualTo(1);
        assertThat(second.get("modality")).isEqualTo("TLG");
        assertThat(second.get("departureTime")).isEqualTo(LocalTime.of(5, 0));
        assertThat(second.get("returnTime")).isEqualTo(LocalTime.of(12, 0));
        assertThat(second.get("departureDate")).isEqualTo(LocalDate.of(2026, 5, 6));
        assertThat(second.get("returnDate")).isEqualTo(LocalDate.of(2026, 5, 8));
        assertThat(second.get("observations")).isEqualTo("Viajan académicos");
        assertThat(result.getRows().get(0)).doesNotContainKey("driver");
        assertThat(result.getRows().get(0).get("returnTime")).isEqualTo(LocalTime.of(22, 15));
        assertThat(result.getRows().get(2)).doesNotContainKey("vehicle");
        assertThat(result.getRows().get(2).get("returnTime")).isEqualTo(LocalTime.of(17, 45));
    }

    private Map<String, Object> draftRow(
            int rowNumber,
            String driver,
            String number,
            String vehicle,
            int passengers,
            String departureTime,
            String returnTime,
            String departureDate,
            String returnDate,
            String observations
    ) {
        Map<String, Object> row = new HashMap<>();
        row.put("rowNumber", rowNumber);
        row.put("driver", driver);
        row.put("number", number);
        row.put("vehicle", vehicle);
        row.put("passengers", passengers);
        row.put("executingUnit", "Escuela De Ciencias Agrarias");
        row.put("responsible", "Rafael Evelio Granados");
        row.put("destination", "Tabarcia");
        row.put("durationDays", 2);
        row.put("priority", 1);
        row.put("modality", "TLG");
        row.put("departureTime", departureTime);
        row.put("returnTime", returnTime);
        row.put("departureDate", departureDate);
        row.put("returnDate", returnDate);
        row.put("observations", observations);
        return row;
    }

    private List<String> headerValues(Row headerRow) {
        List<String> values = new ArrayList<>();
        for (Cell cell : headerRow) {
            values.add(cell.getStringCellValue());
        }
        return values;
    }
}
