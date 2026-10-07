package com.akshit.comefort.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MigrationRunner Tests")
class MigrationRunnerTest {

    private DatabaseManager dbManager;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        dbManager = new DatabaseManager(tempDir);
        dbManager.initialize();
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("Migrations table is created and populated with schema versions")
    void testMigrationsApplied() throws Exception {
        Connection conn = dbManager.getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM schema_migrations")) {
            assertTrue(rs.next());
            int migrationCount = rs.getInt(1);
            assertTrue(migrationCount >= 2, "Expected at least 2 migrations to be recorded in schema_migrations");
        }
    }

    @Test
    @DisplayName("Running migrations multiple times is idempotent")
    void testMigrationIdempotency() {
        assertDoesNotThrow(() -> {
            MigrationRunner.runMigrations(dbManager.getConnection());
            MigrationRunner.runMigrations(dbManager.getConnection());
        });
    }
}
