package com.proseg.msvc_transport.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public final class ObservationKeywordMatcher {

    private ObservationKeywordMatcher() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    public static boolean matches(String observation, String keyword) {
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword.isEmpty()) {
            return false;
        }
        return normalize(observation).contains(normalizedKeyword);
    }

    public static List<String> matching(String observation, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }
        return keywords.stream().filter(keyword -> matches(observation, keyword)).toList();
    }
}
