package com.akshit.comefort.util;

import java.util.UUID;

/**
 * Centralized UUID generation for all entity IDs.
 * Using a dedicated class allows future customization
 * (e.g., shorter IDs, sequential IDs).
 */
public final class IdGenerator {

    private IdGenerator() {
        // utility class — no instantiation
    }

    /**
     * Generates a new UUID string.
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the first 8 characters of an ID for display.
     */
    public static String shorten(String id) {
        if (id == null) return "";
        return id.length() > 8 ? id.substring(0, 8) : id;
    }
}
