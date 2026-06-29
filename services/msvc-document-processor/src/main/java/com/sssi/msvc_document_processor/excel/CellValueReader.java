package com.sssi.msvc_document_processor.excel;

import com.sssi.msvc_document_processor.excel.exception.InvalidCellValueException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class CellValueReader {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    );

    private CellValueReader() {
    }

    public static String readString(Cell cell) {
        if (cell == null) return null;
        CellType type = effectiveType(cell);
        String value = switch (type) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> formatNumeric(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static LocalDate readDate(Cell cell, String columnLabel) {
        if (cell == null) return null;
        CellType type = effectiveType(cell);
        if (type == CellType.NUMERIC) {
            if (!DateUtil.isCellDateFormatted(cell)) {
                throw new InvalidCellValueException("Valor de fecha inválido en la columna '" + columnLabel + "'");
            }
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String text = readString(cell);
        if (text == null) return null;
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new InvalidCellValueException(
                "Formato de fecha no reconocido en la columna '" + columnLabel + "'");
    }

    public static BigDecimal readBigDecimal(Cell cell, String columnLabel) {
        if (cell == null) return null;
        CellType type = effectiveType(cell);
        if (type == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String text = readString(cell);
        if (text == null) return null;
        try {
            return new BigDecimal(text.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new InvalidCellValueException(
                    "Valor numérico inválido en la columna '" + columnLabel + "'");
        }
    }

    private static CellType effectiveType(Cell cell) {
        CellType type = cell.getCellType();
        return type == CellType.FORMULA ? cell.getCachedFormulaResultType() : type;
    }

    private static String formatNumeric(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
