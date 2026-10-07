package com.akshit.comefort.core.enums;

/**
 * Represents the type of action performed on an entity,
 * tracked in the activity log.
 */
public enum ActionType {

    CREATED("Created"),
    UPDATED("Updated"),
    COMPLETED("Completed"),
    DELETED("Deleted");

    private final String displayName;

    ActionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parses an action type string case-insensitively.
     */
    public static ActionType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Action type cannot be empty");
        }
        return valueOf(value.trim().toUpperCase());
    }
}
