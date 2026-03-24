# java-orm-benchmark results

> Last updated: `{TIMESTAMP}` · Run ID: `{RUN_ID}` · [Source](https://github.com/LGalabov/java-orm-benchmark) · [Reproduce](#environment)

-----

## At a Glance

|                            |jdbc-raw|jdbc-hikari|hibernate|spring-jpa|spring-jdbc|spring-jooq|spring-r2dbc|quarkus|quarkus-rx|mn-jdbc|mn-jpa|helidon|
|----------------------------|:------:|:---------:|:-------:|:--------:|:---------:|:---------:|:----------:|:-----:|:--------:|:-----:|:----:|:-----:|
|**PK Lookup** (μs p50)      |`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|
|**N+1 Naive** (μs p50)      |`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|
|**N+1 Optimized** (μs p50)  |`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|
|**Batch 1K Insert** (ms p50)|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|
|**Stack Depth** (frames)    |`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|
|**Alloc/Query** (bytes)     |`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|
|**Cold Start** (ms)         |`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|`—`|

*All latency numbers are PostgreSQL 17. See per-database breakdown below.*

-----

## 1 · Latency — PostgreSQL 17

### Read Queries

|Query Type                    |jdbc-raw|jdbc-hikari|hibernate|spring-jpa|spring-jdbc|spring-jooq|spring-r2dbc|quarkus|quarkus-rx|mn-jdbc|mn-jpa|helidon|
|------------------------------|-------:|----------:|--------:|---------:|----------:|----------:|-----------:|------:|---------:|------:|-----:|------:|
|PK Lookup — p50               |        |           |         |          |           |           |            |       |          |       |      |       |
|PK Lookup — p95               |        |           |         |          |           |           |            |       |          |       |      |       |
|PK Lookup — p99               |        |           |         |          |           |           |            |       |          |       |      |       |
|Filter + Sort + Paginate — p50|        |           |         |          |           |           |            |       |          |       |      |       |
|Filter + Sort + Paginate — p95|        |           |         |          |           |           |            |       |          |       |      |       |
|Multi-table Join — p50        |        |           |         |          |           |           |            |       |          |       |      |       |
|Multi-table Join — p95        |        |           |         |          |           |           |            |       |          |       |      |       |
|N+1 Naive (101 queries) — p50 |        |           |         |          |           |           |            |       |          |       |      |       |
|N+1 Optimized (1 query) — p50 |        |           |         |          |           |           |            |       |          |       |      |       |
|Aggregation — p50             |        |           |         |          |           |           |            |       |          |       |      |       |
|Projection — p50              |        |           |         |          |           |           |            |       |          |       |      |       |
|Pagination (page 1) — p50     |        |           |         |          |           |           |            |       |          |       |      |       |
|Pagination (page 1000) — p50  |        |           |         |          |           |           |            |       |          |       |      |       |

### Write Queries

|Query Type              |jdbc-raw|jdbc-hikari|hibernate|spring-jpa|spring-jdbc|spring-jooq|spring-r2dbc|quarkus|quarkus-rx|mn-jdbc|mn-jpa|helidon|
|------------------------|-------:|----------:|--------:|---------:|----------:|----------:|-----------:|------:|---------:|------:|-----:|------:|
|Single Insert — p50     |        |           |         |          |           |           |            |       |          |       |      |       |
|Batch Insert (100) — p50|        |           |         |          |           |           |            |       |          |       |      |       |
|Batch Insert (1K) — p50 |        |           |         |          |           |           |            |       |          |       |      |       |
|Batch Insert (10K) — p50|        |           |         |          |           |           |            |       |          |       |      |       |
|Batch Update — p50      |        |           |         |          |           |           |            |       |          |       |      |       |

*All times in microseconds unless noted. Lower is better.*

## 2 · Latency — MySQL 9.x

*Same structure as above.*

-----

## 3 · Overhead Analysis

### Stack Depth

|Subject               |Frames|Notes                                            |
|----------------------|-----:|-------------------------------------------------|
|jdbc-raw (baseline)   |`—`   |Direct call                                      |
|jdbc-hikari           |`—`   |Pool acquisition overhead only                   |
|hibernate-standalone  |`—`   |Proxy → context → flush check → SQL gen → execute|
|spring-data-jpa       |`—`   |Repository proxy → Hibernate → execute           |
|spring-data-jdbc      |`—`   |Repository proxy → RowMapper → execute           |
|spring-jooq           |`—`   |DSL → render → bind → execute                    |
|spring-r2dbc          |`—`   |Reactive pipeline → block → execute              |
|quarkus-panache       |`—`   |Panache → Hibernate → execute                    |
|quarkus-panache-rx    |`—`   |Panache → HR → Vert.x → execute                  |
|micronaut-data-jdbc   |`—`   |Compile-time generated → execute                 |
|micronaut-data-jpa    |`—`   |Compile-time → Hibernate → execute               |
|helidon-jpa           |`—`   |CDI → JPA → Hibernate → execute                  |

### Allocations per Query

|Subject              |Bytes/Op|Objects/Op|
|---------------------|-------:|---------:|
|jdbc-raw             |        |          |
|jdbc-hikari          |        |          |
|hibernate-standalone |        |          |
|spring-data-jpa      |        |          |
|spring-data-jdbc     |        |          |
|spring-jooq          |        |          |
|spring-r2dbc         |        |          |
|quarkus-panache      |        |          |
|quarkus-panache-rx   |        |          |
|micronaut-data-jdbc  |        |          |
|micronaut-data-jpa   |        |          |
|helidon-jpa          |        |          |

### Cold Start

|Subject              |Time (ms)|Notes                                    |
|---------------------|--------:|-----------------------------------------|
|jdbc-raw             |         |No init                                  |
|jdbc-hikari          |         |Pool init only                           |
|hibernate-standalone |         |Schema validation, L2 cache init         |
|spring-data-jpa      |         |ApplicationContext + Hibernate            |
|spring-data-jdbc     |         |ApplicationContext                        |
|spring-jooq          |         |ApplicationContext + jOOQ DSL context     |
|spring-r2dbc         |         |ApplicationContext + R2DBC pool           |
|quarkus-panache      |         |Build-time optimized startup              |
|quarkus-panache-rx   |         |Build-time optimized + Vert.x pool       |
|micronaut-data-jdbc  |         |Compile-time DI                          |
|micronaut-data-jpa   |         |Compile-time DI + Hibernate              |
|helidon-jpa          |         |CDI + JPA init                           |

-----

## 4 · Crossover Report

### ORM Overhead vs. Network Latency

|Subject              |Overhead (μs)|Negligible When RTT >|
|---------------------|------------:|---------------------|
|hibernate-standalone |             |                     |
|spring-data-jpa      |             |                     |
|spring-data-jdbc     |             |                     |
|spring-jooq          |             |                     |
|spring-r2dbc         |             |                     |
|quarkus-panache      |             |                     |
|quarkus-panache-rx   |             |                     |
|micronaut-data-jdbc  |             |                     |
|micronaut-data-jpa   |             |                     |
|helidon-jpa          |             |                     |

### Query Complexity Scaling

|Subject              |PK Lookup|3-Table Join|Aggregation|Pattern|
|---------------------|--------:|-----------:|----------:|-------|
|hibernate-standalone |         |            |           |       |
|spring-data-jpa      |         |            |           |       |
|spring-jooq          |         |            |           |       |
|quarkus-panache      |         |            |           |       |
|micronaut-data-jdbc  |         |            |           |       |
|helidon-jpa          |         |            |           |       |

-----

## 5 · Feature Matrix

|Feature          |hibernate|spring-jpa|spring-jdbc|spring-jooq|spring-r2dbc|quarkus|quarkus-rx|mn-jdbc|mn-jpa|helidon|
|-----------------|---------|----------|-----------|-----------|------------|-------|----------|-------|------|-------|
|CTE (WITH)       |         |          |           |           |            |       |          |       |      |       |
|Window functions |         |          |           |           |            |       |          |       |      |       |
|JSONB containment|         |          |           |           |            |       |          |       |      |       |
|Full-text search |         |          |           |           |            |       |          |       |      |       |
|RETURNING clause |         |          |           |           |            |       |          |       |      |       |
|Keyset pagination|         |          |           |           |            |       |          |       |      |       |
|Batch strategy   |         |          |           |           |            |       |          |       |      |       |

*Values: **native**, **passthrough**, **blocked**, **N/A**.*

-----

## Environment

|Component     |Value            |
|--------------|-----------------|
|**Java**      |`{JAVA_VERSION}` |
|**PostgreSQL**|`{PG_VERSION}`   |
|**MySQL**     |`{MYSQL_VERSION}`|
|**OS**        |`{OS}`           |
|**CPU**       |`{CPU}`          |

-----

## How to Read These Results

**Latency**: p50 is the median, p95/p99 capture tail. If p99 is 10× p50, something is stalling.

**Overhead**: Stack depth, allocations, and GC reveal what the framework spends time on besides your query.

**Crossover**: If your DB has 2ms RTT, a 50μs framework overhead is noise. If local, that 50μs is half your query time.

**Feature matrix**: If you use Postgres for JSONB/FTS, a framework that can't express those natively costs more than latency alone suggests.

-----

*Results generated by CI. Reproduce: clone, docker compose up, ./scripts/run-benchmarks.sh*
