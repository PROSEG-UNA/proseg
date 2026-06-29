package com.sssi.msvc_document_processor.excel;

import java.text.Normalizer;

public final class HeaderNormalizer {

    private HeaderNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) return "";
        String trimmed = value.trim().toLowerCase();
        String withoutAccents = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.replaceAll("\\s+", " ");
    }
}
