package com.proseg.msvc_document_processor.exception;

import com.proseg.common.api.exception.BaseException;
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

    public static DocumentProcessorException invalidTourFileType() {
        return new DocumentProcessorException(
                HttpStatus.BAD_REQUEST,
                "INVALID_FILE_TYPE",
                "El archivo debe tener extensión .xlsx, .xls, .xlsm o .csv"
        );
    }

    public static DocumentProcessorException unsupportedFileFormat() {
        return new DocumentProcessorException(
                HttpStatus.BAD_REQUEST,
                "UNSUPPORTED_FILE_FORMAT",
                "El contenido del archivo no corresponde a un Excel, un CSV ni una tabla HTML"
        );
    }

    public static DocumentProcessorException htmlTableNotFound() {
        return new DocumentProcessorException(
                HttpStatus.BAD_REQUEST,
                "HTML_TABLE_NOT_FOUND",
                "El archivo no contiene una tabla"
        );
    }

    public static DocumentProcessorException transportUnavailable() {
        return new DocumentProcessorException(
                HttpStatus.BAD_GATEWAY,
                "TRANSPORT_UNAVAILABLE",
                "No fue posible registrar las giras en transporte"
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

    public static DocumentProcessorException maintenanceUnavailable() {
        return new DocumentProcessorException(
                HttpStatus.BAD_GATEWAY,
                "MAINTENANCE_UNAVAILABLE",
                "No fue posible obtener los datos de mantenimiento"
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
