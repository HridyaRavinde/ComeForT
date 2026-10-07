package com.akshit.comefort.repository;

import com.akshit.comefort.db.DatabaseManager;
import com.akshit.comefort.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Data access layer for key-value configuration entries.
 */
public class ConfigRepository {

    private final DatabaseManager dbManager;

    public ConfigRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Gets a config value by key.
     */
    public Optional<String> get(String key) {
        String sql = "SELECT value FROM config WHERE key = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("value"));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to read config: " + e.getMessage(), e);
        }
    }

    /**
     * Sets a config value (insert or update).
     */
    public void set(String key, String value) {
        String sql = """
                INSERT INTO config (key, value) VALUES (?, ?)
                ON CONFLICT(key) DO UPDATE SET value = excluded.value
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save config: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a config entry.
     */
    public void delete(String key) {
        String sql = "DELETE FROM config WHERE key = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete config: " + e.getMessage(), e);
        }
    }

    private Connection getConnection() {
        return dbManager.getConnection();
    }
}
