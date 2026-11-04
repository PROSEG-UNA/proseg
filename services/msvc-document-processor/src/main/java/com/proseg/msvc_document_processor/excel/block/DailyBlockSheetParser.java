package com.proseg.msvc_document_processor.excel.block;

import com.proseg.msvc_document_processor.dto.response.ColumnSchemaDto;
import com.proseg.msvc_document_processor.dto.response.RowIssueDto;
import com.proseg.msvc_document_processor.dto.response.SchemaDto;
import com.proseg.msvc_document_processor.excel.CellValueConverter;
import com.proseg.msvc_document_processor.excel.HeaderNormalizer;
import com.proseg.msvc_document_processor.excel.ParseResult;
import com.proseg.msvc_document_processor.excel.exception.InvalidCellValueException;
import com.proseg.msvc_document_processor.excel.reader.SheetCell;
import com.proseg.msvc_document_processor.excel.reader.SheetRow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class DailyBlockSheetParser {

    private static final Pattern BLOCK_TITLE = Pattern.compile(
            "^(lunes|martes|miercoles|jueves|viernes|sabado|domingo)\\s+\\d{1,2}\\s+de\\s+\\p{L}+(\\s+de\\s+\\d{4})?$");
    private static final Set<String> BLOCK_GROUP_LABELS = Set.of("hora", "fecha");
    private static final Set<String> IGNORED_HEADERS = Set.of("x dia", "x mes");
    private static final int MIN_HEADER_MATCHES = 3;

    public ParseResult parse(List<SheetRow> rows, SchemaDto schema) {
        List<ColumnSchemaDto> columns = schema == null || schema.getColumns() == null ? List.of() : schema.getColumns();
        Set<String> knownNames = columns.stream()
                .flatMap(column -> column.getNames().stream())
                .map(HeaderNormalizer::normalize)
                .collect(Collectors.toSet());

        Map<String, RowIssueDto> headerIssues = new LinkedHashMap<>();
        List<RowIssueDto> rowIssues = new ArrayList<>();
        List<Map<String, Object>> parsedRows = new ArrayList<>();
        Map<Integer, ColumnSchemaDto> mapping = null;
        boolean orphanRowReported = false;
        int blocksDetected = 0;
        int totalRows = 0;

        for (SheetRow row : rows) {
            if (row.isBlank()) {
                continue;
            }
            if (isBlockSeparator(row)) {
                blocksDetected++;
                mapping = null;
                orphanRowReported = false;
                continue;
            }
            if (isHeaderRow(row, knownNames)) {
                mapping = resolveHeader(row, columns, headerIssues);
                continue;
            }
            if (mapping == null) {
                if (!orphanRowReported) {
                    rowIssues.add(issue(row.rowNumber(), "Fila de datos antes de la fila de encabezados"));
                    orphanRowReported = true;
                }
                continue;
            }
            totalRows++;
            parseDataRow(row, mapping, parsedRows, rowIssues);
        }

        List<RowIssueDto> errors = new ArrayList<>(headerIssues.values());
        errors.addAll(rowIssues);
        errors.sort(Comparator.comparing(RowIssueDto::getRow, Comparator.nullsFirst(Comparator.naturalOrder())));
        return ParseResult.builder()
                .totalRows(totalRows)
                .blocksDetected(blocksDetected)
                .rows(parsedRows)
                .errors(errors)
                .build();
    }

    private boolean isBlockSeparator(SheetRow row) {
        List<SheetCell> filled = row.cells().stream()
                .filter(cell -> !cell.isBlank())
                .toList();
        SheetCell first = filled.get(0);
        boolean textTitle = BLOCK_TITLE.matcher(HeaderNormalizer.normalize(first.text())).matches();
        boolean dateTitle = first.dateValue() != null && filled.size() > 1;
        if (!textTitle && !dateTitle) {
            return false;
        }
        return filled.stream()
                .skip(1)
                .allMatch(cell -> BLOCK_GROUP_LABELS.contains(HeaderNormalizer.normalize(cell.text())));
    }

    private boolean isHeaderRow(SheetRow row, Set<String> knownNames) {
        long matches = row.cells().stream()
                .filter(cell -> !cell.isBlank())
                .filter(cell -> knownNames.contains(HeaderNormalizer.normalize(cell.text())))
                .count();
        return matches >= MIN_HEADER_MATCHES;
    }

    private Map<Integer, ColumnSchemaDto> resolveHeader(
            SheetRow row,
            List<ColumnSchemaDto> columns,
            Map<String, RowIssueDto> headerIssues
    ) {
        Map<Integer, ColumnSchemaDto> mapping = new LinkedHashMap<>();
        Set<String> assigned = new HashSet<>();
        for (int i = 0; i < row.cells().size(); i++) {
            SheetCell cell = row.cells().get(i);
            if (cell.isBlank()) {
                continue;
            }
            String name = HeaderNormalizer.normalize(cell.text());
            if (IGNORED_HEADERS.contains(name)) {
                continue;
            }
            List<ColumnSchemaDto> candidates = columns.stream()
                    .filter(column -> acceptsName(column, name))
                    .toList();
            if (candidates.isEmpty()) {
                addHeaderIssue(headerIssues, row, "Columna no reconocida: '" + cell.text() + "'");
                continue;
            }
            Optional<ColumnSchemaDto> free = candidates.stream()
                    .filter(column -> !assigned.contains(column.getAttribute()))
                    .findFirst();
            if (free.isEmpty()) {
                addHeaderIssue(headerIssues, row, "Columna repetida: '" + cell.text() + "'");
                continue;
            }
            mapping.put(i, free.get());
            assigned.add(free.get().getAttribute());
        }
        for (ColumnSchemaDto column : columns) {
            if (column.isRequired() && !assigned.contains(column.getAttribute())) {
                addHeaderIssue(headerIssues, row, "Falta la columna obligatoria '" + label(column) + "'");
            }
        }
        return mapping;
    }

    private void parseDataRow(
            SheetRow row,
            Map<Integer, ColumnSchemaDto> mapping,
            List<Map<String, Object>> parsedRows,
            List<RowIssueDto> rowIssues
    ) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("rowNumber", row.rowNumber());
        boolean valid = true;
        for (Map.Entry<Integer, ColumnSchemaDto> entry : mapping.entrySet()) {
            ColumnSchemaDto column = entry.getValue();
            SheetCell cell = row.cell(entry.getKey());
            if (cell.isBlank()) {
                if (column.isRequired()) {
                    rowIssues.add(issue(row.rowNumber(), "El campo obligatorio '" + label(column) + "' está vacío"));
                    valid = false;
                }
                continue;
            }
            try {
                values.put(column.getAttribute(), CellValueConverter.convert(cell, column.getType(), label(column)));
            } catch (InvalidCellValueException e) {
                rowIssues.add(issue(row.rowNumber(), e.getMessage()));
                valid = false;
            }
        }
        if (valid) {
            parsedRows.add(values);
        }
    }

    private boolean acceptsName(ColumnSchemaDto column, String normalizedName) {
        return column.getNames().stream()
                .map(HeaderNormalizer::normalize)
                .anyMatch(normalizedName::equals);
    }

    private void addHeaderIssue(Map<String, RowIssueDto> headerIssues, SheetRow row, String reason) {
        headerIssues.putIfAbsent(reason, issue(row.rowNumber(), reason));
    }

    private String label(ColumnSchemaDto column) {
        List<String> names = column.getNames();
        return names == null || names.isEmpty() ? column.getAttribute() : names.get(0);
    }

    private RowIssueDto issue(Integer row, String reason) {
        return RowIssueDto.builder()
                .row(row)
                .reason(reason)
                .build();
    }
}
