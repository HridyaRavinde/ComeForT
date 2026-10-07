package com.akshit.comefort.db;

import com.akshit.comefort.exception.DatabaseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages the SQLite database connection lifecycle.
 * The database file lives at {@code ~/.comefort/comefort.db}.
 *
 * <p>This is a singleton — one connection per application run.
 * SQLite handles concurrency via file locks, so a single connection
 * is both safe and optimal for a CLI tool.</p>
 */
public class DatabaseManager {

    private static final String COMEFORT_DIR_NAME = ".comefort";
    private static final String DB_FILE_NAME = "comefort.db";

    private final Path dbDirectory;
    private final String jdbcUrl;
    private Connection connection;

    /**
     * Creates a DatabaseManager using the default location (~/.comefort/).
     */
    public DatabaseManager() {
        this(Path.of(System.getProperty("user.home"), COMEFORT_DIR_NAME));
    }

    /**
     * Creates a DatabaseManager with a custom directory.
     * Used primarily for testing with temp directories.
     */
    public DatabaseManager(Path dbDirectory) {
        this.dbDirectory = dbDirectory;
        this.jdbcUrl = "jdbc:sqlite:" + dbDirectory.resolve(DB_FILE_NAME);
    }

    /**
     * Initializes the database: creates the directory if needed,
     * opens the connection, enables WAL mode, and runs schema initialization.
     *
     * @return true if this was a fresh initialization (new database)
     */
    public boolean initialize() {
        boolean isNewDatabase = !Files.exists(dbDirectory.resolve(DB_FILE_NAME));

        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    return isNewDatabase;
                }
            } catch (SQLException ignored) {
            }
        }

        try {
            // Create the .comefort directory if it doesn't exist
            Files.createDirectories(dbDirectory);

            // Open the SQLite connection
            connection = DriverManager.getConnection(jdbcUrl);

            // Enable WAL mode for better concurrent read performance
            try (var stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA foreign_keys=ON");
            }

            // Run schema initialization
            SchemaInitializer.initialize(connection);

            return isNewDatabase;
        } catch (IOException e) {
            throw new DatabaseException(
                    "Failed to create ComeFort directory: " + dbDirectory, e);
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Failed to initialize database: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the active database connection.
     *
     * @throws DatabaseException if the database has not been initialized
     */
    public Connection getConnection() {
        if (connection == null) {
            throw new DatabaseException(
                    "Database not initialized. Run 'cmf init' or 'comefort init' first.");
        }
        try {
            if (connection.isClosed()) {
                throw new DatabaseException(
                        "Database connection is closed. Restart ComeFort.");
            }
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Failed to check connection status: " + e.getMessage(), e);
        }
        return connection;
    }

    /**
     * Returns the path to the database directory.
     */
    public Path getDbDirectory() {
        return dbDirectory;
    }

    /**
     * Returns the path to the database file.
     */
    public Path getDbFilePath() {
        return dbDirectory.resolve(DB_FILE_NAME);
    }

    /**
     * Returns true if the database file exists at the expected location.
     */
    public boolean isDatabaseInitialized() {
        return Files.exists(dbDirectory.resolve(DB_FILE_NAME));
    }

    /**
     * Closes the database connection gracefully.
     */
    public void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    try (var stmt = connection.createStatement()) {
                        stmt.execute("PRAGMA wal_checkpoint(TRUNCATE)");
                    } catch (SQLException ignored) {
                    }
                    connection.close();
                }
            } catch (SQLException e) {
                // Log but don't throw — we're shutting down
                System.err.println("Warning: Failed to close database: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }
}
