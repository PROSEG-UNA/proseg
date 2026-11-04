package com.proseg.msvc_document_processor.excel;

import com.proseg.msvc_document_processor.dto.response.OvertimeRequestReportDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class OvertimeRequestWorkbookGeneratorTest {

    private XSSFWorkbook workbook;
    private FormulaEvaluator evaluator;
    private Sheet projection;
    private Sheet summary;

    @BeforeEach
    void setUp() throws IOException {
        byte[] content = new OvertimeRequestWorkbookGenerator().generate(report());
        workbook = new XSSFWorkbook(new ByteArrayInputStream(content));
        evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        evaluator.evaluateAll();
        projection = workbook.getSheet(OvertimeRequestWorkbookGenerator.PROJECTION_SHEET);
        summary = workbook.getSheet(OvertimeRequestWorkbookGenerator.SUMMARY_SHEET);
    }

    @Test
    void createsProjectionAndSummarySheets() {
        assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
        assertThat(workbook.getSheetName(0)).isEqualTo("PROYECCION");
        assertThat(workbook.getSheetName(1)).isEqualTo("RESUMEN");
        assertThat(text(projection, 0, 0)).isEqualTo("PROYECCIÓN DE CUMPLIMIENTO DE SOBRESUELDO Y SOLICITUD DE HORAS EXTRA");
        assertThat(projection.getRow(1).getCell(0).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test
    void writesOneRowPerTourDayWithTourDataMergedOverItsDays() {
        int titleRow = rowWithText(projection, "1. ÁLVARO PARADA");
        int headerRow = titleRow + 2;
        int firstDay = headerRow + 2;

        assertThat(text(projection, headerRow, 0)).isEqualTo("Nº boleta\nde gira");
        assertThat(text(projection, headerRow, 4)).isEqualTo("FECHA");
        assertThat(text(projection, headerRow + 1, 6)).isEqualTo("Inicio");
        assertThat(projection.getMergedRegions()).contains(
                new CellRangeAddress(headerRow, headerRow + 1, 0, 0),
                new CellRangeAddress(headerRow, headerRow + 1, 4, 5),
                new CellRangeAddress(headerRow, headerRow, 6, 7),
                new CellRangeAddress(firstDay, firstDay + 1, 2, 2)
        );

        assertThat(number(projection, firstDay, 0)).isEqualTo(3622);
        assertThat(text(projection, firstDay, 2)).isEqualTo("Escuela de Química");
        assertThat(cachedText(projection, firstDay, 5)).isEqualTo("Viernes");
        assertThat(cachedText(projection, firstDay + 1, 5)).isEqualTo("Sábado");
        assertThat(number(projection, firstDay, 6)).isEqualTo(11.0);
        assertThat(number(projection, firstDay, 7)).isEqualTo(20.5);
        assertThat(number(projection, firstDay, 8)).isEqualTo(4.0);
        assertThat(projection.getRow(firstDay).getCell(9).getCellType()).isEqualTo(CellType.BLANK);
        assertThat(number(projection, firstDay + 1, 9)).isEqualTo(6.5);
        assertThat(projection.getSheetConditionalFormatting().getNumConditionalFormattings()).isEqualTo(2);
    }

    @Test
    void totalsEachDriverAndComparesAgainstTheSharedCap() {
        int totalRow = rowWithText(projection, "Nota: Cantidad de horas de sobresueldo correspondiente a los 28 días de mayo del 2026");

        assertThat(text(projection, totalRow, 7)).isEqualTo("TOTAL");
        assertThat(projection.getRow(totalRow).getCell(8).getCellFormula()).startsWith("SUM(");
        assertThat(cachedNumber(projection, totalRow, 8)).isEqualTo(4.0);
        assertThat(cachedNumber(projection, totalRow, 9)).isEqualTo(6.5);
        assertThat(cachedNumber(projection, totalRow + 1, 8)).isEqualTo(37.5);
        assertThat(cachedNumber(projection, totalRow + 1, 9)).isCloseTo(4.0 / 37.5, within(1e-9));

        int secondDriverTitle = rowWithText(projection, "2. ZACARÍAS MORA");
        int secondDriverTotal = secondDriverTitle + 6;
        assertThat(cachedNumber(projection, secondDriverTotal, 8)).isZero();
        assertThat(cachedNumber(projection, secondDriverTotal + 1, 9)).isZero();
    }

    @Test
    void summarizesTheWholeMonth() {
        assertThat(cachedNumber(projection, rowWithText(projection, "TOTAL DE HORAS DE TIEMPO EXTRAORDINARIO REQUERIDAS"), 8)).isEqualTo(8.0);
        assertThat(cachedNumber(projection, rowWithText(projection, "TOTAL DE HORAS SOBRESUELDO QUE SE EJECUTARÁN"), 8)).isEqualTo(4.0);
        assertThat(cachedNumber(projection, rowWithText(projection, "TOTAL DE HORAS SOBRESUELDO POR CUMPLIR"), 8)).isEqualTo(75.0);
        assertThat(number(projection, rowWithText(projection, "HORAS SOBRESUELDO POR CUMPLIR POR CHOFER"), 8)).isEqualTo(37.5);
        assertThat(cachedNumber(projection, rowWithText(projection, "DIFERENCIA EN HORAS SOBRESUELDO"), 8)).isEqualTo(71.0);
        assertThat(cachedNumber(projection, rowWithText(projection, "TOTAL DE HORAS SOBRESUELDO QUE SE EJECUTARÁN"), 14)).isEqualTo(1.5);
    }

    @Test
    void buildsTheSummarySheetFromProjectionFormulas() {
        assertThat(text(summary, 1, 1)).isEqualTo("NOMBRE");
        assertThat(summary.getRow(2).getCell(1).getCellFormula()).startsWith("+PROYECCION!A");
        assertThat(cachedText(summary, 2, 1)).isEqualTo("1. ÁLVARO PARADA");
        assertThat(cachedNumber(summary, 2, 3)).isEqualTo(4.0);
        assertThat(cachedNumber(summary, 2, 4)).isEqualTo(37.5);
        assertThat(cachedNumber(summary, 2, 5)).isEqualTo(6.5);
        assertThat(cachedText(summary, 3, 1)).isEqualTo("2. ZACARÍAS MORA");
        assertThat(text(summary, 4, 1)).isEqualTo("TOTALES");
        assertThat(cachedNumber(summary, 4, 4)).isEqualTo(75.0);
        assertThat(text(summary, 5, 0)).isEqualTo("% E.S.");
    }

    private OvertimeRequestReportDto report() {
        OvertimeRequestReportDto.TourBlock weekendTour = OvertimeRequestReportDto.TourBlock.builder()
                .number("03622")
                .priority(1)
                .executingUnit("Escuela de Química")
                .responsible("Ana Mora")
                .days(List.of(
                        day("2026-05-08", 11.0, 20.5, 4.0, 0.0),
                        day("2026-05-09", 7.5, 14.0, 0.0, 6.5)
                ))
                .build();
        OvertimeRequestReportDto.TourBlock overflowTour = OvertimeRequestReportDto.TourBlock.builder()
                .number("03700")
                .priority(2)
                .executingUnit("Escuela de Historia")
                .responsible("Luis Solís")
                .days(List.of(day("2026-05-12", 16.5, 18.0, 0.0, 1.5)))
                .build();
        return OvertimeRequestReportDto.builder()
                .year(2026)
                .month(5)
                .businessDays(28)
                .surplusCapPerDriver(37.5)
                .drivers(List.of(
                        OvertimeRequestReportDto.DriverBlock.builder().name("Álvaro Parada").surplusHours(4.0).overtimeHours(8.0)
                                .tours(List.of(weekendTour)).build(),
                        OvertimeRequestReportDto.DriverBlock.builder().name("Zacarías Mora").tours(List.of(overflowTour)).build()
                ))
                .build();
    }

    private OvertimeRequestReportDto.TourDay day(String date, double start, double end, double surplus, double overtime) {
        return OvertimeRequestReportDto.TourDay.builder()
                .date(LocalDate.parse(date))
                .start(start)
                .end(end)
                .surplusHours(surplus)
                .overtimeHours(overtime)
                .build();
    }

    private int rowWithText(Sheet sheet, String value) {
        for (Row row : sheet) {
            Cell cell = row.getCell(0);
            if (cell != null && cell.getCellType() == CellType.STRING && value.equals(cell.getStringCellValue())) {
                return row.getRowNum();
            }
        }
        throw new AssertionError("No row with text " + value);
    }

    private String text(Sheet sheet, int row, int column) {
        return sheet.getRow(row).getCell(column).getStringCellValue();
    }

    private double number(Sheet sheet, int row, int column) {
        return sheet.getRow(row).getCell(column).getNumericCellValue();
    }

    private String cachedText(Sheet sheet, int row, int column) {
        return evaluator.evaluate(sheet.getRow(row).getCell(column)).getStringValue();
    }

    private double cachedNumber(Sheet sheet, int row, int column) {
        return evaluator.evaluate(sheet.getRow(row).getCell(column)).getNumberValue();
    }
}
