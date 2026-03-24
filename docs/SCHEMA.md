# Schema Specification

This document defines the database schema, seed data strategy, and index design for all benchmarks. It is the single source of truth — all subject implementations and the harness itself derive from this.

## Tables

### PostgreSQL

```sql
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    department  VARCHAR(50),
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    category    VARCHAR(50) NOT NULL,
    price       NUMERIC(10,2) NOT NULL,
    attributes  JSONB,
    search_doc  TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('english', name || ' ' || category)
    ) STORED
);

CREATE TABLE orders (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    total       NUMERIC(10,2) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    placed_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders(id),
    product_id  BIGINT NOT NULL REFERENCES products(id),
    quantity    INT NOT NULL,
    price       NUMERIC(10,2) NOT NULL
);
```

### MySQL

Same structure minus Postgres-specific features:

```sql
CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    department  VARCHAR(50),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    category    VARCHAR(50) NOT NULL,
    price       DECIMAL(10,2) NOT NULL,
    attributes  JSON
);

CREATE TABLE orders (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    total       DECIMAL(10,2) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    placed_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE order_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT NOT NULL,
    product_id  BIGINT NOT NULL,
    quantity    INT NOT NULL,
    price       DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
```

## Indexes

### PostgreSQL

```sql
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_placed_at ON orders(placed_at);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_attrs ON products USING GIN (attributes);
CREATE INDEX idx_products_search ON products USING GIN (search_doc);
```

### MySQL

```sql
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_placed_at ON orders(placed_at);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
CREATE INDEX idx_products_category ON products(category);
```

## Seed Data

### Volumes

|Table      |Rows   |Rationale                                  |
|-----------|------:|-------------------------------------------|
|users      |10,000 |Large enough for join cardinality to matter|
|products   |5,000  |Varied categories and JSONB attributes     |
|orders     |100,000|~10 per user, mixed statuses               |
|order_items|300,000|~3 per order                               |

### Data Generation Rules

**users**: Names from a fixed word list (deterministic). Emails derived from name + sequential suffix. Departments drawn from: Engineering, Sales, Marketing, Support, Finance, Product, Legal, HR (uniform distribution). created_at spread across the last 2 years.

**products**: Names from a product word list. Categories: Electronics, Clothing, Books, Home, Sports, Food, Toys, Tools. price: uniform 1.00–999.99. attributes (Postgres only): JSONB object with 2–5 random key-value pairs from a fixed attribute pool (color, size, weight, material, brand, rating). This ensures GIN index queries have realistic selectivity.

**orders**: user_id: uniform random across users. status: PENDING (10%), CONFIRMED (20%), SHIPPED (30%), DELIVERED (35%), CANCELLED (5%). total: derived from order_items after generation (sum of quantity * price). placed_at: spread across the last 2 years, correlated with user's created_at (orders come after user creation).

**order_items**: order_id: sequential across orders. product_id: uniform random. quantity: 1–10. price: copied from product at generation time (snapshot).

### Determinism

Seed data generation uses a fixed seed (`42`). The same seed must produce byte-identical data across runs. This is enforced in CI.

### Implementation

Seed data is generated by a shared `DataSeeder` class in the `common` module. It writes directly via JDBC (not through any ORM) to avoid framework-specific behavior affecting the baseline data state.

The seeder is called once per benchmark suite invocation, not per benchmark method. All subjects operate on the same pre-seeded data.

## Domain Model (Java Records)

These records live in `common/` and are the canonical types that all subjects map query results to.

```java
public record User(
    Long id,
    String name,
    String email,
    String department,
    Instant createdAt
) {}

public record Product(
    Long id,
    String name,
    String category,
    BigDecimal price,
    String attributes  // raw JSON string — no framework-specific JSONB type
) {}

public record Order(
    Long id,
    Long userId,
    BigDecimal total,
    String status,
    Instant placedAt
) {}

public record OrderItem(
    Long id,
    Long orderId,
    Long productId,
    int quantity,
    BigDecimal price
) {}
```

### Projection Records

```java
public record UserSummary(
    Long id,
    String name,
    String email
) {}

public record UserSpendSummary(
    Long id,
    String name,
    String email,
    int orderCount,
    BigDecimal totalSpend,
    Instant lastOrder
) {}

public record OrderWithItems(
    Long orderId,
    BigDecimal total,
    String status,
    String userName,
    String productName,
    int quantity,
    BigDecimal itemPrice
) {}

public record UserWithRank(
    Long id,
    String name,
    String department,
    int deptRank
) {}
```
