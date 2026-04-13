package com.sssi.common.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for formatting timestamps across microservices.
 * Provides a centralized way to convert epoch milliseconds into
 * human-readable date formats using a default timezone and pattern.
 * Default configuration:
 * - Pattern: dd/MM/yyyy HH:mm
 * - Zone: America/Costa_Rica
 * This class is stateless and thread-safe.
 */
public final class DateUtils {

    /**
     * Default date-time pattern (example: 13/04/2026 13:05)
     */
    private static final String DEFAULT_PATTERN = "dd/MM/yyyy HH:mm";

    /**
     * Default timezone used across the system
     */
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Costa_Rica");

    /**
     * Private constructor to prevent instantiation.
     */
    private DateUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Formats a timestamp using the default pattern and timezone.
     *
     * @param timestamp epoch time in milliseconds
     * @return formatted date string
     */
    public static String formatTimestamp(long timestamp) {
        return format(timestamp, DEFAULT_PATTERN, DEFAULT_ZONE);
    }

    /**
     * Formats a timestamp using a custom pattern and default timezone.
     *
     * @param timestamp epoch time in milliseconds
     * @param pattern   date format pattern (e.g. dd/MM/yyyy HH:mm)
     * @return formatted date string
     */
    public static String formatTimestamp(long timestamp, String pattern) {
        return format(timestamp, pattern, DEFAULT_ZONE);
    }

    /**
     * Core formatter method used internally.
     *
     * @param timestamp epoch time in milliseconds
     * @param pattern   date format pattern
     * @param zone      timezone to apply
     * @return formatted date string
     */
    public static String format(long timestamp, String pattern, ZoneId zone) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(zone)
                .format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Formats a timestamp into a more readable format.
     * Example: 13 Apr 2026, 13:05
     *
     * @param timestamp epoch time in milliseconds
     * @return formatted readable date string
     */
    public static String formatReadable(long timestamp) {
        return format(timestamp, "dd MMM yyyy, HH:mm", DEFAULT_ZONE);
    }

    /**
     * Formats a timestamp into ISO-8601 format.
     * Example: 2026-04-13T19:05:00Z
     *
     * @param timestamp epoch time in milliseconds
     * @return ISO formatted string
     */
    public static String formatISO(long timestamp) {
        return Instant.ofEpochMilli(timestamp).toString();
    }
}