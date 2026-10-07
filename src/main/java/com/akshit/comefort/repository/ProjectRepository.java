package com.akshit.comefort.repository;

import com.akshit.comefort.core.Project;
import com.akshit.comefort.core.ProjectSummary;
import com.akshit.comefort.db.DatabaseManager;
import com.akshit.comefort.exception.DatabaseException;
import com.akshit.comefort.util.DateTimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data access layer for Project entities.
 */
public class ProjectRepository {

    private final DatabaseManager dbManager;

    public ProjectRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Inserts a new project.
     */
    public void save(Project project) {
        String sql = """
                INSERT INTO projects (id, name, description, path, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, project.getId());
            ps.setString(2, project.getName());
            ps.setString(3, project.getDescription());
            ps.setString(4, project.getPath());
            ps.setString(5, DateTimeUtil.formatForStorage(project.getCreatedAt()));
            ps.setString(6, DateTimeUtil.formatForStorage(project.getUpdatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save project: " + e.getMessage(), e);
        }
    }

    /**
     * Finds a project by its UUID.
     */
    public Optional<Project> findById(String id) {
        String sql = "SELECT * FROM projects WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find project: " + e.getMessage(), e);
        }
    }

    /**
     * Finds a project by its exact name (case-insensitive).
     */
    public Optional<Project> findByName(String name) {
        String sql = "SELECT * FROM projects WHERE LOWER(name) = LOWER(?)";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find project by name: " + e.getMessage(), e);
        }
    }

    /**
     * Finds projects whose name contains the given fragment (case-insensitive).
     */
    public List<Project> findByNameFragment(String fragment) {
        String sql = "SELECT * FROM projects WHERE LOWER(name) LIKE LOWER(?) ORDER BY name";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, "%" + fragment + "%");
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search projects: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all projects ordered by name.
     */
    public List<Project> findAll() {
        String sql = "SELECT * FROM projects ORDER BY name";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list projects: " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing project.
     */
    public void update(Project project) {
        String sql = """
                UPDATE projects SET name = ?, description = ?, path = ?, updated_at = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, project.getName());
            ps.setString(2, project.getDescription());
            ps.setString(3, project.getPath());
            ps.setString(4, DateTimeUtil.formatForStorage(project.getUpdatedAt()));
            ps.setString(5, project.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update project: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a project by ID.
     */
    public void delete(String id) {
        String sql = "DELETE FROM projects WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete project: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the total count of projects.
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM projects";

        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count projects: " + e.getMessage(), e);
        }
    }

    /**
     * Searches projects by name, description, or path.
     */
    public List<Project> searchByText(String query) {
        String sql = """
                SELECT * FROM projects
                WHERE LOWER(name) LIKE LOWER(?)
                   OR LOWER(description) LIKE LOWER(?)
                   OR LOWER(path) LIKE LOWER(?)
                ORDER BY name
                """;

        String pattern = "%" + query + "%";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search projects: " + e.getMessage(), e);
        }
    }

    /**
     * Returns project summaries with aggregated task and note counts in a single SQL query.
     * Prevents N+1 query problem.
     */
    public List<ProjectSummary> findProjectSummaries() {
        String sql = """
                SELECT 
                    p.id, p.name, p.description, p.path, p.created_at, p.updated_at,
                    COUNT(DISTINCT t.id) AS total_tasks,
                    COUNT(DISTINCT CASE WHEN t.status != 'DONE' AND t.status != 'ARCHIVED' THEN t.id END) AS open_tasks,
                    COUNT(DISTINCT n.id) AS total_notes
                FROM projects p
                LEFT JOIN tasks t ON p.id = t.project_id
                LEFT JOIN notes n ON p.id = n.project_id
                GROUP BY p.id
                ORDER BY p.name
                """;

        List<ProjectSummary> summaries = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Project project = mapRow(rs);
                int totalTasks = rs.getInt("total_tasks");
                int openTasks = rs.getInt("open_tasks");
                int totalNotes = rs.getInt("total_notes");
                summaries.add(new ProjectSummary(project, totalTasks, openTasks, totalNotes));
            }
            return summaries;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load project summaries: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a map of project ID to project name in a single query.
     */
    public Map<String, String> getProjectNameMap() {
        String sql = "SELECT id, name FROM projects";
        Map<String, String> map = new HashMap<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("id"), rs.getString("name"));
            }
            return map;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load project name map: " + e.getMessage(), e);
        }
    }

    // --- Internal helpers ---

    private Connection getConnection() {
        return dbManager.getConnection();
    }

    private List<Project> collectResults(PreparedStatement ps) throws SQLException {
        List<Project> results = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        }
        return results;
    }

    private Project mapRow(ResultSet rs) throws SQLException {
        return Project.builder(rs.getString("id"), rs.getString("name"))
                .description(rs.getString("description"))
                .path(rs.getString("path"))
                .createdAt(DateTimeUtil.parseDateTime(rs.getString("created_at")))
                .updatedAt(DateTimeUtil.parseDateTime(rs.getString("updated_at")))
                .build();
    }
}
