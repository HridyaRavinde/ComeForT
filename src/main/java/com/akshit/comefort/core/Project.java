package com.akshit.comefort.core;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a project — a logical container for tasks, notes, and captures.
 * A project groups related work together (e.g., "HoneyChain", "JavaOS").
 */
public class Project {

    private final String id;
    private String name;
    private String description;
    private String path;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Project(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Project ID cannot be null");
        this.name = Objects.requireNonNull(builder.name, "Project name cannot be null");
        this.description = builder.description;
        this.path = builder.path;
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(builder.updatedAt, "updatedAt cannot be null");
    }

    public static Builder builder(String id, String name) {
        return new Builder(id, name);
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPath() {
        return path;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // --- Mutable setters (for updates) ---

    public void setName(String name) {
        this.name = Objects.requireNonNull(name);
        this.updatedAt = LocalDateTime.now();
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public void setPath(String path) {
        this.path = path;
        this.updatedAt = LocalDateTime.now();
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(id, project.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Project{id='" + id + "', name='" + name + "'}";
    }

    /**
     * Builder for constructing Project instances.
     */
    public static class Builder {
        private final String id;
        private final String name;
        private String description;
        private String path;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();

        private Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Project build() {
            return new Project(this);
        }
    }
}
