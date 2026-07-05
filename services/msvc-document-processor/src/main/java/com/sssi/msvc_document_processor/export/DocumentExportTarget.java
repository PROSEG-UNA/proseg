package com.sssi.msvc_document_processor.export;

import org.springframework.util.MultiValueMap;

import java.util.List;

public interface DocumentExportTarget {

    String documentType();

    List<ExportColumnDefinition> columns();

    ExportPage fetchPage(MultiValueMap<String, String> queryParams);
}
