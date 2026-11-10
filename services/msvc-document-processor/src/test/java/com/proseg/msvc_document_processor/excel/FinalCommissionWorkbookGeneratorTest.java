package com.proseg.msvc_document_processor.excel;

import com.proseg.msvc_document_processor.dto.response.FinalCommissionReportDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class FinalCommissionWorkbookGeneratorTest {

    private XSSFWorkbook workbook;
    private FormulaEvaluator evaluator;

    @BeforeEach
    void setUp() throws IOException {
        byte[] content = new FinalCommissionWorkbookGenerator().generate(report());
        workbook = new XSSFWorkbook(new ByteArrayInputStream(content));
        evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        evaluator.evaluateAll();
    }

    @Test
    void createsTheSameSheetsAsTheOriginalWithTheDraftSheetHidden() {
        assertThat(sheetNames()).containsExactly("BORRADOR", "TOTALES", "R.INTERNOS", "R.EXTERNOS", "P.VEHICULO", "NO APROBADAS", "RESUMEN");
        assertThat(workbook.isSheetHidden(0)).isTrue();
        assertThat(workbook.getActiveSheetIndex()).isEqualTo(1);
        assertThat(text("TOTALES", "A1")).isEqualTo("TOTAL DE GIRAS SOLICITADAS CORRESPONDIENTES MAYO 2026");
        assertThat(text("R.INTERNOS", "A1")).isEqualTo("TOTAL DE GIRAS ATENDIDAS CON RECURSOS INTERNOS MAYO 2026");
        assertThat(text("TOTALES", "CZ3")).isEqualTo("SUBTOTAL / \nHORAS\nSOBRESUELDO");
        assertThat(workbook.getSheet("TOTALES").isColumnHidden(new CellReference("V1").getCol())).isTrue();
    }

    @Test
    void placesEachTourInItsCategorySheetGroupedByPriority() {
        assertThat(number("TOTALES", "B4")).isEqualTo(3901);
        assertThat(number("TOTALES", "B5")).isEqualTo(3902);
        assertThat(number("TOTALES", "B6")).isEqualTo(3903);
        assertThat(number("R.INTERNOS", "B4")).isEqualTo(3901);
        assertThat(number("R.EXTERNOS", "B4")).isEqualTo(3902);
        assertThat(number("NO APROBADAS", "B4")).isEqualTo(3903);
        assertThat(workbook.getSheet("P.VEHICULO").getRow(3) == null).isTrue();
        assertThat(workbook.getSheet("TOTALES").getMergedRegions().stream().map(region -> region.formatAsString()).toList()).contains("CZ4:CZ5");
    }

    @Test
    void computesSurplusAndOvertimeFromTheDaysOfEachTour() {
        assertThat(text("TOTALES", "M4")).isEqualTo("Viernes");
        assertThat(number("TOTALES", "R4")).isCloseTo(4.0, within(0.001));
        assertThat(number("TOTALES", "S4")).isCloseTo(6.5, within(0.001));
        assertThat(number("TOTALES", "R5")).isCloseTo(6.0, within(0.001));
        assertThat(number("TOTALES", "Q5")).isCloseTo(16.0, within(0.001));
        assertThat(number("TOTALES", "S6")).isCloseTo(4.0, within(0.001));
        assertThat(number("TOTALES", "CZ4")).isCloseTo(10.0, within(0.001));
    }

    @Test
    void summarizesCountsAndHoursByPriorityAndCategory() {
        assertThat(number("RESUMEN", "C6")).isEqualTo(1);
        assertThat(number("RESUMEN", "D6")).isEqualTo(1);
        assertThat(number("RESUMEN", "F8")).isEqualTo(1);
        assertThat(number("RESUMEN", "G16")).isEqualTo(3);
        assertThat(number("RESUMEN", "K6")).isCloseTo(4.0, within(0.001));
        assertThat(number("RESUMEN", "L6")).isCloseTo(6.5, within(0.001));
        assertThat(number("RESUMEN", "M6")).isCloseTo(6.0, within(0.001));
        assertThat(number("RESUMEN", "R8")).isCloseTo(4.0, within(0.001));
    }

    @Test
    void availableHoursAddUpEachActiveDriversOwnBusinessDaysAndMax() throws IOException {
        assertThat(hasNumber(16 * 28 * 8)).isTrue();
        assertThat(hasNumber(16 * 37.5)).isTrue();

        FinalCommissionReportDto custom = report();
        custom.setActiveDriversBusinessDays(16 * 28 - 7);
        custom.setActiveDriversSurplusCap(16 * 37.5 - 9.5);
        workbook = new XSSFWorkbook(new ByteArrayInputStream(new FinalCommissionWorkbookGenerator().generate(custom)));
        workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();

        assertThat(hasNumber((16 * 28 - 7) * 8)).isTrue();
        assertThat(hasNumber(16 * 37.5 - 9.5)).isTrue();
        assertThat(hasNumber(16 * 28 * 8)).isFalse();
        assertThat(hasNumber(16 * 37.5)).isFalse();
    }

    @Test
    void addsOtherUnitDriverSurplusAndOvertimeFromBorrowedVehicleTours() throws IOException {
        FinalCommissionReportDto custom = report();
        FinalCommissionReportDto.Tour weekday = tour("4012", 1, "BORROWED_VEHICLE", 1,
                LocalDate.of(2026, 5, 29), LocalTime.of(6, 0), LocalDate.of(2026, 5, 29), LocalTime.of(18, 0),
                List.of(day(LocalDate.of(2026, 5, 29), 6, 18)));
        FinalCommissionReportDto.Tour weekend = tour("4013", 1, "BORROWED_VEHICLE", 1,
                LocalDate.of(2026, 5, 30), LocalTime.of(8, 0), LocalDate.of(2026, 5, 30), LocalTime.of(12, 0),
                List.of(day(LocalDate.of(2026, 5, 30), 8, 12)));
        weekday.setOrdinaryStart(12);
        weekday.setOrdinaryEnd(21);
        weekend.setOrdinaryStart(12);
        weekend.setOrdinaryEnd(21);
        custom.setTours(List.of(weekday, weekend));
        workbook = new XSSFWorkbook(new ByteArrayInputStream(new FinalCommissionWorkbookGenerator().generate(custom)));
        evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        evaluator.evaluateAll();

        assertThat(number("P.VEHICULO", "R4")).isCloseTo(6, within(0.001));
        assertThat(number("P.VEHICULO", "S5")).isCloseTo(4, within(0.001));
        assertThat(number("TOTALES", "R4")).isCloseTo(6, within(0.001));
        assertThat(number("TOTALES", "S5")).isCloseTo(4, within(0.001));
        assertThat(number("RESUMEN", "O6")).isCloseTo(6, within(0.001));
        assertThat(number("RESUMEN", "P6")).isCloseTo(4, within(0.001));
    }

    private boolean hasNumber(double expected) {
        for (Sheet sheet : workbook) {
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                            && Math.abs(cell.getNumericCellValue() - expected) < 1e-9) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<String> sheetNames() {
        return java.util.stream.IntStream.range(0, workbook.getNumberOfSheets()).mapToObj(workbook::getSheetName).toList();
    }

    private Cell cell(String sheetName, String address) {
        Sheet sheet = workbook.getSheet(sheetName);
        CellReference reference = new CellReference(address);
        Row row = sheet.getRow(reference.getRow());
        return row.getCell(reference.getCol());
    }

    private String text(String sheetName, String address) {
        return cell(sheetName, address).getStringCellValue();
    }

    private double number(String sheetName, String address) {
        return cell(sheetName, address).getNumericCellValue();
    }

    private FinalCommissionReportDto report() {
        return FinalCommissionReportDto.builder()
                .year(2026)
                .month(5)
                .businessDays(28)
                .surplusCapPerDriver(37.5)
                .activeDrivers(16)
                .tours(List.of(
                        tour("3901", 1, "INTERNAL", 1, LocalDate.of(2026, 5, 29), LocalTime.of(13, 0), LocalDate.of(2026, 5, 30), LocalTime.of(14, 0),
                                List.of(day(LocalDate.of(2026, 5, 29), 13, 20.5), day(LocalDate.of(2026, 5, 30), 7.5, 14))),
                        tour("3902", 1, "EXTERNAL", 2, LocalDate.of(2026, 5, 4), LocalTime.of(6, 0), LocalDate.of(2026, 5, 4), LocalTime.of(18, 0),
                                List.of(day(LocalDate.of(2026, 5, 4), 6, 18))),
                        tour("3903", 3, "NOT_APPROVED", 1, LocalDate.of(2026, 5, 3), LocalTime.of(8, 0), LocalDate.of(2026, 5, 3), LocalTime.of(12, 0),
                                List.of(day(LocalDate.of(2026, 5, 3), 8, 12)))
                ))
                .build();
    }

    private FinalCommissionReportDto.Tour tour(String number, int priority, String category, int vehicles, LocalDate departureDate,
                                               LocalTime departureTime, LocalDate returnDate, LocalTime returnTime,
                                               List<FinalCommissionReportDto.Day> days) {
        return FinalCommissionReportDto.Tour.builder()
                .number(number)
                .passengers(20)
                .executingUnit("Escuela de Química")
                .responsible("Ana Pérez")
                .destination("Limón")
                .durationDays(days.size())
                .priority(priority)
                .departureDate(departureDate)
                .departureTime(departureTime)
                .returnDate(returnDate)
                .returnTime(returnTime)
                .category(category)
                .vehicles(vehicles)
                .ordinaryStart(7.5)
                .ordinaryEnd(16.5)
                .days(days)
                .build();
    }

    private FinalCommissionReportDto.Day day(LocalDate date, double start, double end) {
        return FinalCommissionReportDto.Day.builder().date(date).start(start).end(end).build();
    }
}
