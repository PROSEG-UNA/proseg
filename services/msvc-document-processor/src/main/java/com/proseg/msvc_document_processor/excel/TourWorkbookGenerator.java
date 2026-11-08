package com.proseg.msvc_document_processor.excel;

import com.proseg.msvc_document_processor.dto.response.ColumnSchemaDto;
import com.proseg.msvc_document_processor.dto.response.SchemaDto;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TourWorkbookGenerator {

    private static final String SHEET_NAME = "Giras";
    private static final String TITLE_PLACEHOLDER = "{día} {número} de {mes} de {año}";
    private static final List<String> LEADING_HEADERS = List.of("x dia", "x mes");
    private static final Map<String, String> GROUP_LABELS = Map.of(
            "TIME", "Hora",
            "DATE", "Fecha"
    );
    private static final String GENERAL_FORMAT = "General";
    private static final Map<String, String> DATA_FORMATS = Map.of(
            "TEXT", "@",
            "DATE", "dd-mm-yyyy",
            "TIME", "hh:mm"
    );
    private static final String FONT_NAME = "Arial";
    private static final short FONT_SIZE = 10;
    private static final float ROW_HEIGHT = 15f;
    private static final int FIRST_COLUMN_WIDTH = 7716;
    private static final int DEFAULT_COLUMN_WIDTH = 5302;
    private static final int LAST_COLUMN_WIDTH = 7862;
    private static final double SECONDS_PER_DAY = 86_400d;

    public byte[] generateTemplate(SchemaDto schema) {
        try {
            return write(schema, List.of(new TourSheetBlock(TITLE_PLACEHOLDER, List.of())));
        } catch (IOException e) {
            throw DocumentProcessorException.templateGenerationFailed();
        }
    }

    public byte[] generate(SchemaDto schema, List<TourSheetBlock> blocks) {
        try {
            return write(schema, blocks);
        } catch (IOException e) {
            throw DocumentProcessorException.exportGenerationFailed();
        }
    }

    private byte[] write(SchemaDto schema, List<TourSheetBlock> blocks) throws IOException {
        List<ColumnSchemaDto> columns = schema != null && schema.getColumns() != null ? schema.getColumns() : List.of();
        int leading = LEADING_HEADERS.size();
        int columnCount = leading + columns.size();
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            sheet.setDefaultRowHeightInPoints(ROW_HEIGHT);
            Font font = workbook.createFont();
            font.setFontName(FONT_NAME);
            font.setFontHeightInPoints(FONT_SIZE);
            Map<String, CellStyle> styles = new HashMap<>();
            CellStyle baseStyle = styleFor(workbook, font, styles, GENERAL_FORMAT);
            List<CellStyle> dataStyles = columns.stream()
                    .map(column -> styleFor(workbook, font, styles,
                            DATA_FORMATS.getOrDefault(column.getType(), GENERAL_FORMAT)))
                    .toList();

            for (int i = 0; i < columnCount; i++) {
                sheet.setColumnWidth(i, widthOf(i, columnCount));
                sheet.setDefaultColumnStyle(i, i < leading ? baseStyle : dataStyles.get(i - leading));
            }

            int rowIndex = 0;
            for (TourSheetBlock block : blocks) {
                Row titleRow = createRow(sheet, rowIndex++);
                writeCell(titleRow, 0, block.title(), baseStyle);
                GROUP_LABELS.forEach((type, label) ->
                        writeCell(titleRow, firstIndexOfType(columns, type, leading), label, baseStyle));

                Row headerRow = createRow(sheet, rowIndex++);
                for (int i = 0; i < leading; i++) {
                    writeCell(headerRow, i, LEADING_HEADERS.get(i), baseStyle);
                }
                for (int i = 0; i < columns.size(); i++) {
                    writeCell(headerRow, leading + i, headerLabel(columns.get(i)), baseStyle);
                }

                for (Map<String, Object> values : block.rows()) {
                    Row dataRow = createRow(sheet, rowIndex++);
                    for (int i = 0; i < columns.size(); i++) {
                        ColumnSchemaDto column = columns.get(i);
                        writeValue(dataRow, leading + i, column.getType(), values.get(column.getAttribute()), dataStyles.get(i));
                    }
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private Row createRow(Sheet sheet, int index) {
        Row row = sheet.createRow(index);
        row.setHeightInPoints(ROW_HEIGHT);
        return row;
    }

    private CellStyle styleFor(XSSFWorkbook workbook, Font font, Map<String, CellStyle> styles, String format) {
        return styles.computeIfAbsent(format, value -> {
            CellStyle style = workbook.createCellStyle();
            style.setFont(font);
            style.setWrapText(true);
            style.setDataFormat(workbook.createDataFormat().getFormat(value));
            return style;
        });
    }

    private int widthOf(int index, int columnCount) {
        if (index == 0) {
            return FIRST_COLUMN_WIDTH;
        }
        return index == columnCount - 1 ? LAST_COLUMN_WIDTH : DEFAULT_COLUMN_WIDTH;
    }

    private String headerLabel(ColumnSchemaDto column) {
        String name = column.getNames().get(0);
        String groupPrefix = GROUP_LABELS.getOrDefault(column.getType(), "") + " ";
        String label = name.startsWith(groupPrefix) && groupPrefix.length() > 1 ? name.substring(groupPrefix.length()) : name;
        return Normalizer.normalize(label, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private void writeCell(Row row, int index, String value, CellStyle style) {
        if (index < 0) {
            return;
        }
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void writeValue(Row row, int index, String type, Object value, CellStyle style) {
        if (value == null) {
            return;
        }
        Cell cell = row.createCell(index);
        cell.setCellStyle(style);
        switch (type) {
            case "INTEGER" -> cell.setCellValue(Double.parseDouble(value.toString()));
            case "DATE" -> cell.setCellValue(LocalDate.parse(value.toString()));
            case "TIME" -> cell.setCellValue(LocalTime.parse(value.toString()).toSecondOfDay() / SECONDS_PER_DAY);
            default -> cell.setCellValue(value.toString());
        }
    }

    private int firstIndexOfType(List<ColumnSchemaDto> columns, String type, int offset) {
        for (int i = 0; i < columns.size(); i++) {
            if (type.equals(columns.get(i).getType())) {
                return offset + i;
            }
        }
        return -1;
    }
}
