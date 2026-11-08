package com.proseg.msvc_document_processor.excel.reader;

import java.util.List;

public record TabularFile(SpreadsheetFormat format, List<SheetRow> rows) {
}
