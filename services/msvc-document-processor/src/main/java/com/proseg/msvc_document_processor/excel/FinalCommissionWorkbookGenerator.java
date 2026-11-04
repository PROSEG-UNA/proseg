package com.proseg.msvc_document_processor.excel;

import com.proseg.msvc_document_processor.dto.response.FinalCommissionReportDto;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.FontFormatting;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PatternFormatting;
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
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class FinalCommissionWorkbookGenerator {

    public static final String DRAFT_SHEET = "BORRADOR";
    public static final String TOTALS_SHEET = "TOTALES";
    public static final String INTERNAL_SHEET = "R.INTERNOS";
    public static final String EXTERNAL_SHEET = "R.EXTERNOS";
    public static final String BORROWED_SHEET = "P.VEHICULO";
    public static final String NOT_APPROVED_SHEET = "NO APROBADAS";
    public static final String SUMMARY_SHEET = "RESUMEN";

    private static final String INTERNAL_CATEGORY = "INTERNAL";
    private static final String EXTERNAL_CATEGORY = "EXTERNAL";
    private static final String BORROWED_CATEGORY = "BORROWED_VEHICLE";
    private static final String NOT_APPROVED_CATEGORY = "NOT_APPROVED";

    private static final List<String> MONTH_NAMES = List.of(
            "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
            "JULIO", "AGOSTO", "SETIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"
    );
    private static final String REQUESTED_TITLE = "TOTAL DE GIRAS SOLICITADAS CORRESPONDIENTES ";
    private static final String INTERNAL_TITLE = "TOTAL DE GIRAS ATENDIDAS CON RECURSOS INTERNOS ";
    private static final String EXTERNAL_TITLE = "TOTAL DE GIRAS ATENDIDAS CON RECURSOS EXTERNOS ";
    private static final String BORROWED_TITLE = "TOTAL DE GIRAS ATENDIDAS CON PRESTAMO DE VEHICULO ";
    private static final String NOT_APPROVED_TITLE = "TOTAL DE GIRAS NO APROBADAS ";
    private static final String DATES_HEADER = "FECHAS Y HORAS DE SALIDA Y REGRESO";
    private static final String SUMS_HEADER = "SUMATORIAS TOTALES";
    private static final String HOURS_HEADER = "CALCULO DE HORAS SOBRESUELDO Y EXTRAS";
    private static final String TOTALS_HEADER = "TOTALES";
    private static final String GENERAL_HEADER = "DATOS GENERALES";
    private static final String SCHEDULE_HEADER = "HORARIOS";
    private static final String DAY_HEADER = "DIA ";
    private static final String[] TOUR_LABELS = {
            "#", "GIRA", "PAS", "UNIDAD EJECUTORA", "RESPONSABLE", "DESTINO", "DURACION", "PRIOR",
            "S", null, "R", null, "D", "S", "D", "R", "H.J.O", "H.J.S.", "H.J.E.", "E", "F"
    };
    private static final String[] FIRST_DAY_LABELS = {"I", "F", "TOTAL", "H.S", "H.E"};
    private static final String[] NEXT_DAY_LABELS = {"DIA", "FECHA", "I", "F", "TOTAL", "H.S", "H.E"};
    private static final String SURPLUS_SUBTOTAL_LABEL = "SUBTOTAL / \nHORAS\nSOBRESUELDO";
    private static final String OVERTIME_SUBTOTAL_LABEL = "SUBTOTAL / \nHORAS\nEXTRA";
    private static final String WEEKDAY_FORMULA =
            "CHOOSE(WEEKDAY(%s,2),\"Lunes\",\"Martes\",\"Miércoles\",\"Jueves\",\"Viernes\",\"Sábado\",\"Domingo\")";
    private static final String WEEKEND_TEST = "OR(%1$s=\"Sábado\",%1$s=\"Domingo\")";

    private static final String WORKED_HOURS_LABEL = "TOTAL HORAS JOR. Y SOB SOL";
    private static final String OVERTIME_LABEL = "TOTAL HORAS EXTRA";
    private static final String WEEKEND_LABEL = "SAB Y DOM";
    private static final String AVAILABLE_LABEL = "TOTAL GENERAL DISP.";
    private static final String REQUIRED_OVERTIME_LABEL = "HORAS EXTRAS NECESARIAS";
    private static final String USAGE_LABEL = "TOTAL GENERAL UTILIZ.";
    private static final int OVERTIME_HOUR_COST = 17000;
    private static final int ORDINARY_HOURS_PER_DAY = 8;

    private static final String SUMMARY_COUNT_TITLE = "GIRAS SOLICITADAS POR PRIORIDAD (CANTIDAD)";
    private static final String SUMMARY_HOURS_TITLE = "GIRAS SOLICITADAS POR PRIORIDAD (HORAS REQUERIDAS DE SOBRESUELDO Y EXTRAS)";
    private static final String SUMMARY_NOTE = "Son la cantidad de horas extras requeridas.";
    private static final int SUMMARY_PRIORITIES = 10;

    private static final String FONT = "Tenorite";
    private static final short FONT_SIZE = 11;
    private static final short SUBTOTAL_FONT_SIZE = 16;
    private static final byte[] HEADER_FILL = rgb(0xFB, 0xE5, 0xD6);
    private static final byte[] SUMMARY_FILL = rgb(0xFF, 0x99, 0x99);
    private static final byte[] DEFAULT_PRIORITY_FILL = rgb(0xF2, 0xF2, 0xF2);
    private static final Map<Integer, byte[]> PRIORITY_FILLS = Map.of(
            1, rgb(0xE2, 0xF0, 0xD9),
            2, rgb(0xDE, 0xEB, 0xF7),
            3, rgb(0xFF, 0xF2, 0xCC),
            4, rgb(0xED, 0xED, 0xED),
            5, rgb(0xE4, 0xDF, 0xEC),
            6, rgb(0xFB, 0xE5, 0xD6),
            7, rgb(0xDA, 0xE3, 0xF3)
    );
    private static final byte[] RED = rgb(0xFF, 0x00, 0x00);
    private static final byte[] YELLOW = rgb(0xFF, 0xFF, 0x00);
    private static final byte[] WEEKEND_FONT = rgb(0x9C, 0x00, 0x06);
    private static final byte[] WEEKEND_FILL = rgb(0xFF, 0xC7, 0xCE);
    private static final byte[] LIMIT_FONT = rgb(0x9C, 0x57, 0x00);
    private static final byte[] LIMIT_FILL = rgb(0xFF, 0xEB, 0x9C);
    private static final String HOURS_FORMAT = "_-* #,##0.00_-;\\-* #,##0.00_-;_-* \"-\"??_-;_-@_-";
    private static final String TOTAL_HOURS_FORMAT = "_-* #,##0.0_-;\\-* #,##0.0_-;_-* \"-\"??_-;_-@_-";
    private static final String INTEGER_FORMAT = "_-* #,##0_-;\\-* #,##0_-;_-* \"-\"??_-;_-@_-";
    private static final String DECIMAL_FORMAT = "0.00";
    private static final String TIME_FORMAT = "hh:mm";
    private static final String LONG_DATE_FORMAT = "dd-mm-yyyy";
    private static final short SHORT_DATE_FORMAT = 14;
    private static final String PERCENT_FORMAT = "0%";
    private static final String DECIMAL_PERCENT_FORMAT = "0.0%";
    private static final double[] TOUR_WIDTHS = {
            4.4, 10.1, 8.6, 40.7, 25.7, 25.1, 12.0, 9.0, 7.1, 7.1, 7.1, 7.1, 9.9, 14.3, 9.9, 12.3, 13.9, 11.3, 11.3,
            6.1, 6.6, 8.1, 8.1, 12.1, 9.3, 9.3
    };
    private static final double[] DAY_WIDTHS = {12.1, 12.3, 7.0, 8.1, 12.1, 9.3, 9.3};
    private static final double SURPLUS_SUBTOTAL_WIDTH = 19.4;
    private static final double OVERTIME_SUBTOTAL_WIDTH = 17.3;
    private static final double[] SUMMARY_WIDTHS = {
            3.7, 14.4, 15.1, 15.0, 14.4, 15.3, 10.3, 11.4, 3.7, 14.4, 10.4, 9.1, 10.9, 11.1, 9.1, 9.4, 10.0, 9.4, 12.1, 10.1, 3.7
    };
    private static final float TITLE_ROW_HEIGHT = 15f;
    private static final float LABEL_ROW_HEIGHT = 45.75f;
    private static final float SUMMARY_HEADER_HEIGHT = 30.75f;
    private static final int COLUMN_WIDTH_UNIT = 256;

    private static final int TOUR_COLUMN = 1;
    private static final int PASSENGERS_COLUMN = 2;
    private static final int DESTINATION_COLUMN = 5;
    private static final int DURATION_COLUMN = 6;
    private static final int PRIORITY_COLUMN = 7;
    private static final int DEPARTURE_TIME_COLUMN = 8;
    private static final int DEPARTURE_HOUR_COLUMN = 9;
    private static final int RETURN_TIME_COLUMN = 10;
    private static final int RETURN_HOUR_COLUMN = 11;
    private static final int DEPARTURE_WEEKDAY_COLUMN = 12;
    private static final int DEPARTURE_DATE_COLUMN = 13;
    private static final int RETURN_WEEKDAY_COLUMN = 14;
    private static final int RETURN_DATE_COLUMN = 15;
    private static final int ORDINARY_HOURS_COLUMN = 16;
    private static final int SURPLUS_HOURS_COLUMN = 17;
    private static final int OVERTIME_HOURS_COLUMN = 18;
    private static final int ORDINARY_START_COLUMN = 19;
    private static final int ORDINARY_END_COLUMN = 20;
    private static final int FIRST_DAY_COLUMN = 21;
    private static final int NEXT_DAYS_COLUMN = 26;
    private static final int DAY_WIDTH = 7;
    private static final int MAX_DAYS = 12;
    private static final int LAST_DAY_COLUMN = NEXT_DAYS_COLUMN + (MAX_DAYS - 1) * DAY_WIDTH - 1;
    private static final int SURPLUS_SUBTOTAL_COLUMN = LAST_DAY_COLUMN + 1;
    private static final int OVERTIME_SUBTOTAL_COLUMN = LAST_DAY_COLUMN + 2;
    private static final int TAIL_LABEL_COLUMN = 13;
    private static final int TAIL_LABEL_LAST_COLUMN = 15;
    private static final int FIRST_DATA_ROW = 3;
    private static final int TAIL_GAP = 4;
    private static final int TAIL_STEP = 2;

    public byte[] generate(FinalCommissionReportDto report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            List<FinalCommissionReportDto.Tour> tours = report.getTours() == null ? List.of() : report.getTours();
            String period = MONTH_NAMES.get(report.getMonth() - 1) + " " + report.getYear();

            writeTourSheet(workbook.createSheet(DRAFT_SHEET), REQUESTED_TITLE + period, tours, Tail.FULL, report, null, styles);
            writeTourSheet(workbook.createSheet(TOTALS_SHEET), REQUESTED_TITLE + period, tours, Tail.FULL, report, null, styles);
            SheetRows internal = writeTourSheet(workbook.createSheet(INTERNAL_SHEET), INTERNAL_TITLE + period,
                    of(tours, INTERNAL_CATEGORY), Tail.FULL, report, null, styles);
            SheetRows external = writeTourSheet(workbook.createSheet(EXTERNAL_SHEET), EXTERNAL_TITLE + period,
                    of(tours, EXTERNAL_CATEGORY), Tail.AVAILABLE, report, null, styles);
            SheetRows borrowed = writeTourSheet(workbook.createSheet(BORROWED_SHEET), BORROWED_TITLE + period,
                    of(tours, BORROWED_CATEGORY), Tail.SHORT, report, null, styles);
            SheetRows notApproved = writeTourSheet(workbook.createSheet(NOT_APPROVED_SHEET), NOT_APPROVED_TITLE + period,
                    of(tours, NOT_APPROVED_CATEGORY), Tail.REMAINING, report, internal, styles);
            writeSummary(workbook.createSheet(SUMMARY_SHEET), period, List.of(internal, external, borrowed, notApproved), styles);

            workbook.setSheetHidden(workbook.getSheetIndex(DRAFT_SHEET), true);
            int totalsIndex = workbook.getSheetIndex(TOTALS_SHEET);
            workbook.setActiveSheet(totalsIndex);
            workbook.setFirstVisibleTab(totalsIndex);
            workbook.getSheet(DRAFT_SHEET).setSelected(false);
            workbook.getSheet(TOTALS_SHEET).setSelected(true);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw DocumentProcessorException.exportGenerationFailed();
        }
    }

    private List<FinalCommissionReportDto.Tour> of(List<FinalCommissionReportDto.Tour> tours, String category) {
        return tours.stream().filter(tour -> category.equals(tour.getCategory())).toList();
    }

    private SheetRows writeTourSheet(
            Sheet sheet,
            String title,
            List<FinalCommissionReportDto.Tour> tours,
            Tail tail,
            FinalCommissionReportDto report,
            SheetRows internal,
            Styles styles
    ) {
        setupTourSheet(sheet);
        writeTourHeader(sheet, title, styles);

        int rowIndex = FIRST_DATA_ROW;
        int groupStart = 0;
        for (int i = 0; i < tours.size(); i++) {
            boolean firstInGroup = i == groupStart;
            boolean lastInGroup = i == tours.size() - 1 || !Objects.equals(tours.get(i + 1).getPriority(), tours.get(i).getPriority());
            writeTourRow(sheet, rowIndex + i, tours.get(i), firstInGroup, lastInGroup, styles);
            if (lastInGroup) {
                writeGroupSubtotals(sheet, rowIndex + groupStart, rowIndex + i, styles.fillOf(tours.get(i).getPriority()), styles);
                groupStart = i + 1;
            }
        }
        SheetRows rows = new SheetRows(sheet.getSheetName(), FIRST_DATA_ROW, FIRST_DATA_ROW + tours.size() - 1, -1, -1);
        if (!tours.isEmpty()) {
            addConditionalFormatting(sheet, rows.lastDataRow());
            sheet.setAutoFilter(new CellRangeAddress(FIRST_DATA_ROW - 1, rows.lastDataRow(), 0, OVERTIME_SUBTOTAL_COLUMN));
        }
        return writeTail(sheet, rows, tail, report, internal, styles);
    }

    private void setupTourSheet(Sheet sheet) {
        for (int column = 0; column <= OVERTIME_SUBTOTAL_COLUMN; column++) {
            sheet.setColumnWidth(column, (int) Math.round(tourColumnWidth(column) * COLUMN_WIDTH_UNIT));
        }
        sheet.setColumnHidden(DEPARTURE_HOUR_COLUMN, true);
        sheet.setColumnHidden(RETURN_HOUR_COLUMN, true);
        for (int column = ORDINARY_START_COLUMN; column <= LAST_DAY_COLUMN; column++) {
            sheet.setColumnHidden(column, true);
        }
        sheet.setDisplayGridlines(false);
        sheet.createFreezePane(0, FIRST_DATA_ROW);
    }

    private double tourColumnWidth(int column) {
        if (column < TOUR_WIDTHS.length) {
            return TOUR_WIDTHS[column];
        }
        if (column == SURPLUS_SUBTOTAL_COLUMN) {
            return SURPLUS_SUBTOTAL_WIDTH;
        }
        if (column == OVERTIME_SUBTOTAL_COLUMN) {
            return OVERTIME_SUBTOTAL_WIDTH;
        }
        return DAY_WIDTHS[(column - NEXT_DAYS_COLUMN) % DAY_WIDTH];
    }

    private void writeTourHeader(Sheet sheet, String title, Styles styles) {
        row(sheet, 0).setHeightInPoints(TITLE_ROW_HEIGHT);
        row(sheet, 1).setHeightInPoints(TITLE_ROW_HEIGHT);
        row(sheet, 2).setHeightInPoints(LABEL_ROW_HEIGHT);
        writeMerged(sheet, 0, 0, 0, PRIORITY_COLUMN, title, styles.header);
        writeMerged(sheet, 0, 1, DEPARTURE_TIME_COLUMN, RETURN_DATE_COLUMN, DATES_HEADER, styles.header);
        writeMerged(sheet, 0, 1, ORDINARY_HOURS_COLUMN, OVERTIME_HOURS_COLUMN, SUMS_HEADER, styles.header);
        writeMerged(sheet, 0, 0, ORDINARY_START_COLUMN, ORDINARY_END_COLUMN, null, styles.header);
        writeMerged(sheet, 0, 0, FIRST_DAY_COLUMN, LAST_DAY_COLUMN, HOURS_HEADER, styles.header);
        writeMerged(sheet, 0, 1, SURPLUS_SUBTOTAL_COLUMN, OVERTIME_SUBTOTAL_COLUMN, TOTALS_HEADER, styles.header);
        writeMerged(sheet, 1, 1, 0, PRIORITY_COLUMN, GENERAL_HEADER, styles.header);
        writeMerged(sheet, 1, 1, ORDINARY_START_COLUMN, ORDINARY_END_COLUMN, SCHEDULE_HEADER, styles.header);
        for (int day = 0; day < MAX_DAYS; day++) {
            writeMerged(sheet, 1, 1, dayFirstColumn(day), dayLastColumn(day), DAY_HEADER + (day + 1), styles.header);
        }

        for (int column = 0; column < TOUR_LABELS.length; column++) {
            cell(sheet, 2, column, styles.label(column == 0, false)).setCellValue(TOUR_LABELS[column] == null ? "" : TOUR_LABELS[column]);
        }
        for (int day = 0; day < MAX_DAYS; day++) {
            String[] labels = day == 0 ? FIRST_DAY_LABELS : NEXT_DAY_LABELS;
            for (int offset = 0; offset < labels.length; offset++) {
                int column = dayFirstColumn(day) + offset;
                cell(sheet, 2, column, styles.label(offset == 0, offset == labels.length - 1)).setCellValue(labels[offset]);
            }
        }
        cell(sheet, 2, SURPLUS_SUBTOTAL_COLUMN, styles.label(true, true)).setCellValue(SURPLUS_SUBTOTAL_LABEL);
        cell(sheet, 2, OVERTIME_SUBTOTAL_COLUMN, styles.label(true, true)).setCellValue(OVERTIME_SUBTOTAL_LABEL);
    }

    private void writeTourRow(Sheet sheet, int rowIndex, FinalCommissionReportDto.Tour tour, boolean first, boolean last, Styles styles) {
        byte[] fill = styles.fillOf(tour.getPriority());
        IntFunction<CellStyle> style = column -> styles.data(kindOf(column), fill, first, last,
                leftBorderColumn(column), rightBorderColumn(column));
        int excelRow = rowIndex + 1;
        int vehicles = Math.max(1, tour.getVehicles());
        String multiplier = vehicles > 1 ? "*" + vehicles : "";

        Cell numberCell = cell(sheet, rowIndex, 0, style.apply(0));
        if (rowIndex == FIRST_DATA_ROW) {
            numberCell.setCellValue(1);
        } else {
            numberCell.setCellFormula("1+A" + rowIndex);
        }
        setValue(cell(sheet, rowIndex, TOUR_COLUMN, style.apply(TOUR_COLUMN)), tourNumber(tour.getNumber()));
        setValue(cell(sheet, rowIndex, PASSENGERS_COLUMN, style.apply(PASSENGERS_COLUMN)), tour.getPassengers());
        setValue(cell(sheet, rowIndex, 3, style.apply(3)), tour.getExecutingUnit());
        setValue(cell(sheet, rowIndex, 4, style.apply(4)), tour.getResponsible());
        setValue(cell(sheet, rowIndex, DESTINATION_COLUMN, style.apply(DESTINATION_COLUMN)), tour.getDestination());
        setValue(cell(sheet, rowIndex, DURATION_COLUMN, style.apply(DURATION_COLUMN)), tour.getDurationDays());
        setValue(cell(sheet, rowIndex, PRIORITY_COLUMN, style.apply(PRIORITY_COLUMN)), tour.getPriority());
        setTime(cell(sheet, rowIndex, DEPARTURE_TIME_COLUMN, style.apply(DEPARTURE_TIME_COLUMN)), tour.getDepartureTime());
        cell(sheet, rowIndex, DEPARTURE_HOUR_COLUMN, style.apply(DEPARTURE_HOUR_COLUMN)).setCellFormula("+$I" + excelRow + "*24");
        setTime(cell(sheet, rowIndex, RETURN_TIME_COLUMN, style.apply(RETURN_TIME_COLUMN)), tour.getReturnTime());
        cell(sheet, rowIndex, RETURN_HOUR_COLUMN, style.apply(RETURN_HOUR_COLUMN)).setCellFormula("+K" + excelRow + "*24");
        cell(sheet, rowIndex, DEPARTURE_WEEKDAY_COLUMN, style.apply(DEPARTURE_WEEKDAY_COLUMN))
                .setCellFormula(String.format(WEEKDAY_FORMULA, ref(DEPARTURE_DATE_COLUMN, rowIndex)));
        Cell departureDate = cell(sheet, rowIndex, DEPARTURE_DATE_COLUMN, style.apply(DEPARTURE_DATE_COLUMN));
        if (tour.getDepartureDate() != null) {
            departureDate.setCellValue(tour.getDepartureDate());
        }
        cell(sheet, rowIndex, RETURN_WEEKDAY_COLUMN, style.apply(RETURN_WEEKDAY_COLUMN))
                .setCellFormula(String.format(WEEKDAY_FORMULA, ref(RETURN_DATE_COLUMN, rowIndex)));
        Cell returnDate = cell(sheet, rowIndex, RETURN_DATE_COLUMN, style.apply(RETURN_DATE_COLUMN));
        if (tour.getReturnDate() != null) {
            returnDate.setCellValue(tour.getReturnDate());
        }
        cell(sheet, rowIndex, ORDINARY_HOURS_COLUMN, style.apply(ORDINARY_HOURS_COLUMN))
                .setCellFormula("+G" + excelRow + "*" + ORDINARY_HOURS_PER_DAY + multiplier);
        cell(sheet, rowIndex, SURPLUS_HOURS_COLUMN, style.apply(SURPLUS_HOURS_COLUMN))
                .setCellFormula("+(" + dayRefs(rowIndex, this::surplusColumn) + ")" + multiplier);
        cell(sheet, rowIndex, OVERTIME_HOURS_COLUMN, style.apply(OVERTIME_HOURS_COLUMN))
                .setCellFormula("+(" + dayRefs(rowIndex, this::overtimeColumn) + ")" + multiplier);
        cell(sheet, rowIndex, ORDINARY_START_COLUMN, style.apply(ORDINARY_START_COLUMN)).setCellValue(tour.getOrdinaryStart());
        cell(sheet, rowIndex, ORDINARY_END_COLUMN, style.apply(ORDINARY_END_COLUMN)).setCellValue(tour.getOrdinaryEnd());

        List<FinalCommissionReportDto.Day> days = tour.getDays() == null ? List.of() : tour.getDays();
        for (int day = 0; day < MAX_DAYS; day++) {
            writeDay(sheet, rowIndex, day, day < days.size() ? days.get(day) : null, style);
        }
    }

    private void writeDay(Sheet sheet, int rowIndex, int day, FinalCommissionReportDto.Day value, IntFunction<CellStyle> style) {
        int start = startColumn(day);
        String weekday = day == 0 ? "$" + ref(DEPARTURE_WEEKDAY_COLUMN, rowIndex) : ref(dayFirstColumn(day), rowIndex);
        if (day > 0) {
            Cell weekdayCell = cell(sheet, rowIndex, dayFirstColumn(day), style.apply(dayFirstColumn(day)));
            Cell dateCell = cell(sheet, rowIndex, dayFirstColumn(day) + 1, style.apply(dayFirstColumn(day) + 1));
            if (value == null) {
                weekdayCell.setCellValue(0);
            } else {
                dateCell.setCellValue(value.getDate());
                weekdayCell.setCellFormula(String.format(WEEKDAY_FORMULA, ref(dayFirstColumn(day) + 1, rowIndex)));
            }
        }
        Cell startCell = cell(sheet, rowIndex, start, style.apply(start));
        Cell endCell = cell(sheet, rowIndex, start + 1, style.apply(start + 1));
        Cell totalCell = cell(sheet, rowIndex, start + 2, style.apply(start + 2));
        Cell surplusCell = cell(sheet, rowIndex, start + 3, style.apply(start + 3));
        Cell overtimeCell = cell(sheet, rowIndex, start + 4, style.apply(start + 4));
        if (value == null) {
            startCell.setCellValue(0);
            endCell.setCellValue(0);
            totalCell.setCellValue(0);
            surplusCell.setCellValue(0);
            overtimeCell.setCellValue(0);
            return;
        }
        String startRef = ref(start, rowIndex);
        String endRef = ref(start + 1, rowIndex);
        String totalRef = ref(start + 2, rowIndex);
        String weekend = String.format(WEEKEND_TEST, weekday);
        String ordinaryStart = "$" + ref(ORDINARY_START_COLUMN, rowIndex);
        String ordinaryEnd = "$" + ref(ORDINARY_END_COLUMN, rowIndex);
        startCell.setCellValue(value.getStart());
        endCell.setCellValue(value.getEnd());
        totalCell.setCellFormula("+IF(" + weekend + "," + endRef + "-" + startRef + ",IF(" + startRef + "<" + ordinaryStart + ","
                + ordinaryStart + "-" + startRef + ",0)+IF(" + endRef + ">" + ordinaryEnd + "," + endRef + "-" + ordinaryEnd + ",0))");
        surplusCell.setCellFormula("+IF(" + weekend + ",0," + totalRef + ")");
        overtimeCell.setCellFormula("+IF(" + weekend + "," + totalRef + ",0)");
    }

    private void writeGroupSubtotals(Sheet sheet, int firstRow, int lastRow, byte[] fill, Styles styles) {
        CellStyle subtotal = styles.subtotal(fill);
        Cell surplus = writeMerged(sheet, firstRow, lastRow, SURPLUS_SUBTOTAL_COLUMN, SURPLUS_SUBTOTAL_COLUMN, null, subtotal);
        surplus.setCellFormula("+SUM(" + range(SURPLUS_HOURS_COLUMN, firstRow, lastRow) + ")");
        Cell overtime = writeMerged(sheet, firstRow, lastRow, OVERTIME_SUBTOTAL_COLUMN, OVERTIME_SUBTOTAL_COLUMN, null, subtotal);
        String overtimeSum = "SUM(" + range(OVERTIME_HOURS_COLUMN, firstRow, lastRow) + ")";
        overtime.setCellFormula("+IF(" + overtimeSum + "=0,0," + overtimeSum + ")");
    }

    private void addConditionalFormatting(Sheet sheet, int lastDataRow) {
        SheetConditionalFormatting formatting = sheet.getSheetConditionalFormatting();
        int first = FIRST_DATA_ROW;
        ConditionalFormattingRule passengers = formatting.createConditionalFormattingRule(ComparisonOperator.GT, "32");
        format(passengers, RED, true, YELLOW);
        formatting.addConditionalFormatting(new CellRangeAddress[]{new CellRangeAddress(first, lastDataRow, PASSENGERS_COLUMN, PASSENGERS_COLUMN)}, passengers);

        ConditionalFormattingRule weekendDates = formatting.createConditionalFormattingRule(
                String.format(WEEKEND_TEST, ref(DEPARTURE_WEEKDAY_COLUMN, first)));
        format(weekendDates, WEEKEND_FONT, false, WEEKEND_FILL);
        formatting.addConditionalFormatting(new CellRangeAddress[]{
                new CellRangeAddress(first, lastDataRow, DEPARTURE_WEEKDAY_COLUMN, DEPARTURE_WEEKDAY_COLUMN),
                new CellRangeAddress(first, lastDataRow, RETURN_WEEKDAY_COLUMN, RETURN_WEEKDAY_COLUMN)
        }, weekendDates);

        ConditionalFormattingRule weekendDays = formatting.createConditionalFormattingRule(
                String.format(WEEKEND_TEST, ref(dayFirstColumn(1), first)));
        format(weekendDays, WEEKEND_FONT, false, WEEKEND_FILL);
        formatting.addConditionalFormatting(columns(first, lastDataRow, IntStream.range(1, MAX_DAYS).map(this::dayFirstColumn)), weekendDays);

        ConditionalFormattingRule surplus = formatting.createConditionalFormattingRule(ComparisonOperator.GT, "4");
        format(surplus, LIMIT_FONT, false, LIMIT_FILL);
        formatting.addConditionalFormatting(columns(first, lastDataRow, IntStream.range(0, MAX_DAYS).map(this::surplusColumn)), surplus);

        ConditionalFormattingRule overtime = formatting.createConditionalFormattingRule(ComparisonOperator.GT, "12");
        format(overtime, LIMIT_FONT, false, LIMIT_FILL);
        formatting.addConditionalFormatting(columns(first, lastDataRow, IntStream.range(0, MAX_DAYS).map(this::overtimeColumn)), overtime);
    }

    private CellRangeAddress[] columns(int firstRow, int lastRow, IntStream columns) {
        return columns.mapToObj(column -> new CellRangeAddress(firstRow, lastRow, column, column)).toArray(CellRangeAddress[]::new);
    }

    private void format(ConditionalFormattingRule rule, byte[] fontColor, boolean bold, byte[] fillColor) {
        FontFormatting font = rule.createFontFormatting();
        font.setFontColor(new XSSFColor(fontColor, new DefaultIndexedColorMap()));
        if (bold) {
            font.setFontStyle(false, true);
        }
        PatternFormatting fill = rule.createPatternFormatting();
        fill.setFillBackgroundColor(new XSSFColor(fillColor, new DefaultIndexedColorMap()));
        fill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
    }

    private SheetRows writeTail(Sheet sheet, SheetRows rows, Tail tail, FinalCommissionReportDto report, SheetRows internal, Styles styles) {
        int workedRow = (rows.isEmpty() ? FIRST_DATA_ROW - 1 : rows.lastDataRow()) + TAIL_GAP;
        int overtimeRow = workedRow + TAIL_STEP;
        int availableRow = overtimeRow + TAIL_STEP;
        int requiredRow = availableRow + TAIL_STEP;
        int usageRow = requiredRow + TAIL_STEP;

        writeTailLabel(sheet, workedRow, WORKED_HOURS_LABEL, styles);
        writeFormula(sheet, workedRow, ORDINARY_HOURS_COLUMN, subtotal(rows, ORDINARY_HOURS_COLUMN), styles.tailValue);
        writeFormula(sheet, workedRow, SURPLUS_HOURS_COLUMN, subtotal(rows, SURPLUS_HOURS_COLUMN), styles.tailValue);
        writeTailLabel(sheet, overtimeRow, OVERTIME_LABEL, styles);
        cell(sheet, overtimeRow, ORDINARY_HOURS_COLUMN, styles.tailValue).setCellValue(WEEKEND_LABEL);
        writeFormula(sheet, overtimeRow, SURPLUS_HOURS_COLUMN, subtotal(rows, OVERTIME_HOURS_COLUMN), styles.tailValue);
        if (tail == Tail.SHORT) {
            return new SheetRows(rows.name(), rows.firstDataRow(), rows.lastDataRow(), workedRow, -1);
        }

        writeTailLabel(sheet, availableRow, AVAILABLE_LABEL, styles);
        Cell ordinaryAvailable = cell(sheet, availableRow, ORDINARY_HOURS_COLUMN, styles.tailValue);
        Cell surplusAvailable = cell(sheet, availableRow, SURPLUS_HOURS_COLUMN, styles.tailValue);
        if (tail == Tail.REMAINING && internal != null && internal.availableRow() >= 0) {
            String source = sheetRef(internal.name());
            ordinaryAvailable.setCellFormula("+" + source + ref(ORDINARY_HOURS_COLUMN, internal.availableRow())
                    + "-" + source + ref(ORDINARY_HOURS_COLUMN, internal.workedRow()));
            surplusAvailable.setCellFormula("+" + source + ref(SURPLUS_HOURS_COLUMN, internal.availableRow())
                    + "-" + source + ref(SURPLUS_HOURS_COLUMN, internal.workedRow()));
        } else {
            ordinaryAvailable.setCellValue((double) report.getActiveDrivers() * report.getBusinessDays() * ORDINARY_HOURS_PER_DAY);
            surplusAvailable.setCellValue(report.getActiveDrivers() * report.getSurplusCapPerDriver());
        }
        SheetRows result = new SheetRows(rows.name(), rows.firstDataRow(), rows.lastDataRow(), workedRow, availableRow);
        if (tail != Tail.FULL) {
            return result;
        }

        String worked = ref(SURPLUS_HOURS_COLUMN, workedRow);
        String available = ref(SURPLUS_HOURS_COLUMN, availableRow);
        writeTailLabel(sheet, requiredRow, REQUIRED_OVERTIME_LABEL, styles);
        writeFormula(sheet, requiredRow, ORDINARY_HOURS_COLUMN, "(IF(" + available + "<" + worked + "," + worked + "-" + available + ",0))+"
                + ref(SURPLUS_HOURS_COLUMN, overtimeRow), styles.tailValueRed);
        writeFormula(sheet, requiredRow, OVERTIME_HOURS_COLUMN, "+" + ref(ORDINARY_HOURS_COLUMN, requiredRow) + "*" + OVERTIME_HOUR_COST, styles.tailValueRed);
        writeTailLabel(sheet, usageRow, USAGE_LABEL, styles);
        writeFormula(sheet, usageRow, ORDINARY_HOURS_COLUMN, ratio(ref(ORDINARY_HOURS_COLUMN, workedRow), ref(ORDINARY_HOURS_COLUMN, availableRow)), styles.tailPercent);
        writeFormula(sheet, usageRow, SURPLUS_HOURS_COLUMN, ratio(worked, available), styles.tailPercent);
        return result;
    }

    private void writeTailLabel(Sheet sheet, int rowIndex, String label, Styles styles) {
        cell(sheet, rowIndex, TAIL_LABEL_COLUMN, styles.tailLabelStart).setCellValue(label);
        for (int column = TAIL_LABEL_COLUMN + 1; column < TAIL_LABEL_LAST_COLUMN; column++) {
            cell(sheet, rowIndex, column, styles.tailLabelMiddle);
        }
        cell(sheet, rowIndex, TAIL_LABEL_LAST_COLUMN, styles.tailLabelEnd);
    }

    private String subtotal(SheetRows rows, int column) {
        return rows.isEmpty() ? "0" : "SUBTOTAL(9," + range(column, rows.firstDataRow(), rows.lastDataRow()) + ")";
    }

    private String ratio(String numerator, String denominator) {
        return "IF(" + denominator + "=0,0," + numerator + "/" + denominator + ")";
    }

    private void writeSummary(Sheet sheet, String period, List<SheetRows> categories, Styles styles) {
        for (int column = 0; column < SUMMARY_WIDTHS.length; column++) {
            sheet.setColumnWidth(column, (int) Math.round(SUMMARY_WIDTHS[column] * COLUMN_WIDTH_UNIT));
        }
        sheet.setDisplayGridlines(false);

        writeMerged(sheet, 1, 1, 1, 7, SUMMARY_COUNT_TITLE, styles.summaryTitle);
        writeMerged(sheet, 1, 1, 9, 19, SUMMARY_HOURS_TITLE, styles.summaryTitle);
        writeMerged(sheet, 2, 2, 1, 7, "(" + period + ")", styles.summaryHeader);
        writeMerged(sheet, 2, 2, 9, 19, null, styles.summaryHeader).setCellFormula("+B3");

        row(sheet, 4).setHeightInPoints(SUMMARY_HEADER_HEIGHT);
        String[] countHeaders = {"PRIORIDAD", "RECURSOS\nINTERNOS", "RECURSOS\nEXTERNOS", "PRESTAMO\nVEHICULO", "RECHAZADAS", "TOTAL DE GIRAS", "%"};
        for (int i = 0; i < countHeaders.length; i++) {
            writeMerged(sheet, 3, 4, 1 + i, 1 + i, countHeaders[i], styles.summaryHeader);
        }
        writeMerged(sheet, 3, 4, 9, 9, "PRIORIDAD", styles.summaryHeader);
        String[] hourHeaders = {"R.INTERNOS", "R.EXTERNOS", "P.VEHICULO", "RECHAZADAS"};
        for (int i = 0; i < hourHeaders.length; i++) {
            int column = 10 + i * 2;
            writeMerged(sheet, 3, 3, column, column + 1, hourHeaders[i], styles.summaryHeader);
            cell(sheet, 4, column, styles.summaryHeader).setCellValue("HORAS SOBRE");
            cell(sheet, 4, column + 1, styles.summaryHeaderRed).setCellValue("HORAS EXTRA");
        }
        writeMerged(sheet, 3, 4, 18, 18, "TOTAL DE HORAS", styles.summaryHeader);
        writeMerged(sheet, 3, 4, 19, 19, "%", styles.summaryHeader);

        int firstRow = 5;
        int totalRow = firstRow + SUMMARY_PRIORITIES;
        for (int priority = 1; priority <= SUMMARY_PRIORITIES; priority++) {
            int rowIndex = firstRow + priority - 1;
            boolean lastPriority = priority == SUMMARY_PRIORITIES;
            int excelRow = rowIndex + 1;
            cell(sheet, rowIndex, 1, styles.summaryKey(lastPriority)).setCellValue(priority);
            for (int i = 0; i < categories.size(); i++) {
                writeFormula(sheet, rowIndex, 2 + i, countIf(categories.get(i), "$B" + excelRow), styles.summaryCount(lastPriority));
                writeFormula(sheet, rowIndex, 10 + i * 2, sumIf(categories.get(i), "$J" + excelRow, SURPLUS_HOURS_COLUMN), styles.summaryHours(lastPriority));
                writeFormula(sheet, rowIndex, 11 + i * 2, sumIf(categories.get(i), "$J" + excelRow, OVERTIME_HOURS_COLUMN), styles.summaryHours(lastPriority));
            }
            writeFormula(sheet, rowIndex, 6, "SUM(C" + excelRow + ":F" + excelRow + ")", styles.summaryKeyCount(lastPriority));
            writeFormula(sheet, rowIndex, 7, ratio("G" + excelRow, "$G$" + (totalRow + 1)), styles.summaryKeyPercent(lastPriority));
            cell(sheet, rowIndex, 9, styles.summaryKey(lastPriority)).setCellValue(priority);
            writeFormula(sheet, rowIndex, 18, "SUM(K" + excelRow + ":R" + excelRow + ")", styles.summaryKeyHours(lastPriority));
            writeFormula(sheet, rowIndex, 19, ratio("S" + excelRow, "$S$" + (totalRow + 1)), styles.summaryKeyDecimalPercent(lastPriority));
        }

        cell(sheet, totalRow, 1, styles.summaryKey(true)).setCellValue("TOTAL");
        cell(sheet, totalRow, 9, styles.summaryKey(true)).setCellValue("TOTAL");
        for (int column = 2; column <= 6; column++) {
            writeFormula(sheet, totalRow, column, "SUM(" + range(column, firstRow, totalRow - 1) + ")", styles.summaryKeyCount(true));
        }
        writeFormula(sheet, totalRow, 7, "SUM(" + range(7, firstRow, totalRow - 1) + ")", styles.summaryKeyPercent(true));
        for (int column = 10; column <= 17; column++) {
            writeFormula(sheet, totalRow, column, "SUM(" + range(column, firstRow, totalRow - 1) + ")", styles.summaryKeyHours(true));
        }
        writeFormula(sheet, totalRow, 18, "SUM(K" + (totalRow + 1) + ":R" + (totalRow + 1) + ")", styles.summaryKeyHours(true));
        writeFormula(sheet, totalRow, 19, "SUM(" + range(19, firstRow, totalRow - 1) + ")", styles.summaryKeyDecimalPercent(true));

        writeSummaryFooter(sheet, totalRow + 1, styles);
    }

    private void writeSummaryFooter(Sheet sheet, int totalExcelRow, Styles styles) {
        String total = String.valueOf(totalExcelRow);
        cell(sheet, 17, 1, styles.summaryFooterLabel).setCellValue("Total de Giras");
        writeFormula(sheet, 17, 2, "+G" + total, styles.summaryFooterCount);
        cell(sheet, 17, 3, styles.summaryFooterPercent).setCellValue(1);
        cell(sheet, 18, 1, styles.summaryFooterLabel).setCellValue("Atendidas");
        writeFormula(sheet, 18, 2, "+C" + total + "+D" + total + "+E" + total, styles.summaryFooterCount);
        writeFormula(sheet, 18, 3, ratio("C19", "C18"), styles.summaryFooterPercent);
        cell(sheet, 19, 1, styles.summaryFooterLabel).setCellValue("Rechazadas");
        writeFormula(sheet, 19, 2, "+F" + total, styles.summaryFooterCount);
        writeFormula(sheet, 19, 3, ratio("C20", "C18"), styles.summaryFooterPercent);

        String[] headers = {"MODAL", "SOBRES", "EXTRAS", "TOTAL"};
        for (int i = 0; i < headers.length; i++) {
            cell(sheet, 17, 11 + i, styles.summaryFooterLabel).setCellValue(headers[i]);
        }
        cell(sheet, 18, 9, styles.summaryFooterLabel).setCellValue("Total de Giras");
        writeFormula(sheet, 18, 10, "+G" + total, styles.summaryFooterCount);
        writeFormula(sheet, 18, 12, "+K" + total + "+M" + total + "+Q" + total, styles.summaryFooterHours);
        writeFormula(sheet, 18, 13, "+L" + total + "+N" + total + "+R" + total, styles.summaryFooterHours);
        writeFormula(sheet, 18, 14, "N19+M19", styles.summaryFooterHours);
        cell(sheet, 18, 15, styles.summaryFooterPercent).setCellValue(1);

        writeMerged(sheet, 19, 20, 9, 9, "Atendidas", styles.summaryFooterLabel);
        writeMerged(sheet, 19, 20, 10, 10, null, styles.summaryFooterCount).setCellFormula("+C" + total + "+D" + total + "+E" + total);
        writeFooterLine(sheet, 19, "R.E", "M", "N", total, styles);
        writeFooterLine(sheet, 20, "R.I", "K", "L", total, styles);
        writeFormula(sheet, 20, 16, "+N21", styles.summaryFooterHours);
        writeMerged(sheet, 20, 20, 17, 19, null, styles.summaryFooterHours).setCellFormula(OVERTIME_HOUR_COST + "*Q21");
        cell(sheet, 21, 9, styles.summaryFooterLabel).setCellValue("Rechazadas");
        writeFormula(sheet, 21, 10, "+F" + total, styles.summaryFooterCount);
        writeFormula(sheet, 21, 12, "+Q" + total, styles.summaryFooterHours);
        writeFormula(sheet, 21, 13, "+R" + total, styles.summaryFooterHours);
        writeFormula(sheet, 21, 14, "SUM(M22:N22)", styles.summaryFooterHours);
        writeFormula(sheet, 21, 15, ratio("O22", "$O$19"), styles.summaryFooterPercent);
        cell(sheet, 23, 9, styles.summaryNote).setCellValue(SUMMARY_NOTE);
    }

    private void writeFooterLine(Sheet sheet, int rowIndex, String label, String surplusColumn, String overtimeColumn, String total, Styles styles) {
        int excelRow = rowIndex + 1;
        cell(sheet, rowIndex, 11, styles.summaryFooterLabel).setCellValue(label);
        writeFormula(sheet, rowIndex, 12, "+" + surplusColumn + total, styles.summaryFooterHours);
        writeFormula(sheet, rowIndex, 13, "+" + overtimeColumn + total, styles.summaryFooterHours);
        writeFormula(sheet, rowIndex, 14, "SUM(M" + excelRow + ":N" + excelRow + ")", styles.summaryFooterHours);
        writeFormula(sheet, rowIndex, 15, ratio("O" + excelRow, "$O$19"), styles.summaryFooterPercent);
    }

    private String countIf(SheetRows rows, String priority) {
        return rows.isEmpty() ? "0"
                : "COUNTIF(" + sheetRef(rows.name()) + absoluteRange(PRIORITY_COLUMN, rows) + "," + priority + ")";
    }

    private String sumIf(SheetRows rows, String priority, int column) {
        return rows.isEmpty() ? "0"
                : "SUMIF(" + sheetRef(rows.name()) + absoluteRange(PRIORITY_COLUMN, rows) + "," + priority + ","
                + sheetRef(rows.name()) + absoluteRange(column, rows) + ")";
    }

    private String absoluteRange(int column, SheetRows rows) {
        String letter = CellReference.convertNumToColString(column);
        return "$" + letter + "$" + (rows.firstDataRow() + 1) + ":$" + letter + "$" + (rows.lastDataRow() + 1);
    }

    private String sheetRef(String sheetName) {
        return "'" + sheetName + "'!";
    }

    private String dayRefs(int rowIndex, IntFunction<Integer> column) {
        return IntStream.range(0, MAX_DAYS).mapToObj(day -> ref(column.apply(day), rowIndex)).collect(Collectors.joining("+"));
    }

    private int dayFirstColumn(int day) {
        return day == 0 ? FIRST_DAY_COLUMN : NEXT_DAYS_COLUMN + (day - 1) * DAY_WIDTH;
    }

    private int dayLastColumn(int day) {
        return startColumn(day) + 4;
    }

    private int startColumn(int day) {
        return day == 0 ? FIRST_DAY_COLUMN : dayFirstColumn(day) + 2;
    }

    private int surplusColumn(int day) {
        return startColumn(day) + 3;
    }

    private int overtimeColumn(int day) {
        return startColumn(day) + 4;
    }

    private boolean leftBorderColumn(int column) {
        if (column == 0 || column == FIRST_DAY_COLUMN || column >= SURPLUS_SUBTOTAL_COLUMN) {
            return true;
        }
        return column >= NEXT_DAYS_COLUMN && column <= LAST_DAY_COLUMN && (column - NEXT_DAYS_COLUMN) % DAY_WIDTH == 0;
    }

    private boolean rightBorderColumn(int column) {
        if (column == FIRST_DAY_COLUMN + 4 || column >= SURPLUS_SUBTOTAL_COLUMN) {
            return true;
        }
        return column >= NEXT_DAYS_COLUMN && column <= LAST_DAY_COLUMN && (column - NEXT_DAYS_COLUMN) % DAY_WIDTH == DAY_WIDTH - 1;
    }

    private Kind kindOf(int column) {
        if (column == 3 || column == 4 || column == DESTINATION_COLUMN) {
            return Kind.WRAPPED_TEXT;
        }
        if (column == DEPARTURE_TIME_COLUMN || column == RETURN_TIME_COLUMN) {
            return Kind.TIME;
        }
        if (column == DEPARTURE_HOUR_COLUMN || column == RETURN_HOUR_COLUMN || column == ORDINARY_START_COLUMN || column == ORDINARY_END_COLUMN) {
            return Kind.DECIMAL;
        }
        if (column == DEPARTURE_DATE_COLUMN || column == RETURN_DATE_COLUMN) {
            return Kind.LONG_DATE;
        }
        if (column == ORDINARY_HOURS_COLUMN) {
            return Kind.ORDINARY_HOURS;
        }
        if (column == SURPLUS_HOURS_COLUMN || column == OVERTIME_HOURS_COLUMN) {
            return Kind.HOURS;
        }
        if (column >= NEXT_DAYS_COLUMN && column <= LAST_DAY_COLUMN) {
            int offset = (column - NEXT_DAYS_COLUMN) % DAY_WIDTH;
            return offset == 0 ? Kind.TEXT : offset == 1 ? Kind.SHORT_DATE : Kind.HOURS;
        }
        if (column >= FIRST_DAY_COLUMN && column <= LAST_DAY_COLUMN) {
            return Kind.HOURS;
        }
        return Kind.TEXT;
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

    private Row row(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row == null ? sheet.createRow(rowIndex) : row;
    }

    private Cell cell(Sheet sheet, int rowIndex, int column, CellStyle style) {
        Row row = row(sheet, rowIndex);
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

    private void setTime(Cell cell, LocalTime time) {
        if (time != null) {
            cell.setCellValue((time.getHour() + time.getMinute() / 60.0) / 24.0);
        }
    }

    private Object tourNumber(String number) {
        if (number == null) {
            return null;
        }
        String trimmed = number.trim();
        return !trimmed.isEmpty() && trimmed.chars().allMatch(Character::isDigit) ? Long.parseLong(trimmed) : trimmed;
    }

    private String range(int column, int firstRow, int lastRow) {
        return ref(column, firstRow) + ":" + ref(column, lastRow);
    }

    private static String ref(int column, int rowIndex) {
        return CellReference.convertNumToColString(column) + (rowIndex + 1);
    }

    private static byte[] rgb(int red, int green, int blue) {
        return new byte[]{(byte) red, (byte) green, (byte) blue};
    }

    private enum Tail {
        FULL,
        AVAILABLE,
        SHORT,
        REMAINING
    }

    private enum Kind {
        TEXT,
        WRAPPED_TEXT,
        TIME,
        DECIMAL,
        LONG_DATE,
        SHORT_DATE,
        ORDINARY_HOURS,
        HOURS
    }

    private record SheetRows(String name, int firstDataRow, int lastDataRow, int workedRow, int availableRow) {

        boolean isEmpty() {
            return lastDataRow < firstDataRow;
        }
    }

    private static final class Styles {
        private final XSSFWorkbook workbook;
        private final Map<String, XSSFCellStyle> cache = new HashMap<>();
        private final XSSFFont plain;
        private final XSSFFont bold;
        private final XSSFFont boldRed;
        private final XSSFFont subtotalFont;

        private final CellStyle header;
        private final CellStyle tailLabelStart;
        private final CellStyle tailLabelMiddle;
        private final CellStyle tailLabelEnd;
        private final CellStyle tailValue;
        private final CellStyle tailValueRed;
        private final CellStyle tailPercent;
        private final CellStyle summaryTitle;
        private final CellStyle summaryHeader;
        private final CellStyle summaryHeaderRed;
        private final CellStyle summaryFooterLabel;
        private final CellStyle summaryFooterCount;
        private final CellStyle summaryFooterHours;
        private final CellStyle summaryFooterPercent;
        private final CellStyle summaryNote;

        private Styles(XSSFWorkbook workbook) {
            this.workbook = workbook;
            plain = font(false, FONT_SIZE, null);
            bold = font(true, FONT_SIZE, null);
            boldRed = font(true, FONT_SIZE, RED);
            subtotalFont = font(true, SUBTOTAL_FONT_SIZE, null);

            header = allMedium(base(bold, HEADER_FILL, null, true));
            tailLabelStart = borders(base(bold, HEADER_FILL, null, false), BorderStyle.MEDIUM, BorderStyle.MEDIUM, BorderStyle.MEDIUM, null);
            tailLabelStart.setAlignment(HorizontalAlignment.LEFT);
            tailLabelMiddle = borders(base(bold, HEADER_FILL, null, false), BorderStyle.MEDIUM, BorderStyle.MEDIUM, null, null);
            tailLabelEnd = borders(base(bold, HEADER_FILL, null, false), BorderStyle.MEDIUM, BorderStyle.MEDIUM, null, BorderStyle.MEDIUM);
            tailValue = allMedium(base(bold, HEADER_FILL, TOTAL_HOURS_FORMAT, false));
            tailValueRed = allMedium(base(boldRed, HEADER_FILL, TOTAL_HOURS_FORMAT, false));
            tailPercent = allMedium(base(bold, HEADER_FILL, DECIMAL_PERCENT_FORMAT, false));

            summaryTitle = borders(base(bold, SUMMARY_FILL, null, false), BorderStyle.THIN, null, null, null);
            summaryHeader = borders(base(bold, SUMMARY_FILL, null, true), null, BorderStyle.DOUBLE, null, null);
            summaryHeaderRed = borders(base(boldRed, SUMMARY_FILL, null, true), null, BorderStyle.DOUBLE, null, null);
            summaryFooterLabel = base(bold, SUMMARY_FILL, null, false);
            summaryFooterLabel.setAlignment(HorizontalAlignment.LEFT);
            summaryFooterCount = base(bold, SUMMARY_FILL, INTEGER_FORMAT, false);
            summaryFooterHours = base(bold, SUMMARY_FILL, TOTAL_HOURS_FORMAT, false);
            summaryFooterPercent = base(bold, SUMMARY_FILL, PERCENT_FORMAT, false);
            summaryNote = base(plain, null, null, false);
            summaryNote.setAlignment(HorizontalAlignment.LEFT);
        }

        private byte[] fillOf(Integer priority) {
            return priority == null ? DEFAULT_PRIORITY_FILL : PRIORITY_FILLS.getOrDefault(priority, DEFAULT_PRIORITY_FILL);
        }

        private CellStyle label(boolean left, boolean right) {
            return cached("label|" + left + "|" + right, () -> borders(base(bold, HEADER_FILL, null, true),
                    BorderStyle.MEDIUM, null, left ? BorderStyle.MEDIUM : BorderStyle.THIN, right ? BorderStyle.MEDIUM : BorderStyle.THIN));
        }

        private CellStyle data(Kind kind, byte[] fill, boolean top, boolean bottom, boolean left, boolean right) {
            String key = "data|" + kind + "|" + HexFormat.of().formatHex(fill) + "|" + top + bottom + left + right;
            return cached(key, () -> {
                XSSFCellStyle style = base(kind == Kind.ORDINARY_HOURS ? bold : plain, fill, formatOf(kind), kind == Kind.WRAPPED_TEXT);
                if (kind == Kind.SHORT_DATE) {
                    style.setDataFormat(SHORT_DATE_FORMAT);
                }
                return borders(style, top ? BorderStyle.MEDIUM : null, bottom ? BorderStyle.MEDIUM : null,
                        left ? BorderStyle.MEDIUM : null, right ? BorderStyle.MEDIUM : null);
            });
        }

        private CellStyle subtotal(byte[] fill) {
            return cached("subtotal|" + HexFormat.of().formatHex(fill), () -> allMedium(base(subtotalFont, fill, HOURS_FORMAT, false)));
        }

        private CellStyle summaryKey(boolean doubleBottom) {
            return summaryStyle("key", bold, SUMMARY_FILL, null, doubleBottom);
        }

        private CellStyle summaryKeyCount(boolean doubleBottom) {
            return summaryStyle("keyCount", bold, SUMMARY_FILL, INTEGER_FORMAT, doubleBottom);
        }

        private CellStyle summaryKeyPercent(boolean doubleBottom) {
            return summaryStyle("keyPercent", bold, SUMMARY_FILL, PERCENT_FORMAT, doubleBottom);
        }

        private CellStyle summaryKeyHours(boolean doubleBottom) {
            return summaryStyle("keyHours", bold, SUMMARY_FILL, TOTAL_HOURS_FORMAT, doubleBottom);
        }

        private CellStyle summaryKeyDecimalPercent(boolean doubleBottom) {
            return summaryStyle("keyDecimalPercent", bold, SUMMARY_FILL, DECIMAL_PERCENT_FORMAT, doubleBottom);
        }

        private CellStyle summaryCount(boolean doubleBottom) {
            return summaryStyle("count", plain, null, INTEGER_FORMAT, doubleBottom);
        }

        private CellStyle summaryHours(boolean doubleBottom) {
            return summaryStyle("hours", plain, null, TOTAL_HOURS_FORMAT, doubleBottom);
        }

        private CellStyle summaryStyle(String name, XSSFFont font, byte[] fill, String format, boolean doubleBottom) {
            return cached("summary|" + name + "|" + doubleBottom,
                    () -> borders(base(font, fill, format, false), null, doubleBottom ? BorderStyle.DOUBLE : null, null, null));
        }

        private XSSFCellStyle cached(String key, java.util.function.Supplier<XSSFCellStyle> factory) {
            return cache.computeIfAbsent(key, ignored -> factory.get());
        }

        private String formatOf(Kind kind) {
            return switch (kind) {
                case TIME -> TIME_FORMAT;
                case DECIMAL -> DECIMAL_FORMAT;
                case LONG_DATE -> LONG_DATE_FORMAT;
                case ORDINARY_HOURS -> INTEGER_FORMAT;
                case HOURS -> HOURS_FORMAT;
                default -> null;
            };
        }

        private XSSFFont font(boolean isBold, short size, byte[] color) {
            XSSFFont font = workbook.createFont();
            font.setFontName(FONT);
            font.setFontHeightInPoints(size);
            font.setBold(isBold);
            if (color != null) {
                font.setColor(new XSSFColor(color, new DefaultIndexedColorMap()));
            }
            return font;
        }

        private XSSFCellStyle base(XSSFFont font, byte[] fill, String format, boolean wrap) {
            XSSFCellStyle style = workbook.createCellStyle();
            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setWrapText(wrap);
            if (fill != null) {
                style.setFillForegroundColor(new XSSFColor(fill, new DefaultIndexedColorMap()));
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            if (format != null) {
                style.setDataFormat(workbook.createDataFormat().getFormat(format));
            }
            return style;
        }

        private XSSFCellStyle allMedium(XSSFCellStyle style) {
            return borders(style, BorderStyle.MEDIUM, BorderStyle.MEDIUM, BorderStyle.MEDIUM, BorderStyle.MEDIUM);
        }

        private XSSFCellStyle borders(XSSFCellStyle style, BorderStyle top, BorderStyle bottom, BorderStyle left, BorderStyle right) {
            if (top != null) {
                style.setBorderTop(top);
            }
            if (bottom != null) {
                style.setBorderBottom(bottom);
            }
            if (left != null) {
                style.setBorderLeft(left);
            }
            if (right != null) {
                style.setBorderRight(right);
            }
            return style;
        }
    }
}
