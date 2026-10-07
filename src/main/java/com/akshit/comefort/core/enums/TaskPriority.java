package com.akshit.comefort.core.enums;

/**
 * Represents the priority level of a task.
 * Ordered from lowest to highest for natural sorting.
 */
public enum TaskPriority {

    LOW("⬜", "Low", 0),
    MEDIUM("🟨", "Medium", 1),
    HIGH("🟧", "High", 2),
    CRITICAL("🟥", "Critical", 3);

    private final String icon;
    private final String displayName;
    private final int weight;

    TaskPriority(String icon, String displayName, int weight) {
        this.icon = icon;
        this.displayName = displayName;
        this.weight = weight;
    }

    public String getIcon() {
        return icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getWeight() {
        return weight;
    }

    /**
     * Parses a priority string case-insensitively.
     */
    public static TaskPriority fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Task priority cannot be empty");
        }
        String normalized = value.trim().toUpperCase();
        for (TaskPriority priority : values()) {
            if (priority.name().equals(normalized)
                    || priority.displayName.equalsIgnoreCase(value.trim())) {
                return priority;
            }
        }
        throw new IllegalArgumentException("Unknown task priority: " + value
                + ". Valid values: LOW, MEDIUM, HIGH, CRITICAL");
    }
}
