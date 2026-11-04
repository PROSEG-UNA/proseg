package com.proseg.msvc_document_processor.excel;

import com.proseg.msvc_document_processor.excel.exception.InvalidCellValueException;
import com.proseg.msvc_document_processor.excel.reader.SheetCell;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class CellValueConverter {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            strict("d-M-uuuu"),
            strict("d/M/uuuu"),
            strict("uuuu-M-d")
    );
    private static final List<DateTimeFormatter> TIME_FORMATS = List.of(
            strict("H:mm"),
            strict("H:mm:ss"),
            strict("h:mm a"),
            strict("h:mm:ss a")
    );
    private static final Pattern INTEGER_TEXT = Pattern.compile("-?\\d+(\\.0+)?");
    private static final Pattern MERIDIEM_AM = Pattern.compile("\\s*A\\.?\\s*M\\.?$");
    private static final Pattern MERIDIEM_PM = Pattern.compile("\\s*P\\.?\\s*M\\.?$");

    private CellValueConverter() {
    }

    public static Object convert(SheetCell cell, String type, String columnLabel) {
        if (cell.isBlank()) {
            return null;
        }
        return switch (type) {
            case "DATE" -> toDate(cell, columnLabel);
            case "TIME" -> toTime(cell, columnLabel);
            case "INTEGER" -> toInteger(cell, columnLabel);
            default -> cell.text();
        };
    }

    private static LocalDate toDate(SheetCell cell, String columnLabel) {
        if (cell.dateValue() != null) {
            return cell.dateValue().toLocalDate();
        }
        String text = cell.text();
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new InvalidCellValueException(
                "Fecha inválida en la columna '" + columnLabel + "': '" + text + "'");
    }

    private static LocalTime toTime(SheetCell cell, String columnLabel) {
        if (cell.dateValue() != null) {
            return cell.dateValue().toLocalTime();
        }
        String text = normalizeMeridiem(cell.text());
        for (DateTimeFormatter formatter : TIME_FORMATS) {
            try {
                return LocalTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new InvalidCellValueException(
                "Hora inválida en la columna '" + columnLabel + "': '" + cell.text() + "'");
    }

    private static Integer toInteger(SheetCell cell, String columnLabel) {
        if (cell.numericValue() != null && cell.dateValue() == null) {
            double value = cell.numericValue();
            if (value == Math.rint(value) && Math.abs(value) <= Integer.MAX_VALUE) {
                return (int) value;
            }
        }
        String text = cell.text();
        if (text != null && INTEGER_TEXT.matcher(text).matches()) {
            try {
                return Integer.parseInt(text.replaceFirst("\\.0+$", ""));
            } catch (NumberFormatException ignored) {
            }
        }
        throw new InvalidCellValueException(
                "Valor entero inválido en la columna '" + columnLabel + "': '" + text + "'");
    }

    private static String normalizeMeridiem(String text) {
        String upper = text.toUpperCase(Locale.ROOT);
        if (MERIDIEM_AM.matcher(upper).find()) {
            return MERIDIEM_AM.matcher(upper).replaceFirst(" AM");
        }
        if (MERIDIEM_PM.matcher(upper).find()) {
            return MERIDIEM_PM.matcher(upper).replaceFirst(" PM");
        }
        return upper;
    }

    private static DateTimeFormatter strict(String pattern) {
        return DateTimeFormatter.ofPattern(pattern, Locale.US).withResolverStyle(ResolverStyle.STRICT);
    }
}
