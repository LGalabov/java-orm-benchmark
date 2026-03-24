# Design Decisions

Rationale behind key choices. Reference this when implementing — it answers "why did we do it this way?" so that Claude Code doesn't re-litigate settled decisions.

## D1: No web framework in the benchmark

**Decision**: Measure persistence only. No Spring MVC, no Micronaut HTTP, no Quarkus REST.

**Why**: HTTP routing, JSON serialization, request lifecycle, and middleware all add noise that varies independently of the persistence layer. A developer choosing between Hibernate and jOOQ cares about persistence overhead, not whether Spring MVC adds 200μs of request processing.

**Implication**: The benchmark harness calls OrmAdapter methods directly. There is no HTTP server, no request context, no dependency injection container (except where a framework requires one).

## D2: Standalone Hibernate as its own subject

**Decision**: `hibernate-standalone` uses Hibernate 7 SessionFactory directly, separate from framework subjects like `spring-data-jpa` or `quarkus-panache`.

**Why**: Spring Data JPA, Panache, and Micronaut Data JPA all add their own abstraction layers. Having a standalone Hibernate subject isolates the cost of the ORM itself from the cost of the framework wrapping it.

**Implication**: The `hibernate-standalone` subject creates a SessionFactory from programmatic configuration. No Spring ApplicationContext, no CDI container. Framework-specific Hibernate subjects (`spring-data-jpa`, `quarkus-panache`, etc.) are measured separately.

## D3: JDBC baseline uses best practices

**Decision**: The JDBC baseline uses PreparedStatement, proper try-with-resources, and addBatch() for bulk operations. No connection pooling (raw DriverManager).

**Why**: The baseline must represent what a competent developer writes, not a strawman.

**Exception**: If a framework requires a DataSource, use HikariCP with maxPoolSize=1 identically across all subjects.

## D4: Deterministic seed data

**Decision**: Fixed PRNG seed (42). Every run produces byte-identical data.

**Why**: Reproducibility. No ThreadLocalRandom, no Math.random(), no UUID.randomUUID() for fields that affect query behavior.

## D5: Flattened join results

**Decision**: Q3 returns OrderWithItems as a flat record (one row per item), not nested Order with List<OrderItem>.

**Why**: Nested object assembly is framework-specific. Flat projection keeps the comparison fair.

## D6: CTE tracked as capability, not benchmarked

**Decision**: CTE (WITH clause) support is captured in the Feature Matrix as a capability check (native / passthrough / blocked) but is not a timed benchmark.

**Why**: A CTE that expresses the same logic as a plain aggregation produces an identical query plan in modern Postgres. Benchmarking both would measure the database optimizer, not the framework — which contradicts D1's principle of isolating persistence-layer overhead. The Feature Matrix still records whether each framework can express a CTE natively.

## D7: Query metadata captured alongside timing

**Decision**: Every benchmark records how the framework expressed the query (native_dsl, raw_sql, etc.).

**Why**: A framework at 50μs with raw SQL tells a different story than one at 60μs with type-safe DSL.

## D8: JMH SampleTime + Throughput modes

**Decision**: Use both @BenchmarkMode({Mode.SampleTime, Mode.Throughput}).

**Why**: SampleTime gives p50/p95/p99. Throughput gives ops/sec. Both needed.

## D9: Postgres-specific features are capability checks, not timed benchmarks

**Decision**: JSONB containment, full-text search, CTEs, and window functions are tracked in the Feature Matrix as capability checks (native / passthrough / blocked) but are not timed benchmark queries.

**Why**: These features test whether the framework can express Postgres-specific SQL, not how fast the database executes it. The database execution time is identical regardless of framework — only the expressibility differs. The Feature Matrix captures this without adding noise to the latency numbers.

## D10: GPL-3.0 license

**Decision**: GPL-3.0 for now. May reconsider before public launch.

**Open question**: MIT or Apache-2.0 would maximize community adoption. Revisit before promoting publicly.

## D11: Shadow JAR for benchmark distribution

**Decision**: Single executable fat JAR including all subjects.

**Why**: JMH's recommended approach. Simplifies Docker runner and CI.

## D12: Testcontainers for tests, Docker Compose for benchmarks

**Decision**: Tests use Testcontainers. Benchmarks use long-lived Docker Compose databases.

**Why**: Testcontainers adds startup overhead that would pollute benchmark numbers.

## D13: JMH warmup tuned for database benchmarks

**Decision**: Use `@Warmup(iterations = 5, time = 2)` and run `ANALYZE` on all tables after seeding.

**Why**: Database benchmarks differ from CPU microbenchmarks. The first iterations after setup may be significantly slower due to cold Postgres shared buffers, unpopulated query plan caches, and JIT compilation warmup in both the JVM and the database. Five warmup iterations of 2 seconds each allow both the JVM (JIT, class loading) and Postgres (buffer pool, plan cache) to stabilize. The post-seeding ANALYZE ensures the planner has accurate statistics from the start, preventing the optimizer from choosing sequential scans on indexed columns during early iterations. If p99 is >5× p50 in warmup-free runs but <2× with this warmup count, the configuration is sufficient.

## D14: Postgres-only for v1

**Decision**: v1 benchmarks run on PostgreSQL 17 only. MySQL is deferred.

**Why**: The interesting persistence-layer differences (framework overhead, result mapping cost, batching strategy, N+1 penalty) are nearly identical regardless of which database sits underneath — the framework does the same work either way. Adding MySQL doubles the test matrix and implementation effort for results that would almost certainly show the same relative ordering between frameworks. Postgres-specific features (JSONB, full-text search, RETURNING clause) also make it the more interesting test target. MySQL support is a natural v2 addition once v1 ships.
