package com.proseg.msvc_document_processor.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentProcessorExceptionTest {

    @Test
    void factories_preserve_expected_status_codes_and_messages() {
        List<DocumentProcessorException> errors = List.of(
                DocumentProcessorException.emptyFile(),
                DocumentProcessorException.invalidFileType(),
                DocumentProcessorException.unreadableWorkbook(),
                DocumentProcessorException.inventoryUnavailable(),
                DocumentProcessorException.maintenanceUnavailable(),
                DocumentProcessorException.unsupportedDocumentType("custom"),
                DocumentProcessorException.templateGenerationFailed(),
                DocumentProcessorException.unsupportedExportFormat("pdf"),
                DocumentProcessorException.exportGenerationFailed(),
                DocumentProcessorException.exportMaxRowsExceeded(5));

        assertThat(errors).extracting(DocumentProcessorException::getErrorCode).containsExactly(
                "EMPTY_FILE", "INVALID_FILE_TYPE", "UNREADABLE_WORKBOOK", "INVENTORY_UNAVAILABLE",
                "MAINTENANCE_UNAVAILABLE", "UNSUPPORTED_DOCUMENT_TYPE", "TEMPLATE_GENERATION_FAILED",
                "UNSUPPORTED_EXPORT_FORMAT", "EXPORT_GENERATION_FAILED", "EXPORT_MAX_ROWS_EXCEEDED");
        assertThat(errors).extracting(DocumentProcessorException::getHttpStatus).containsExactly(
                HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST, HttpStatus.BAD_GATEWAY,
                HttpStatus.BAD_GATEWAY, HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_REQUEST);
        assertThat(errors.get(5).getMessage()).contains("custom");
        assertThat(errors.get(9).getMessage()).contains("5");
    }
}
