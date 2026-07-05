package com.sssi.msvc_document_processor.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class DocumentProcessorException extends BaseException {

    public DocumentProcessorException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static DocumentProcessorException emptyFile() {
        return new DocumentProcessorException(
                HttpStatus.BAD_REQUEST,
                "EMPTY_FILE",
                "El archivo está vacío"
        );
    }

    public static DocumentProcessorException invalidFileType() {
        return new DocumentProcessorException(
                HttpStatus.BAD_REQUEST,
                "INVALID_FILE_TYPE",
                "El archivo debe tener extensión .xlsx o .xls"
        );
    }

    public static DocumentProcessorException unreadableWorkbook() {
        return new DocumentProcessorException(
                HttpStatus.BAD_REQUEST,
                "UNREADABLE_WORKBOOK",
                "No fue posible leer el archivo Excel"
        );
    }

    public static DocumentProcessorException inventoryUnavailable() {
        return new DocumentProcessorException(
                HttpStatus.BAD_GATEWAY,
                "INVENTORY_UNAVAILABLE",
                "No fue posible registrar los activos en inventario"
        );
    }

    public static DocumentProcessorException unsupportedDocumentType(String documentType) {
        return new DocumentProcessorException(
                HttpStatus.BAD_REQUEST,
                "UNSUPPORTED_DOCUMENT_TYPE",
                "Tipo de documento no soportado: " + documentType
        );
    }

    public static DocumentProcessorException templateGenerationFailed() {
        return new DocumentProcessorException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "TEMPLATE_GENERATION_FAILED",
                "No fue posible generar la plantilla"
        );
    }

    public static DocumentProcessorException unsupportedExportFormat(String format) {
        return new DocumentProcessorException(
                HttpStatus.BAD_REQUEST,
                "UNSUPPORTED_EXPORT_FORMAT",
                "Formato de exportación no soportado: " + format
        );
    }

    public static DocumentProcessorException exportGenerationFailed() {
        return new DocumentProcessorException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "EXPORT_GENERATION_FAILED",
                "No fue posible generar el archivo de exportación"
        );
    }

    public static DocumentProcessorException exportMaxRowsExceeded(int maxRows) {
        return new DocumentProcessorException(
                HttpStatus.BAD_REQUEST,
                "EXPORT_MAX_ROWS_EXCEEDED",
                "La exportación excede el máximo permitido de filas: " + maxRows
        );
    }
}
