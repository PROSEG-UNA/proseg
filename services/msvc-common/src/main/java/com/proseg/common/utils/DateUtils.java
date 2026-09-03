package com.proseg.common.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private static final String DEFAULT_PATTERN = "dd/MM/yyyy HH:mm";

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Costa_Rica");

    private DateUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String formatTimestamp(long timestamp) {
        return format(timestamp, DEFAULT_PATTERN, DEFAULT_ZONE);
    }

    public static String formatTimestamp(long timestamp, String pattern) {
        return format(timestamp, pattern, DEFAULT_ZONE);
    }

    public static String format(long timestamp, String pattern, ZoneId zone) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(zone)
                .format(DateTimeFormatter.ofPattern(pattern));
    }

    public static String formatReadable(long timestamp) {
        return format(timestamp, "dd MMM yyyy, HH:mm", DEFAULT_ZONE);
    }

    public static String formatISO(long timestamp) {
        return Instant.ofEpochMilli(timestamp).toString();
    }
}