package com.lgalabov.ormbench.common;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class DataSeederTest {

    private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/ormbench";
    private static final String USERNAME = "bench";
    private static final String PASSWORD = "bench";

    @BeforeAll
    static void seed() throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS order_items CASCADE");
            stmt.execute("DROP TABLE IF EXISTS orders CASCADE");
            stmt.execute("DROP TABLE IF EXISTS products CASCADE");
            stmt.execute("DROP TABLE IF EXISTS users CASCADE");
            stmt.execute("DROP TABLE IF EXISTS bench_users CASCADE");
        }
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            DataSeeder.seed(conn);
        }
    }

    @Test
    void seedCreatesExpectedRowCounts() throws SQLException {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            assertRowCount(stmt, "users", 10_000);
            assertRowCount(stmt, "products", 5_000);
            assertRowCount(stmt, "orders", 100_000);
            assertRowCount(stmt, "order_items", 300_000);
            assertRowCount(stmt, "bench_users", 0);
        }
    }

    @Test
    void schemaIndexesExist() throws SQLException {
        String[] expectedIndexes = {
            "idx_orders_user_id",
            "idx_orders_status",
            "idx_orders_placed_at",
            "idx_order_items_order_id",
            "idx_order_items_product_id",
            "idx_products_category",
            "idx_products_attrs",
            "idx_products_search"
        };

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            for (String index : expectedIndexes) {
                ResultSet rs = stmt.executeQuery(
                    "SELECT 1 FROM pg_indexes WHERE indexname = '" + index + "'");
                assertThat(rs.next())
                    .as("Index %s should exist", index)
                    .isTrue();
                rs.close();
            }
        }
    }

    @Test
    void orderTotalsMatchItems() throws SQLException {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("""
                SELECT COUNT(*) FROM (
                    SELECT o.id, o.total, SUM(oi.quantity * oi.price) AS computed
                    FROM orders o
                    JOIN order_items oi ON oi.order_id = o.id
                    GROUP BY o.id
                    HAVING ABS(o.total - SUM(oi.quantity * oi.price)) > 0.01
                ) mismatched
                """);
            rs.next();
            assertThat(rs.getLong(1))
                .as("All order totals should match sum of their items")
                .isZero();
            rs.close();
        }
    }

    @Test
    void seedIsDeterministic() throws SQLException {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            // Verify first users match expected deterministic output
            ResultSet rs = stmt.executeQuery(
                "SELECT name, email, department FROM users ORDER BY id LIMIT 2");

            rs.next();
            assertThat(rs.getString("name")).isEqualTo("Alice Smith");
            assertThat(rs.getString("email")).isEqualTo("alice.smith.0@bench.test");
            assertThat(rs.getString("department")).isEqualTo("Engineering");

            rs.next();
            assertThat(rs.getString("name")).isEqualTo("Alice Jones");
            assertThat(rs.getString("email")).isEqualTo("alice.jones.1@bench.test");
            assertThat(rs.getString("department")).isEqualTo("Sales");

            rs.close();
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
    }

    private void assertRowCount(Statement stmt, String table, int expected) throws SQLException {
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table);
        rs.next();
        assertThat(rs.getLong(1))
            .as("Row count for %s", table)
            .isEqualTo(expected);
        rs.close();
    }
}
