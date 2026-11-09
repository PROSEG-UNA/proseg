package com.proseg.msvc_document_processor.export.target;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import com.proseg.msvc_document_processor.export.ExportPage;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.NumberFormat;

final class TransportExportLabels {

    private static final Locale LOCALE = Locale.forLanguageTag("es-CR");

    private TransportExportLabels() {
    }

    static ExportPage page(ApiResponse<PageResponse<Map<String, Object>>> response) {
        if (response == null || response.getData() == null) {
            throw DocumentProcessorException.transportExportFailed();
        }
        PageResponse<Map<String, Object>> page = response.getData();
        List<Map<String, Object>> content = page.getContent() != null ? page.getContent() : List.of();
        return new ExportPage(content, page.isLast(), page.getTotalElements());
    }

    static String vehicleType(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return "—";
        }
        return switch (String.valueOf(value)) {
            case "MICROBUS" -> "Microbús";
            case "SEDAN" -> "Sedán";
            case "ALL_TERRAIN" -> "Todo Terreno";
            case "SPECIAL" -> "Especial";
            case "PICK_UP" -> "Pick-Up";
            case "STATION_WAGON" -> "Station Wagon";
            default -> String.valueOf(value);
        };
    }

    static String availability(Object value) {
        if (value == null) {
            return "";
        }
        boolean available = value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
        return available ? "Disponible" : "No disponible";
    }

    static String driverStatus(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return "—";
        }
        return switch (String.valueOf(value)) {
            case "ACTIVE" -> "Activo";
            case "INACTIVE" -> "Inactivo";
            default -> String.valueOf(value);
        };
    }

    static String unit(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return "Servicios Generales";
        }
        return switch (String.valueOf(value)) {
            case "GENERAL_SERVICES" -> "Servicios Generales";
            case "OTHER_UNIT" -> "Otra Unidad";
            default -> String.valueOf(value);
        };
    }

    static String schedule(Object start, Object end) {
        return time(start, "07:30") + " - " + time(end, "16:30");
    }

    static String hours(Object value) {
        NumberFormat format = NumberFormat.getNumberInstance(LOCALE);
        format.setMaximumFractionDigits(1);
        return format.format(number(value));
    }

    static String percent(Object value) {
        if (value == null) {
            return "—";
        }
        NumberFormat format = NumberFormat.getPercentInstance(LOCALE);
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(1);
        return format.format(number(value));
    }

    static String whole(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return "—";
        }
        double number = number(value);
        if (number == Math.rint(number)) {
            return Long.toString((long) number);
        }
        return Double.toString(number);
    }

    static double number(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return 0;
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private static String time(Object value, String fallback) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        if (value instanceof List<?> parts && parts.size() >= 2) {
            return pad(parts.get(0)) + ":" + pad(parts.get(1));
        }
        String text = String.valueOf(value);
        return text.length() >= 5 ? text.substring(0, 5) : text;
    }

    private static String pad(Object value) {
        String text = String.valueOf(value);
        return text.length() == 1 ? "0" + text : text;
    }
}
