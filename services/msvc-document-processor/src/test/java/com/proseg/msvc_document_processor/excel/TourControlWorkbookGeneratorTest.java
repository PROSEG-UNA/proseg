package com.proseg.msvc_document_processor.excel;

import com.proseg.msvc_document_processor.dto.response.TourControlReportDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TourControlWorkbookGeneratorTest {

    private XSSFWorkbook workbook;

    @BeforeEach
    void setUp() throws IOException {
        byte[] content = new TourControlWorkbookGenerator().generate(report());
        workbook = new XSSFWorkbook(new ByteArrayInputStream(content));
        workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
    }

    @Test
    void createsTheSameSheetsAsTheOriginalWithOneBlockPerDay() {
        assertThat(IntStream.range(0, workbook.getNumberOfSheets()).mapToObj(workbook::getSheetName).toList())
                .containsExactly("HRS.EXTRAOFICIAL", "Hoja2", "Hoja1");
        assertThat(text("HRS.EXTRAOFICIAL", "A2")).isEqualTo("DISTRIBUCION DE GIRAS MAYO 2026");
        assertThat(text("HRS.EXTRAOFICIAL", "D4")).isEqualTo("VIERNES");
        assertThat(text("HRS.EXTRAOFICIAL", "D9")).isEqualTo("SABADO");
        assertThat(text("HRS.EXTRAOFICIAL", "B5")).isEqualTo("AJUSTE");
        assertThat(workbook.getSheet("HRS.EXTRAOFICIAL").isColumnHidden(new CellReference("Q1").getCol())).isTrue();
    }

    @Test
    void writesEachDriverTourInTheDriverRowAndKeepsTheSystemHours() {
        assertThat(text("HRS.EXTRAOFICIAL", "G11")).isEqualTo("ANA PÉREZ");
        assertThat(number("HRS.EXTRAOFICIAL", "F11")).isEqualTo(617);
        assertThat(number("HRS.EXTRAOFICIAL", "N11")).isEqualTo(3625);
        assertThat(number("HRS.EXTRAOFICIAL", "C11")).isCloseTo(13.5, within(0.001));
        assertThat(number("HRS.EXTRAOFICIAL", "C25")).isCloseTo(1.5, within(0.001));
        assertThat(text("HRS.EXTRAOFICIAL", "G26")).isEqualTo("ANA PÉREZ");
        assertThat(number("HRS.EXTRAOFICIAL", "B26")).isCloseTo(-0.5, within(0.001));
        assertThat(number("HRS.EXTRAOFICIAL", "C26")).isCloseTo(2.0, within(0.001));
        assertThat(text("HRS.EXTRAOFICIAL", "G27")).isEqualTo("ZOE MORA");
        assertThat(number("HRS.EXTRAOFICIAL", "C27")).isCloseTo(2.0, within(0.001));
        assertThat(number("HRS.EXTRAOFICIAL", "C12")).isZero();
    }

    @Test
    void listsOtherUnitAndUnattendedToursBelowTheDrivers() {
        assertThat(number("HRS.EXTRAOFICIAL", "F14")).isEqualTo(414);
        assertThat(text("HRS.EXTRAOFICIAL", "G14")).isEqualTo("JORGE MENA");
        assertThat(text("HRS.EXTRAOFICIAL", "H16")).isEqualTo("San Francisco");
        assertThat(number("HRS.EXTRAOFICIAL", "N16")).isEqualTo(3531);
    }

    @Test
    void summarizesSurplusAndOvertimePerDriver() {
        int summary = rowOf("HRS.EXTRAOFICIAL", 6, "NOMBRE") + 1;
        assertThat(text("HRS.EXTRAOFICIAL", "G" + summary)).isEqualTo("ANA PÉREZ");
        assertThat(number("HRS.EXTRAOFICIAL", "J" + summary)).isCloseTo(3.5, within(0.001));
        assertThat(number("HRS.EXTRAOFICIAL", "H" + summary)).isCloseTo(3.0, within(0.001));
        assertThat(number("HRS.EXTRAOFICIAL", "I" + summary)).isCloseTo(14.0, within(0.001));
        assertThat(number("HRS.EXTRAOFICIAL", "L" + summary)).isCloseTo(37.5, within(0.001));
        assertThat(number("HRS.EXTRAOFICIAL", "H" + (summary + 1))).isCloseTo(3.0, within(0.001));
        assertThat(number("HRS.EXTRAOFICIAL", "D11")).isCloseTo(3.0, within(0.001));
    }

    @Test
    void usesEachDriversOwnBusinessDaysForTheirSurplusMax() throws IOException {
        TourControlReportDto custom = report();
        custom.getDrivers().get(1).setBusinessDays(21);
        workbook = new XSSFWorkbook(new ByteArrayInputStream(new TourControlWorkbookGenerator().generate(custom)));
        workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();

        int summary = rowOf("HRS.EXTRAOFICIAL", 6, "NOMBRE") + 1;

        assertThat(number("HRS.EXTRAOFICIAL", "N" + summary)).isEqualTo(28);
        assertThat(number("HRS.EXTRAOFICIAL", "L" + summary)).isCloseTo(37.5, within(0.001));
        assertThat(number("HRS.EXTRAOFICIAL", "N" + (summary + 1))).isEqualTo(21);
        assertThat(number("HRS.EXTRAOFICIAL", "L" + (summary + 1))).isCloseTo(28.0, within(0.001));
    }

    @Test
    void addsBlocksForDaysAfterTheMonthWhenToursContinue() {
        int lastBlock = rowOf("HRS.EXTRAOFICIAL", 6, "NOMBRE") - 1;
        while (cell("HRS.EXTRAOFICIAL", "A" + lastBlock) == null || !"FECHA DE ".equals(cellText("A" + lastBlock))) {
            lastBlock--;
        }
        assertThat(text("HRS.EXTRAOFICIAL", "D" + lastBlock)).isEqualTo("LUNES");
        assertThat(number("HRS.EXTRAOFICIAL", "N" + (lastBlock + 3))).isEqualTo(3522);
    }

    @Test
    void listsExternalToursAndWorkedWeekends() {
        assertThat(text("Hoja2", "B2")).isEqualTo("HB4085");
        assertThat(text("Hoja2", "C2")).isEqualTo("TRANSVANU");
        assertThat(text("Hoja2", "C3")).isEqualTo("CONTRATAR");
        assertThat(text("Hoja1", "A2")).isEqualTo("FINES DE SEMANA");
        assertThat(number("Hoja1", "B4")).isEqualTo(1);
        assertThat(number("Hoja1", "G4")).isEqualTo(1);
        assertThat(number("Hoja1", "G5")).isZero();
    }

    private int rowOf(String sheetName, int column, String value) {
        Sheet sheet = workbook.getSheet(sheetName);
        for (Row row : sheet) {
            Cell cell = row.getCell(column);
            if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING && value.equals(cell.getStringCellValue())) {
                return row.getRowNum() + 1;
            }
        }
        throw new AssertionError(value + " not found");
    }

    private Cell cell(String sheetName, String address) {
        CellReference reference = new CellReference(address);
        Row row = workbook.getSheet(sheetName).getRow(reference.getRow());
        return row == null ? null : row.getCell(reference.getCol());
    }

    private String cellText(String address) {
        Cell cell = cell("HRS.EXTRAOFICIAL", address);
        return cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING ? cell.getStringCellValue() : "";
    }

    private String text(String sheetName, String address) {
        return cell(sheetName, address).getStringCellValue();
    }

    private double number(String sheetName, String address) {
        return cell(sheetName, address).getNumericCellValue();
    }

    private TourControlReportDto report() {
        return TourControlReportDto.builder()
                .year(2026)
                .month(5)
                .businessDays(28)
                .weekdays(21)
                .drivers(List.of(
                        TourControlReportDto.Driver.builder().name("Ana Pérez").ordinaryStart(7.5).ordinaryEnd(16.5)
                                .surplusHours(3.0).overtimeHours(14.0).build(),
                        TourControlReportDto.Driver.builder().name("Zoe Mora").ordinaryStart(11).ordinaryEnd(20).build()))
                .entries(List.of(
                        driver(0, "2026-05-02", 5, 18.5, 13.5, "3625"),
                        driver(0, "2026-05-04", 15, 19, 2.0, "3700"),
                        driver(0, "2026-05-04", 6, 14.5, 1.5, "3701"),
                        driver(1, "2026-05-04", 12, 22, 2.0, "3796"),
                        driver(1, "2026-06-01", 7.5, 17.5, 1.0, "3522"),
                        entry("OTHER_UNIT", "2026-05-02", "Jorge Mena", "301-414", "4040"),
                        entry("EXTERNAL", "2026-05-06", "TRANSVANU", "HB4085", "3471"),
                        entry("EXTERNAL", "2026-05-06", null, null, "3989"),
                        entry("UNATTENDED", "2026-05-02", null, null, "3531")))
                .build();
    }

    private TourControlReportDto.Entry driver(int index, String date, double start, double end, double hours, String number) {
        return TourControlReportDto.Entry.builder().section("DRIVER").date(LocalDate.parse(date)).driverIndex(index)
                .plate("301-617").destination("Tayutic").responsible("Selvin Fallas").departureDate(LocalDate.parse(date))
                .returnDate(LocalDate.parse(date)).start(start).end(end).passengers(3).number(number).priority(1).hours(hours).build();
    }

    private TourControlReportDto.Entry entry(String section, String date, String driver, String plate, String number) {
        return TourControlReportDto.Entry.builder().section(section).date(LocalDate.parse(date)).driver(driver).plate(plate)
                .destination("San Francisco").responsible("Ana Chaves").departureDate(LocalDate.parse(date))
                .returnDate(LocalDate.parse(date)).start(7.5).end(10.5).passengers(22).number(number).priority(1).build();
    }
}
