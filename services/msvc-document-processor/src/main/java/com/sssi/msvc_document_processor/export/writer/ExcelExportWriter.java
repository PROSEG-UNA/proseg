package com.sssi.msvc_document_processor.export.writer;

import com.sssi.msvc_document_processor.exception.DocumentProcessorException;
import com.sssi.msvc_document_processor.export.ExportColumnDefinition;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class ExcelExportWriter implements ExportWriter {

    private static final String SHEET_NAME = "Export";

    @Override
    public byte[] write(List<ExportColumnDefinition> columns, List<Map<String, Object>> rows) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(SHEET_NAME);
            CellStyle headerStyle = buildHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(i).header());
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (Map<String, Object> row : rows) {
                Row excelRow = sheet.createRow(rowIndex++);
                for (int i = 0; i < columns.size(); i++) {
                    Object value = columns.get(i).valueExtractor().apply(row);
                    excelRow.createCell(i).setCellValue(value == null ? "" : String.valueOf(value));
                }
            }

            for (int i = 0; i < columns.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw DocumentProcessorException.exportGenerationFailed();
        }
    }

    private CellStyle buildHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
