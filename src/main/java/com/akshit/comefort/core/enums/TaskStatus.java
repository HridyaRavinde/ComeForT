package com.akshit.comefort.core.enums;

/**
 * Represents the lifecycle status of a task.
 */
public enum TaskStatus {

    OPEN("○", "Open"),
    IN_PROGRESS("◐", "In Progress"),
    DONE("●", "Done"),
    ARCHIVED("◌", "Archived");

    private final String icon;
    private final String displayName;

    TaskStatus(String icon, String displayName) {
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
     * Parses a status string case-insensitively.
     * Accepts both enum names ("OPEN") and display names ("In Progress").
     */
    public static TaskStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Task status cannot be empty");
        }
        String normalized = value.trim().toUpperCase().replace(" ", "_");
        for (TaskStatus status : values()) {
            if (status.name().equals(normalized)
                    || status.displayName.equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown task status: " + value
                + ". Valid values: OPEN, IN_PROGRESS, DONE, ARCHIVED");
    }
}
