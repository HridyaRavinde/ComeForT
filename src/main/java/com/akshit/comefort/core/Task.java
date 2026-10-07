package com.akshit.comefort.core;

import com.akshit.comefort.core.enums.TaskPriority;
import com.akshit.comefort.core.enums.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a task — the primary unit of work in ComeFort.
 * Tasks can be standalone or belong to a project.
 */
public class Task {

    private final String id;
    private String projectId;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    private Task(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Task ID cannot be null");
        this.projectId = builder.projectId;
        this.title = Objects.requireNonNull(builder.title, "Task title cannot be null");
        this.description = builder.description;
        this.status = builder.status;
        this.priority = builder.priority;
        this.dueDate = builder.dueDate;
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(builder.updatedAt, "updatedAt cannot be null");
        this.completedAt = builder.completedAt;
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

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    /**
     * Returns a short ID (first 8 chars) for display purposes.
     */
    public String getShortId() {
        return id.length() > 8 ? id.substring(0, 8) : id;
    }

    public boolean isOverdue() {
        return dueDate != null
                && dueDate.isBefore(LocalDate.now())
                && status != TaskStatus.DONE
                && status != TaskStatus.ARCHIVED;
    }

    public boolean isDueToday() {
        return dueDate != null && dueDate.equals(LocalDate.now());
    }

    public boolean isOpen() {
        return status == TaskStatus.OPEN || status == TaskStatus.IN_PROGRESS;
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

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public void setStatus(TaskStatus status) {
        this.status = Objects.requireNonNull(status);
        this.updatedAt = LocalDateTime.now();
        if (status == TaskStatus.DONE) {
            this.completedAt = LocalDateTime.now();
        } else {
            this.completedAt = null;
        }
    }

    public void setPriority(TaskPriority priority) {
        this.priority = Objects.requireNonNull(priority);
        this.updatedAt = LocalDateTime.now();
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
        this.updatedAt = LocalDateTime.now();
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Task{id='" + id + "', title='" + title
                + "', status=" + status + ", priority=" + priority + '}';
    }

    /**
     * Builder for constructing Task instances.
     */
    public static class Builder {
        private final String id;
        private final String title;
        private String projectId;
        private String description;
        private TaskStatus status = TaskStatus.OPEN;
        private TaskPriority priority = TaskPriority.MEDIUM;
        private LocalDate dueDate;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();
        private LocalDateTime completedAt;

        private Builder(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder status(TaskStatus status) {
            this.status = status;
            return this;
        }

        public Builder priority(TaskPriority priority) {
            this.priority = priority;
            return this;
        }

        public Builder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
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

        public Builder completedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Task build() {
            return new Task(this);
        }
    }
}
