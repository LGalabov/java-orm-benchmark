# java-orm-benchmark results

> Last updated: `{TIMESTAMP}` · Run ID: `{RUN_ID}` · [Source](https://github.com/LGalabov/java-orm-benchmark) · [Reproduce](#environment)

-----

## At a Glance

|                            |JDBC|Hibernate 7|jOOQ 3.x|Micronaut Data|JDBI 3|MyBatis 3|
|----------------------------|:--:|:---------:|:------:|:------------:|:----:|:-------:|
|**PK Lookup** (μs p50)      |`—` |`—`        |`—`     |`—`           |`—`   |`—`      |
|**3-Table Join** (μs p50)   |`—` |`—`        |`—`     |`—`           |`—`   |`—`      |
|**CTE** (μs p50)            |`—` |`—`        |`—`     |`—`           |`—`   |`—`      |
|**Batch 1K Insert** (ms p50)|`—` |`—`        |`—`     |`—`           |`—`   |`—`      |
|**Stack Depth** (frames)    |`—` |`—`        |`—`     |`—`           |`—`   |`—`      |
|**Alloc/Query** (bytes)     |`—` |`—`        |`—`     |`—`           |`—`   |`—`      |
|**Cold Start** (ms)         |`—` |`—`        |`—`     |`—`           |`—`   |`—`      |

*All latency numbers are PostgreSQL 17. See per-database breakdown below.*

-----

## 1 · Latency — PostgreSQL 17

### Read Queries

|Query Type            |JDBC|Hibernate 7|jOOQ 3.x|Micronaut Data|JDBI 3|MyBatis 3|
|----------------------|---:|----------:|-------:|-------------:|-----:|--------:|
|PK Lookup — p50       |    |           |        |              |      |         |
|PK Lookup — p95       |    |           |        |              |      |         |
|PK Lookup — p99       |    |           |        |              |      |         |
|Filter + Sort — p50   |    |           |        |              |      |         |
|3-Table Join — p50    |    |           |        |              |      |         |
|Aggregation — p50     |    |           |        |              |      |         |
|Projection — p50      |    |           |        |              |      |         |
|CTE — p50             |    |           |        |              |      |         |
|Window Function — p50 |    |           |        |              |      |         |
|JSONB Query — p50     |    |           |        |              |      |         |
|Full-Text Search — p50|    |           |        |              |      |         |

### Write Queries

|Query Type              |JDBC|Hibernate 7|jOOQ 3.x|Micronaut Data|JDBI 3|MyBatis 3|
|------------------------|---:|----------:|-------:|-------------:|-----:|--------:|
|Batch Insert (100) — p50|    |           |        |              |      |         |
|Batch Insert (1K) — p50 |    |           |        |              |      |         |
|Batch Insert (10K) — p50|    |           |        |              |      |         |
|Batch Update — p50      |    |           |        |              |      |         |

*All times in microseconds unless noted. Lower is better.*

## 2 · Latency — MySQL 9.x

*Same structure, JSONB and FTS rows omitted.*

-----

## 3 · Overhead Analysis

### Stack Depth

|Framework      |Frames|Notes                                            |
|---------------|-----:|-------------------------------------------------|
|JDBC (baseline)|`—`   |Direct call                                      |
|Hibernate 7    |`—`   |Proxy → context → flush check → SQL gen → execute|
|jOOQ 3.x       |`—`   |DSL → render → bind → execute                    |

### Allocations per Query

|Framework  |Bytes/Op|Objects/Op|
|-----------|-------:|---------:|
|JDBC       |        |          |
|Hibernate 7|        |          |
|jOOQ 3.x   |        |          |

### Cold Start

|Framework  |Time (ms)|Notes                                    |
|-----------|--------:|-----------------------------------------|
|JDBC       |         |                                         |
|Hibernate 7|         |Includes schema validation, L2 cache init|

-----

## 4 · Crossover Report

### ORM Overhead vs. Network Latency

|Framework  |Overhead (μs)|Negligible When RTT >|
|-----------|------------:|---------------------|
|Hibernate 7|             |                     |
|jOOQ 3.x   |             |                     |

### Query Complexity Scaling

|Framework  |PK Lookup|3-Table Join|CTE|Pattern|
|-----------|--------:|-----------:|--:|-------|
|Hibernate 7|         |            |   |       |
|jOOQ 3.x   |         |            |   |       |

-----

## 5 · Database Feature Utilization

|Feature          |Hibernate 7|jOOQ 3.x|Micronaut Data|JDBI 3|MyBatis 3|
|-----------------|-----------|--------|--------------|------|---------|
|JSONB containment|           |        |              |      |         |
|Full-text search |           |        |              |      |         |
|CTE              |           |        |              |      |         |
|Window functions |           |        |              |      |         |
|Batch insert     |           |        |              |      |         |
|RETURNING clause |           |        |              |      |         |

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

**Feature utilization**: If you use Postgres for JSONB/FTS, a framework that can't express those natively costs more than latency alone suggests.

-----

*Results generated by CI. Reproduce: clone, docker compose up, ./scripts/run-benchmarks.sh*
