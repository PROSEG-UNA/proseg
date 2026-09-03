package com.proseg.msvc_document_processor.export.writer;

import com.proseg.msvc_document_processor.export.ExportColumnDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CsvExportWriter implements ExportWriter {

    @Override
    public byte[] write(List<ExportColumnDefinition> columns, List<Map<String, Object>> rows) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append(columns.stream()
                .map(ExportColumnDefinition::header)
                .map(this::escapeCsv)
                .collect(Collectors.joining(",")))
                .append("\r\n");

        for (Map<String, Object> row : rows) {
            String line = columns.stream()
                    .map(column -> column.valueExtractor().apply(row))
                    .map(this::toText)
                    .map(this::escapeCsv)
                    .collect(Collectors.joining(","));
            csv.append(line).append("\r\n");
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String toText(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private String escapeCsv(String value) {
        String normalized = value == null ? "" : value;
        boolean requiresQuotes = normalized.contains(",")
                || normalized.contains("\"")
                || normalized.contains("\n")
                || normalized.contains("\r");

        if (!requiresQuotes) {
            return normalized;
        }

        return "\"" + normalized.replace("\"", "\"\"") + "\"";
    }
}
