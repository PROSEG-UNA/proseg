package com.sssi.msvc_document_processor.export.writer;

import com.sssi.msvc_document_processor.export.ExportColumnDefinition;

import java.util.List;
import java.util.Map;

public interface ExportWriter {

    byte[] write(List<ExportColumnDefinition> columns, List<Map<String, Object>> rows);
}
