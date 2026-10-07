package com.akshit.comefort.repository;

import com.akshit.comefort.core.Task;
import com.akshit.comefort.core.enums.TaskPriority;
import com.akshit.comefort.core.enums.TaskStatus;
import com.akshit.comefort.db.DatabaseManager;
import com.akshit.comefort.exception.DatabaseException;
import com.akshit.comefort.util.DateTimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for Task entities.
 */
public class TaskRepository {

    private final DatabaseManager dbManager;

    public TaskRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Inserts a new task.
     */
    public void save(Task task) {
        String sql = """
                INSERT INTO tasks (id, project_id, title, description, status, priority,
                                   due_date, created_at, updated_at, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, task.getId());
            ps.setString(2, task.getProjectId());
            ps.setString(3, task.getTitle());
            ps.setString(4, task.getDescription());
            ps.setString(5, task.getStatus().name());
            ps.setString(6, task.getPriority().name());
            ps.setString(7, DateTimeUtil.formatDateForStorage(task.getDueDate()));
            ps.setString(8, DateTimeUtil.formatForStorage(task.getCreatedAt()));
            ps.setString(9, DateTimeUtil.formatForStorage(task.getUpdatedAt()));
            ps.setString(10, DateTimeUtil.formatForStorage(task.getCompletedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save task: " + e.getMessage(), e);
        }
    }

    /**
     * Finds a task by its UUID.
     */
    public Optional<Task> findById(String id) {
        String sql = "SELECT * FROM tasks WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find task: " + e.getMessage(), e);
        }
    }

    /**
     * Finds tasks whose ID starts with the given prefix.
     * Useful for short-ID matching (e.g., "a3f" matches "a3f12b45-...").
     */
    public List<Task> findByIdPrefix(String prefix) {
        String sql = "SELECT * FROM tasks WHERE id LIKE ? ORDER BY created_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find task by prefix: " + e.getMessage(), e);
        }
    }

    /**
     * Finds tasks whose title contains the given fragment (case-insensitive).
     */
    public List<Task> findByTitleFragment(String fragment) {
        String sql = "SELECT * FROM tasks WHERE LOWER(title) LIKE LOWER(?) ORDER BY created_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, "%" + fragment + "%");
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search tasks by title: " + e.getMessage(), e);
        }
    }

    /**
     * Finds all tasks for a given project.
     */
    public List<Task> findByProjectId(String projectId) {
        String sql = "SELECT * FROM tasks WHERE project_id = ? ORDER BY priority DESC, created_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, projectId);
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find tasks for project: " + e.getMessage(), e);
        }
    }

    /**
     * Finds all tasks with the given status.
     */
    public List<Task> findByStatus(TaskStatus status) {
        String sql = "SELECT * FROM tasks WHERE status = ? ORDER BY priority DESC, created_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find tasks by status: " + e.getMessage(), e);
        }
    }

    /**
     * Finds all tasks due on or before the given date that are still open.
     */
    public List<Task> findDueOnOrBefore(LocalDate date) {
        String sql = """
                SELECT * FROM tasks
                WHERE due_date <= ? AND status NOT IN ('DONE', 'ARCHIVED')
                ORDER BY due_date ASC, priority DESC
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, DateTimeUtil.formatDateForStorage(date));
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find due tasks: " + e.getMessage(), e);
        }
    }

    /**
     * Finds high-priority open tasks (HIGH or CRITICAL).
     */
    public List<Task> findHighPriorityOpen() {
        String sql = """
                SELECT * FROM tasks
                WHERE priority IN ('HIGH', 'CRITICAL') AND status NOT IN ('DONE', 'ARCHIVED')
                ORDER BY priority DESC, due_date ASC
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find high-priority tasks: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all tasks, optionally filtered.
     */
    public List<Task> findAll() {
        String sql = "SELECT * FROM tasks ORDER BY status ASC, priority DESC, created_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list tasks: " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing task.
     */
    public void update(Task task) {
        String sql = """
                UPDATE tasks SET project_id = ?, title = ?, description = ?,
                    status = ?, priority = ?, due_date = ?,
                    updated_at = ?, completed_at = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, task.getProjectId());
            ps.setString(2, task.getTitle());
            ps.setString(3, task.getDescription());
            ps.setString(4, task.getStatus().name());
            ps.setString(5, task.getPriority().name());
            ps.setString(6, DateTimeUtil.formatDateForStorage(task.getDueDate()));
            ps.setString(7, DateTimeUtil.formatForStorage(task.getUpdatedAt()));
            ps.setString(8, DateTimeUtil.formatForStorage(task.getCompletedAt()));
            ps.setString(9, task.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update task: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a task by ID.
     */
    public void delete(String id) {
        String sql = "DELETE FROM tasks WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete task: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the count of tasks grouped by status.
     */
    public int countByStatus(TaskStatus status) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE status = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql);) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count tasks: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the total count of tasks.
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM tasks";

        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count tasks: " + e.getMessage(), e);
        }
    }

    /**
     * Searches tasks by title or description text.
     */
    public List<Task> searchByText(String query) {
        String sql = """
                SELECT * FROM tasks
                WHERE LOWER(title) LIKE LOWER(?) OR LOWER(description) LIKE LOWER(?)
                ORDER BY status ASC, priority DESC
                """;

        String pattern = "%" + query + "%";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search tasks: " + e.getMessage(), e);
        }
    }

    // --- Internal helpers ---

    private Connection getConnection() {
        return dbManager.getConnection();
    }

    private List<Task> collectResults(PreparedStatement ps) throws SQLException {
        List<Task> results = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        }
        return results;
    }

    private Task mapRow(ResultSet rs) throws SQLException {
        return Task.builder(rs.getString("id"), rs.getString("title"))
                .projectId(rs.getString("project_id"))
                .description(rs.getString("description"))
                .status(TaskStatus.fromString(rs.getString("status")))
                .priority(TaskPriority.fromString(rs.getString("priority")))
                .dueDate(DateTimeUtil.parseDate(rs.getString("due_date")))
                .createdAt(DateTimeUtil.parseDateTime(rs.getString("created_at")))
                .updatedAt(DateTimeUtil.parseDateTime(rs.getString("updated_at")))
                .completedAt(DateTimeUtil.parseDateTime(rs.getString("completed_at")))
                .build();
    }
}
