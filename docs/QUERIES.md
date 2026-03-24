# Query Catalog

Every query that the benchmark suite executes, defined with exact SQL, intent, and the method signature each OrmAdapter must implement. This is the contract between the harness and the subjects.

## OrmAdapter Interface

Each subject implements this interface. The harness calls these methods; the subject translates them into framework-idiomatic code.

```java
public interface OrmAdapter extends AutoCloseable {

    // --- Lifecycle ---
    void setup(DataSource ds);
    void teardown();

    // --- Read: Simple ---
    User findUserById(long id);
    List<User> findUsersByDepartment(String department, String sortBy, int limit, int offset);

    // --- Read: Join ---
    List<OrderWithItems> findOrdersWithItems(long userId);

    // --- Read: N+1 vs Eager ---
    List<Order> findOrdersNaive(String status);       // N+1: loads orders, then accesses user per order
    List<Order> findOrdersOptimized(String status);   // Eager: loads orders + users in one query

    // --- Read: Aggregation ---
    List<UserSpendSummary> findTopSpenders(String department, int minOrders);

    // --- Read: Projection ---
    List<UserSummary> findUserSummaries(String department);

    // --- Read: Pagination at Depth ---
    List<User> findUsersPaginated(int pageSize, int pageNumber);  // OFFSET-based
    List<User> findUsersAfter(long lastSeenId, int pageSize);     // Keyset-based (if supported)

    // --- Write: Single ---
    long insertUser(User user);  // returns generated ID

    // --- Write: Batch ---
    void batchInsertUsers(List<User> users);
    void batchUpdateOrderStatus(String fromStatus, String toStatus);

    // --- Feature Matrix (not timed, capability check only) ---
    default boolean supportsCte() { return false; }
    default boolean supportsWindowFunctions() { return false; }
    default boolean supportsJsonbContainment() { return false; }
    default boolean supportsFullTextSearch() { return false; }
    default boolean supportsReturningClause() { return false; }
    default boolean supportsKeysetPagination() { return false; }
    default String batchInsertStrategy() { return "unknown"; }  // "multi_row", "add_batch", "individual"
}
```

## Query Definitions

### Q1: PK Lookup

**Intent**: Single-row fetch by primary key. The absolute baseline — every framework should be near-JDBC on this.

**SQL**:

```sql
SELECT id, name, email, department, created_at
FROM users
WHERE id = ?
```

**Method**: `findUserById(long id)`

**Parameters**: Random id from seeded users (uniform distribution).

**Expected result**: Exactly one User record.

-----

### Q2: Filter + Sort + Paginate

**Intent**: Indexed WHERE clause with ORDER BY and LIMIT/OFFSET. The most common production read pattern. Tests whether the framework generates a query that uses the index or forces a scan, and how it handles pagination parameters.

**SQL**:

```sql
SELECT id, name, email, department, created_at
FROM users
WHERE department = ?
ORDER BY created_at DESC
LIMIT ? OFFSET ?
```

**Method**: `findUsersByDepartment(String department, String sortBy, int limit, int offset)`

**Parameters**: Random department from the 8 seeded departments. limit = 20, offset = 0.

**Expected result**: Up to 20 User records, sorted by created_at descending.

-----

### Q3: Multi-table Join

**Intent**: 3-table join with projection to a non-entity type. Tests result mapping overhead, especially how frameworks handle joins that don't map cleanly to a single entity.

**SQL**:

```sql
SELECT o.id AS order_id, o.total, o.status,
       u.name AS user_name,
       p.name AS product_name,
       oi.quantity, oi.price AS item_price
FROM orders o
JOIN users u ON o.user_id = u.id
JOIN order_items oi ON oi.order_id = o.id
JOIN products p ON oi.product_id = p.id
WHERE o.user_id = ?
ORDER BY o.placed_at DESC
```

**Method**: `findOrdersWithItems(long userId)`

**Parameters**: Random userId that has orders.

**Expected result**: List of OrderWithItems records (flattened — one row per item, not nested).

-----

### Q4: N+1 vs Eager Load

**Intent**: The #1 ORM performance problem. Benchmarks both the naive case (N+1 queries) and the optimized case (JOIN FETCH / eager load). Measures the cost of getting it wrong and the framework's ability to fix it.

**Naive (N+1):**

```sql
-- Query 1: fetch orders
SELECT id, user_id, total, status, placed_at FROM orders WHERE status = ? LIMIT 100
-- Then for EACH order, a separate query:
SELECT id, name, email, department, created_at FROM users WHERE id = ?
-- Total: 101 queries
```

**Optimized (Eager/JOIN FETCH):**

```sql
SELECT o.id, o.user_id, o.total, o.status, o.placed_at,
       u.id, u.name, u.email, u.department, u.created_at
FROM orders o
JOIN users u ON o.user_id = u.id
WHERE o.status = ?
LIMIT 100
-- Total: 1 query
```

**Methods**: `findOrdersNaive(String status)` and `findOrdersOptimized(String status)`

**Parameters**: status = 'SHIPPED' (the most common status at 30%).

**Expected result**: Both methods return the same data. The benchmark shows the cost of N+1 vs the cost of a single JOIN. For raw JDBC, both are implemented manually. For Hibernate, naive = lazy loading default, optimized = JOIN FETCH or entity graph. For Micronaut Data JDBC, there's no lazy loading so both should be equivalent.

**Why this matters**: This is the single most impactful ORM performance difference in real applications. A developer choosing between frameworks needs to know: how bad is the default? How easy is the fix?

-----

### Q5: Aggregation

**Intent**: GROUP BY with HAVING, aggregate functions, and a join. Tests whether the framework pushes computation to the database or pulls rows and aggregates in Java.

**SQL**:

```sql
SELECT u.id, u.name, u.email,
       COUNT(o.id) AS order_count,
       SUM(o.total) AS total_spend,
       MAX(o.placed_at) AS last_order
FROM users u
JOIN orders o ON o.user_id = u.id
WHERE u.department = ?
GROUP BY u.id, u.name, u.email
HAVING COUNT(o.id) > ?
ORDER BY total_spend DESC
```

**Method**: `findTopSpenders(String department, int minOrders)`

**Parameters**: Random department. minOrders = 3.

**Expected result**: List of UserSpendSummary records.

-----

### Q6: Projection

**Intent**: Fetching a subset of columns to a DTO. Tests whether the framework can avoid fetching and mapping unused columns.

**SQL**:

```sql
SELECT id, name, email
FROM users
WHERE department = ?
```

**Method**: `findUserSummaries(String department)`

**Parameters**: Random department.

**Expected result**: List of UserSummary records (3 fields, not the full User).

-----

### Q7: Single Insert

**Intent**: INSERT one row and return the generated ID. The most common write operation. Tests ID generation strategy, RETURNING clause usage, and unnecessary round-trips.

**SQL**:

```sql
INSERT INTO bench_users (name, email, department, created_at) VALUES (?, ?, ?, ?)
RETURNING id
```

**Method**: `insertUser(User user)` — returns generated `long id`.

**Parameters**: Pre-generated User record (id is null — database assigns).

**Cleanup**: Benchmark inserts target a dedicated `bench_users` table (identical schema to `users`). The table is truncated in `@TearDown(Level.Invocation)`, which JMH excludes from measurement. This prevents table growth during warmup and avoids mixing DELETE overhead into INSERT measurements.

**What we measure**: Round-trip cost for a single write. Whether the framework uses RETURNING or a separate SELECT. Whether it opens a transaction unnecessarily. The write-path baseline — batch insert numbers are meaningless without this.

-----

### Q8: Batch Insert

**Intent**: Bulk write at three scales (100, 1K, 10K rows). Tests batching strategy — does the framework use multi-row INSERT, JDBC batching, or individual statements?

**SQL** (JDBC baseline):

```sql
INSERT INTO bench_users (name, email, department, created_at)
VALUES (?, ?, ?, ?)
-- batched via PreparedStatement.addBatch()
```

**Method**: `batchInsertUsers(List<User> users)`

**Parameters**: Pre-generated list of User records (ids are null — database assigns). Sizes: 100, 1000, 10000. Run as three separate benchmark methods.

**Cleanup**: Same as Q7 — inserts target `bench_users`, truncated in `@TearDown(Level.Invocation)`.

-----

### Q9: Batch Update

**Intent**: Bulk conditional update. Tests dirty checking overhead (Hibernate) vs. direct UPDATE.

**SQL**:

```sql
UPDATE orders
SET status = ?
WHERE status = ?
```

**Method**: `batchUpdateOrderStatus(String fromStatus, String toStatus)`

**Parameters**: fromStatus = 'PENDING', toStatus = 'CONFIRMED'. Rows are reset back to 'PENDING' in `@TearDown(Level.Invocation)` so each measurement starts from the same state.

**Expected result**: Number of rows updated (verified against expected count from seed data).

-----

### Q10: Pagination at Depth

**Intent**: OFFSET pagination at increasing depth, showing degradation. Keyset pagination comparison where supported.

**OFFSET-based:**

```sql
SELECT id, name, email, department, created_at
FROM users
ORDER BY id
LIMIT 20 OFFSET ?
-- Run at offset 0 (page 1), 1980 (page 100), 19980 (page 1000)
```

**Keyset-based:**

```sql
SELECT id, name, email, department, created_at
FROM users
WHERE id > ?
ORDER BY id
LIMIT 20
-- lastSeenId from previous page
```

**Methods**: `findUsersPaginated(int pageSize, int pageNumber)` and `findUsersAfter(long lastSeenId, int pageSize)`

**Parameters**: pageSize = 20. Benchmarked at page 1, 100, 1000.

**What we measure**: How OFFSET pagination degrades at depth. Whether keyset pagination avoids that degradation. Whether the framework supports keyset natively or requires manual SQL. At page 1000 with 10K users, OFFSET=19980 forces the DB to scan and skip rows — this is where the difference becomes dramatic.

## Feature Matrix (Capability Check)

After all benchmark queries, the harness runs a capability check for each subject. These are **not timed** — they test whether the framework can express the feature natively, requires raw SQL passthrough, or is blocked entirely.

```java
// Called once per subject, not timed
FeatureMatrix matrix = new FeatureMatrix();
matrix.cte = adapter.supportsCte();
matrix.windowFunctions = adapter.supportsWindowFunctions();
matrix.jsonbContainment = adapter.supportsJsonbContainment();
matrix.fullTextSearch = adapter.supportsFullTextSearch();
matrix.returningClause = adapter.supportsReturningClause();
matrix.keysetPagination = adapter.supportsKeysetPagination();
matrix.batchStrategy = adapter.batchInsertStrategy();
```

Each subject's adapter returns honest answers. The harness does NOT infer these — the implementer declares them.

**Output in results JSON**:

```json
{
  "subject": "spring-data-jpa",
  "feature_matrix": {
    "cte": "passthrough",
    "window_functions": "passthrough",
    "jsonb_containment": "passthrough",
    "full_text_search": "passthrough",
    "returning_clause": "blocked",
    "keyset_pagination": "native",
    "batch_insert_strategy": "framework_managed"
  }
}
```

Values: `native` (DSL/API support), `passthrough` (raw SQL required), `blocked` (framework interferes), `not_applicable`.

## Parameter Randomization

All random parameters use a deterministic PRNG seeded with a fixed value per benchmark iteration. This ensures:

1. Every subject receives the same sequence of parameters within a run
1. Results are comparable across subjects
1. Runs are reproducible

The PRNG seed is recorded in the results JSON alongside the benchmark data.

## Recording Framework Capabilities

For each query, the harness records not just the timing but also how the framework expressed the query:

|Capability        |Values                                                             |
|------------------|-------------------------------------------------------------------|
|`query_expression`|`native_dsl`, `annotation_query`, `raw_sql`, `not_supported`       |
|`result_mapping`  |`automatic`, `manual_mapper`, `raw_resultset`                      |
|`batch_strategy`  |`jdbc_batch`, `multi_row_insert`, `individual`, `framework_managed`|

This metadata feeds into the Feature Matrix section of the results.
