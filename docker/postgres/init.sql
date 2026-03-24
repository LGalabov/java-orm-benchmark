-- PostgreSQL bootstrap for java-orm-benchmark
-- The ormbench database and bench user are created by environment variables.
-- Schema tables are created at runtime by SchemaManager in the common module.

-- Benchmark scratch table for insert tests (Q7, Q8).
-- Truncated between invocations; never seeded.
CREATE TABLE IF NOT EXISTS bench_users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    department  VARCHAR(50),
    created_at  TIMESTAMP DEFAULT NOW()
);
