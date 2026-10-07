package com.akshit.comefort.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * Utility for date/time formatting, parsing, and human-readable relative time.
 * All storage uses ISO-8601 strings. This class handles the conversion.
 */
public final class DateTimeUtil {

    private static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter DISPLAY_DATETIME = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

    private DateTimeUtil() {
        // utility class
    }

    // --- Formatting for storage ---

    /**
     * Formats a LocalDateTime to ISO-8601 string for SQLite storage.
     */
    public static String formatForStorage(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(ISO_DATETIME);
    }

    /**
     * Formats a LocalDate to ISO-8601 string for SQLite storage.
     */
    public static String formatDateForStorage(LocalDate date) {
        if (date == null) return null;
        return date.format(ISO_DATE);
    }

    // --- Parsing from storage ---

    /**
     * Parses an ISO-8601 datetime string from SQLite.
     */
    public static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDateTime.parse(value, ISO_DATETIME);
    }

    /**
     * Parses an ISO-8601 date string from SQLite.
     */
    public static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value, ISO_DATE);
    }

    // --- Human-readable display ---

    /**
     * Formats a date for display: "Oct 7, 2026".
     */
    public static String formatForDisplay(LocalDate date) {
        if (date == null) return "—";
        return date.format(DISPLAY_DATE);
    }

    /**
     * Formats a datetime for display: "Oct 7, 2026 17:30".
     */
    public static String formatForDisplay(LocalDateTime dateTime) {
        if (dateTime == null) return "—";
        return dateTime.format(DISPLAY_DATETIME);
    }

    /**
     * Returns a human-readable relative time string.
     * Examples: "just now", "5 min ago", "2 hours ago", "3 days ago".
     */
    public static String relativeTime(LocalDateTime dateTime) {
        if (dateTime == null) return "—";

        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);

        if (minutes < 0) {
            // Future — shouldn't normally happen, but handle gracefully
            return formatForDisplay(dateTime);
        }
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " min ago";

        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");

        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days < 30) return days + (days == 1 ? " day ago" : " days ago");

        long months = days / 30;
        if (months < 12) return months + (months == 1 ? " month ago" : " months ago");

        return formatForDisplay(dateTime);
    }

    /**
     * Formats a due date with context: "Today", "Tomorrow", "Overdue (Oct 5)", etc.
     */
    public static String formatDueDate(LocalDate dueDate) {
        if (dueDate == null) return "No due date";

        LocalDate today = LocalDate.now();
        if (dueDate.equals(today)) return "Today";
        if (dueDate.equals(today.plusDays(1))) return "Tomorrow";
        if (dueDate.equals(today.minusDays(1))) return "Yesterday";
        if (dueDate.isBefore(today)) return "Overdue (" + formatForDisplay(dueDate) + ")";

        long daysUntil = ChronoUnit.DAYS.between(today, dueDate);
        if (daysUntil <= 7) return "In " + daysUntil + " days";

        return formatForDisplay(dueDate);
    }

    // --- Natural language parsing ---

    /**
     * Parses natural date expressions like "today", "tomorrow", "2026-10-15".
     *
     * @return the parsed LocalDate, or null if unparseable
     */
    public static LocalDate parseFriendlyDate(String input) {
        if (input == null || input.isBlank()) return null;

        String normalized = input.trim().toLowerCase();

        return switch (normalized) {
            case "today" -> LocalDate.now();
            case "tomorrow" -> LocalDate.now().plusDays(1);
            case "yesterday" -> LocalDate.now().minusDays(1);
            default -> {
                // Try ISO date format: yyyy-MM-dd
                try {
                    yield LocalDate.parse(normalized, ISO_DATE);
                } catch (DateTimeParseException e) {
                    // Try common formats
                    try {
                        yield LocalDate.parse(normalized,
                                DateTimeFormatter.ofPattern("d/M/yyyy"));
                    } catch (DateTimeParseException e2) {
                        yield null;
                    }
                }
            }
        };
    }

    /**
     * Returns the current timestamp formatted for storage.
     */
    public static String now() {
        return formatForStorage(LocalDateTime.now());
    }
}
