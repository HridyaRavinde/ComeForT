package com.akshit.comefort.exception;

/**
 * Thrown when attempting to create an entity that already exists,
 * typically a project with a duplicate name.
 */
public class DuplicateEntityException extends ComeFortException {

    private final String entityType;
    private final String identifier;

    public DuplicateEntityException(String entityType, String identifier) {
        super(entityType + " already exists: " + identifier);
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
