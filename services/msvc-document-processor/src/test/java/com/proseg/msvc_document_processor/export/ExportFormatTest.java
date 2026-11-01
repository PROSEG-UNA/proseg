package com.proseg.msvc_document_processor.export;

import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportFormatTest {

    @Test
    void resolves_default_aliases_and_case_insensitively() {
        assertThat(ExportFormat.fromValue(null)).isEqualTo(ExportFormat.XLSX);
        assertThat(ExportFormat.fromValue(" ")).isEqualTo(ExportFormat.XLSX);
        assertThat(ExportFormat.fromValue(" Excel ")).isEqualTo(ExportFormat.XLSX);
        assertThat(ExportFormat.fromValue("CSV")).isEqualTo(ExportFormat.CSV);
        assertThat(ExportFormat.CSV.getContentType()).contains("text/csv");
        assertThat(ExportFormat.XLSX.getFileExtension()).isEqualTo("xlsx");
    }

    @Test
    void rejects_unknown_format() {
        assertThatThrownBy(() -> ExportFormat.fromValue("pdf"))
                .isInstanceOf(DocumentProcessorException.class)
                .hasMessageContaining("pdf");
    }
}
