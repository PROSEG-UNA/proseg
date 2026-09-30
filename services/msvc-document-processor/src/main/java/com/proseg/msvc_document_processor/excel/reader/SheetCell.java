package com.proseg.msvc_document_processor.excel.reader;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

public record SheetCell(String text, LocalDateTime dateValue, Double numericValue) {

    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("\\p{Cntrl}");
    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");
    private static final SheetCell EMPTY = new SheetCell(null, null, null);

    public static SheetCell empty() {
        return EMPTY;
    }

    public static SheetCell ofText(String raw) {
        return new SheetCell(sanitize(raw), null, null);
    }

    public static SheetCell ofNumber(String raw, Double numericValue, LocalDateTime dateValue) {
        return new SheetCell(sanitize(raw), dateValue, numericValue);
    }

    public boolean isBlank() {
        return text == null && dateValue == null && numericValue == null;
    }

    static String sanitize(String raw) {
        if (raw == null) {
            return null;
        }
        String withoutControls = CONTROL_CHARACTERS.matcher(raw.replace(' ', ' ')).replaceAll(" ");
        String collapsed = WHITESPACE_RUN.matcher(withoutControls).replaceAll(" ").trim();
        return collapsed.isEmpty() ? null : collapsed;
    }
}
