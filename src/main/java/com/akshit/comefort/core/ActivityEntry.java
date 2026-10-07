package com.akshit.comefort.core;

import com.akshit.comefort.core.enums.ActionType;
import com.akshit.comefort.core.enums.EntityType;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a single entry in the activity log.
 * Tracks what happened, to which entity, and when — enabling
 * the "what was I doing?" feature.
 */
public class ActivityEntry {

    private final String id;
    private final EntityType entityType;
    private final String entityId;
    private final ActionType action;
    private final String summary;
    private final LocalDateTime createdAt;

    /**
     * Constructs a fully immutable activity entry.
     */
    public ActivityEntry(String id, EntityType entityType, String entityId,
                         ActionType action, String summary, LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "ActivityEntry ID cannot be null");
        this.entityType = Objects.requireNonNull(entityType, "entityType cannot be null");
        this.entityId = Objects.requireNonNull(entityId, "entityId cannot be null");
        this.action = Objects.requireNonNull(action, "action cannot be null");
        this.summary = summary;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public ActionType getAction() {
        return action;
    }

    public String getSummary() {
        return summary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivityEntry that = (ActivityEntry) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ActivityEntry{" + action.getDisplayName() + " " + entityType
                + " at " + createdAt + '}';
    }
}
