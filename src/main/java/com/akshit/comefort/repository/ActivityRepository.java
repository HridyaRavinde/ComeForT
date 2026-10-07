package com.akshit.comefort.repository;

import com.akshit.comefort.core.ActivityEntry;
import com.akshit.comefort.core.enums.ActionType;
import com.akshit.comefort.core.enums.EntityType;
import com.akshit.comefort.db.DatabaseManager;
import com.akshit.comefort.exception.DatabaseException;
import com.akshit.comefort.util.DateTimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access layer for activity log entries.
 */
public class ActivityRepository {

    private final DatabaseManager dbManager;

    public ActivityRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(ActivityEntry entry) {
        String sql = """
                INSERT INTO activity_log (id, entity_type, entity_id, action, summary, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, entry.getId());
            ps.setString(2, entry.getEntityType().name());
            ps.setString(3, entry.getEntityId());
            ps.setString(4, entry.getAction().name());
            ps.setString(5, entry.getSummary());
            ps.setString(6, DateTimeUtil.formatForStorage(entry.getCreatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save activity: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the most recent activity entries, limited to the given count.
     */
    public List<ActivityEntry> findRecent(int limit) {
        String sql = "SELECT * FROM activity_log ORDER BY created_at DESC LIMIT ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find recent activity: " + e.getMessage(), e);
        }
    }

    /**
     * Returns activity entries for a specific entity.
     */
    public List<ActivityEntry> findByEntity(EntityType entityType, String entityId) {
        String sql = """
                SELECT * FROM activity_log
                WHERE entity_type = ? AND entity_id = ?
                ORDER BY created_at DESC
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, entityType.name());
            ps.setString(2, entityId);
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find entity activity: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all activity for entities of a given type within a project.
     * Uses a subquery to find entity IDs belonging to the project.
     */
    public List<ActivityEntry> findByProjectActivity(String projectId, int limit) {
        // Activity for the project itself + tasks/notes/captures in that project
        String sql = """
                SELECT * FROM activity_log
                WHERE (entity_type = 'PROJECT' AND entity_id = ?)
                   OR (entity_type = 'TASK' AND entity_id IN
                       (SELECT id FROM tasks WHERE project_id = ?))
                   OR (entity_type = 'NOTE' AND entity_id IN
                       (SELECT id FROM notes WHERE project_id = ?))
                   OR (entity_type = 'CAPTURE' AND entity_id IN
                       (SELECT id FROM captures WHERE project_id = ?))
                ORDER BY created_at DESC
                LIMIT ?
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, projectId);
            ps.setString(2, projectId);
            ps.setString(3, projectId);
            ps.setString(4, projectId);
            ps.setInt(5, limit);
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find project activity: " + e.getMessage(), e);
        }
    }

    // --- Internal helpers ---

    private Connection getConnection() {
        return dbManager.getConnection();
    }

    private List<ActivityEntry> collectResults(PreparedStatement ps) throws SQLException {
        List<ActivityEntry> results = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        }
        return results;
    }

    private ActivityEntry mapRow(ResultSet rs) throws SQLException {
        return new ActivityEntry(
                rs.getString("id"),
                EntityType.fromString(rs.getString("entity_type")),
                rs.getString("entity_id"),
                ActionType.fromString(rs.getString("action")),
                rs.getString("summary"),
                DateTimeUtil.parseDateTime(rs.getString("created_at"))
        );
    }
}
