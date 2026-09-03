package com.proseg.msvcinventory.importer;

import java.text.Normalizer;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImportNameNormalizer {

    private static final Set<String> CAMPUS_PREFIXES = Set.of("campus", "sede", "recinto");
    private static final Set<String> BUILDING_PREFIXES = Set.of("edificio");
    private static final Set<String> FLOOR_PREFIXES = Set.of("planta", "piso", "nivel");

    private static final Pattern INTEGER = Pattern.compile("-?\\d+");

    private ImportNameNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().toLowerCase().replace('_', ' ');
        String withoutAccents = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.replaceAll("\\s+", " ").trim();
    }

    public static String campusCore(String value) {
        return stripLeadingPrefix(normalize(value), CAMPUS_PREFIXES);
    }

    public static String buildingCore(String value) {
        return stripLeadingPrefix(normalize(value), BUILDING_PREFIXES);
    }

    public static String floorCore(String value) {
        String normalized = normalize(value);
        Matcher matcher = INTEGER.matcher(normalized);
        if (matcher.find()) {
            return String.valueOf(Long.parseLong(matcher.group()));
        }
        return stripLeadingPrefix(normalized, FLOOR_PREFIXES);
    }

    private static String stripLeadingPrefix(String normalized, Set<String> prefixes) {
        int firstSpace = normalized.indexOf(' ');
        if (firstSpace < 0) {
            return normalized;
        }
        String firstToken = normalized.substring(0, firstSpace);
        if (prefixes.contains(firstToken)) {
            return normalized.substring(firstSpace + 1).trim();
        }
        return normalized;
    }
}
