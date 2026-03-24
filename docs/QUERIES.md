# Query Catalog

Every query that the benchmark suite executes, defined with exact SQL, intent, and the method signature each OrmAdapter must implement. This is the contract between the harness and the subjects.

## OrmAdapter Interface

Each subject implements this interface. The harness calls these methods; the subject translates them into framework-idiomatic code.

```java
public interface OrmAdapter extends AutoCloseable {

    // --- Lifecycle ---
    void setup(DataSource ds);     // called once before benchmarks
    void teardown();               // called once after benchmarks

    // --- Read: Simple ---
    User findUserById(long id);
    List<User> findUsersByDepartment(String department, int limit);

    // --- Read: Join ---
    List<OrderWithItems> findOrdersWithItems(long userId);

    // --- Read: Aggregation ---
    List<UserSpendSummary> findTopSpenders(String department, int minOrders);

    // --- Read: CTE ---
    List<UserSpendSummary> findTopSpendersCte(String department, int minOrders);

    // --- Read: Projection ---
    List<UserSummary> findUserSummaries(String department);

    // --- Read: Window Function ---
    List<UserWithRank> findUsersRankedByDepartment(List<String> departments);

    // --- Read: Postgres-specific ---
    List<Product> findProductsByJsonAttribute(String key, String value);  // JSONB
    List<Product> searchProducts(String query);                          // FTS

    // --- Write: Batch ---
    void batchInsertUsers(List<User> users);
    void batchUpdateOrderStatus(String fromStatus, String toStatus);
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

### Q2: Filter + Sort

**Intent**: Indexed WHERE clause with ORDER BY. Tests whether the framework generates a query that uses the index or forces a scan.

**SQL**:

```sql
SELECT id, name, email, department, created_at
FROM users
WHERE department = ?
ORDER BY created_at DESC
LIMIT ?
```

**Method**: `findUsersByDepartment(String department, int limit)`

**Parameters**: Random department from the 8 seeded departments. Limit = 20.

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

### Q4: Aggregation

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

### Q5: CTE

**Intent**: Same business logic as Q4, but expressed as a CTE. Tests whether the framework can express WITH clauses natively or forces a workaround.

**SQL**:

```sql
WITH user_orders AS (
    SELECT user_id,
           COUNT(*) AS order_count,
           SUM(total) AS total_spend,
           MAX(placed_at) AS last_order
    FROM orders
    GROUP BY user_id
)
SELECT u.id, u.name, u.email,
       uo.order_count, uo.total_spend, uo.last_order
FROM users u
JOIN user_orders uo ON u.id = uo.user_id
WHERE u.department = ?
  AND uo.order_count > ?
ORDER BY uo.total_spend DESC
```

**Method**: `findTopSpendersCte(String department, int minOrders)`

**Parameters**: Same as Q4.

**Expected result**: Same as Q4 (validates that CTE produces identical results).

**Note**: Frameworks that cannot express CTEs natively should use raw SQL passthrough. The benchmark records whether native or passthrough was used.

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

### Q7: JSONB Query (Postgres only)

**Intent**: GIN-indexed JSONB containment query. Tests whether the framework can express database-native JSON operators or forces raw SQL.

**SQL**:

```sql
SELECT id, name, category, price, attributes::text
FROM products
WHERE attributes @> ?::jsonb
```

**Method**: `findProductsByJsonAttribute(String key, String value)`

**Parameters**: Random key-value pair from the seeded attribute pool. The method constructs the JSONB literal (`{"color": "red"}`).

**Expected result**: List of Product records matching the containment query.

**Frameworks that can't express `@>`**: Use raw SQL. The benchmark records this.

-----

### Q8: Full-Text Search (Postgres only)

**Intent**: tsvector/tsquery search using the GIN index. Tests whether the framework supports FTS or obstructs it.

**SQL**:

```sql
SELECT id, name, category, price, attributes::text
FROM products
WHERE search_doc @@ plainto_tsquery('english', ?)
ORDER BY ts_rank(search_doc, plainto_tsquery('english', ?)) DESC
LIMIT 20
```

**Method**: `searchProducts(String query)`

**Parameters**: Random word from the product name word list.

**Expected result**: Up to 20 Product records ranked by relevance.

-----

### Q9: Batch Insert

**Intent**: Bulk write at three scales (100, 1K, 10K rows). Tests batching strategy — does the framework use multi-row INSERT, JDBC batching, or individual statements?

**SQL** (JDBC baseline):

```sql
INSERT INTO users (name, email, department, created_at)
VALUES (?, ?, ?, ?)
-- batched via PreparedStatement.addBatch()
```

**Method**: `batchInsertUsers(List<User> users)`

**Parameters**: Pre-generated list of User records (ids are null — database assigns). Sizes: 100, 1000, 10000. Run as three separate benchmark methods.

**Cleanup**: Inserted rows are deleted after each invocation to keep the table at baseline size.

-----

### Q10: Batch Update

**Intent**: Bulk conditional update. Tests dirty checking overhead (Hibernate) vs. direct UPDATE.

**SQL**:

```sql
UPDATE orders
SET status = ?
WHERE status = ?
```

**Method**: `batchUpdateOrderStatus(String fromStatus, String toStatus)`

**Parameters**: fromStatus = 'PENDING', toStatus = 'CONFIRMED'. After each invocation, reset the affected rows back to PENDING.

**Expected result**: Number of rows updated (verified against expected count from seed data).

-----

### Q11: Window Function

**Intent**: Ranked results using a window function. Tests whether the framework can express OVER/PARTITION BY natively or forces raw SQL.

**SQL**:

```sql
SELECT id, name, department,
       ROW_NUMBER() OVER (PARTITION BY department ORDER BY created_at DESC) AS dept_rank
FROM users
WHERE department IN (?, ?, ?)
```

**Method**: `findUsersRankedByDepartment(List<String> departments)`

**Parameters**: Three random departments from the seeded set.

**Expected result**: List of UserWithRank records. Each user has their rank within their department.

**Projection record**:

```java
public record UserWithRank(
    Long id,
    String name,
    String department,
    int deptRank
) {}
```

**Note**: This query directly populates the "Window functions" row in the Database Feature Utilization matrix.

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

This metadata feeds into the Database Feature Utilization section of the results gist.
