# Test Subjects

Four independent dimensions. Each subject is a coordinate across all four.

## Databases

|Database  |Version|Image        |
|----------|-------|-------------|
|PostgreSQL|17     |`postgres:17`|
|MySQL     |9.1    |`mysql:9.1`  |

## Frameworks

|Framework        |Version|Notes                                |
|-----------------|-------|-------------------------------------|
|None (standalone)|—      |No framework overhead. Raw baseline. |
|Spring Boot      |4.x    |Dominant market share.               |
|Quarkus          |3.x    |Build-time optimization. Agroal pool.|
|Micronaut        |4.x    |Compile-time DI/AOP.                 |
|Helidon          |4.x    |Oracle-backed. Virtual-thread native.|

## Persistence Options

|Persistence        |Type                                  |Available In       |
|-------------------|--------------------------------------|-------------------|
|Raw JDBC           |Direct SQL                            |All                |
|Hibernate 7        |Full ORM (JPA)                        |All                |
|Hibernate Reactive |Reactive ORM                          |Quarkus, standalone|
|jOOQ 3.x           |Type-safe SQL DSL                     |All                |
|Spring Data JPA    |Repository over Hibernate             |Spring Boot        |
|Spring Data JDBC   |Lightweight repository, no ORM        |Spring Boot        |
|Spring Data R2DBC  |Reactive repository                   |Spring Boot        |
|Micronaut Data JDBC|Compile-time repository, no ORM       |Micronaut          |
|Micronaut Data JPA |Compile-time repository over Hibernate|Micronaut          |
|Panache (blocking) |Active record over Hibernate          |Quarkus            |
|Panache (reactive) |Active record over Hibernate Reactive |Quarkus            |

## Connection Pools

|Pool           |Type             |Used By                             |
|---------------|-----------------|------------------------------------|
|HikariCP       |Blocking JDBC    |Spring Boot default, standalone     |
|Agroal         |Blocking JDBC    |Quarkus default                     |
|Vert.x reactive|Non-JDBC reactive|Hibernate Reactive, Quarkus reactive|
|R2DBC pool     |Reactive         |Spring Data R2DBC                   |

## Baseline Subjects (12)

|# |ID                        |Framework    |Persistence                   |Pool                |
|--|--------------------------|-------------|------------------------------|--------------------|
|1 |`jdbc-raw`                |None         |Raw JDBC                      |None (DriverManager)|
|2 |`jdbc-hikari`             |None         |Raw JDBC                      |HikariCP            |
|3 |`hibernate-standalone`    |None         |Hibernate 7                   |HikariCP            |
|4 |`spring-data-jpa`         |Spring Boot 4|Spring Data JPA (Hibernate)   |HikariCP            |
|5 |`spring-data-jdbc`        |Spring Boot 4|Spring Data JDBC              |HikariCP            |
|6 |`spring-jooq`             |Spring Boot 4|jOOQ                          |HikariCP            |
|7 |`spring-r2dbc`            |Spring Boot 4|Spring Data R2DBC             |R2DBC pool          |
|8 |`quarkus-panache`         |Quarkus 3    |Panache (Hibernate)           |Agroal              |
|9 |`quarkus-panache-reactive`|Quarkus 3    |Panache Reactive              |Vert.x              |
|10|`micronaut-data-jdbc`     |Micronaut 4  |Micronaut Data JDBC           |HikariCP            |
|11|`micronaut-data-jpa`      |Micronaut 4  |Micronaut Data JPA (Hibernate)|HikariCP            |
|12|`helidon-jpa`             |Helidon 4    |JPA (Hibernate)               |HikariCP            |

### What Each Subject Answers

|Subject                   |Question                                         |
|--------------------------|-------------------------------------------------|
|`jdbc-raw`                |Absolute floor — no pool, no framework, no ORM   |
|`jdbc-hikari`             |Cost or benefit of connection pooling alone      |
|`hibernate-standalone`    |Pure Hibernate cost without any framework        |
|`spring-data-jpa`         |Cost of the most popular Java persistence stack  |
|`spring-data-jdbc`        |Does dropping the ORM in Spring help?            |
|`spring-jooq`             |Does type-safe SQL beat repository abstraction?  |
|`spring-r2dbc`            |Does reactive Spring beat blocking Spring?       |
|`quarkus-panache`         |Does Quarkus build-time optimization beat Spring?|
|`quarkus-panache-reactive`|Does Quarkus reactive beat Quarkus blocking?     |
|`micronaut-data-jdbc`     |Does compile-time SQL generation beat everything?|
|`micronaut-data-jpa`      |Is it Micronaut or the ORM that matters?         |
|`helidon-jpa`             |Does virtual-thread-native change the numbers?   |

### Comparisons Enabled

**Framework comparison (same ORM):** spring-data-jpa vs quarkus-panache vs micronaut-data-jpa vs helidon-jpa — all Hibernate underneath.

**ORM comparison (same framework):** Spring: Data JPA vs Data JDBC vs jOOQ vs R2DBC. Micronaut: Data JDBC vs Data JPA.

**Blocking vs reactive:** spring-data-jpa vs spring-r2dbc. quarkus-panache vs quarkus-panache-reactive.

**Pool comparison:** jdbc-raw vs jdbc-hikari. HikariCP vs Agroal vs Vert.x across frameworks.

**Framework overhead vs standalone:** hibernate-standalone vs spring-data-jpa vs quarkus-panache — same ORM, different wrappers.

## Module Structure

```
subjects/
├── jdbc-raw/
├── jdbc-hikari/
├── hibernate-standalone/
├── spring-data-jpa/
├── spring-data-jdbc/
├── spring-jooq/
├── spring-r2dbc/
├── quarkus-panache/
├── quarkus-panache-reactive/
├── micronaut-data-jdbc/
├── micronaut-data-jpa/
└── helidon-jpa/
```

## Implementation Notes

**Reactive subjects** (`spring-r2dbc`, `quarkus-panache-reactive`): The adapter blocks internally to present a synchronous interface to the harness. Blocking cost is excluded from measurement.

**Spring Boot subjects**: Minimal app with only the persistence starter. No web, no security. `setup()` creates ApplicationContext; `teardown()` closes it. Cold start includes context init.

**Quarkus subjects**: Panache entities extend `PanacheEntity`. Reactive variant returns `Uni<T>`.

**Micronaut subjects**: Repositories use `@JdbcRepository` or `@Repository`. Data JDBC generates SQL at compile time — expect smallest gap from raw JDBC.

**Helidon subjects**: Helidon MP with CDI. JPA via `@PersistenceContext`. Helidon 4 runs on virtual threads by default.

## Build Order

|Phase|Subjects                                            |When               |
|-----|----------------------------------------------------|-------------------|
|S1–S5|`jdbc-raw`, `jdbc-hikari`                           |Core pipeline      |
|S6   |`hibernate-standalone`                              |First ORM          |
|S7   |(overhead profiling)                                |                   |
|S8   |`spring-data-jpa`, `spring-data-jdbc`, `spring-jooq`|Spring trio        |
|S9   |`spring-r2dbc`                                      |Reactive comparison|
|S10a |`quarkus-panache`, `quarkus-panache-reactive`       |Quarkus pair       |
|S10b |`micronaut-data-jdbc`, `micronaut-data-jpa`         |Micronaut pair     |
|S10c |`helidon-jpa`                                       |Last framework     |
