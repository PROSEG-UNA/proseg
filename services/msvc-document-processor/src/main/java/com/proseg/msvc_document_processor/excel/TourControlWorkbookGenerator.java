package com.proseg.msvc_document_processor.excel;

import com.proseg.msvc_document_processor.dto.response.TourControlReportDto;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.FillPatternType;
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
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class TourControlWorkbookGenerator {

    public static final String CONTROL_SHEET = "HRS.EXTRAOFICIAL";
    public static final String EXTERNAL_SHEET = "Hoja2";
    public static final String WEEKEND_SHEET = "Hoja1";

    private static final String DRIVER_SECTION = "DRIVER";
    private static final String OTHER_UNIT_SECTION = "OTHER_UNIT";
    private static final String EXTERNAL_SECTION = "EXTERNAL";
    private static final String UNATTENDED_SECTION = "UNATTENDED";
    private static final List<String> EXTRA_SECTIONS = List.of(OTHER_UNIT_SECTION, EXTERNAL_SECTION, UNATTENDED_SECTION);

    private static final List<String> MONTH_NAMES = List.of(
            "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
            "JULIO", "AGOSTO", "SETIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"
    );
    private static final Locale SPANISH = Locale.forLanguageTag("es");
    private static final String TITLE = "DISTRIBUCION DE GIRAS ";
    private static final String[] TOP_LABELS = {
            "FECHA DE ", null, null, null, null, null, null, null, null, "HORA", "HORA", "FECHA ", "No.", "NUMERO ",
            "HORA", "HORA", "TOTAL", "CALCULO", "ROL POR", "PRIOR"
    };
    private static final String[] BOTTOM_LABELS = {
            "SALIDA", "AJUSTE", "X DIA", "SOBRES", "EXTRA", "PLACA", " CHOFER", "DESTINO", " SOLICITANTE", " SALIDA",
            " REGRESO", "REGRESO", "PERS", "GIRA", " SALIDA", " REGRESO", "HRS./DIA", "CONDICION", "CHOFER", null
    };
    private static final String WEEKDAY_FORMULA = "IF(WEEKDAY(%1$s,2)=1,\"LUNES\",IF(WEEKDAY(%1$s,2)=2,\"MARTES\","
            + "IF(WEEKDAY(%1$s,2)=3,\"MIERCOLES\",IF(WEEKDAY(%1$s,2)=4,\"JUEVES\",IF(WEEKDAY(%1$s,2)=5,\"VIERNES\","
            + "IF(WEEKDAY(%1$s,2)=6,\"SABADO\",IF(WEEKDAY(%1$s,2)=7,\"DOMINGO\")))))))";
    private static final String OUTSIDE_WINDOW_FORMULA = "+IF(%1$s<%2$s,SUM(%2$s-%1$s),IF(%1$s>%3$s,SUM(%1$s-%3$s),0))";
    private static final String WEEKEND_HOURS_FORMULA = "+IF(%1$s=\"SABADO\",(%3$s-%2$s),IF(%1$s=\"DOMINGO\",(%3$s-%2$s),0))";
    private static final String CONTRACT_PENDING = "CONTRATAR";

    private static final String SUMMARY_TITLE = "CALCULO DE AMPLIACION JORNADA Y TIEMPO EXTRAORDINARIO CHOFERES";
    private static final String SURPLUS_LABEL = "SOBRESUELDO";
    private static final String CALCULATION_LABEL = "CALCULO";
    private static final String ORDINARY_LABEL = "JORNADA ORDINARIA";
    private static final String NAME_LABEL = "NOMBRE";
    private static final String OVERTIME_LABEL = "HORAS EXTRA";
    private static final String DAYS_LABEL = "DIAS";
    private static final String FACTOR_LABEL = "FACTOR";
    private static final String HOURS_LABEL = "HORAS";
    private static final String AVAILABLE_LABEL = "DISPONIBLES";
    private static final double SURPLUS_FACTOR = 40.0 / 30.0;
    private static final int ORDINARY_HOURS_PER_DAY = 8;

    private static final String[] EXTERNAL_LABELS = {"FECHA", "PLACA", "PROVEEDOR", "DESTINO", "RESP", "H.I", "H.F", "F. RE", "CANT", "GIRA"};
    private static final String WEEKEND_TITLE = "FINES DE SEMANA";

    private static final String FONT = "Tenorite";
    private static final String WEEKEND_SHEET_FONT = "Aptos";
    private static final String DEFAULT_FONT = "Arial";
    private static final short FONT_SIZE = 11;
    private static final short DEFAULT_FONT_SIZE = 10;
    private static final byte[] TITLE_FILL = rgb(0x00, 0xB0, 0xF0);
    private static final byte[] WEEKEND_FILL = rgb(0xFF, 0xCC, 0xCC);
    private static final byte[] TOUR_FILL = rgb(0xFF, 0xF2, 0xCC);
    private static final byte[] OTHER_UNIT_FILL = rgb(0xF4, 0xB1, 0x83);
    private static final byte[] EXTERNAL_FILL = rgb(0xFF, 0xFF, 0x00);
    private static final byte[] WHITE_FILL = rgb(0xFF, 0xFF, 0xFF);
    private static final byte[] LIMIT_FONT = rgb(0x9C, 0x00, 0x06);
    private static final byte[] LIMIT_FILL = rgb(0xFF, 0xC7, 0xCE);
    private static final String WEEKDAY_LIMIT = "4";
    private static final String WEEKEND_LIMIT = "12";
    private static final String INTEGER_FORMAT = "_-* #,##0_-;\\-* #,##0_-;_-* \"-\"_-;_-@";
    private static final String DECIMAL_FORMAT = "_-* #,##0.00_-;\\-* #,##0.00_-;_-* \"-\"_-;_-@";
    private static final String ONE_DECIMAL_FORMAT = "_-* #,##0.0_-;\\-* #,##0.0_-;_-* \"-\"??_-;_-@_-";
    private static final String PERCENT_FORMAT = "0%";
    private static final String DECIMAL_PERCENT_FORMAT = "0.0%";
    private static final short SHORT_DATE_FORMAT = 14;
    private static final Pattern INSTITUTIONAL_PLATE = Pattern.compile("301-(\\d+)");

    private static final double[] CONTROL_WIDTHS = {
            13.14, 9.57, 8.57, 10.57, 9.43, 10.43, 35.14, 26.14, 25.0, 11.43, 13.0, 18.14, 9.43, 14.86, 9.57, 13.0, 10.57,
            13.57, 11.43, 7.43, 18.57, 23.57, 12.57, 11.43
    };
    private static final int[] HIDDEN_CONTROL_COLUMNS = {1, 14, 15, 16, 17, 18};
    private static final double[] EXTERNAL_WIDTHS = {11.86, 11.43, 10.0, 15.71, 24.57, 11.43, 10.0, 11.86, 11.43, 10.0};
    private static final double WEEKEND_NAME_WIDTH = 23.57;
    private static final float EXTERNAL_ROW_HEIGHT = 30f;
    private static final int COLUMN_WIDTH_UNIT = 256;

    private static final int DATE_COLUMN = 0;
    private static final int ADJUSTMENT_COLUMN = 1;
    private static final int DAY_TOTAL_COLUMN = 2;
    private static final int SURPLUS_COLUMN = 3;
    private static final int OVERTIME_COLUMN = 4;
    private static final int PLATE_COLUMN = 5;
    private static final int DRIVER_COLUMN = 6;
    private static final int DESTINATION_COLUMN = 7;
    private static final int RESPONSIBLE_COLUMN = 8;
    private static final int START_COLUMN = 9;
    private static final int END_COLUMN = 10;
    private static final int RETURN_DATE_COLUMN = 11;
    private static final int PASSENGERS_COLUMN = 12;
    private static final int NUMBER_COLUMN = 13;
    private static final int BEFORE_COLUMN = 14;
    private static final int AFTER_COLUMN = 15;
    private static final int HOURS_COLUMN = 16;
    private static final int CONDITION_COLUMN = 17;
    private static final int WEEKEND_COLUMN = 18;
    private static final int PRIORITY_COLUMN = 19;
    private static final int LAST_COLUMN = PRIORITY_COLUMN;
    private static final int WEEKDAY_FIRST_COLUMN = 3;
    private static final int WEEKDAY_LAST_COLUMN = 8;

    private static final int AVAILABLE_COLUMN = 5;
    private static final int NAME_COLUMN = 6;
    private static final int SUMMARY_SURPLUS_COLUMN = 7;
    private static final int SUMMARY_OVERTIME_COLUMN = 8;
    private static final int WEEKDAY_SUM_COLUMN = 9;
    private static final int USAGE_COLUMN = 10;
    private static final int CAP_COLUMN = 11;
    private static final int DAYS_COLUMN = 13;
    private static final int FACTOR_COLUMN = 14;
    private static final int ORDINARY_DAYS_COLUMN = 21;
    private static final int ORDINARY_HOURS_COLUMN = 22;

    private static final int TITLE_ROW = 1;
    private static final int FIRST_BLOCK_ROW = 3;
    private static final int SUMMARY_GAP = 3;

    public byte[] generate(TourControlReportDto report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFFont defaultFont = workbook.getFontAt(0);
            defaultFont.setFontName(DEFAULT_FONT);
            defaultFont.setFontHeightInPoints(DEFAULT_FONT_SIZE);
            Styles styles = new Styles(workbook);
            List<TourControlReportDto.Driver> drivers = report.getDrivers() == null ? List.of() : report.getDrivers();
            List<TourControlReportDto.Entry> entries = report.getEntries() == null ? List.of() : report.getEntries();
            YearMonth period = YearMonth.of(report.getYear(), report.getMonth());

            writeControlSheet(workbook.createSheet(CONTROL_SHEET), report, period, drivers, entries, styles);
            writeExternalSheet(workbook.createSheet(EXTERNAL_SHEET), period, entries, styles);
            writeWeekendSheet(workbook.createSheet(WEEKEND_SHEET), period, drivers, entries, styles);

            workbook.setActiveSheet(0);
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw DocumentProcessorException.exportGenerationFailed();
        }
    }

    private void writeControlSheet(
            Sheet sheet,
            TourControlReportDto report,
            YearMonth period,
            List<TourControlReportDto.Driver> drivers,
            List<TourControlReportDto.Entry> entries,
            Styles styles
    ) {
        sheet.setDisplayGridlines(false);
        sheet.getPrintSetup().setLandscape(true);
        for (int column = 0; column < CONTROL_WIDTHS.length; column++) {
            sheet.setColumnWidth(column, (int) Math.round(CONTROL_WIDTHS[column] * COLUMN_WIDTH_UNIT));
        }
        for (int column : HIDDEN_CONTROL_COLUMNS) {
            sheet.setColumnHidden(column, true);
        }
        writeMerged(sheet, TITLE_ROW, TITLE_ROW, 0, LAST_COLUMN, TITLE + MONTH_NAMES.get(period.getMonthValue() - 1) + " " + period.getYear(),
                styles.title);

        List<Block> blocks = plan(period, drivers, entries);
        int summaryRow = (blocks.isEmpty() ? FIRST_BLOCK_ROW : blocks.get(blocks.size() - 1).lastRow()) + SUMMARY_GAP;
        int firstSummaryDriverRow = summaryRow + 3;
        List<List<String>> weekdayRefs = drivers.stream().map(driver -> (List<String>) new ArrayList<String>()).toList();
        List<List<String>> weekendRefs = drivers.stream().map(driver -> (List<String>) new ArrayList<String>()).toList();
        double[] weekdayHours = new double[drivers.size()];
        SheetConditionalFormatting formatting = sheet.getSheetConditionalFormatting();

        Block previous = null;
        for (Block block : blocks) {
            writeBlockHeader(sheet, block, styles);
            for (DriverRow driverRow : block.driverRows()) {
                writeDriverRow(sheet, block, previous, driverRow, drivers.get(driverRow.driverIndex()), firstSummaryDriverRow, styles);
                (block.weekend() ? weekendRefs : weekdayRefs).get(driverRow.driverIndex()).add(ref(HOURS_COLUMN, driverRow.row()));
                if (!block.weekend()) {
                    weekdayHours[driverRow.driverIndex()] += dayHours(driverRow.entry(), drivers.get(driverRow.driverIndex()), false);
                }
            }
            for (Section section : block.sections()) {
                writeSection(sheet, section, styles);
            }
            addLimitFormatting(formatting, block);
            previous = block;
        }
        writeSummary(sheet, summaryRow, report, drivers, weekdayRefs, weekendRefs, weekdayHours, styles);
    }

    private List<Block> plan(YearMonth period, List<TourControlReportDto.Driver> drivers, List<TourControlReportDto.Entry> entries) {
        Map<LocalDate, List<TourControlReportDto.Entry>> byDate = entries.stream()
                .filter(entry -> entry.getDate() != null)
                .collect(Collectors.groupingBy(TourControlReportDto.Entry::getDate));
        LocalDate firstDate = byDate.keySet().stream().min(Comparator.naturalOrder())
                .filter(date -> date.isBefore(period.atDay(1))).orElse(period.atDay(1));
        LocalDate lastDate = byDate.keySet().stream().max(Comparator.naturalOrder())
                .filter(date -> date.isAfter(period.atEndOfMonth())).orElse(period.atEndOfMonth());
        List<Block> blocks = new ArrayList<>();
        int row = FIRST_BLOCK_ROW;
        for (LocalDate date = firstDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
            List<TourControlReportDto.Entry> dayEntries = byDate.getOrDefault(date, List.of()).stream()
                    .sorted(Comparator.comparing(TourControlReportDto.Entry::getStart, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            Map<Integer, List<TourControlReportDto.Entry>> byDriver = dayEntries.stream()
                    .filter(entry -> isDriverEntry(entry, drivers.size()))
                    .collect(Collectors.groupingBy(TourControlReportDto.Entry::getDriverIndex));
            int current = row + 2;
            int[] mainRows = new int[drivers.size()];
            List<DriverRow> driverRows = new ArrayList<>();
            for (int index = 0; index < drivers.size(); index++) {
                mainRows[index] = current;
                List<TourControlReportDto.Entry> driverEntries = byDriver.getOrDefault(index, List.of());
                if (driverEntries.isEmpty()) {
                    driverRows.add(new DriverRow(current++, index, null, true));
                }
                for (int position = 0; position < driverEntries.size(); position++) {
                    driverRows.add(new DriverRow(current++, index, driverEntries.get(position), position == 0));
                }
            }
            int lastDriverRow = Math.max(row + 1, current - 1);
            List<Section> sections = new ArrayList<>();
            for (String sectionName : EXTRA_SECTIONS) {
                List<TourControlReportDto.Entry> sectionEntries = dayEntries.stream()
                        .filter(entry -> sectionName.equals(sectionOf(entry, drivers.size())))
                        .toList();
                if (!sectionEntries.isEmpty()) {
                    current++;
                    sections.add(new Section(sectionName, current, sectionEntries));
                    current += sectionEntries.size();
                }
            }
            int lastRow = Math.max(lastDriverRow, current - 1);
            blocks.add(new Block(date, isWeekend(date), row, mainRows, driverRows, lastDriverRow, sections, lastRow));
            row = lastRow + 2;
        }
        return blocks;
    }

    private boolean isDriverEntry(TourControlReportDto.Entry entry, int driverCount) {
        return DRIVER_SECTION.equals(entry.getSection()) && entry.getDriverIndex() != null
                && entry.getDriverIndex() >= 0 && entry.getDriverIndex() < driverCount;
    }

    private String sectionOf(TourControlReportDto.Entry entry, int driverCount) {
        if (DRIVER_SECTION.equals(entry.getSection())) {
            return isDriverEntry(entry, driverCount) ? DRIVER_SECTION : OTHER_UNIT_SECTION;
        }
        return entry.getSection();
    }

    private void writeBlockHeader(Sheet sheet, Block block, Styles styles) {
        byte[] fill = block.weekend() ? WEEKEND_FILL : null;
        int top = block.headerRow();
        int bottom = top + 1;
        for (int column = 0; column <= LAST_COLUMN; column++) {
            boolean left = column == 0;
            boolean right = column == LAST_COLUMN;
            if (column < WEEKDAY_FIRST_COLUMN || column > WEEKDAY_LAST_COLUMN) {
                setValue(cell(sheet, top, column, styles.block(kindOf(column), fill, true, false, left, right)), TOP_LABELS[column]);
            }
            setValue(cell(sheet, bottom, column, styles.block(kindOf(column), fill, false, false, left, right)), BOTTOM_LABELS[column]);
        }
        Cell weekday = writeMerged(sheet, top, top, WEEKDAY_FIRST_COLUMN, WEEKDAY_LAST_COLUMN, null,
                styles.block(Kind.TEXT, fill, true, false, false, false));
        weekday.setCellFormula(String.format(WEEKDAY_FORMULA, ref(DATE_COLUMN, top + 2)));
    }

    private void writeDriverRow(
            Sheet sheet,
            Block block,
            Block previous,
            DriverRow driverRow,
            TourControlReportDto.Driver driver,
            int firstSummaryDriverRow,
            Styles styles
    ) {
        TourControlReportDto.Entry entry = driverRow.entry();
        byte[] fill = entry != null ? TOUR_FILL : (block.weekend() ? WEEKEND_FILL : null);
        boolean bottom = driverRow.row() == block.lastDriverRow();
        int row = driverRow.row();
        for (int column = 0; column <= LAST_COLUMN; column++) {
            cell(sheet, row, column, styles.block(kindOf(column), fill, false, bottom, column == 0, column == LAST_COLUMN));
        }

        int firstRow = block.headerRow() + 2;
        Cell date = sheet.getRow(row).getCell(DATE_COLUMN);
        if (row != firstRow) {
            date.setCellFormula("+" + ref(DATE_COLUMN, row - 1));
        } else if (previous == null) {
            date.setCellValue(block.date());
        } else {
            date.setCellFormula("1+" + ref(DATE_COLUMN, previous.headerRow() + 2));
        }

        int mainRow = block.mainRows()[driverRow.driverIndex()];
        int summaryDriverRow = firstSummaryDriverRow + driverRow.driverIndex();
        if (!driverRow.main()) {
            setFormula(sheet, row, SURPLUS_COLUMN, "+" + ref(SURPLUS_COLUMN, mainRow));
            setFormula(sheet, row, OVERTIME_COLUMN, "+" + ref(OVERTIME_COLUMN, mainRow));
            setFormula(sheet, row, DRIVER_COLUMN, "+" + ref(DRIVER_COLUMN, mainRow));
        } else if (previous == null) {
            setFormula(sheet, row, SURPLUS_COLUMN, "+" + ref(SUMMARY_SURPLUS_COLUMN, summaryDriverRow));
            setFormula(sheet, row, OVERTIME_COLUMN, "+" + ref(SUMMARY_OVERTIME_COLUMN, summaryDriverRow));
            setFormula(sheet, row, DRIVER_COLUMN, "+" + ref(NAME_COLUMN, summaryDriverRow));
        } else {
            int previousRow = previous.mainRows()[driverRow.driverIndex()];
            setFormula(sheet, row, SURPLUS_COLUMN, "+" + ref(SURPLUS_COLUMN, previousRow));
            setFormula(sheet, row, OVERTIME_COLUMN, "+" + ref(OVERTIME_COLUMN, previousRow));
            setFormula(sheet, row, DRIVER_COLUMN, "+" + ref(DRIVER_COLUMN, previousRow));
        }

        String windowStart = number(driver.getOrdinaryStart());
        String windowEnd = number(driver.getOrdinaryEnd());
        setValue(sheet.getRow(row).getCell(ADJUSTMENT_COLUMN), adjustment(entry, driver, block.weekend()));
        setFormula(sheet, row, DAY_TOTAL_COLUMN, "0+" + ref(HOURS_COLUMN, row));
        setFormula(sheet, row, BEFORE_COLUMN, String.format(OUTSIDE_WINDOW_FORMULA, ref(START_COLUMN, row), windowStart, windowEnd));
        setFormula(sheet, row, AFTER_COLUMN, String.format(OUTSIDE_WINDOW_FORMULA, ref(END_COLUMN, row), windowStart, windowEnd));
        setFormula(sheet, row, HOURS_COLUMN, "+IF(COUNTA(" + ref(PLATE_COLUMN, row) + "," + ref(NUMBER_COLUMN, row) + ")=0,0,"
                + ref(CONDITION_COLUMN, row) + ")");
        setFormula(sheet, row, CONDITION_COLUMN, "+IF(" + ref(WEEKEND_COLUMN, row) + "=0,(" + ref(BEFORE_COLUMN, row) + "+"
                + ref(AFTER_COLUMN, row) + ")," + ref(WEEKEND_COLUMN, row) + ")+" + ref(ADJUSTMENT_COLUMN, row));
        setFormula(sheet, row, WEEKEND_COLUMN, String.format(WEEKEND_HOURS_FORMULA,
                absoluteRef(WEEKDAY_FIRST_COLUMN, block.headerRow()), ref(START_COLUMN, row), ref(END_COLUMN, row)));
        if (entry != null) {
            writeTourValues(sheet, row, entry, plateValue(entry.getPlate()), null);
        }
    }

    private double adjustment(TourControlReportDto.Entry entry, TourControlReportDto.Driver driver, boolean weekend) {
        if (entry == null || entry.getHours() == null || entry.getStart() == null || entry.getEnd() == null) {
            return 0;
        }
        return round(entry.getHours() - formulaHours(entry, driver, weekend));
    }

    private double dayHours(TourControlReportDto.Entry entry, TourControlReportDto.Driver driver, boolean weekend) {
        if (entry == null) {
            return 0;
        }
        if (entry.getHours() != null) {
            return entry.getHours();
        }
        return entry.getStart() == null || entry.getEnd() == null ? 0 : formulaHours(entry, driver, weekend);
    }

    private double formulaHours(TourControlReportDto.Entry entry, TourControlReportDto.Driver driver, boolean weekend) {
        double start = entry.getStart();
        double end = entry.getEnd();
        double computed = weekend ? end - start : 0;
        return computed == 0 ? outsideWindow(start, driver) + outsideWindow(end, driver) : computed;
    }

    private int businessDaysOf(TourControlReportDto.Driver driver, TourControlReportDto report) {
        return driver.getBusinessDays() == null ? report.getBusinessDays() : driver.getBusinessDays();
    }

    private double surplusAdjustment(TourControlReportDto.Driver driver, double weekdayHours, int businessDays) {
        if (driver.getSurplusHours() == null) {
            return 0;
        }
        double cap = Math.ceil(Math.round(businessDays * SURPLUS_FACTOR * 2 * 1e6) / 1e6) / 2;
        return round(Math.min(weekdayHours, cap) - driver.getSurplusHours());
    }

    private String signed(double value) {
        if (value == 0) {
            return "";
        }
        return (value > 0 ? "+" : "-") + number(Math.abs(value));
    }

    private static double round(double hours) {
        return Math.round(hours * 100) / 100.0;
    }

    private double outsideWindow(double hour, TourControlReportDto.Driver driver) {
        if (hour < driver.getOrdinaryStart()) {
            return driver.getOrdinaryStart() - hour;
        }
        return hour > driver.getOrdinaryEnd() ? hour - driver.getOrdinaryEnd() : 0;
    }

    private void writeSection(Sheet sheet, Section section, Styles styles) {
        boolean unattended = UNATTENDED_SECTION.equals(section.name());
        byte[] fill = EXTERNAL_SECTION.equals(section.name()) ? EXTERNAL_FILL : OTHER_UNIT_FILL;
        int lastRow = section.firstRow() + section.entries().size() - 1;
        for (int offset = 0; offset < section.entries().size(); offset++) {
            int row = section.firstRow() + offset;
            TourControlReportDto.Entry entry = section.entries().get(offset);
            if (unattended) {
                for (int column = DESTINATION_COLUMN; column <= NUMBER_COLUMN; column++) {
                    cell(sheet, row, column, styles.thin(kindOf(column), fill));
                }
                cell(sheet, row, PRIORITY_COLUMN, styles.thin(kindOf(PRIORITY_COLUMN), fill));
                writeTourValues(sheet, row, entry, null, null);
                continue;
            }
            for (int column = 0; column <= LAST_COLUMN; column++) {
                cell(sheet, row, column, styles.block(kindOf(column), fill, row == section.firstRow(), row == lastRow,
                        column == 0, column == LAST_COLUMN));
            }
            setDate(sheet.getRow(row).getCell(DATE_COLUMN), entry.getDepartureDate());
            String driver = EXTERNAL_SECTION.equals(section.name()) && entry.getDriver() == null ? CONTRACT_PENDING : upper(entry.getDriver());
            writeTourValues(sheet, row, entry, plateValue(entry.getPlate()), driver);
        }
    }

    private void writeTourValues(Sheet sheet, int row, TourControlReportDto.Entry entry, Object plate, String driver) {
        Row sheetRow = sheet.getRow(row);
        if (plate != null) {
            setValue(sheetRow.getCell(PLATE_COLUMN), plate);
        }
        if (driver != null) {
            setValue(sheetRow.getCell(DRIVER_COLUMN), driver);
        }
        setValue(sheetRow.getCell(DESTINATION_COLUMN), entry.getDestination());
        setValue(sheetRow.getCell(RESPONSIBLE_COLUMN), entry.getResponsible());
        setValue(sheetRow.getCell(START_COLUMN), entry.getStart());
        setValue(sheetRow.getCell(END_COLUMN), entry.getEnd());
        setDate(sheetRow.getCell(RETURN_DATE_COLUMN), entry.getReturnDate());
        setValue(sheetRow.getCell(PASSENGERS_COLUMN), entry.getPassengers());
        setValue(sheetRow.getCell(NUMBER_COLUMN), tourNumber(entry.getNumber()));
        Cell priority = sheetRow.getCell(PRIORITY_COLUMN);
        if (priority != null) {
            setValue(priority, entry.getPriority());
        }
    }

    private void addLimitFormatting(SheetConditionalFormatting formatting, Block block) {
        if (block.driverRows().isEmpty()) {
            return;
        }
        ConditionalFormattingRule rule = formatting.createConditionalFormattingRule(ComparisonOperator.GT,
                block.weekend() ? WEEKEND_LIMIT : WEEKDAY_LIMIT);
        rule.createFontFormatting().setFontColor(new XSSFColor(LIMIT_FONT, new DefaultIndexedColorMap()));
        PatternFormatting pattern = rule.createPatternFormatting();
        pattern.setFillBackgroundColor(new XSSFColor(LIMIT_FILL, new DefaultIndexedColorMap()));
        pattern.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
        formatting.addConditionalFormatting(new CellRangeAddress[]{
                new CellRangeAddress(block.headerRow() + 2, block.lastDriverRow(), DAY_TOTAL_COLUMN, DAY_TOTAL_COLUMN)
        }, rule);
    }

    private void writeSummary(
            Sheet sheet,
            int summaryRow,
            TourControlReportDto report,
            List<TourControlReportDto.Driver> drivers,
            List<List<String>> weekdayRefs,
            List<List<String>> weekendRefs,
            double[] weekdayHours,
            Styles styles
    ) {
        writeMerged(sheet, summaryRow, summaryRow + 1, NAME_COLUMN, SUMMARY_OVERTIME_COLUMN, SUMMARY_TITLE, styles.summaryTitle);
        writeMerged(sheet, summaryRow + 1, summaryRow + 2, CAP_COLUMN, CAP_COLUMN, SURPLUS_LABEL, styles.summaryLabel(false));
        setValue(cell(sheet, summaryRow + 1, DAYS_COLUMN, styles.summaryLabel(false)), CALCULATION_LABEL);
        writeMerged(sheet, summaryRow + 1, summaryRow + 1, ORDINARY_DAYS_COLUMN, ORDINARY_HOURS_COLUMN, ORDINARY_LABEL, styles.summaryLabel(false));
        int labelsRow = summaryRow + 2;
        setValue(cell(sheet, labelsRow, NAME_COLUMN, styles.summaryLabel(true)), NAME_LABEL);
        setValue(cell(sheet, labelsRow, SUMMARY_SURPLUS_COLUMN, styles.summaryLabel(true)), SURPLUS_LABEL);
        setValue(cell(sheet, labelsRow, SUMMARY_OVERTIME_COLUMN, styles.summaryLabel(true)), OVERTIME_LABEL);
        cell(sheet, labelsRow, WEEKDAY_SUM_COLUMN, styles.summaryLabel(true));
        cell(sheet, labelsRow, USAGE_COLUMN, styles.summaryLabel(true));
        cell(sheet, labelsRow, CAP_COLUMN + 1, styles.summaryLabel(true));
        setValue(cell(sheet, labelsRow, DAYS_COLUMN, styles.summaryLabel(true)), DAYS_LABEL);
        setValue(cell(sheet, labelsRow, FACTOR_COLUMN, styles.summaryLabel(false)), FACTOR_LABEL);
        setValue(cell(sheet, labelsRow, ORDINARY_DAYS_COLUMN, styles.summaryLabel(true)), DAYS_LABEL);
        setValue(cell(sheet, labelsRow, ORDINARY_HOURS_COLUMN, styles.summaryLabel(true)), HOURS_LABEL);

        int firstRow = labelsRow + 1;
        int lastRow = firstRow + drivers.size() - 1;
        for (int index = 0; index < drivers.size(); index++) {
            int row = firstRow + index;
            boolean last = row == lastRow;
            String cap = ref(CAP_COLUMN, row);
            String weekdaySum = ref(WEEKDAY_SUM_COLUMN, row);
            int driverBusinessDays = businessDaysOf(drivers.get(index), report);
            double surplusAdjustment = surplusAdjustment(drivers.get(index), weekdayHours[index], driverBusinessDays);
            setFormula(cell(sheet, row, AVAILABLE_COLUMN, styles.summaryValue(Kind.DECIMAL, false, false)), "+" + cap + "-" + weekdaySum);
            setValue(cell(sheet, row, NAME_COLUMN, styles.summaryValue(Kind.TEXT, false, last)), upper(drivers.get(index).getName()));
            setFormula(cell(sheet, row, SUMMARY_SURPLUS_COLUMN, styles.summaryValue(Kind.DECIMAL, false, last)),
                    "IF(" + weekdaySum + "<" + cap + "," + weekdaySum + "," + cap + ")" + signed(-surplusAdjustment));
            setFormula(cell(sheet, row, SUMMARY_OVERTIME_COLUMN, styles.summaryValue(Kind.DECIMAL, false, last)),
                    "+SUM(" + sumOf(weekendRefs.get(index)) + ")+IF(" + weekdaySum + ">" + cap + ",(" + weekdaySum + "-" + cap + "),0)"
                            + signed(surplusAdjustment));
            setFormula(cell(sheet, row, WEEKDAY_SUM_COLUMN, styles.summaryValue(Kind.DECIMAL, false, last)), "+" + sumOf(weekdayRefs.get(index)));
            setFormula(cell(sheet, row, USAGE_COLUMN, styles.summaryValue(Kind.PERCENT, false, last)), "+" + ref(SUMMARY_SURPLUS_COLUMN, row) + "/" + cap);
            setFormula(cell(sheet, row, CAP_COLUMN, styles.summaryValue(Kind.DECIMAL, true, last)),
                    "ROUNDUP(ROUND(" + ref(DAYS_COLUMN, row) + "*" + ref(FACTOR_COLUMN, row) + "*2,6),0)/2");
            cell(sheet, row, CAP_COLUMN + 1, styles.summaryValue(Kind.TEXT, false, last));
            setValue(cell(sheet, row, DAYS_COLUMN, styles.summaryValue(Kind.ONE_DECIMAL, true, last)), driverBusinessDays);
            setValue(cell(sheet, row, FACTOR_COLUMN, styles.summaryValue(Kind.DECIMAL, true, false)), SURPLUS_FACTOR);
            setValue(cell(sheet, row, ORDINARY_DAYS_COLUMN, styles.summaryValue(Kind.INTEGER, true, last)), report.getWeekdays());
            setFormula(cell(sheet, row, ORDINARY_HOURS_COLUMN, styles.summaryValue(Kind.DECIMAL, false, last)),
                    "+" + ref(ORDINARY_DAYS_COLUMN, row) + "*" + ORDINARY_HOURS_PER_DAY);
        }
        if (drivers.isEmpty()) {
            return;
        }
        int totalRow = lastRow + 1;
        String count = String.valueOf(drivers.size());
        setFormula(cell(sheet, totalRow, SUMMARY_SURPLUS_COLUMN, styles.summaryValue(Kind.DECIMAL, false, false)), "SUM(" + range(SUMMARY_SURPLUS_COLUMN, firstRow, lastRow) + ")");
        setFormula(cell(sheet, totalRow, SUMMARY_OVERTIME_COLUMN, styles.summaryValue(Kind.DECIMAL, false, false)), "SUM(" + range(SUMMARY_OVERTIME_COLUMN, firstRow, lastRow) + ")");
        setFormula(cell(sheet, totalRow, WEEKDAY_SUM_COLUMN, styles.summaryValue(Kind.DECIMAL, true, false)), "SUM(" + range(WEEKDAY_SUM_COLUMN, firstRow, lastRow) + ")");
        setFormula(cell(sheet, totalRow, USAGE_COLUMN, styles.summaryValue(Kind.DECIMAL_PERCENT, true, false)),
                "+" + ref(SUMMARY_SURPLUS_COLUMN, totalRow) + "/" + ref(CAP_COLUMN, totalRow));
        setFormula(cell(sheet, totalRow, CAP_COLUMN, styles.summaryValue(Kind.DECIMAL, true, false)), "SUM(" + range(CAP_COLUMN, firstRow, lastRow) + ")");
        setFormula(cell(sheet, totalRow, DAYS_COLUMN, styles.summaryValue(Kind.ONE_DECIMAL, true, false)),
                "(SUM(" + range(DAYS_COLUMN, firstRow, lastRow) + "))/" + count);
        setFormula(cell(sheet, totalRow, ORDINARY_DAYS_COLUMN, styles.summaryValue(Kind.ONE_DECIMAL, true, false)),
                "(SUM(" + range(ORDINARY_DAYS_COLUMN, firstRow, lastRow) + "))/" + count);
        setFormula(cell(sheet, totalRow, ORDINARY_HOURS_COLUMN, styles.summaryValue(Kind.DECIMAL, true, false)), "SUM(" + range(ORDINARY_HOURS_COLUMN, firstRow, lastRow) + ")");

        int availableRow = totalRow + 2;
        setValue(cell(sheet, availableRow, NAME_COLUMN, styles.summaryTotal(Kind.TEXT)), AVAILABLE_LABEL);
        setFormula(cell(sheet, availableRow, SUMMARY_SURPLUS_COLUMN, styles.summaryTotal(Kind.DECIMAL)),
                "+" + ref(CAP_COLUMN, totalRow) + "-" + ref(SUMMARY_SURPLUS_COLUMN, totalRow));
        cell(sheet, availableRow, SUMMARY_OVERTIME_COLUMN, styles.summaryTotal(Kind.TEXT));
        setFormula(cell(sheet, availableRow, WEEKDAY_SUM_COLUMN, styles.summaryTotal(Kind.DECIMAL_PERCENT)),
                "+" + ref(SUMMARY_SURPLUS_COLUMN, totalRow) + "/" + ref(CAP_COLUMN, totalRow));
    }

    private void writeExternalSheet(Sheet sheet, YearMonth period, List<TourControlReportDto.Entry> entries, Styles styles) {
        sheet.getPrintSetup().setLandscape(true);
        for (int column = 0; column < EXTERNAL_WIDTHS.length; column++) {
            sheet.setColumnWidth(column, (int) Math.round(EXTERNAL_WIDTHS[column] * COLUMN_WIDTH_UNIT));
        }
        for (int column = 0; column < EXTERNAL_LABELS.length; column++) {
            setValue(cell(sheet, 0, column, styles.externalHeader), EXTERNAL_LABELS[column]);
        }
        List<TourControlReportDto.Entry> externals = entries.stream()
                .filter(entry -> EXTERNAL_SECTION.equals(entry.getSection()))
                .filter(entry -> entry.getDepartureDate() != null && YearMonth.from(entry.getDepartureDate()).equals(period))
                .filter(entry -> entry.getDate() == null || entry.getDate().equals(entry.getDepartureDate()))
                .sorted(Comparator.comparing(TourControlReportDto.Entry::getDepartureDate)
                        .thenComparing(TourControlReportDto.Entry::getStart, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        int lastRow = externals.size();
        for (int index = 0; index < externals.size(); index++) {
            int row = index + 1;
            boolean bottom = row == lastRow;
            TourControlReportDto.Entry entry = externals.get(index);
            row(sheet, row).setHeightInPoints(EXTERNAL_ROW_HEIGHT);
            setDate(cell(sheet, row, 0, styles.external(Kind.SHORT_DATE, true, bottom)), entry.getDepartureDate());
            setValue(cell(sheet, row, 1, styles.external(Kind.WRAPPED_TEXT, false, bottom)), entry.getPlate());
            setValue(cell(sheet, row, 2, styles.external(Kind.TEXT, false, bottom)), entry.getDriver() == null ? CONTRACT_PENDING : entry.getDriver());
            setValue(cell(sheet, row, 3, styles.external(Kind.WRAPPED_TEXT, false, bottom)), entry.getDestination());
            setValue(cell(sheet, row, 4, styles.external(Kind.TEXT, false, bottom)), entry.getResponsible());
            setValue(cell(sheet, row, 5, styles.external(Kind.DECIMAL, false, bottom)), entry.getStart());
            setValue(cell(sheet, row, 6, styles.external(Kind.DECIMAL, false, bottom)), entry.getEnd());
            setDate(cell(sheet, row, 7, styles.external(Kind.SHORT_DATE, false, bottom)), entry.getReturnDate());
            setValue(cell(sheet, row, 8, styles.external(Kind.INTEGER, false, bottom)), entry.getPassengers());
            setValue(cell(sheet, row, 9, styles.external(Kind.TEXT, false, bottom)), tourNumber(entry.getNumber()));
        }
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(1, lastRow), 0, EXTERNAL_LABELS.length - 1));
    }

    private void writeWeekendSheet(
            Sheet sheet,
            YearMonth period,
            List<TourControlReportDto.Driver> drivers,
            List<TourControlReportDto.Entry> entries,
            Styles styles
    ) {
        sheet.setDisplayGridlines(false);
        sheet.setColumnWidth(0, (int) Math.round(WEEKEND_NAME_WIDTH * COLUMN_WIDTH_UNIT));
        List<LocalDate> weekends = weekendsOf(period);
        Map<Integer, Set<LocalDate>> worked = new LinkedHashMap<>();
        entries.stream()
                .filter(entry -> isDriverEntry(entry, drivers.size()) && entry.getDate() != null && isWeekend(entry.getDate()))
                .forEach(entry -> worked.computeIfAbsent(entry.getDriverIndex(), ignored -> new TreeSet<>()).add(weekendKey(entry.getDate())));
        int lastWeekendColumn = weekends.size();
        int totalColumn = lastWeekendColumn + 1;

        writeMerged(sheet, 1, 1, 0, Math.max(1, lastWeekendColumn), WEEKEND_TITLE, styles.weekendTitle);
        for (int index = 0; index < weekends.size(); index++) {
            setValue(cell(sheet, 2, index + 1, styles.weekend(Kind.TEXT, index == 0, false, false, index == weekends.size() - 1)), index + 1);
        }
        int firstRow = 3;
        int lastRow = firstRow + drivers.size() - 1;
        for (int driverIndex = 0; driverIndex < drivers.size(); driverIndex++) {
            int row = firstRow + driverIndex;
            boolean top = row == firstRow;
            boolean bottom = row == lastRow;
            setValue(cell(sheet, row, 0, styles.weekend(Kind.TEXT, true, top, bottom, false)), upper(drivers.get(driverIndex).getName()));
            Set<LocalDate> driverWeekends = worked.getOrDefault(driverIndex, Set.of());
            for (int index = 0; index < weekends.size(); index++) {
                Cell value = cell(sheet, row, index + 1, styles.weekend(Kind.INTEGER, false, top, bottom, index == weekends.size() - 1));
                if (driverWeekends.contains(weekends.get(index))) {
                    value.setCellValue(1);
                }
            }
            if (lastWeekendColumn > 0) {
                setFormula(cell(sheet, row, totalColumn, styles.weekendPlain), "SUM(" + ref(1, row) + ":" + ref(lastWeekendColumn, row) + ")");
            }
        }
        if (drivers.isEmpty() || weekends.isEmpty()) {
            return;
        }
        int totalRow = lastRow + 2;
        for (int column = 1; column <= lastWeekendColumn; column++) {
            setFormula(cell(sheet, totalRow, column, styles.weekendPlain), "SUM(" + range(column, firstRow, lastRow) + ")");
        }
        sheet.setAutoFilter(new CellRangeAddress(2, lastRow, 1, lastWeekendColumn));
    }

    private List<LocalDate> weekendsOf(YearMonth period) {
        return IntStream.rangeClosed(1, period.lengthOfMonth())
                .mapToObj(period::atDay)
                .filter(this::isWeekend)
                .map(this::weekendKey)
                .distinct()
                .toList();
    }

    private LocalDate weekendKey(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private Kind kindOf(int column) {
        return switch (column) {
            case DATE_COLUMN, RETURN_DATE_COLUMN -> Kind.SHORT_DATE;
            case ADJUSTMENT_COLUMN, PLATE_COLUMN, PASSENGERS_COLUMN, PRIORITY_COLUMN -> Kind.INTEGER;
            case DRIVER_COLUMN, RESPONSIBLE_COLUMN, NUMBER_COLUMN -> Kind.TEXT;
            case DESTINATION_COLUMN -> Kind.WRAPPED_TEXT;
            default -> Kind.DECIMAL;
        };
    }

    private Object plateValue(String plate) {
        if (plate == null) {
            return null;
        }
        Matcher matcher = INSTITUTIONAL_PLATE.matcher(plate.trim());
        return matcher.matches() ? Long.parseLong(matcher.group(1)) : plate.trim();
    }

    private Object tourNumber(String number) {
        if (number == null) {
            return null;
        }
        String trimmed = number.trim();
        return !trimmed.isEmpty() && trimmed.chars().allMatch(Character::isDigit) ? Long.parseLong(trimmed) : trimmed;
    }

    private String upper(String value) {
        return value == null ? null : value.toUpperCase(SPANISH);
    }

    private String sumOf(List<String> refs) {
        return refs.isEmpty() ? "0" : String.join("+", refs);
    }

    private String number(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
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
        Cell cell = sheet.getRow(firstRow).getCell(firstColumn);
        if (value != null) {
            cell.setCellValue(value);
        }
        return cell;
    }

    private void setFormula(Sheet sheet, int rowIndex, int column, String formula) {
        sheet.getRow(rowIndex).getCell(column).setCellFormula(formula);
    }

    private void setFormula(Cell cell, String formula) {
        cell.setCellFormula(formula);
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

    private void setDate(Cell cell, LocalDate date) {
        if (date != null) {
            cell.setCellValue(date);
        }
    }

    private String range(int column, int firstRow, int lastRow) {
        return ref(column, firstRow) + ":" + ref(column, lastRow);
    }

    private static String ref(int column, int rowIndex) {
        return CellReference.convertNumToColString(column) + (rowIndex + 1);
    }

    private static String absoluteRef(int column, int rowIndex) {
        return "$" + CellReference.convertNumToColString(column) + "$" + (rowIndex + 1);
    }

    private static byte[] rgb(int red, int green, int blue) {
        return new byte[]{(byte) red, (byte) green, (byte) blue};
    }

    private enum Kind {
        TEXT,
        WRAPPED_TEXT,
        INTEGER,
        DECIMAL,
        ONE_DECIMAL,
        PERCENT,
        DECIMAL_PERCENT,
        SHORT_DATE
    }

    private record DriverRow(int row, int driverIndex, TourControlReportDto.Entry entry, boolean main) {
    }

    private record Section(String name, int firstRow, List<TourControlReportDto.Entry> entries) {
    }

    private record Block(
            LocalDate date,
            boolean weekend,
            int headerRow,
            int[] mainRows,
            List<DriverRow> driverRows,
            int lastDriverRow,
            List<Section> sections,
            int lastRow
    ) {
    }

    private static final class Styles {
        private final XSSFWorkbook workbook;
        private final Map<String, XSSFCellStyle> cache = new HashMap<>();
        private final XSSFFont plain;
        private final XSSFFont bold;
        private final XSSFFont weekendFont;
        private final XSSFFont weekendBold;
        private final XSSFFont headerFont;

        private final CellStyle title;
        private final CellStyle summaryTitle;
        private final CellStyle externalHeader;
        private final CellStyle weekendTitle;
        private final CellStyle weekendPlain;

        private Styles(XSSFWorkbook workbook) {
            this.workbook = workbook;
            plain = font(FONT, FONT_SIZE, false);
            bold = font(FONT, FONT_SIZE, true);
            weekendFont = font(WEEKEND_SHEET_FONT, FONT_SIZE, false);
            weekendBold = font(WEEKEND_SHEET_FONT, FONT_SIZE, true);
            headerFont = font(DEFAULT_FONT, DEFAULT_FONT_SIZE, true);

            title = borders(base(bold, TITLE_FILL, null, false), BorderStyle.THIN, BorderStyle.DOUBLE, null, null);
            summaryTitle = borders(base(bold, null, null, true), BorderStyle.THIN, BorderStyle.DOUBLE, null, null);
            externalHeader = base(headerFont, WHITE_FILL, null, false);
            externalHeader.setVerticalAlignment(VerticalAlignment.BOTTOM);
            weekendTitle = borders(base(weekendBold, null, null, false), BorderStyle.MEDIUM, BorderStyle.MEDIUM, BorderStyle.MEDIUM, BorderStyle.MEDIUM);
            weekendPlain = base(weekendFont, null, null, false);
        }

        private CellStyle block(Kind kind, byte[] fill, boolean top, boolean bottom, boolean left, boolean right) {
            String key = "block|" + kind + "|" + (fill == null ? "-" : HexFormat.of().formatHex(fill)) + "|" + top + bottom + left + right;
            return cached(key, () -> borders(formatted(plain, kind, fill),
                    top ? BorderStyle.MEDIUM : BorderStyle.THIN, bottom ? BorderStyle.MEDIUM : BorderStyle.THIN,
                    left ? BorderStyle.MEDIUM : BorderStyle.THIN, right ? BorderStyle.MEDIUM : BorderStyle.THIN));
        }

        private CellStyle thin(Kind kind, byte[] fill) {
            return block(kind, fill, false, false, false, false);
        }

        private CellStyle summaryLabel(boolean doubleBottom) {
            return cached("summaryLabel|" + doubleBottom, () -> borders(base(bold, null, null, true),
                    null, doubleBottom ? BorderStyle.DOUBLE : null, null, null));
        }

        private CellStyle summaryValue(Kind kind, boolean isBold, boolean doubleBottom) {
            return cached("summaryValue|" + kind + "|" + isBold + "|" + doubleBottom, () -> borders(formatted(isBold ? bold : plain, kind, null),
                    null, doubleBottom ? BorderStyle.DOUBLE : null, null, null));
        }

        private CellStyle summaryTotal(Kind kind) {
            return cached("summaryTotal|" + kind, () -> borders(formatted(bold, kind, null), BorderStyle.THIN, BorderStyle.DOUBLE, null, null));
        }

        private CellStyle external(Kind kind, boolean left, boolean bottom) {
            return cached("external|" + kind + "|" + left + "|" + bottom, () -> borders(formatted(plain, kind, WHITE_FILL),
                    BorderStyle.THIN, bottom ? BorderStyle.MEDIUM : BorderStyle.THIN, left ? BorderStyle.MEDIUM : BorderStyle.THIN, BorderStyle.THIN));
        }

        private CellStyle weekend(Kind kind, boolean left, boolean top, boolean bottom, boolean right) {
            return cached("weekend|" + kind + "|" + left + top + bottom + right, () -> borders(formatted(weekendFont, kind, null),
                    top ? BorderStyle.MEDIUM : BorderStyle.THIN, bottom ? BorderStyle.MEDIUM : BorderStyle.THIN,
                    left ? BorderStyle.MEDIUM : BorderStyle.THIN, right ? BorderStyle.MEDIUM : BorderStyle.THIN));
        }

        private XSSFCellStyle formatted(XSSFFont font, Kind kind, byte[] fill) {
            XSSFCellStyle style = base(font, fill, formatOf(kind), kind == Kind.WRAPPED_TEXT);
            if (kind == Kind.SHORT_DATE) {
                style.setDataFormat(SHORT_DATE_FORMAT);
            }
            return style;
        }

        private String formatOf(Kind kind) {
            return switch (kind) {
                case INTEGER -> INTEGER_FORMAT;
                case DECIMAL -> DECIMAL_FORMAT;
                case ONE_DECIMAL -> ONE_DECIMAL_FORMAT;
                case PERCENT -> PERCENT_FORMAT;
                case DECIMAL_PERCENT -> DECIMAL_PERCENT_FORMAT;
                default -> null;
            };
        }

        private XSSFCellStyle cached(String key, java.util.function.Supplier<XSSFCellStyle> factory) {
            return cache.computeIfAbsent(key, ignored -> factory.get());
        }

        private XSSFFont font(String name, short size, boolean isBold) {
            XSSFFont font = workbook.createFont();
            font.setFontName(name);
            font.setFontHeightInPoints(size);
            font.setBold(isBold);
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
