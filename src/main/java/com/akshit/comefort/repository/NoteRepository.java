package com.akshit.comefort.repository;

import com.akshit.comefort.core.Note;
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
 * Data access layer for Note entities.
 */
public class NoteRepository {

    private final DatabaseManager dbManager;

    public NoteRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(Note note) {
        String sql = """
                INSERT INTO notes (id, project_id, title, content, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, note.getId());
            ps.setString(2, note.getProjectId());
            ps.setString(3, note.getTitle());
            ps.setString(4, note.getContent());
            ps.setString(5, DateTimeUtil.formatForStorage(note.getCreatedAt()));
            ps.setString(6, DateTimeUtil.formatForStorage(note.getUpdatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save note: " + e.getMessage(), e);
        }
    }

    public Optional<Note> findById(String id) {
        String sql = "SELECT * FROM notes WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find note: " + e.getMessage(), e);
        }
    }

    public List<Note> findByIdPrefix(String prefix) {
        String sql = "SELECT * FROM notes WHERE id LIKE ? ORDER BY updated_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find note by prefix: " + e.getMessage(), e);
        }
    }

    public List<Note> findByTitleFragment(String fragment) {
        String sql = "SELECT * FROM notes WHERE LOWER(title) LIKE LOWER(?) ORDER BY updated_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, "%" + fragment + "%");
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search notes: " + e.getMessage(), e);
        }
    }

    public List<Note> findByProjectId(String projectId) {
        String sql = "SELECT * FROM notes WHERE project_id = ? ORDER BY updated_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, projectId);
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find notes for project: " + e.getMessage(), e);
        }
    }

    public List<Note> findAll() {
        String sql = "SELECT * FROM notes ORDER BY updated_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list notes: " + e.getMessage(), e);
        }
    }

    public void update(Note note) {
        String sql = """
                UPDATE notes SET project_id = ?, title = ?, content = ?, updated_at = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, note.getProjectId());
            ps.setString(2, note.getTitle());
            ps.setString(3, note.getContent());
            ps.setString(4, DateTimeUtil.formatForStorage(note.getUpdatedAt()));
            ps.setString(5, note.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update note: " + e.getMessage(), e);
        }
    }

    public void delete(String id) {
        String sql = "DELETE FROM notes WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete note: " + e.getMessage(), e);
        }
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM notes";

        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count notes: " + e.getMessage(), e);
        }
    }

    public List<Note> searchByText(String query) {
        String sql = """
                SELECT * FROM notes
                WHERE LOWER(title) LIKE LOWER(?) OR LOWER(content) LIKE LOWER(?)
                ORDER BY updated_at DESC
                """;

        String pattern = "%" + query + "%";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search notes: " + e.getMessage(), e);
        }
    }

    // --- Internal helpers ---

    private Connection getConnection() {
        return dbManager.getConnection();
    }

    private List<Note> collectResults(PreparedStatement ps) throws SQLException {
        List<Note> results = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        }
        return results;
    }

    private Note mapRow(ResultSet rs) throws SQLException {
        return Note.builder(rs.getString("id"), rs.getString("title"))
                .projectId(rs.getString("project_id"))
                .content(rs.getString("content"))
                .createdAt(DateTimeUtil.parseDateTime(rs.getString("created_at")))
                .updatedAt(DateTimeUtil.parseDateTime(rs.getString("updated_at")))
                .build();
    }
}
