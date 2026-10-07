package com.akshit.comefort.gui;

/**
 * Theme definitions for ComeFort's Notion-inspired UI.
 * Supports Dark and Light modes with CSS variable overrides.
 */
public final class Theme {

    private Theme() {
    }

    public enum Mode {
        DARK, LIGHT
    }

    /**
     * Returns the CSS stylesheet path for the given mode.
     */
    public static String getStylesheet(Mode mode) {
        return switch (mode) {
            case DARK -> "/styles/dark-theme.css";
            case LIGHT -> "/styles/light-theme.css";
        };
    }

    /**
     * Returns the base (common) stylesheet path.
     */
    public static String getBaseStylesheet() {
        return "/styles/base.css";
    }
}
