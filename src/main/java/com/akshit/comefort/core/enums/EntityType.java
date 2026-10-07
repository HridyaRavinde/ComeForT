package com.akshit.comefort.core.enums;

/**
 * Represents the type of entity being tracked in the activity log.
 */
public enum EntityType {

    PROJECT,
    TASK,
    NOTE,
    CAPTURE;

    /**
     * Parses an entity type string case-insensitively.
     */
    public static EntityType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Entity type cannot be empty");
        }
        return valueOf(value.trim().toUpperCase());
    }
}
