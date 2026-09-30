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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TourTemplateGenerator {

    private static final int COLUMN_WIDTH = 22 * 256;
    private static final String EXAMPLE_BLOCK_TITLE = "Viernes 1 de Mayo de 2026";
    private static final Map<String, String> DATA_FORMATS = Map.of(
            "TEXT", "@",
            "DATE", "dd-mm-yyyy",
            "TIME", "hh:mm"
    );

    public byte[] generate(SchemaDto schema) {
        List<ColumnSchemaDto> columns = schema != null && schema.getColumns() != null ? schema.getColumns() : List.of();
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Giras");
            CellStyle boldStyle = buildBoldStyle(workbook);
            Map<String, CellStyle> dataStyles = new HashMap<>();

            Row blockRow = sheet.createRow(0);
            writeCell(blockRow, 0, EXAMPLE_BLOCK_TITLE, boldStyle);
            writeCell(blockRow, firstIndexOfType(columns, "TIME"), "Hora", boldStyle);
            writeCell(blockRow, firstIndexOfType(columns, "DATE"), "Fecha", boldStyle);

            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < columns.size(); i++) {
                ColumnSchemaDto column = columns.get(i);
                writeCell(headerRow, i, column.getNames().get(0), boldStyle);
                sheet.setColumnWidth(i, COLUMN_WIDTH);
                String format = DATA_FORMATS.get(column.getType());
                if (format != null) {
                    sheet.setDefaultColumnStyle(i, dataStyles.computeIfAbsent(format, value -> {
                        CellStyle style = workbook.createCellStyle();
                        style.setDataFormat(workbook.createDataFormat().getFormat(value));
                        return style;
                    }));
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw DocumentProcessorException.templateGenerationFailed();
        }
    }

    private CellStyle buildBoldStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void writeCell(Row row, int index, String value, CellStyle style) {
        if (index < 0) {
            return;
        }
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private int firstIndexOfType(List<ColumnSchemaDto> columns, String type) {
        for (int i = 0; i < columns.size(); i++) {
            if (type.equals(columns.get(i).getType())) {
                return i;
            }
        }
        return -1;
    }
}
