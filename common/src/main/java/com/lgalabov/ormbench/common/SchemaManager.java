package com.lgalabov.ormbench.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class SchemaManager {

    private static final Logger log = LoggerFactory.getLogger(SchemaManager.class);
    private static final String SCHEMA_RESOURCE = "/schema.sql";

    private SchemaManager() {}

    public static void apply(Connection conn) throws SQLException {
        String sql = loadSchema();
        try (Statement stmt = conn.createStatement()) {
            for (String command : sql.split(";")) {
                String trimmed = command.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }
        log.info("Schema applied successfully");
    }

    private static String loadSchema() {
        try (InputStream is = SchemaManager.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (is == null) {
                throw new IllegalStateException("Schema resource not found: " + SCHEMA_RESOURCE);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read schema resource", e);
        }
    }
}
