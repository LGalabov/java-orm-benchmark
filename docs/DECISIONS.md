# Design Decisions

Rationale behind key choices. Reference this when implementing — it answers "why did we do it this way?" so that Claude Code doesn't re-litigate settled decisions.

## D1: No web framework in the benchmark

**Decision**: Measure persistence only. No Spring MVC, no Micronaut HTTP, no Quarkus REST.

**Why**: HTTP routing, JSON serialization, request lifecycle, and middleware all add noise that varies independently of the persistence layer. A developer choosing between Hibernate and jOOQ cares about persistence overhead, not whether Spring MVC adds 200μs of request processing.

**Implication**: The benchmark harness calls OrmAdapter methods directly. There is no HTTP server, no request context, no dependency injection container (except where a framework requires one).

## D2: Standalone Hibernate, not Spring Data JPA

**Decision**: Use Hibernate 7 SessionFactory directly, not through Spring Boot / Spring Data JPA.

**Why**: Spring Data JPA adds its own abstraction layer (repositories, query derivation, transaction proxies). Measuring "Hibernate" through Spring Data JPA would conflate two frameworks' overhead.

**Implication**: The Hibernate subject creates a SessionFactory from programmatic configuration. No Spring ApplicationContext.

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

## D6: CTE as separate query from aggregation

**Decision**: Q4 (aggregation) and Q5 (CTE) express the same business logic differently. Both are benchmarked.

**Why**: Directly measures whether CTE support adds or removes overhead vs. plain aggregation.

## D7: Query metadata captured alongside timing

**Decision**: Every benchmark records how the framework expressed the query (native_dsl, raw_sql, etc.).

**Why**: A framework at 50μs with raw SQL tells a different story than one at 60μs with type-safe DSL.

## D8: JMH SampleTime + Throughput modes

**Decision**: Use both @BenchmarkMode({Mode.SampleTime, Mode.Throughput}).

**Why**: SampleTime gives p50/p95/p99. Throughput gives ops/sec. Both needed.

## D9: Postgres-specific queries run on Postgres only

**Decision**: JSONB and FTS queries skip MySQL.

**Why**: Comparing Postgres JSONB @> to MySQL JSON_CONTAINS measures database differences, not framework differences.

## D10: GPL-3.0 license

**Decision**: GPL-3.0 for now. May reconsider before public launch.

**Open question**: MIT or Apache-2.0 would maximize community adoption. Revisit before promoting publicly.

## D11: Shadow JAR for benchmark distribution

**Decision**: Single executable fat JAR including all subjects.

**Why**: JMH's recommended approach. Simplifies Docker runner and CI.

## D12: Testcontainers for tests, Docker Compose for benchmarks

**Decision**: Tests use Testcontainers. Benchmarks use long-lived Docker Compose databases.

**Why**: Testcontainers adds startup overhead that would pollute benchmark numbers.
