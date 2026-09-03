package com.proseg.msvc_document_processor.excel;

import com.proseg.msvc_document_processor.dto.response.ColumnSchemaDto;
import com.proseg.msvc_document_processor.dto.response.SchemaDto;
import com.proseg.msvc_document_processor.dto.response.RowIssueDto;
import com.proseg.msvc_document_processor.excel.exception.InvalidCellValueException;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GenericExcelParser {

    public ParseResult parse(InputStream inputStream, SchemaDto schema) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            return parseSheet(workbook.getSheetAt(0), schema);
        } catch (IOException | IllegalArgumentException e) {
            throw DocumentProcessorException.unreadableWorkbook();
        }
    }

    private ParseResult parseSheet(Sheet sheet, SchemaDto schema) {
        List<RowIssueDto> errors = new ArrayList<>();

        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow == null) {
            throw DocumentProcessorException.unreadableWorkbook();
        }

        Map<String, ColumnSchemaDto> byNormalizedName = indexSchema(schema);
        Map<Integer, ColumnSchemaDto> columnsByIndex = new LinkedHashMap<>();

        for (int i = headerRow.getFirstCellNum(); i < headerRow.getLastCellNum(); i++) {
            String raw = CellValueReader.readString(headerRow.getCell(i));
            if (raw == null) {
                continue;
            }
            ColumnSchemaDto column = byNormalizedName.get(HeaderNormalizer.normalize(raw));
            if (column == null) {
                errors.add(RowIssueDto.builder()
                        .reason("Columna no reconocida: '" + raw + "'")
                        .build());
                continue;
            }
            columnsByIndex.putIfAbsent(i, column);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        int totalRows = 0;

        for (int i = headerRow.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isBlankRow(row, columnsByIndex)) {
                continue;
            }
            totalRows++;
            int excelRowNumber = i + 1;
            try {
                rows.add(parseRow(row, excelRowNumber, columnsByIndex));
            } catch (InvalidCellValueException e) {
                errors.add(RowIssueDto.builder()
                        .row(excelRowNumber)
                        .reason(e.getMessage())
                        .build());
            }
        }

        return ParseResult.builder()
                .totalRows(totalRows)
                .rows(rows)
                .errors(errors)
                .build();
    }

    private Map<String, ColumnSchemaDto> indexSchema(SchemaDto schema) {
        Map<String, ColumnSchemaDto> index = new HashMap<>();
        if (schema == null || schema.getColumns() == null) {
            return index;
        }
        for (ColumnSchemaDto column : schema.getColumns()) {
            if (column.getNames() == null) {
                continue;
            }
            for (String name : column.getNames()) {
                index.putIfAbsent(HeaderNormalizer.normalize(name), column);
            }
        }
        return index;
    }

    private boolean isBlankRow(Row row, Map<Integer, ColumnSchemaDto> columnsByIndex) {
        if (row == null) {
            return true;
        }
        for (Integer index : columnsByIndex.keySet()) {
            if (CellValueReader.readString(row.getCell(index)) != null) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Object> parseRow(Row row, int excelRowNumber,
                                         Map<Integer, ColumnSchemaDto> columnsByIndex) {
        Map<String, Object> values = new HashMap<>();
        values.put("rowNumber", excelRowNumber);

        for (Map.Entry<Integer, ColumnSchemaDto> entry : columnsByIndex.entrySet()) {
            ColumnSchemaDto column = entry.getValue();
            Object value = readValue(row.getCell(entry.getKey()), column);
            if (value != null) {
                values.put(column.getAttribute(), value);
            }
        }
        return values;
    }

    private Object readValue(Cell cell, ColumnSchemaDto column) {
        String label = column.getNames().get(0);
        return switch (column.getType()) {
            case "DATE" -> CellValueReader.readDate(cell, label);
            case "DECIMAL", "INTEGER" -> CellValueReader.readBigDecimal(cell, label);
            default -> CellValueReader.readString(cell);
        };
    }
}
