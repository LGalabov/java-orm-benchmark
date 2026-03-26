package com.lgalabov.ormbench.common;

public record BenchmarkConfig(
    String jdbcUrl,
    String username,
    String password
) {

    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/ormbench";
    private static final String DEFAULT_USER = "bench";
    private static final String DEFAULT_PASSWORD = "bench";

    public static BenchmarkConfig fromEnvironment() {
        return new BenchmarkConfig(
            env("BENCH_DB_URL", DEFAULT_URL),
            env("BENCH_DB_USER", DEFAULT_USER),
            env("BENCH_DB_PASSWORD", DEFAULT_PASSWORD)
        );
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
