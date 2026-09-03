package com.proseg.msvc_document_processor.export;

import java.util.List;
import java.util.Map;

public record ExportPage(
        List<Map<String, Object>> rows,
        boolean last,
        long totalElements
) {
}
