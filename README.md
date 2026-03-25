# java-orm-benchmark

Real-world benchmarks for Java persistence frameworks on PostgreSQL — latency, overhead, and the hidden costs that framework authors don't publish.

## Why This Exists

TechEmpower Framework Benchmarks [sunsetted in March 2026](https://github.com/TechEmpower/FrameworkBenchmarks/issues/10932). This project picks up the persistence piece — framework overhead in isolation, measured with JMH, on real queries against real data. No HTTP layer, no serialization noise. Just the cost of getting rows in and out of PostgreSQL through different Java stacks.

## What We Measure

- Latency (p50/p95/p99) and throughput per subject per query type
- Overhead: stack depth, heap allocations, GC pressure, cold start time
- Crossover: at what RTT / batch size / concurrency does framework overhead become negligible
- Feature matrix: which frameworks express CTEs, JSONB, FTS, window functions natively vs raw SQL
- N+1 penalty: cost of the default vs the optimized path, per framework

## Subjects

|#  |Subject                    |Framework    |Persistence                   |Pool                |Status |
|---|---------------------------|-------------|------------------------------|--------------------|-------|
|1  |`jdbc-raw`                 |None         |Raw JDBC                      |None (DriverManager)|Planned|
|2  |`jdbc-hikari`              |None         |Raw JDBC                      |HikariCP            |Planned|
|3  |`hibernate-standalone`     |None         |Hibernate 7                   |HikariCP            |Planned|
|4  |`spring-data-jpa`          |Spring Boot 4|Spring Data JPA (Hibernate)   |HikariCP            |Planned|
|5  |`spring-data-jdbc`         |Spring Boot 4|Spring Data JDBC              |HikariCP            |Planned|
|6  |`spring-jooq`              |Spring Boot 4|jOOQ                          |HikariCP            |Planned|
|7  |`spring-r2dbc`             |Spring Boot 4|Spring Data R2DBC             |R2DBC pool          |Planned|
|8  |`quarkus-panache`          |Quarkus 3    |Panache (Hibernate)           |Agroal              |Planned|
|9  |`quarkus-panache-reactive` |Quarkus 3    |Panache Reactive              |Vert.x              |Planned|
|10 |`micronaut-data-jdbc`      |Micronaut 4  |Micronaut Data JDBC           |HikariCP            |Planned|
|11 |`micronaut-data-jpa`       |Micronaut 4  |Micronaut Data JPA (Hibernate)|HikariCP            |Planned|
|12 |`helidon-jpa`              |Helidon 4    |JPA (Hibernate)               |HikariCP            |Planned|

## Queries

|Query                       |What It Tests                                                               |
|----------------------------|----------------------------------------------------------------------------|
|**PK Lookup**               |Single-row fetch by primary key — the universal baseline                    |
|**Filter + Sort + Paginate**|WHERE + ORDER BY + LIMIT/OFFSET — the most common production read           |
|**Multi-table Join**        |3-table join to flat DTO — result mapping overhead for non-entity types     |
|**N+1 vs Eager Load**       |Load parents then access children — naive (N+1) vs optimized (JOIN FETCH)   |
|**Aggregation**             |GROUP BY + HAVING + SUM/COUNT — pushed-down vs application-side computation |
|**Projection**              |Subset of columns to DTO — framework overhead for partial fetches           |
|**Single Insert**           |INSERT one row, return generated ID — write baseline, ID generation strategy|
|**Batch Insert**            |Bulk write at 100, 1K, 10K rows — batching strategy and round-trip cost     |
|**Batch Update**            |Conditional UPDATE on multiple rows — dirty checking vs direct SQL          |
|**Pagination at Depth**     |OFFSET at page 1 vs 100 vs 1000 — degradation pattern and keyset support    |

### Feature Matrix (Capability Only)

These are not benchmarked for latency — they test whether the framework can express the feature natively, requires raw SQL passthrough, or is blocked entirely.

|Feature                    |What We Check                                                      |
|---------------------------|-------------------------------------------------------------------|
|CTE (WITH clause)          |Can the framework express CTEs in its DSL/API?                     |
|Window functions (OVER)    |Can it express PARTITION BY / ROW_NUMBER natively?                 |
|JSONB containment (@>)     |Can it express Postgres JSONB operators?                           |
|Full-text search (tsvector)|Can it express FTS without raw SQL?                                |
|RETURNING clause           |Does INSERT/UPDATE return generated values in one round-trip?      |
|Batch insert strategy      |Does it use multi-row VALUES, addBatch(), or individual statements?|
|Keyset pagination          |Does the framework support cursor-based pagination natively?       |

## Database

PostgreSQL 17 via Docker. MySQL deferred to v2 ([D14](docs/DECISIONS.md)).

## Results

Latency (p50/p95/p99) and throughput per subject per query type.
Overhead: stack depth, allocations per query, GC activity, cold start time.
Crossover: at what data volume, concurrency, and query complexity does each abstraction become the bottleneck.

All results published to a [public GitHub Gist](https://gist.github.com/LGalabov/0c660c130278e8a7b39fc702d8693f4e) on every CI run.

## Project Structure

```
java-orm-benchmark/
├── common/                  Shared: domain model, config, seeding, result models
├── benchmark-harness/       Central JMH runner with ORM adapter pattern
├── subjects/                One module per subject (jdbc-raw, hibernate-standalone, etc.)
├── docker/                  Docker Compose for PostgreSQL + benchmark runner
├── scripts/                 Automation: run benchmarks, upload results, generate reports
├── results/                 Benchmark output (gitignored locally, published to Gist)
├── .github/workflows/       CI (build/test) + Benchmark (manual dispatch)
├── docs/                    Project documentation driving development
└── gradle/libs.versions.toml   Single source of truth for all versions
```

## Quick Start

```bash
# Prerequisites: Java 25, Docker, jq

# Start database
docker compose -f docker/docker-compose.yml up -d

# Build
./gradlew clean build

# Run all benchmarks
./scripts/run-benchmarks.sh

# Quick mode (reduced iterations, useful for development)
./scripts/run-benchmarks.sh --quick

# Docker-based benchmark (isolated environment)
./gradlew clean shadowJar
docker compose -f docker/docker-compose.benchmark.yml up --build

# Publish results
GIST_TOKEN=<your-token> GIST_ID=<gist-id> ./scripts/upload-gist.sh results/latest.json
```

## Adding a New Framework

1. Create a module directory with a `build.gradle`
1. Add it to `settings.gradle`
1. Add version and library entries to `gradle/libs.versions.toml`
1. Implement the `OrmAdapter` interface
1. Register in `benchmark-harness/build.gradle`

## CI / Benchmarks

- **CI** (`ci.yml`): Runs on every push and PR — builds and tests all modules
- **Benchmark** (`benchmark.yml`): Manual dispatch via GitHub Actions — runs the full JMH suite and publishes results to the public Gist

## License

GPL-3.0. See <LICENSE>.
