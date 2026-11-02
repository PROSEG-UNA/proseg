package com.proseg.msvc_document_processor.excel;

import java.util.List;
import java.util.Map;

public record TourSheetBlock(String title, List<Map<String, Object>> rows) {
}
