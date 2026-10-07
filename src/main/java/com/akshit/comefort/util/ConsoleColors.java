package com.akshit.comefort.util;

/**
 * ANSI escape codes for colored terminal output.
 * Falls back gracefully on terminals that don't support colors.
 */
public final class ConsoleColors {

    private ConsoleColors() {
        // utility class
    }

    // --- Reset ---
    public static final String RESET = "\u001B[0m";

    // --- Regular Colors ---
    public static final String BLACK  = "\u001B[30m";
    public static final String RED    = "\u001B[31m";
    public static final String GREEN  = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE   = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN   = "\u001B[36m";
    public static final String WHITE  = "\u001B[37m";

    // --- Bold ---
    public static final String BOLD       = "\u001B[1m";
    public static final String BOLD_RED   = "\u001B[1;31m";
    public static final String BOLD_GREEN = "\u001B[1;32m";
    public static final String BOLD_YELLOW = "\u001B[1;33m";
    public static final String BOLD_BLUE  = "\u001B[1;34m";
    public static final String BOLD_CYAN  = "\u001B[1;36m";
    public static final String BOLD_WHITE = "\u001B[1;37m";

    // --- Dim ---
    public static final String DIM = "\u001B[2m";

    // --- Background ---
    public static final String BG_RED    = "\u001B[41m";
    public static final String BG_GREEN  = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";

    // --- Convenience methods ---

    /**
     * Wraps text in the given color, appending RESET automatically.
     */
    public static String colorize(String text, String color) {
        return color + text + RESET;
    }

    public static String bold(String text) {
        return BOLD + text + RESET;
    }

    public static String dim(String text) {
        return DIM + text + RESET;
    }

    public static String success(String text) {
        return GREEN + text + RESET;
    }

    public static String error(String text) {
        return RED + text + RESET;
    }

    public static String warning(String text) {
        return YELLOW + text + RESET;
    }

    public static String info(String text) {
        return CYAN + text + RESET;
    }

    public static String highlight(String text) {
        return BOLD_CYAN + text + RESET;
    }
}
