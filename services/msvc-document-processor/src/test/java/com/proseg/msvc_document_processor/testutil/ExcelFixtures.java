package com.proseg.msvc_document_processor.testutil;

import com.proseg.msvc_document_processor.dto.response.ColumnSchemaDto;
import com.proseg.msvc_document_processor.dto.response.SchemaDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public final class ExcelFixtures {

    private ExcelFixtures() {
    }

    public static SchemaDto schema() {
        return SchemaDto.builder()
                .columns(List.of(
                        ColumnSchemaDto.builder().attribute("name").names(List.of("Nombre", "Name")).type("TEXT").build(),
                        ColumnSchemaDto.builder().attribute("date").names(List.of("Fecha")).type("DATE").build(),
                        ColumnSchemaDto.builder().attribute("amount").names(List.of("Monto")).type("DECIMAL").build(),
                        ColumnSchemaDto.builder().attribute("count").names(List.of("Cantidad")).type("INTEGER").build()
                ))
                .build();
    }

    public static byte[] workbook(String[] headers, Object[][] rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Datos");
            writeRow(sheet.createRow(0), headers);
            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < rows[i].length; j++) {
                    Object value = rows[i][j];
                    if (value != null) {
                        Cell cell = row.createCell(j);
                        if (value instanceof Number number) {
                            cell.setCellValue(number.doubleValue());
                        } else {
                            cell.setCellValue(value.toString());
                        }
                    }
                }
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeRow(Row row, String[] values) {
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }
}
