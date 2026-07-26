package com.sssi.msvc_document_processor.export;

import java.util.Map;
import java.util.function.Function;

public record ExportColumnDefinition(
        String key,
        String header,
        Function<Map<String, Object>, Object> valueExtractor
) {
}
