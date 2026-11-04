package com.proseg.msvc_document_processor.excel.reader;

import java.util.List;

public record SheetRow(int rowNumber, List<SheetCell> cells) {

    public SheetCell cell(int index) {
        return index >= 0 && index < cells.size() ? cells.get(index) : SheetCell.empty();
    }

    public boolean isBlank() {
        return cells.stream().allMatch(SheetCell::isBlank);
    }
}
