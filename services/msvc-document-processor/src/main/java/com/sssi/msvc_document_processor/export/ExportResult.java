package com.sssi.msvc_document_processor.export;

public record ExportResult(
        ExportFormat format,
        byte[] content
) {
}
