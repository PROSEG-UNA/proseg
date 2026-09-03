package com.proseg.msvc_document_processor.export;

import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import lombok.Getter;

import java.util.Locale;

@Getter
public enum ExportFormat {
    CSV("csv", "text/csv; charset=UTF-8", "csv"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

    private static final String FORMAT_XLSX = "xlsx";
    private static final String FORMAT_EXCEL = "excel";
    private static final String FORMAT_CSV = "csv";

    private final String value;
    private final String contentType;
    private final String fileExtension;

    ExportFormat(String value, String contentType, String fileExtension) {
        this.value = value;
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }

    public static ExportFormat fromValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return XLSX;
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case FORMAT_XLSX, FORMAT_EXCEL -> XLSX;
            case FORMAT_CSV -> CSV;
            default -> throw DocumentProcessorException.unsupportedExportFormat(rawValue);
        };
    }
}
