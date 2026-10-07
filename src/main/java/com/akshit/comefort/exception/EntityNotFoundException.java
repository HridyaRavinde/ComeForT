package com.akshit.comefort.exception;

/**
 * Thrown when a requested entity (project, task, note, etc.) cannot be found
 * by its ID or name fragment.
 */
public class EntityNotFoundException extends ComeFortException {

    private final String entityType;
    private final String identifier;

    public EntityNotFoundException(String entityType, String identifier) {
        super(entityType + " not found: " + identifier);
        this.entityType = entityType;
        this.identifier = identifier;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getIdentifier() {
        return identifier;
    }
}
