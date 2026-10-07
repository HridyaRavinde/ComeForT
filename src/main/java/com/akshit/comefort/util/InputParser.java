package com.akshit.comefort.util;

import com.akshit.comefort.core.enums.CaptureType;

/**
 * Parses raw user input to detect intent and extract structured data.
 * Supports prefix-based detection for quick captures.
 *
 * Examples:
 *   "task: finish Java notes"       → type=TASK, content="finish Java notes"
 *   "idea: AI GitHub assistant"     → type=IDEA, content="AI GitHub assistant"
 *   "note: Sepolia architecture"    → type=NOTE, content="Sepolia architecture"
 *   "just some random thought"      → type=AUTO, content="just some random thought"
 */
public final class InputParser {

    private InputParser() {
        // utility class
    }

    /**
     * Result of parsing a raw input string.
     */
    public record ParsedInput(CaptureType type, String content) {
    }

    /**
     * Parses a raw input string, detecting type prefixes.
     * Recognized prefixes: "task:", "note:", "idea:"
     * Anything else is classified as AUTO.
     */
    public static ParsedInput parse(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return new ParsedInput(CaptureType.AUTO, "");
        }

        String trimmed = rawInput.trim();
        String lower = trimmed.toLowerCase();

        // Check for type prefixes
        if (lower.startsWith("task:")) {
            return new ParsedInput(CaptureType.TASK, extractAfterPrefix(trimmed, 5));
        }
        if (lower.startsWith("note:")) {
            return new ParsedInput(CaptureType.NOTE, extractAfterPrefix(trimmed, 5));
        }
        if (lower.startsWith("idea:")) {
            return new ParsedInput(CaptureType.IDEA, extractAfterPrefix(trimmed, 5));
        }

        return new ParsedInput(CaptureType.AUTO, trimmed);
    }

    /**
     * Extracts and trims the content after a prefix of the given length.
     */
    private static String extractAfterPrefix(String input, int prefixLength) {
        if (input.length() <= prefixLength) return "";
        return input.substring(prefixLength).trim();
    }

    /**
     * Checks if the input looks like it contains a due date hint.
     * Simple heuristic for V0.1 — looks for keywords.
     */
    public static boolean hasDueDateHint(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return lower.contains("today") || lower.contains("tomorrow")
                || lower.contains("by ") || lower.contains("due ");
    }
}
