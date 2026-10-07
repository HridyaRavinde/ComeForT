package com.akshit.comefort.db;

import com.akshit.comefort.exception.DatabaseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Reads and executes the DDL schema from {@code schema.sql} on the classpath.
 * Uses {@code IF NOT EXISTS} guards so it's safe to run on every startup.
 */
public final class SchemaInitializer {

    private static final String SCHEMA_RESOURCE = "/schema.sql";

    private SchemaInitializer() {
        // utility class
    }

    /**
     * Loads the schema SQL from the classpath and executes it against the connection.
     * All statements use IF NOT EXISTS, making this idempotent.
     */
    public static void initialize(Connection connection) {
        String schemaSql = loadSchemaFromClasspath();
        executeSql(connection, schemaSql);
    }

    /**
     * Reads the schema.sql file from the classpath resources.
     */
    private static String loadSchemaFromClasspath() {
        try (InputStream is = SchemaInitializer.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (is == null) {
                throw new DatabaseException(
                        "Schema file not found on classpath: " + SCHEMA_RESOURCE);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new DatabaseException("Failed to read schema file: " + e.getMessage(), e);
        }
    }

    /**
     * Executes the SQL schema. Splits on semicolons and executes each statement.
     */
    private static void executeSql(Connection connection, String sql) {
        try (Statement stmt = connection.createStatement()) {
            // Strip out single-line comments (-- ...)
            StringBuilder cleanSql = new StringBuilder();
            for (String line : sql.split("\n")) {
                String lineTrimmed = line.trim();
                if (!lineTrimmed.startsWith("--")) {
                    cleanSql.append(line).append("\n");
                }
            }

            // Split by semicolons and execute each statement individually
            String[] statements = cleanSql.toString().split(";");
            for (String stmtText : statements) {
                String trimmed = stmtText.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Failed to initialize database schema: " + e.getMessage(), e);
        }
    }
}
