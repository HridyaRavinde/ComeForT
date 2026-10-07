package com.akshit.comefort.db;

import com.akshit.comefort.exception.DatabaseException;
import com.akshit.comefort.util.DateTimeUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight, zero-dependency schema migration runner.
 * Tracks schema version in `schema_migrations` table and applies
 * incremental migrations sequentially within database transactions.
 */
public final class MigrationRunner {

    public record Migration(int version, String name, String sql) {}

    private static final List<Migration> MIGRATIONS = new ArrayList<>();

    static {
        MIGRATIONS.add(new Migration(1, "001_initial_schema", """
                CREATE TABLE IF NOT EXISTS projects (
                    id          TEXT PRIMARY KEY,
                    name        TEXT NOT NULL UNIQUE,
                    description TEXT,
                    path        TEXT,
                    created_at  TEXT NOT NULL,
                    updated_at  TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS tasks (
                    id           TEXT PRIMARY KEY,
                    project_id   TEXT REFERENCES projects(id) ON DELETE SET NULL,
                    title        TEXT NOT NULL,
                    description  TEXT,
                    status       TEXT NOT NULL DEFAULT 'OPEN',
                    priority     TEXT NOT NULL DEFAULT 'MEDIUM',
                    due_date     TEXT,
                    created_at   TEXT NOT NULL,
                    updated_at   TEXT NOT NULL,
                    completed_at TEXT
                );

                CREATE TABLE IF NOT EXISTS notes (
                    id         TEXT PRIMARY KEY,
                    project_id TEXT REFERENCES projects(id) ON DELETE SET NULL,
                    title      TEXT NOT NULL,
                    content    TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS captures (
                    id         TEXT PRIMARY KEY,
                    project_id TEXT REFERENCES projects(id) ON DELETE SET NULL,
                    content    TEXT NOT NULL,
                    type       TEXT NOT NULL DEFAULT 'AUTO',
                    processed  INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS activity_log (
                    id          TEXT PRIMARY KEY,
                    entity_type TEXT NOT NULL,
                    entity_id   TEXT NOT NULL,
                    action      TEXT NOT NULL,
                    summary     TEXT,
                    created_at  TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS config (
                    key   TEXT PRIMARY KEY,
                    value TEXT
                );
                """));

        MIGRATIONS.add(new Migration(2, "002_add_indexes", """
                CREATE INDEX IF NOT EXISTS idx_tasks_project      ON tasks(project_id);
                CREATE INDEX IF NOT EXISTS idx_tasks_status        ON tasks(status);
                CREATE INDEX IF NOT EXISTS idx_tasks_due_date      ON tasks(due_date);
                CREATE INDEX IF NOT EXISTS idx_tasks_priority      ON tasks(priority);
                CREATE INDEX IF NOT EXISTS idx_notes_project       ON notes(project_id);
                CREATE INDEX IF NOT EXISTS idx_captures_processed  ON captures(processed);
                CREATE INDEX IF NOT EXISTS idx_captures_project    ON captures(project_id);
                CREATE INDEX IF NOT EXISTS idx_activity_entity     ON activity_log(entity_type, entity_id);
                CREATE INDEX IF NOT EXISTS idx_activity_created    ON activity_log(created_at);
                """));
    }

    private MigrationRunner() {
    }

    public static void runMigrations(Connection connection) {
        try {
            ensureMigrationTable(connection);
            int currentVersion = getCurrentVersion(connection);

            for (Migration migration : MIGRATIONS) {
                if (migration.version() > currentVersion) {
                    applyMigration(connection, migration);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to run database migrations: " + e.getMessage(), e);
        }
    }

    private static void ensureMigrationTable(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS schema_migrations (
                        version     INTEGER PRIMARY KEY,
                        name        TEXT NOT NULL,
                        applied_at  TEXT NOT NULL
                    );
                    """);
        }
    }

    private static int getCurrentVersion(Connection connection) throws SQLException {
        String sql = "SELECT MAX(version) FROM schema_migrations";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    private static void applyMigration(Connection connection, Migration migration) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (Statement stmt = connection.createStatement()) {
                String[] statements = migration.sql().split(";");
                for (String sql : statements) {
                    String trimmed = sql.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }

            String recordSql = "INSERT INTO schema_migrations (version, name, applied_at) VALUES (?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(recordSql)) {
                ps.setInt(1, migration.version());
                ps.setString(2, migration.name());
                ps.setString(3, DateTimeUtil.formatForStorage(LocalDateTime.now()));
                ps.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }
}
