# Implementation Plan

Ordered segments for building the project incrementally. Each segment is self-contained: it has a clear input state, a clear output state, and a verification step. Designed to be driven from a phone via Claude Code — one segment per session.

## Current State

The repo has a working Gradle multi-module skeleton with `common/` and `benchmark-harness/` modules, Docker Compose for Postgres and MySQL, CI workflows, and shell scripts. No ORM subjects exist yet. No benchmark code exists yet.

## Segment Order

```
S1  Schema + Domain Model + Seeder
S2  OrmAdapter interface + Harness wiring
S3  JDBC baseline subject
S4  JMH benchmark methods
S5  Results JSON + Gist upload
S6  Hibernate subject
S7  Overhead profiling (stack depth, allocations)
S8  jOOQ subject
S9  Crossover analysis
S10 Remaining subjects (JDBI, MyBatis, Micronaut Data)
S11 Results Gist formatting
S12 Docker benchmark runner
S13 Results Site (GitHub Pages)
```

-----

## S1: Schema + Domain Model + Seeder

**Input state**: Empty `common/` module with build.gradle.

**Work**:

1. Create SQL files in `common/src/main/resources/`:
- `schema-postgres.sql` — tables + indexes per SCHEMA.md
- `schema-mysql.sql` — tables + indexes per SCHEMA.md
1. Create domain records in `common/src/main/java/com/benchmark/model/`:
- `User.java`, `Product.java`, `Order.java`, `OrderItem.java`
- `UserSummary.java`, `UserSpendSummary.java`, `OrderWithItems.java`
1. Create `BenchmarkConfig.java` — holds DB connection params, loaded from env vars or properties file
1. Create `DataSeeder.java` — generates deterministic seed data via raw JDBC
- Fixed seed (42)
- Volumes per SCHEMA.md: 10K users, 5K products, 100K orders, 300K items
- JSONB attributes generation for Postgres products
- Separate `seedPostgres()` and `seedMysql()` methods for DB-specific columns
1. Create `DataSeeder` test that seeds a Testcontainers Postgres instance and verifies row counts

**Verify**: `./gradlew :common:test` passes. Seeder creates correct row counts.

**Dependencies**: Add to `libs.versions.toml`: testcontainers, postgresql JDBC driver, mysql-connector-j.

-----

## S2: OrmAdapter Interface + Harness Wiring

**Input state**: S1 complete. Domain model and seeder exist in `common/`.

**Work**:

1. Create `OrmAdapter.java` interface in `common/` per QUERIES.md
1. Create `BenchmarkCapability.java` enum: `NATIVE_DSL`, `ANNOTATION_QUERY`, `RAW_SQL`, `NOT_SUPPORTED`
1. Create `QueryMetadata.java` record to capture how each query was expressed
1. Create `OrmAdapterRegistry.java` in `benchmark-harness/` — discovers adapters via ServiceLoader
1. Create `BenchmarkResult.java` record in `common/` — structured output for one benchmark run:

   ```java
   public record BenchmarkResult(
       String subject,
       String database,
       String queryType,
       Map<String, Double> latencyPercentiles,  // p50, p95, p99
       double throughput,
       QueryMetadata metadata,
       Map<String, Object> environment
   ) {}
   ```
1. Create `ResultCollector.java` — accumulates BenchmarkResult entries, writes JSON

**Verify**: `./gradlew :benchmark-harness:build` compiles. No runtime tests yet (no adapters registered).

-----

## S3: JDBC Baseline Subject

**Input state**: S2 complete. OrmAdapter interface exists.

**Work**:

1. Create module `subjects/orm-jdbc/` with `build.gradle`
1. Add to `settings.gradle`: `include 'subjects:orm-jdbc'`
1. Implement `JdbcAdapter.java` — implements every OrmAdapter method with raw PreparedStatement
1. Every query uses the exact SQL from QUERIES.md
1. Result mapping: manual ResultSet → record via constructor
1. Batch insert: `PreparedStatement.addBatch()` + `executeBatch()`
1. Register via `META-INF/services/com.benchmark.OrmAdapter`
1. Write a test that runs each adapter method against Testcontainers Postgres and verifies results

**Verify**: `./gradlew :subjects:orm-jdbc:test` passes. All 10 query types return correct results.

**Important**: This is the performance floor. Every measurement from every other subject is compared against these numbers. The JDBC implementation must be clean, idiomatic, and use best practices (connection reuse, proper statement closing, etc.).

-----

## S4: JMH Benchmark Methods

**Input state**: S3 complete. At least one adapter exists and is verified correct.

**Work**:

1. Add JMH dependency to `benchmark-harness/build.gradle` (use `me.champeau.jmh` Gradle plugin)
1. Create `PersistenceBenchmark.java` — JMH benchmark class:

   ```java
   @BenchmarkMode({Mode.SampleTime, Mode.Throughput})
   @OutputTimeUnit(TimeUnit.MICROSECONDS)
   @State(Scope.Benchmark)
   @Fork(2)
   @Warmup(iterations = 5, time = 2)
   @Measurement(iterations = 10, time = 2)
   public class PersistenceBenchmark {
       @Param({"jdbc"})  // expanded as subjects are added
       String subject;
       @Param({"postgres", "mysql"})
       String database;
       // ...
   }
   ```
1. One `@Benchmark` method per query type (Q1–Q10)
1. `@Setup(Level.Trial)`: start DB containers (or connect to running Docker), run seeder, instantiate adapter
1. `@TearDown(Level.Trial)`: close adapter, cleanup
1. Parameter randomization: deterministic PRNG per iteration
1. Batch insert benchmarks: separate methods for 100/1K/10K with cleanup between invocations
1. Configure ShadowJar (uncomment in build.gradle) to produce executable benchmark jar

**Verify**: `./gradlew :benchmark-harness:jmh` runs against the JDBC adapter and produces JMH output. Even a `--quick` run with 1 fork, 1 warmup, 1 measurement is sufficient to verify the harness works.

-----

## S5: Results JSON + Gist Upload

**Input state**: S4 complete. JMH produces raw output.

**Work**:

1. Create `scripts/parse-results.py` (or shell+jq) — transforms JMH JSON output into the structured format:

   ```json
   {
     "timestamp": "2026-03-24T18:00:00Z",
     "run_id": "abc123",
     "environment": { "java": "25", "pg": "17", ... },
     "jmh_params": { "warmup": 5, "measurement": 10, ... },
     "results": [
       {
         "subject": "jdbc",
         "database": "postgres",
         "query": "pk_lookup",
         "latency_us": { "p50": 42, "p95": 67, "p99": 112 },
         "throughput_ops_s": 23500,
         "metadata": { "query_expression": "raw_sql", "result_mapping": "manual_mapper" }
       }
     ]
   }
   ```
1. Create `scripts/render-gist.py` — transforms structured JSON into the markdown tables defined in RESULTS_TEMPLATE.md
1. Update `scripts/upload-gist.sh` — takes rendered markdown, pushes to the Gist via GitHub API
1. Update `scripts/run-benchmarks.sh` — pipeline: run JMH → parse → render → (optionally) upload

**Verify**: Run benchmarks → JSON produced in `results/` → markdown rendered → matches RESULTS_TEMPLATE.md structure.

-----

## S6: Hibernate Subject

**Input state**: S5 complete. Full pipeline works for JDBC.

**Work**:

1. Create module `subjects/orm-hibernate/` with `build.gradle`
1. Dependencies: hibernate-core 7.x, standalone (no Spring Boot) to isolate ORM overhead
1. Create JPA entity classes: `UserEntity`, `ProductEntity`, `OrderEntity`, `OrderItemEntity`
1. Implement `HibernateAdapter.java`:
- Q1 (PK Lookup): `session.find(UserEntity.class, id)` → map to User record
- Q2 (Filter + Sort): HQL or Criteria API
- Q3 (Join): HQL with explicit join + projection
- Q4 (Aggregation): HQL with GROUP BY
- Q5 (CTE): native query (or HQL if Hibernate 7 supports CTEs)
- Q6 (Projection): HQL with SELECT new or Tuple query
- Q7/Q8 (JSONB/FTS): native query
- Q9 (Batch insert): session.persist() with periodic flush/clear
- Q10 (Batch update): HQL UPDATE or native SQL
1. Record QueryMetadata for each method
1. Register via ServiceLoader
1. Test against Testcontainers

**Verify**: `./gradlew :subjects:orm-hibernate:test` passes. Run `./scripts/run-benchmarks.sh --quick` and see both JDBC and Hibernate in results.

-----

## S7: Overhead Profiling

**Input state**: S6 complete. Two subjects producing data.

**Work**:

1. Add JMH GC profiler: `-prof gc`
1. Add JMH stack profiler: `-prof stack`
1. Create `StackDepthMeasurer.java` — captures frame count from user code to JDBC execute
1. Create `ColdStartMeasurer.java` — measures time from setup() to first successful query
1. Extend BenchmarkResult with overhead fields
1. Update parse/render scripts for Overhead Analysis section

**Verify**: Results JSON includes overhead data. Rendered output includes Stack Depth, Allocations, GC Pressure, and Cold Start tables.

-----

## S8: jOOQ Subject

**Input state**: S7 complete.

**Work**:

1. Create module `subjects/orm-jooq/`
1. jOOQ code generation from DDL files (DDLDatabase)
1. Implement `JooqAdapter.java` — all queries via type-safe DSL where possible
1. Record QueryMetadata accurately
1. Test against Testcontainers

**Verify**: `./gradlew :subjects:orm-jooq:test` passes. Results include all three subjects.

-----

## S9: Crossover Analysis

**Input state**: S8 complete. Three subjects with full data.

**Work**:

1. Create `scripts/crossover-analysis.py`:
- ORM overhead = subject_latency - jdbc_latency
- Negligible threshold = RTT where overhead < 5% of total
- Batch size inflection points
- Complexity scaling across query tiers
1. Add concurrency sweep benchmark (threads: 1, 4, 8, 16, 32) per BENCHMARKS.md
1. Update render scripts for Crossover Report section

**Verify**: Crossover data in results. Numbers pass sanity checks.

-----

## S10: Remaining Subjects

**Input state**: S9 complete.

### S10a: JDBI 3

### S10b: MyBatis 3

### S10c: Micronaut Data

Each: create module, implement adapter, test, verify in results.

-----

## S11: Results Gist Formatting

**Input state**: S10 complete. All subjects producing data.

**Work**: Final pass on render scripts, add Feature Matrix, add Environment section, test full render.

-----

## S12: Docker Benchmark Runner

**Input state**: S11 complete.

**Work**: Update Docker Compose, create Dockerfile.benchmark, update CI workflow.

-----

## S13: Results Site (GitHub Pages)

**Input state**: S5 or later. At least one round of data exists in `results/`.

**Work**:

1. Initialize `site/` directory: Vite + React, with Recharts for charts
1. Set up the data layer:
- `results/rounds.json` — index of all rounds
- Per-round directories: `results/round-NNN/{meta,latency,throughput,overhead,crossover}.json`
- `site/src/data/loader.js` — fetches `rounds.json`, then loads data for the selected round on demand
1. Build the shell: dark industrial aesthetic, header with round selector, sticky filter bar
1. Build `LatencyTable` — the main grid with color tiers (green ≤2× baseline, yellow 2–5×, red >5×), optional bar overlay, sticky query column
1. Build `OverheadPanel` — horizontal bar charts for stack depth, allocations, cold start
1. Build `FeatureMatrix` — the native/passthrough/blocked table
1. Build round comparison: select two rounds, show delta percentages (green = improved, red = regressed)
1. Build `FilterBar` — toggle pills for subjects, databases, percentile (p50/p95/p99). State encoded in URL hash for shareable links
1. Build `CrossoverPanel` — ORM overhead vs network RTT, batch inflection, complexity scaling
1. Set up GitHub Pages deployment workflow (`.github/workflows/deploy-site.yml`)
1. Retire the Gist to a one-line redirect

**Verify**: `cd site && npm run dev` shows the dashboard with real data. Deploy to `gh-pages`. All filters, round selection, and comparison work.

**See**: `docs/RESULTS_SITE.md` for full architecture, data formats, and UI specification. The prototype dashboard artifact shows the target aesthetic and interaction model.

-----

## Segment Dependencies

```
S1 ──→ S2 ──→ S3 ──→ S4 ──→ S5  (core pipeline)
                       ↓
                      S6 ──→ S7  (hibernate + overhead)
                              ↓
                             S8 ──→ S9  (jooq + crossover)
                                    ↓
                                   S10 ──→ S11 ──→ S12
                                                    ↓
                                                   S13  (results site)

S13 can start as early as S5 — it only needs one round of data.
```

Each segment builds on the previous. No segment can be skipped. The pipeline is usable and publishable after S5 (JDBC-only results), after S7 (JDBC + Hibernate with overhead data), and fully after S13.

## Estimated Effort Per Segment

|Segment|Effort   |Notes                                       |
|-------|---------|--------------------------------------------|
|S1     |2–3 hours|Schema + seeder is straightforward          |
|S2     |1–2 hours|Interface design, mostly boilerplate        |
|S3     |2–3 hours|JDBC is verbose but well-understood         |
|S4     |3–4 hours|JMH configuration requires care             |
|S5     |2–3 hours|Parsing + rendering scripts                 |
|S6     |4–6 hours|Hibernate has the most configuration surface|
|S7     |3–4 hours|Custom profiling instrumentation            |
|S8     |3–4 hours|jOOQ code generation setup                  |
|S9     |2–3 hours|Analysis scripts, math is simple            |
|S10    |6–9 hours|Three subjects, each 2–3 hours              |
|S11    |1–2 hours|Formatting polish                           |
|S12    |2–3 hours|Docker + CI wiring                          |
|S13    |6–8 hours|React site + deployment + data pipeline     |

**Total**: ~36–53 hours of focused Claude Code work.
