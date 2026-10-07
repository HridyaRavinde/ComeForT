package com.akshit.comefort.repository;

import com.akshit.comefort.core.Capture;
import com.akshit.comefort.core.enums.CaptureType;
import com.akshit.comefort.db.DatabaseManager;
import com.akshit.comefort.exception.DatabaseException;
import com.akshit.comefort.util.DateTimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for Capture (inbox) entities.
 */
public class CaptureRepository {

    private final DatabaseManager dbManager;

    public CaptureRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(Capture capture) {
        String sql = """
                INSERT INTO captures (id, project_id, content, type, processed, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, capture.getId());
            ps.setString(2, capture.getProjectId());
            ps.setString(3, capture.getContent());
            ps.setString(4, capture.getType().name());
            ps.setInt(5, capture.isProcessed() ? 1 : 0);
            ps.setString(6, DateTimeUtil.formatForStorage(capture.getCreatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save capture: " + e.getMessage(), e);
        }
    }

    public Optional<Capture> findById(String id) {
        String sql = "SELECT * FROM captures WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find capture: " + e.getMessage(), e);
        }
    }

    public List<Capture> findByIdPrefix(String prefix) {
        String sql = "SELECT * FROM captures WHERE id LIKE ? ORDER BY created_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find capture by prefix: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all unprocessed captures (the inbox), most recent first.
     */
    public List<Capture> findUnprocessed() {
        String sql = "SELECT * FROM captures WHERE processed = 0 ORDER BY created_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list inbox: " + e.getMessage(), e);
        }
    }

    public List<Capture> findAll() {
        String sql = "SELECT * FROM captures ORDER BY created_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list captures: " + e.getMessage(), e);
        }
    }

    public void update(Capture capture) {
        String sql = """
                UPDATE captures SET project_id = ?, type = ?, processed = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, capture.getProjectId());
            ps.setString(2, capture.getType().name());
            ps.setInt(3, capture.isProcessed() ? 1 : 0);
            ps.setString(4, capture.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update capture: " + e.getMessage(), e);
        }
    }

    public void delete(String id) {
        String sql = "DELETE FROM captures WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete capture: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the count of unprocessed inbox items.
     */
    public int countUnprocessed() {
        String sql = "SELECT COUNT(*) FROM captures WHERE processed = 0";

        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count inbox: " + e.getMessage(), e);
        }
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM captures";

        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count captures: " + e.getMessage(), e);
        }
    }

    public List<Capture> searchByText(String query) {
        String sql = """
                SELECT * FROM captures
                WHERE LOWER(content) LIKE LOWER(?)
                ORDER BY created_at DESC
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, "%" + query + "%");
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search captures: " + e.getMessage(), e);
        }
    }

    // --- Internal helpers ---

    private Connection getConnection() {
        return dbManager.getConnection();
    }

    private List<Capture> collectResults(PreparedStatement ps) throws SQLException {
        List<Capture> results = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        }
        return results;
    }

    private Capture mapRow(ResultSet rs) throws SQLException {
        return Capture.builder(rs.getString("id"), rs.getString("content"))
                .projectId(rs.getString("project_id"))
                .type(CaptureType.fromString(rs.getString("type")))
                .processed(rs.getInt("processed") == 1)
                .createdAt(DateTimeUtil.parseDateTime(rs.getString("created_at")))
                .build();
    }
}
