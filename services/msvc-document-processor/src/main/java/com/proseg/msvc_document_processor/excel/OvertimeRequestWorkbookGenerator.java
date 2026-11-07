package com.proseg.msvc_document_processor.excel;

import com.proseg.msvc_document_processor.dto.response.OvertimeRequestReportDto;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.FontFormatting;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@Component
public class OvertimeRequestWorkbookGenerator {

    public static final String PROJECTION_SHEET = "PROYECCION";
    public static final String SUMMARY_SHEET = "RESUMEN";

    private static final List<String> MONTH_NAMES = List.of(
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "setiembre", "octubre", "noviembre", "diciembre"
    );
    private static final String TITLE = "PROYECCIÓN DE CUMPLIMIENTO DE SOBRESUELDO Y SOLICITUD DE HORAS EXTRA";
    private static final String[] TOUR_HEADERS = {
            "Nº boleta\nde gira", "Prioridad", "Unidad\nEjecutora", "Responsable\nde la gira"
    };
    private static final String DATE_HEADER = "FECHA";
    private static final String TIME_HEADER = "HORA";
    private static final String START_HEADER = "Inicio";
    private static final String END_HEADER = "Fin";
    private static final String SURPLUS_HEADER = "Horas sobresueldo\nrequeridas";
    private static final String OVERTIME_HEADER = "Horas extra\nrequeridas";
    private static final String TOTAL_LABEL = "TOTAL";
    private static final String COMPLIANCE_LABEL = "% CUMP. DEL SOB.";
    private static final String WEEKDAY_FORMULA =
            "CHOOSE(WEEKDAY(%s,2),\"Lunes\",\"Martes\",\"Miércoles\",\"Jueves\",\"Viernes\",\"Sábado\",\"Domingo\")";
    private static final String WEEKEND_RULE_FORMULA = "OR(%s=\"Sábado\",%s=\"Domingo\")";

    private static final String TOTAL_OVERTIME_LABEL = "TOTAL DE HORAS DE TIEMPO EXTRAORDINARIO REQUERIDAS";
    private static final String TOTAL_SURPLUS_LABEL = "TOTAL DE HORAS SOBRESUELDO QUE SE EJECUTARÁN";
    private static final double CAP_TOLERANCE = 1e-9;
    private static final String TOTAL_CAP_LABEL ="TOTAL DE HORAS SOBRESUELDO POR CUMPLIR";
    private static final String CAP_PER_DRIVER_LABEL = "HORAS SOBRESUELDO POR CUMPLIR POR CHOFER";
    private static final String DIFFERENCE_LABEL = "DIFERENCIA EN HORAS SOBRESUELDO";
    private static final String COMPLIANCE_TOTAL_LABEL = "% CUMPLIMIENTO DEL SOB.";
    private static final String PREVIOUS_SURPLUS_LABEL = "H. S. GIRAS DEL MES ANTERIOR";
    private static final String PREVIOUS_OVERTIME_LABEL = "H. E. GIRAS DEL MES ANTERIOR";
    private static final String OVERLAPPED_SURPLUS_LABEL = "H. S. GIRAS DEL MES TRASLAPADAS";
    private static final String NEXT_OVERTIME_LABEL = "H. E. GIRAS PARA EL MES SIGUIENTE";

    private static final String[] SUMMARY_HEADERS = {"NOMBRE", "% E.S", "HSP", "HST", "THTER"};
    private static final String SUMMARY_TOTAL_LABEL = "TOTALES";
    private static final String[][] SUMMARY_LEGEND = {
            {"% E.S.", "Porcentaje de ejecución sobresueldo (Programación)"},
            {"HSP", "Horas sobresueldo programadas (TOTAL DE HORAS SOBRESUELDO QUE SE EJECUTARÁN)"},
            {"HST", "Horas sobresueldo totales (corresponde al total de horas por cumplir, una vez rebajados feriados, vacaciones, asuetos, otros)"},
            {"THTER", "Total de horas de tiempo extraordinario requeridas."},
    };

    private static final String PROJECTION_FONT = "Tenorite";
    private static final String SUMMARY_FONT = "Arial Narrow";
    private static final short PROJECTION_FONT_SIZE = 11;
    private static final short NOTE_FONT_SIZE = 10;
    private static final short SUMMARY_FONT_SIZE = 8;
    private static final byte[] HEADER_FILL = rgb(0xFF, 0xFF, 0xCC);
    private static final byte[] SIDE_FILL = rgb(0x00, 0xB0, 0xF0);
    private static final byte[] SUMMARY_FILL = rgb(0xFF, 0x99, 0x99);
    private static final byte[] RED = rgb(0xFF, 0x00, 0x00);
    private static final byte[] WEEKEND_FILL = rgb(0xFF, 0xC7, 0xCE);
    private static final byte[] WEEKEND_FONT = rgb(0x9C, 0x00, 0x06);
    private static final String HOURS_FORMAT = "_-* #,##0.00_-;\\-* #,##0.00_-;_-* \"-\"??_-;_-@";
    private static final String SUMMARY_HOURS_FORMAT = "_-* #,##0.0_-;\\-* #,##0.0_-;_-* \"-\"??_-;_-@";
    private static final String MONTH_FORMAT = "mmm-yy";
    private static final short SHORT_DATE_FORMAT = 14;
    private static final String PERCENT_FORMAT = "0%";
    private static final String DECIMAL_PERCENT_FORMAT = "0.0%";
    private static final int[] PROJECTION_WIDTHS = {13, 10, 49, 24, 13, 11, 9, 21, 15, 13, 14, 11, 12, 13, 10, 5, 5};
    private static final int[] SUMMARY_WIDTHS = {5, 37, 7, 7, 9, 9};
    private static final float HEADER_SECOND_ROW_HEIGHT = 31.5f;
    private static final int PROJECTION_ZOOM = 110;
    private static final int SUMMARY_ZOOM = 145;
    private static final int COLUMN_WIDTH_UNIT = 256;

    private static final int TOUR_NUMBER_COLUMN = 0;
    private static final int DATE_COLUMN = 4;
    private static final int WEEKDAY_COLUMN = 5;
    private static final int START_COLUMN = 6;
    private static final int END_COLUMN = 7;
    private static final int SURPLUS_COLUMN = 8;
    private static final int OVERTIME_COLUMN = 9;
    private static final int LAST_COLUMN = OVERTIME_COLUMN;
    private static final int NOTE_LAST_COLUMN = 5;
    private static final int LABEL_LAST_COLUMN = 7;
    private static final int SIDE_LABEL_COLUMN = 10;
    private static final int SIDE_LABEL_LAST_COLUMN = 13;
    private static final int SIDE_VALUE_COLUMN = 14;
    private static final int SIDE_SURPLUS_SIGN_COLUMN = 15;
    private static final int SIDE_OVERTIME_SIGN_COLUMN = 16;

    public byte[] generate(OvertimeRequestReportDto report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            List<DriverRows> driverRows = writeProjection(workbook.createSheet(PROJECTION_SHEET), report, styles);
            writeSummary(workbook.createSheet(SUMMARY_SHEET), driverRows, styles);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw DocumentProcessorException.exportGenerationFailed();
        }
    }

    private List<DriverRows> writeProjection(Sheet sheet, OvertimeRequestReportDto report, Styles styles) {
        setupSheet(sheet, PROJECTION_WIDTHS, PROJECTION_ZOOM);
        writeMerged(sheet, 0, 0, LAST_COLUMN, TITLE, styles.title);
        Cell periodCell = writeMerged(sheet, 1, 0, LAST_COLUMN, null, styles.period);
        periodCell.setCellValue(LocalDate.of(report.getYear(), report.getMonth(), 1));

        List<OvertimeRequestReportDto.DriverBlock> drivers = report.getDrivers() == null ? List.of() : report.getDrivers();
        int rowIndex = 3;
        List<DriverRows> driverRows = new ArrayList<>();
        List<Cell> capCells = new ArrayList<>();
        List<String> overlappedCells = new ArrayList<>();
        for (int i = 0; i < drivers.size(); i++) {
            DriverRows rows = writeDriverBlock(sheet, rowIndex, i + 1, drivers.get(i), report, styles, capCells, overlappedCells);
            driverRows.add(rows);
            rowIndex = rows.complianceRow() + 3;
        }
        int capRow = writeTotals(sheet, rowIndex, driverRows, overlappedCells, report, styles);
        String capReference = "$" + column(SURPLUS_COLUMN) + "$" + (capRow + 1);
        capCells.forEach(capCell -> capCell.setCellFormula("+" + capReference));
        return driverRows;
    }

    private DriverRows writeDriverBlock(
            Sheet sheet,
            int titleRow,
            int position,
            OvertimeRequestReportDto.DriverBlock driver,
            OvertimeRequestReportDto report,
            Styles styles,
            List<Cell> capCells,
            List<String> overlappedCells
    ) {
        writeMerged(sheet, titleRow, 0, LAST_COLUMN, position + ". " + upper(driver.getName()), styles.title);

        int headerRow = titleRow + 2;
        writeHeader(sheet, headerRow, styles);

        int firstDataRow = headerRow + 2;
        int rowIndex = firstDataRow;
        List<OvertimeRequestReportDto.TourBlock> tours = driver.getTours() == null ? List.of() : driver.getTours();
        for (OvertimeRequestReportDto.TourBlock tour : tours) {
            rowIndex = writeTour(sheet, rowIndex, tour, styles, overlappedCells);
        }
        if (rowIndex == firstDataRow) {
            writeEmptyDataRow(sheet, rowIndex++, styles);
        }
        int lastDataRow = rowIndex - 1;
        addWeekendFormatting(sheet, firstDataRow, lastDataRow);

        int totalRow = lastDataRow + 2;
        int complianceRow = totalRow + 1;
        writeMerged(sheet, totalRow, complianceRow, 0, NOTE_LAST_COLUMN, note(report, driver), styles.note);
        writeLabel(sheet, totalRow, TOTAL_LABEL, styles.totalLabel);
        writeFormula(sheet, totalRow, SURPLUS_COLUMN, sum(SURPLUS_COLUMN, firstDataRow, lastDataRow), styles.totalHours);
        writeFormula(sheet, totalRow, OVERTIME_COLUMN, sum(OVERTIME_COLUMN, firstDataRow, lastDataRow), styles.totalHours);
        writeLabel(sheet, complianceRow, COMPLIANCE_LABEL, styles.totalLabel);
        Cell capCell = cell(sheet, complianceRow, SURPLUS_COLUMN, styles.totalHours);
        if (usesTheDefaultCap(driver, report)) {
            capCells.add(capCell);
        } else {
            capCell.setCellValue(driver.getSurplusCap());
        }
        String surplusTotal = ref(SURPLUS_COLUMN, totalRow);
        String cap = ref(SURPLUS_COLUMN, complianceRow);
        writeFormula(sheet, complianceRow, OVERTIME_COLUMN, "IF(" + cap + "=0,0," + surplusTotal + "/" + cap + ")", styles.totalPercent);

        return new DriverRows(titleRow, totalRow, complianceRow);
    }

    private void writeHeader(Sheet sheet, int headerRow, Styles styles) {
        int secondRow = headerRow + 1;
        sheet.createRow(headerRow);
        sheet.createRow(secondRow).setHeightInPoints(HEADER_SECOND_ROW_HEIGHT);
        for (int column = 0; column < TOUR_HEADERS.length; column++) {
            writeMerged(sheet, headerRow, secondRow, column, column, TOUR_HEADERS[column], styles.header);
        }
        writeMerged(sheet, headerRow, secondRow, DATE_COLUMN, WEEKDAY_COLUMN, DATE_HEADER, styles.header);
        writeMerged(sheet, headerRow, headerRow, START_COLUMN, END_COLUMN, TIME_HEADER, styles.header);
        cell(sheet, secondRow, START_COLUMN, styles.header).setCellValue(START_HEADER);
        cell(sheet, secondRow, END_COLUMN, styles.header).setCellValue(END_HEADER);
        writeMerged(sheet, headerRow, secondRow, SURPLUS_COLUMN, SURPLUS_COLUMN, SURPLUS_HEADER, styles.header);
        writeMerged(sheet, headerRow, secondRow, OVERTIME_COLUMN, OVERTIME_COLUMN, OVERTIME_HEADER, styles.header);
    }

    private int writeTour(Sheet sheet, int firstRow, OvertimeRequestReportDto.TourBlock tour, Styles styles,
                          List<String> overlappedCells) {
        List<OvertimeRequestReportDto.TourDay> days = tour.getDays() == null ? List.of() : tour.getDays();
        int lastRow = firstRow + Math.max(days.size(), 1) - 1;
        Object[] tourValues = {tourNumber(tour.getNumber()), tour.getPriority(), tour.getExecutingUnit(), tour.getResponsible()};
        for (int column = 0; column < tourValues.length; column++) {
            Cell cell = writeMerged(sheet, firstRow, lastRow, column, column, null, styles.text);
            setValue(cell, tourValues[column]);
        }
        for (int i = 0; i < days.size(); i++) {
            writeDay(sheet, firstRow + i, days.get(i), styles, overlappedCells);
        }
        return lastRow + 1;
    }

    private void writeDay(Sheet sheet, int rowIndex, OvertimeRequestReportDto.TourDay day, Styles styles,
                          List<String> overlappedCells) {
        cell(sheet, rowIndex, DATE_COLUMN, styles.date).setCellValue(day.getDate());
        cell(sheet, rowIndex, WEEKDAY_COLUMN, styles.text)
                .setCellFormula(String.format(WEEKDAY_FORMULA, ref(DATE_COLUMN, rowIndex)));
        cell(sheet, rowIndex, START_COLUMN, styles.hours).setCellValue(day.getStart());
        cell(sheet, rowIndex, END_COLUMN, styles.hours).setCellValue(day.getEnd());
        setPositive(cell(sheet, rowIndex, SURPLUS_COLUMN, styles.surplusHours), day.getSurplusHours());
        setPositive(cell(sheet, rowIndex, OVERTIME_COLUMN, styles.hours), day.getOvertimeHours());
        if (day.getOvertimeHours() > 0 && !isWeekend(day.getDate())) {
            overlappedCells.add(ref(OVERTIME_COLUMN, rowIndex));
        }
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private void writeEmptyDataRow(Sheet sheet, int rowIndex, Styles styles) {
        for (int column = 0; column <= LAST_COLUMN; column++) {
            cell(sheet, rowIndex, column, column == SURPLUS_COLUMN ? styles.surplusHours : styles.text);
        }
    }

    private void addWeekendFormatting(Sheet sheet, int firstDataRow, int lastDataRow) {
        SheetConditionalFormatting formatting = sheet.getSheetConditionalFormatting();
        String firstWeekdayCell = ref(WEEKDAY_COLUMN, firstDataRow).replace("$", "");
        ConditionalFormattingRule rule = formatting.createConditionalFormattingRule(
                String.format(WEEKEND_RULE_FORMULA, firstWeekdayCell, firstWeekdayCell));
        PatternFormatting fill = rule.createPatternFormatting();
        fill.setFillBackgroundColor(new XSSFColor(WEEKEND_FILL, new DefaultIndexedColorMap()));
        fill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
        FontFormatting font = rule.createFontFormatting();
        font.setFontColor(new XSSFColor(WEEKEND_FONT, new DefaultIndexedColorMap()));
        formatting.addConditionalFormatting(
                new CellRangeAddress[]{new CellRangeAddress(firstDataRow, lastDataRow, WEEKDAY_COLUMN, WEEKDAY_COLUMN)},
                rule);
    }

    private int writeTotals(Sheet sheet, int firstRow, List<DriverRows> drivers, List<String> overlappedCells,
                            OvertimeRequestReportDto report, Styles styles) {
        int signRow = firstRow;
        int overtimeRow = firstRow + 1;
        int previousOvertimeRow = overtimeRow + 1;
        int surplusRow = overtimeRow + 2;
        int nextOvertimeRow = surplusRow + 1;
        int capTotalRow = surplusRow + 2;
        int capRow = capTotalRow + 2;
        int differenceRow = capRow + 2;
        int complianceRow = differenceRow + 2;

        cell(sheet, signRow, SIDE_SURPLUS_SIGN_COLUMN, styles.sideValue).setCellValue("S");
        cell(sheet, signRow, SIDE_OVERTIME_SIGN_COLUMN, styles.sideValue).setCellValue("E");

        writeSummaryLine(sheet, overtimeRow, TOTAL_OVERTIME_LABEL,
                "+" + joinRefs(drivers, OVERTIME_COLUMN, DriverRows::totalRow), styles.summaryHours, styles);
        writeSummaryLine(sheet, surplusRow, TOTAL_SURPLUS_LABEL,
                "+" + joinRefs(drivers, SURPLUS_COLUMN, DriverRows::totalRow), styles.summaryHours, styles);
        writeSummaryLine(sheet, capTotalRow, TOTAL_CAP_LABEL,
                "+" + joinRefs(drivers, SURPLUS_COLUMN, DriverRows::complianceRow), styles.summaryHours, styles);
        writeMerged(sheet, capRow, 0, LABEL_LAST_COLUMN, CAP_PER_DRIVER_LABEL, styles.summaryLabel);
        cell(sheet, capRow, SURPLUS_COLUMN, styles.summaryHours).setCellValue(report.getSurplusCapPerDriver());
        writeSummaryLine(sheet, differenceRow, DIFFERENCE_LABEL,
                "+" + ref(SURPLUS_COLUMN, capTotalRow) + "-" + ref(SURPLUS_COLUMN, surplusRow), styles.summaryHours, styles);
        writeSummaryLine(sheet, complianceRow, COMPLIANCE_TOTAL_LABEL,
                "IF(" + ref(SURPLUS_COLUMN, capTotalRow) + "=0,0," + ref(SURPLUS_COLUMN, surplusRow) + "/" + ref(SURPLUS_COLUMN, capTotalRow) + ")",
                styles.summaryPercent, styles);

        writeSideLine(sheet, overtimeRow, PREVIOUS_SURPLUS_LABEL, cell -> cell.setCellValue(0), "+", null, styles);
        writeSideLine(sheet, previousOvertimeRow, PREVIOUS_OVERTIME_LABEL, cell -> cell.setCellValue(0), null, "+", styles);
        writeSideLine(sheet, surplusRow, OVERLAPPED_SURPLUS_LABEL,
                cell -> cell.setCellFormula(overlappedCells.isEmpty() ? "0" : "+" + String.join("+", overlappedCells)), "-", "+", styles);
        writeSideLine(sheet, nextOvertimeRow, NEXT_OVERTIME_LABEL, cell -> cell.setCellValue(0), null, "-", styles);
        return capRow;
    }

    private void writeSummaryLine(Sheet sheet, int rowIndex, String label, String formula, CellStyle valueStyle, Styles styles) {
        writeMerged(sheet, rowIndex, 0, LABEL_LAST_COLUMN, label, styles.summaryLabel);
        writeFormula(sheet, rowIndex, SURPLUS_COLUMN, formula, valueStyle);
    }

    private void writeSideLine(Sheet sheet, int rowIndex, String label, Consumer<Cell> value,
                               String surplusSign, String overtimeSign, Styles styles) {
        writeMerged(sheet, rowIndex, SIDE_LABEL_COLUMN, SIDE_LABEL_LAST_COLUMN, label, styles.sideLabel);
        value.accept(cell(sheet, rowIndex, SIDE_VALUE_COLUMN, styles.sideHours));
        if (surplusSign != null) {
            cell(sheet, rowIndex, SIDE_SURPLUS_SIGN_COLUMN, styles.sideValue).setCellValue(surplusSign);
        }
        if (overtimeSign != null) {
            cell(sheet, rowIndex, SIDE_OVERTIME_SIGN_COLUMN, styles.sideValue).setCellValue(overtimeSign);
        }
    }

    private void writeSummary(Sheet sheet, List<DriverRows> drivers, Styles styles) {
        setupSheet(sheet, SUMMARY_WIDTHS, SUMMARY_ZOOM);
        int headerRow = 1;
        for (int i = 0; i < SUMMARY_HEADERS.length; i++) {
            cell(sheet, headerRow, i + 1, styles.summaryHeader).setCellValue(SUMMARY_HEADERS[i]);
        }

        int firstRow = headerRow + 1;
        int rowIndex = firstRow;
        for (DriverRows driver : drivers) {
            boolean first = rowIndex == firstRow;
            writeFormula(sheet, rowIndex, 1, "+" + PROJECTION_SHEET + "!" + ref(0, driver.titleRow()), first ? styles.summaryNameFirst : styles.summaryName);
            writeFormula(sheet, rowIndex, 2, percentFormula(rowIndex), first ? styles.summaryPercentFirst : styles.summaryPercentCell);
            writeFormula(sheet, rowIndex, 3, "+" + PROJECTION_SHEET + "!" + ref(SURPLUS_COLUMN, driver.totalRow()), first ? styles.summaryNumberFirst : styles.summaryNumber);
            writeFormula(sheet, rowIndex, 4, "+" + PROJECTION_SHEET + "!" + ref(SURPLUS_COLUMN, driver.complianceRow()), first ? styles.summaryNumberFirst : styles.summaryNumber);
            writeFormula(sheet, rowIndex, 5, "+" + PROJECTION_SHEET + "!" + ref(OVERTIME_COLUMN, driver.totalRow()), first ? styles.summaryNumberFirst : styles.summaryNumber);
            rowIndex++;
        }

        int totalRow = rowIndex;
        int lastRow = Math.max(firstRow, totalRow - 1);
        cell(sheet, totalRow, 1, styles.summaryTotalLabel).setCellValue(SUMMARY_TOTAL_LABEL);
        writeFormula(sheet, totalRow, 2, percentFormula(totalRow), styles.summaryTotalPercent);
        for (int column = 3; column <= 5; column++) {
            writeFormula(sheet, totalRow, column, drivers.isEmpty() ? "0" : sum(column, firstRow, lastRow), styles.summaryTotalNumber);
        }

        int legendRow = totalRow + 1;
        for (int i = 0; i < SUMMARY_LEGEND.length; i++) {
            cell(sheet, legendRow + i, 0, styles.summaryLegendCode).setCellValue(SUMMARY_LEGEND[i][0]);
            writeMerged(sheet, legendRow + i, 1, 5, SUMMARY_LEGEND[i][1], styles.summaryLegend);
        }
    }

    private String percentFormula(int rowIndex) {
        return "IF(" + ref(4, rowIndex) + "=0,0," + ref(3, rowIndex) + "/" + ref(4, rowIndex) + ")";
    }

    private void setupSheet(Sheet sheet, int[] widths, int zoom) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * COLUMN_WIDTH_UNIT);
        }
        sheet.setDisplayGridlines(false);
        sheet.setZoom(zoom);
        sheet.setFitToPage(true);
        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(true);
        printSetup.setFitWidth((short) 1);
        printSetup.setFitHeight((short) 0);
    }

    private boolean usesTheDefaultCap(OvertimeRequestReportDto.DriverBlock driver, OvertimeRequestReportDto report) {
        return driver.getSurplusCap() == null || Math.abs(driver.getSurplusCap() - report.getSurplusCapPerDriver()) < CAP_TOLERANCE;
    }

    private String note(OvertimeRequestReportDto report, OvertimeRequestReportDto.DriverBlock driver) {
        int businessDays = driver.getBusinessDays() == null ? report.getBusinessDays() : driver.getBusinessDays();
        return "Nota: Cantidad de horas de sobresueldo correspondiente a los " + businessDays
                + " días de " + MONTH_NAMES.get(report.getMonth() - 1) + " del " + report.getYear();
    }

    private void writeLabel(Sheet sheet, int rowIndex, String label, CellStyle style) {
        cell(sheet, rowIndex, END_COLUMN, style).setCellValue(label);
    }

    private String joinRefs(List<DriverRows> drivers, int column, ToIntFunction<DriverRows> row) {
        if (drivers.isEmpty()) {
            return "0";
        }
        return drivers.stream().map(rows -> ref(column, row.applyAsInt(rows))).collect(Collectors.joining("+"));
    }

    private Cell writeMerged(Sheet sheet, int rowIndex, int firstColumn, int lastColumn, String value, CellStyle style) {
        return writeMerged(sheet, rowIndex, rowIndex, firstColumn, lastColumn, value, style);
    }

    private Cell writeMerged(Sheet sheet, int firstRow, int lastRow, int firstColumn, int lastColumn, String value, CellStyle style) {
        for (int rowIndex = firstRow; rowIndex <= lastRow; rowIndex++) {
            for (int column = firstColumn; column <= lastColumn; column++) {
                cell(sheet, rowIndex, column, style);
            }
        }
        if (firstRow != lastRow || firstColumn != lastColumn) {
            sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstColumn, lastColumn));
        }
        Cell cell = cell(sheet, firstRow, firstColumn, style);
        if (value != null) {
            cell.setCellValue(value);
        }
        return cell;
    }

    private void writeFormula(Sheet sheet, int rowIndex, int column, String formula, CellStyle style) {
        cell(sheet, rowIndex, column, style).setCellFormula(formula);
    }

    private Cell cell(Sheet sheet, int rowIndex, int column, CellStyle style) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        Cell cell = row.getCell(column);
        if (cell == null) {
            cell = row.createCell(column);
        }
        cell.setCellStyle(style);
        return cell;
    }

    private void setValue(Cell cell, Object value) {
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value != null) {
            cell.setCellValue(value.toString());
        }
    }

    private void setPositive(Cell cell, double value) {
        if (value > 0) {
            cell.setCellValue(value);
        }
    }

    private Object tourNumber(String number) {
        if (number == null) {
            return null;
        }
        String trimmed = number.trim();
        return trimmed.chars().allMatch(Character::isDigit) && !trimmed.isEmpty() ? Long.parseLong(trimmed) : trimmed;
    }

    private String sum(int column, int firstRow, int lastRow) {
        return "SUM(" + range(column, firstRow, lastRow) + ")";
    }

    private String range(int column, int firstRow, int lastRow) {
        return ref(column, firstRow) + ":" + ref(column, lastRow);
    }

    private static String ref(int column, int rowIndex) {
        return column(column) + (rowIndex + 1);
    }

    private static String column(int column) {
        return CellReference.convertNumToColString(column);
    }

    private static String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.forLanguageTag("es"));
    }

    private static byte[] rgb(int red, int green, int blue) {
        return new byte[]{(byte) red, (byte) green, (byte) blue};
    }

    private record DriverRows(int titleRow, int totalRow, int complianceRow) {
    }

    private static final class Styles {
        private final XSSFWorkbook workbook;
        private final Map<String, XSSFFont> fonts = new HashMap<>();

        private final CellStyle title;
        private final CellStyle period;
        private final CellStyle header;
        private final CellStyle text;
        private final CellStyle date;
        private final CellStyle hours;
        private final CellStyle surplusHours;
        private final CellStyle note;
        private final CellStyle totalLabel;
        private final CellStyle totalHours;
        private final CellStyle totalPercent;
        private final CellStyle summaryLabel;
        private final CellStyle summaryHours;
        private final CellStyle summaryPercent;
        private final CellStyle sideLabel;
        private final CellStyle sideValue;
        private final CellStyle sideHours;
        private final CellStyle summaryHeader;
        private final CellStyle summaryName;
        private final CellStyle summaryNameFirst;
        private final CellStyle summaryPercentCell;
        private final CellStyle summaryPercentFirst;
        private final CellStyle summaryNumber;
        private final CellStyle summaryNumberFirst;
        private final CellStyle summaryTotalLabel;
        private final CellStyle summaryTotalPercent;
        private final CellStyle summaryTotalNumber;
        private final CellStyle summaryLegendCode;
        private final CellStyle summaryLegend;

        private Styles(XSSFWorkbook workbook) {
            this.workbook = workbook;
            XSSFFont bold = font("bold", PROJECTION_FONT, PROJECTION_FONT_SIZE, true, false, null);
            XSSFFont boldRed = font("boldRed", PROJECTION_FONT, PROJECTION_FONT_SIZE, true, false, RED);
            XSSFFont noteFont = font("note", PROJECTION_FONT, NOTE_FONT_SIZE, true, true, null);
            XSSFFont summaryBold = font("summaryBold", SUMMARY_FONT, SUMMARY_FONT_SIZE, true, false, null);
            XSSFFont summaryPlain = font("summaryPlain", SUMMARY_FONT, SUMMARY_FONT_SIZE, false, false, null);

            title = style(bold, HEADER_FILL, null, BorderStyle.MEDIUM, BorderStyle.MEDIUM, HorizontalAlignment.CENTER, false);
            period = style(bold, HEADER_FILL, MONTH_FORMAT, BorderStyle.MEDIUM, BorderStyle.MEDIUM, HorizontalAlignment.CENTER, false);
            header = style(bold, HEADER_FILL, null, BorderStyle.THIN, BorderStyle.THIN, HorizontalAlignment.CENTER, true);
            text = style(bold, null, null, BorderStyle.THIN, BorderStyle.THIN, HorizontalAlignment.CENTER, true);
            date = style(bold, null, null, BorderStyle.THIN, BorderStyle.THIN, HorizontalAlignment.CENTER, false);
            date.setDataFormat(SHORT_DATE_FORMAT);
            hours = style(bold, null, HOURS_FORMAT, BorderStyle.THIN, BorderStyle.THIN, HorizontalAlignment.CENTER, false);
            surplusHours = style(boldRed, null, HOURS_FORMAT, BorderStyle.THIN, BorderStyle.THIN, HorizontalAlignment.CENTER, false);
            note = style(noteFont, null, null, BorderStyle.MEDIUM, BorderStyle.MEDIUM, HorizontalAlignment.LEFT, false);
            totalLabel = style(bold, HEADER_FILL, null, BorderStyle.MEDIUM, BorderStyle.THIN, HorizontalAlignment.CENTER, false);
            totalHours = style(bold, HEADER_FILL, HOURS_FORMAT, BorderStyle.THIN, BorderStyle.THIN, HorizontalAlignment.CENTER, false);
            totalPercent = style(bold, HEADER_FILL, PERCENT_FORMAT, BorderStyle.THIN, BorderStyle.MEDIUM, HorizontalAlignment.CENTER, false);
            summaryLabel = doubleUnderline(style(bold, HEADER_FILL, null, null, null, HorizontalAlignment.CENTER, false));
            summaryHours = doubleUnderline(style(bold, HEADER_FILL, HOURS_FORMAT, null, null, HorizontalAlignment.CENTER, false));
            summaryPercent = doubleUnderline(style(bold, HEADER_FILL, DECIMAL_PERCENT_FORMAT, null, null, HorizontalAlignment.CENTER, false));
            sideLabel = style(boldRed, SIDE_FILL, null, null, null, HorizontalAlignment.LEFT, false);
            sideValue = style(boldRed, SIDE_FILL, null, null, null, HorizontalAlignment.CENTER, false);
            sideHours = style(boldRed, SIDE_FILL, HOURS_FORMAT, null, null, HorizontalAlignment.CENTER, false);

            summaryHeader = doubleUnderline(style(summaryBold, SUMMARY_FILL, null, null, null, HorizontalAlignment.CENTER, false));
            summaryHeader.setBorderTop(BorderStyle.THIN);
            summaryName = style(summaryBold, null, null, null, null, HorizontalAlignment.LEFT, false);
            summaryNameFirst = topDouble(style(summaryBold, null, null, null, null, HorizontalAlignment.LEFT, false));
            summaryPercentCell = style(summaryPlain, null, DECIMAL_PERCENT_FORMAT, null, null, HorizontalAlignment.CENTER, false);
            summaryPercentFirst = topDouble(style(summaryPlain, null, DECIMAL_PERCENT_FORMAT, null, null, HorizontalAlignment.CENTER, false));
            summaryNumber = style(summaryPlain, null, SUMMARY_HOURS_FORMAT, null, null, HorizontalAlignment.CENTER, false);
            summaryNumberFirst = topDouble(style(summaryPlain, null, SUMMARY_HOURS_FORMAT, null, null, HorizontalAlignment.CENTER, false));
            summaryTotalLabel = doubleUnderline(style(summaryBold, SUMMARY_FILL, null, null, null, HorizontalAlignment.LEFT, false));
            summaryTotalPercent = doubleUnderline(style(summaryBold, SUMMARY_FILL, DECIMAL_PERCENT_FORMAT, null, null, HorizontalAlignment.CENTER, false));
            summaryTotalNumber = doubleUnderline(style(summaryBold, SUMMARY_FILL, SUMMARY_HOURS_FORMAT, null, null, HorizontalAlignment.CENTER, false));
            summaryLegendCode = style(summaryPlain, null, null, null, null, HorizontalAlignment.CENTER, false);
            summaryLegend = style(summaryPlain, null, null, null, null, HorizontalAlignment.LEFT, false);
        }

        private XSSFFont font(String key, String name, short size, boolean bold, boolean italic, byte[] color) {
            return fonts.computeIfAbsent(key, ignored -> {
                XSSFFont font = workbook.createFont();
                font.setFontName(name);
                font.setFontHeightInPoints(size);
                font.setBold(bold);
                font.setItalic(italic);
                if (color != null) {
                    font.setColor(new XSSFColor(color, new DefaultIndexedColorMap()));
                }
                return font;
            });
        }

        private XSSFCellStyle style(XSSFFont font, byte[] fill, String format, BorderStyle outer, BorderStyle inner,
                                    HorizontalAlignment alignment, boolean wrap) {
            XSSFCellStyle style = workbook.createCellStyle();
            style.setFont(font);
            style.setAlignment(alignment);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setWrapText(wrap);
            if (fill != null) {
                style.setFillForegroundColor(new XSSFColor(fill, new DefaultIndexedColorMap()));
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            if (format != null) {
                style.setDataFormat(workbook.createDataFormat().getFormat(format));
            }
            if (outer != null) {
                style.setBorderTop(outer);
                style.setBorderBottom(outer);
            }
            if (inner != null) {
                style.setBorderLeft(inner);
                style.setBorderRight(inner);
            }
            return style;
        }

        private XSSFCellStyle doubleUnderline(XSSFCellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.DOUBLE);
            return style;
        }

        private XSSFCellStyle topDouble(XSSFCellStyle style) {
            style.setBorderTop(BorderStyle.DOUBLE);
            return style;
        }
    }
}
