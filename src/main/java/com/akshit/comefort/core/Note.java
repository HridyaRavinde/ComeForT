package com.akshit.comefort.core;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a free-form note — can be standalone or belong to a project.
 */
public class Note {

    private final String id;
    private String projectId;
    private String title;
    private String content;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Note(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Note ID cannot be null");
        this.projectId = builder.projectId;
        this.title = Objects.requireNonNull(builder.title, "Note title cannot be null");
        this.content = Objects.requireNonNull(builder.content, "Note content cannot be null");
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(builder.updatedAt, "updatedAt cannot be null");
    }

    public static Builder builder(String id, String title) {
        return new Builder(id, title);
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Returns a short ID (first 8 chars) for display purposes.
     */
    public String getShortId() {
        return id.length() > 8 ? id.substring(0, 8) : id;
    }

    /**
     * Returns a preview of the content (first 80 chars).
     */
    public String getContentPreview() {
        if (content == null) return "";
        if (content.length() <= 80) return content;
        return content.substring(0, 77) + "...";
    }

    // --- Mutable setters ---

    public void setProjectId(String projectId) {
        this.projectId = projectId;
        this.updatedAt = LocalDateTime.now();
    }

    public void setTitle(String title) {
        this.title = Objects.requireNonNull(title);
        this.updatedAt = LocalDateTime.now();
    }

    public void setContent(String content) {
        this.content = Objects.requireNonNull(content);
        this.updatedAt = LocalDateTime.now();
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return Objects.equals(id, note.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Note{id='" + id + "', title='" + title + "'}";
    }

    /**
     * Builder for constructing Note instances.
     */
    public static class Builder {
        private final String id;
        private final String title;
        private String projectId;
        private String content = "";
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();

        private Builder(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
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

        public Note build() {
            return new Note(this);
        }
    }
}
