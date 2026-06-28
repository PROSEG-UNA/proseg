package com.sssi.msvc_document_processor.excel;

import com.sssi.msvc_document_processor.dto.response.ColumnSchemaDto;
import com.sssi.msvc_document_processor.dto.response.SchemaDto;
import com.sssi.msvc_document_processor.exception.DocumentProcessorException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExcelTemplateGenerator {

    private static final int COLUMN_WIDTH = 40 * 256;
    private static final float HEADER_HEIGHT_POINTS = 30f;
    private static final byte[] HEADER_FILL_RGB = {(byte) 0xD9, (byte) 0xD9, (byte) 0xD9};
    private static final String TEXT_FORMAT = "@";
    private static final String DATE_FORMAT = "dd/mm/yyyy";

    public byte[] generate(SchemaDto schema) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Plantilla");
            sheet.createFreezePane(0, 1);

            XSSFCellStyle headerStyle = buildHeaderStyle(workbook);
            Map<String, CellStyle> dataStyleCache = new HashMap<>();

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(HEADER_HEIGHT_POINTS);

            List<ColumnSchemaDto> columns = schema != null ? schema.getColumns() : null;
            if (columns != null) {
                for (int i = 0; i < columns.size(); i++) {
                    ColumnSchemaDto column = columns.get(i);

                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(resolveHeader(column));
                    cell.setCellStyle(headerStyle);

                    sheet.setColumnWidth(i, COLUMN_WIDTH);

                    CellStyle dataStyle = dataStyleFor(workbook, column.getType(), dataStyleCache);
                    if (dataStyle != null) {
                        sheet.setDefaultColumnStyle(i, dataStyle);
                    }
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw DocumentProcessorException.templateGenerationFailed();
        }
    }

    private XSSFCellStyle buildHeaderStyle(XSSFWorkbook workbook) {
        XSSFFont font = workbook.createFont();
        font.setBold(true);

        XSSFCellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(new XSSFColor(HEADER_FILL_RGB, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private CellStyle dataStyleFor(XSSFWorkbook workbook, String type, Map<String, CellStyle> cache) {
        String format = formatFor(type);
        if (format == null) {
            return null;
        }
        return cache.computeIfAbsent(format, value -> {
            CellStyle style = workbook.createCellStyle();
            style.setDataFormat(workbook.createDataFormat().getFormat(value));
            return style;
        });
    }

    private String formatFor(String type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "TEXT" -> TEXT_FORMAT;
            case "DATE" -> DATE_FORMAT;
            default -> null;
        };
    }

    private String resolveHeader(ColumnSchemaDto column) {
        List<String> names = column.getNames();
        if (names != null && !names.isEmpty() && names.get(0) != null) {
            return names.get(0);
        }
        return column.getAttribute();
    }
}
