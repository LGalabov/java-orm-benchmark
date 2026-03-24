# java-orm-benchmark

Real-world benchmarks for Java persistence frameworks across PostgreSQL and MySQL — latency, overhead, and the hidden costs that framework authors don't publish.

## Why This Exists

Choosing a persistence stack in Java means navigating trade-offs that are rarely quantified in one place. Developers make architectural decisions — which framework to use, which database features to leverage, when to introduce caching — and finding reliable, comparable data to inform those decisions is harder than it should be.

This project exists to close that gap. One shared, reproducible reference point grounded in real measurements.

With the [sunsetting of TechEmpower Framework Benchmarks](https://github.com/TechEmpower/FrameworkBenchmarks/issues/10932) after a decade of service, the community has lost its most trusted cross-framework performance reference. TechEmpower measured web frameworks end-to-end — HTTP routing, serialization, and persistence combined. This project picks up a narrower, deeper thread: **the persistence layer in isolation**, measured with the precision that architectural decisions actually require.

## What Makes This Different

### The full stack is the unit of measurement

Most benchmarks test frameworks in isolation. Real applications are not isolated. This project measures the complete coordinate: **framework × database × query type**, as a single deployable combination. The question answered is the one developers actually face: how does Hibernate over PostgreSQL compare to jOOQ over MySQL, end to end, on the same workload?

### Published numbers are put in context

Framework authors publish performance characteristics, and those numbers are often accurate — for the specific scenario they were measured in. This project breaks them down by query type, workload shape, and database version so developers can see where a claim holds, where it narrows, and what drives the difference. jOOQ, for example, states it adds approximately 1ms of overhead over raw JDBC. We measure that across every query type and database we cover.

### The database is a first-class variable

Modern databases offer features — GIN indexes, JSONB, full-text search — that can meaningfully reduce the work an application layer needs to do. This project measures the ORM layer in combination with those features, not just against basic CRUD. The goal is to help developers understand what their chosen stack can do before reaching for additional infrastructure like caches or read replicas.

### The hidden costs are made visible

Some of the most meaningful performance characteristics of a persistence framework are never published: call stack depth per query, heap allocations per operation, GC pressure under sustained load, and cold start time. These costs accumulate in production and are worth understanding alongside the headline latency numbers. This project measures and publishes all of them.

### The crossover points are identified

Raw JDBC is always the floor. The question is not whether an ORM adds overhead — it always does — but **when that overhead becomes the dominant cost**. We identify the thresholds: where the ORM layer overtakes network latency as the bottleneck, where connection pool configuration starts to matter, where reactive connectors earn their complexity, and where the database itself is being underutilized compared to what caching would actually buy.

### The data layer is isolated

HTTP servers, JSON serialization, request routing, and middleware all add noise to persistence benchmarks. Every result in this project is measured at the data layer boundary only. No web framework, no serialization overhead, no request lifecycle. This is the only way to understand what the ORM is actually costing.

### The complexity spectrum is covered

Simple CRUD is not a representative workload. Aggregations, multi-table joins, CTEs, batch writes, and projection queries behave differently across frameworks and databases. A framework that performs well on a primary key lookup may degrade significantly on a reporting query. We cover the full spectrum and show where each framework's strengths and weaknesses actually appear.

## Subjects

Each framework is implemented using the approach its own documentation recommends. If a framework has a canonical way to execute a join query, that is what we use.

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

## Query Types

|Query                          |What It Tests                                                          |
|-------------------------------|-----------------------------------------------------------------------|
|**PK Lookup**                  |Single-row fetch by primary key — the baseline operation               |
|**Filter + Sort**              |WHERE clause with ORDER BY — index utilization and plan choice         |
|**Multi-table Join**           |3-table join with projection — how the framework handles result mapping|
|**Aggregation**                |GROUP BY with HAVING — pushed-down vs. application-side computation    |
|**CTE**                        |WITH clause — support for modern SQL features beyond basic CRUD        |
|**Batch Insert**               |Bulk write (100, 1K, 10K rows) — batching strategy and round-trip cost |
|**Batch Update**               |Bulk conditional update — dirty checking overhead vs. explicit SQL     |
|**Projection**                 |Subset of columns to a DTO — framework overhead for non-entity results |
|**JSONB Query** (Postgres)     |GIN-indexed JSONB containment — database-native feature utilization    |
|**Full-Text Search** (Postgres)|tsvector/tsquery — whether the ORM supports or obstructs FTS           |

## Databases

|Database  |Version|Why                                                        |
|----------|-------|-----------------------------------------------------------|
|PostgreSQL|17     |Advanced feature set (JSONB, FTS, CTEs, window functions)  |
|MySQL     |9.x    |Widespread adoption, different optimization characteristics|

Both databases run in Docker with identical hardware allocation. Schema and seed data are shared; database-specific queries (JSONB, FTS) run only where supported.

## What the Results Show

Each benchmark run produces three outputs:

**Performance summary** — latency (p50, p95, p99) and throughput per subject per query type, across both databases.

**Overhead analysis** — stack depth, allocations per query, GC activity, and cold start time. The cost breakdown that frameworks do not publish.

**Crossover report** — at what data volume, concurrency level, and query complexity does each abstraction layer become the bottleneck.

All results are published to a [public GitHub Gist](https://gist.github.com/LGalabov/0c660c130278e8a7b39fc702d8693f4e) on every CI run. The schema, seed data, and benchmark code are in this repository. The hardware profile of each run is recorded alongside the results. Anyone can reproduce, challenge, or extend the findings.

## What This Is Not

**Not a recommendation engine.** It does not tell you which framework to use. It gives you the data to make that decision yourself, for your workload, on your infrastructure.

**Not a web framework benchmark.** HTTP performance is a separate concern. These results measure persistence only.

**Not a synthetic stress test designed to make one framework look good.** Every subject is implemented using the approach its own documentation recommends.

## Project Structure

```
java-orm-benchmark/
├── common/                  Shared: domain model, config, seeding, result models
├── benchmark-harness/       Central JMH runner with ORM adapter pattern
├── subjects/                One module per subject (jdbc-raw, hibernate-standalone, etc.)
├── docker/                  Docker Compose for databases + benchmark runner
├── scripts/                 Automation: run benchmarks, upload results, generate reports
├── results/                 Benchmark output (gitignored locally, published to Gist)
├── .github/workflows/       CI (build/test) + Benchmark (manual dispatch)
├── docs/                    Project documentation driving development
└── gradle/libs.versions.toml   Single source of truth for all versions
```

## Prerequisites

- Java 25 (or the version specified in `gradle/libs.versions.toml`)
- Docker and Docker Compose
- `jq` (for scripts)

## Quick Start

```bash
# Start databases
docker compose -f docker/docker-compose.yml up -d

# Build
./gradlew clean build

# Run all benchmarks
./scripts/run-benchmarks.sh

# Run a specific subject
./scripts/run-benchmarks.sh --subject hibernate --db postgres

# Quick mode (reduced iterations, useful for development)
./scripts/run-benchmarks.sh --quick
```

## Docker-Based Benchmark (Isolated Environment)

```bash
./gradlew clean shadowJar
docker compose -f docker/docker-compose.benchmark.yml up --build
```

## Publishing Results

```bash
GIST_TOKEN=<your-token> GIST_ID=<gist-id> ./scripts/upload-gist.sh results/latest.json
```

## Adding a New Framework

Each benchmark subject lives in its own directory. Adding a new framework means implementing the `OrmAdapter` interface against the shared schema — no changes to the harness required.

1. Create a module directory with a `build.gradle`
1. Add it to `settings.gradle`
1. Add version and library entries to `gradle/libs.versions.toml`
1. Implement the `OrmAdapter` interface
1. Register in `benchmark-harness/build.gradle`

See `CONTRIBUTING.md` for details.

## CI / Benchmarks

- **CI** (`ci.yml`): Runs on every push and PR — builds and tests all modules
- **Benchmark** (`benchmark.yml`): Manual dispatch via GitHub Actions — runs the full JMH suite and publishes results to the public Gist

## Version Management

All versions (Java, databases, libraries, plugins) are centralized in `gradle/libs.versions.toml`. Update a version in one place; it propagates everywhere.

## License

GPL-3.0. See <LICENSE>.
