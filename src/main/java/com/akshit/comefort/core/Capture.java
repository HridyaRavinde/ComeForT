package com.akshit.comefort.core;

import com.akshit.comefort.core.enums.CaptureType;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents an inbox capture — a raw, unprocessed thought dumped into ComeFort.
 * The heart of the "capture first, organize later" philosophy.
 */
public class Capture {

    private final String id;
    private String projectId;
    private final String content;
    private CaptureType type;
    private boolean processed;
    private final LocalDateTime createdAt;

    private Capture(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Capture ID cannot be null");
        this.projectId = builder.projectId;
        this.content = Objects.requireNonNull(builder.content, "Capture content cannot be null");
        this.type = builder.type;
        this.processed = builder.processed;
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt cannot be null");
    }

    public static Builder builder(String id, String content) {
        return new Builder(id, content);
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getContent() {
        return content;
    }

    public CaptureType getType() {
        return type;
    }

    public boolean isProcessed() {
        return processed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns a short ID (first 8 chars) for display purposes.
     */
    public String getShortId() {
        return id.length() > 8 ? id.substring(0, 8) : id;
    }

    /**
     * Returns a preview of the content (first 60 chars).
     */
    public String getContentPreview() {
        if (content.length() <= 60) return content;
        return content.substring(0, 57) + "...";
    }

    // --- Mutable setters ---

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public void setType(CaptureType type) {
        this.type = Objects.requireNonNull(type);
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Capture capture = (Capture) o;
        return Objects.equals(id, capture.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Capture{id='" + id + "', content='" + getContentPreview() + "'}";
    }

    /**
     * Builder for constructing Capture instances.
     */
    public static class Builder {
        private final String id;
        private final String content;
        private String projectId;
        private CaptureType type = CaptureType.AUTO;
        private boolean processed = false;
        private LocalDateTime createdAt = LocalDateTime.now();

        private Builder(String id, String content) {
            this.id = id;
            this.content = content;
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder type(CaptureType type) {
            this.type = type;
            return this;
        }

        public Builder processed(boolean processed) {
            this.processed = processed;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Capture build() {
            return new Capture(this);
        }
    }
}
