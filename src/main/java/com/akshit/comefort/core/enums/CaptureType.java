package com.akshit.comefort.core.enums;

/**
 * Represents the detected or assigned type of an inbox capture.
 * AUTO means the system hasn't classified it yet.
 */
public enum CaptureType {

    AUTO("⚡", "Auto"),
    TASK("☐", "Task"),
    NOTE("📝", "Note"),
    IDEA("💡", "Idea");

    private final String icon;
    private final String displayName;

    CaptureType(String icon, String displayName) {
        this.icon = icon;
        this.displayName = displayName;
    }

    public String getIcon() {
        return icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parses a capture type string case-insensitively.
     */
    public static CaptureType fromString(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        String normalized = value.trim().toUpperCase();
        for (CaptureType type : values()) {
            if (type.name().equals(normalized)
                    || type.displayName.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return AUTO;
    }
}
