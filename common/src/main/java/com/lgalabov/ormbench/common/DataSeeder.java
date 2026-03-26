package com.lgalabov.ormbench.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public final class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final long SEED = 42L;
    private static final int BATCH_SIZE = 1000;

    private static final int USER_COUNT = 10_000;
    private static final int PRODUCT_COUNT = 5_000;
    private static final int ORDER_COUNT = 100_000;
    private static final int ITEMS_PER_ORDER = 3;

    private static final String[] DEPARTMENTS = {
        "Engineering", "Sales", "Marketing", "Support",
        "Finance", "Product", "Legal", "HR"
    };

    private static final String[] CATEGORIES = {
        "Electronics", "Clothing", "Books", "Home",
        "Sports", "Food", "Toys", "Tools"
    };

    private static final String[] FIRST_NAMES = {
        "Alice", "Bob", "Carol", "David", "Eve", "Frank", "Grace", "Henry",
        "Iris", "Jack", "Karen", "Leo", "Mia", "Noah", "Olivia", "Paul",
        "Quinn", "Rose", "Sam", "Tina", "Uma", "Vince", "Wendy", "Xander",
        "Yara", "Zane", "Amber", "Blake", "Clara", "Derek", "Elena", "Felix",
        "Gina", "Hugo", "Ivy", "Joel", "Kara", "Liam", "Maya", "Nate",
        "Opal", "Peter", "Rae", "Seth", "Tara", "Uri", "Vera", "Wade",
        "Xena", "Yuri", "Zara", "Ava", "Beau", "Cleo", "Dale", "Ella",
        "Finn", "Gia", "Hank", "Isla", "Jake", "Kate", "Luke", "Mona",
        "Neil", "Olga", "Phil", "Ruth", "Sean", "Thea", "Ugo", "Val",
        "Will", "Xyla", "York", "Zeke", "Aiden", "Bryn", "Cole", "Dana",
        "Emil", "Faye", "Glen", "Hope", "Ivan", "Jade", "Kyle", "Lily",
        "Mark", "Nina", "Omar", "Page", "Reed", "Sara", "Troy", "Una",
        "Vlad", "Wren", "Xavi", "Yael", "Zion", "Axel"
    };

    private static final String[] LAST_NAMES = {
        "Smith", "Jones", "Brown", "Davis", "Clark", "Lewis", "Young", "Hall",
        "Allen", "King", "Wright", "Scott", "Green", "Baker", "Adams", "Nelson",
        "Hill", "Moore", "White", "Martin", "Thomas", "Taylor", "Anderson", "Jackson",
        "Harris", "Thompson", "Garcia", "Martinez", "Robinson", "Walker", "Lopez", "Lee",
        "Perez", "Wilson", "Turner", "Phillips", "Campbell", "Parker", "Evans", "Edwards",
        "Collins", "Stewart", "Sanchez", "Morris", "Rogers", "Reed", "Cook", "Morgan",
        "Bell", "Murphy", "Bailey", "Rivera", "Cooper", "Richardson", "Cox", "Howard",
        "Ward", "Torres", "Peterson", "Gray", "Ramirez", "James", "Watson", "Brooks",
        "Kelly", "Sanders", "Price", "Bennett", "Wood", "Barnes", "Ross", "Henderson",
        "Coleman", "Jenkins", "Perry", "Powell", "Long", "Patterson", "Hughes", "Flores",
        "Washington", "Butler", "Simmons", "Foster", "Gonzales", "Bryant", "Alexander", "Russell",
        "Griffin", "Diaz", "Hayes", "Myers", "Ford", "Hamilton", "Graham", "Sullivan",
        "Wallace", "Woods", "Cole", "West"
    };

    private static final String[] PRODUCT_ADJECTIVES = {
        "Premium", "Ultra", "Classic", "Pro", "Elite", "Basic", "Deluxe", "Compact",
        "Advanced", "Smart", "Eco", "Turbo", "Mega", "Mini", "Super", "Flex",
        "Rapid", "Prime", "Core", "Max", "Slim", "Bold", "Pure", "Nova",
        "Vivid", "Swift", "Edge", "Apex", "Zen", "Pixel", "Aero", "Terra",
        "Aqua", "Solar", "Lunar", "Fusion", "Spark", "Blaze", "Storm", "Frost",
        "Neon", "Crisp", "Silk", "Onyx", "Jade", "Ruby", "Pearl", "Ivory",
        "Titan", "Atlas"
    };

    private static final String[] PRODUCT_NOUNS = {
        "Widget", "Gadget", "Device", "Module", "Sensor", "Panel", "Board", "Cable",
        "Adapter", "Charger", "Speaker", "Monitor", "Keyboard", "Mouse", "Hub", "Drive",
        "Lamp", "Filter", "Brush", "Roller", "Cutter", "Drill", "Wrench", "Clamp",
        "Jacket", "Shirt", "Pants", "Shoes", "Hat", "Gloves", "Scarf", "Belt",
        "Novel", "Guide", "Manual", "Journal", "Atlas", "Album", "Diary", "Planner",
        "Chair", "Table", "Shelf", "Rack", "Frame", "Mat", "Rug", "Pillow",
        "Ball", "Bat", "Net", "Rope", "Band", "Grip", "Pad", "Guard",
        "Snack", "Blend", "Spice", "Sauce", "Mix", "Pack", "Jar", "Box",
        "Puzzle", "Block", "Figure", "Set", "Kit", "Card", "Dice", "Ring",
        "Saw", "File", "Hook", "Bolt", "Nail", "Tape", "Glue", "Strap",
        "Dock", "Case", "Cover", "Stand", "Mount", "Clip", "Seal", "Valve",
        "Lens", "Scope", "Probe", "Gauge", "Meter", "Timer", "Dial", "Knob",
        "Plate", "Cup", "Tray", "Bin"
    };

    private static final String[] ATTR_KEYS = {
        "color", "size", "weight", "material", "brand", "rating"
    };

    private static final String[][] ATTR_VALUES = {
        {"red", "blue", "green", "black", "white", "silver", "gold", "navy"},
        {"XS", "S", "M", "L", "XL", "XXL", "one-size", "compact"},
        {"100g", "250g", "500g", "1kg", "2kg", "5kg", "10kg", "50g"},
        {"plastic", "metal", "wood", "glass", "ceramic", "fabric", "leather", "rubber"},
        {"Acme", "Zenith", "Apex", "Nova", "Vertex", "Prism", "Orbit", "Flux"},
        {"1.0", "2.0", "2.5", "3.0", "3.5", "4.0", "4.5", "5.0"}
    };

    private static final int[] STATUS_THRESHOLDS = {10, 30, 60, 95, 100};
    private static final String[] STATUSES = {
        "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"
    };

    private DataSeeder() {}

    public static void seed(Connection conn) throws SQLException {
        boolean originalAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            SchemaManager.apply(conn);
            conn.commit();

            Random rng = new Random(SEED);

            Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            Instant twoYearsAgo = now.minus(730, ChronoUnit.DAYS);
            long timeRangeSeconds = now.getEpochSecond() - twoYearsAgo.getEpochSecond();

            Instant[] userCreatedAt = seedUsers(conn, rng, twoYearsAgo, timeRangeSeconds);
            conn.commit();

            BigDecimal[] productPrices = seedProducts(conn, rng);
            conn.commit();

            seedOrdersAndItems(conn, rng, userCreatedAt, productPrices, now);
            conn.commit();

            analyze(conn);
            conn.commit();

            log.info("Seeding complete: {} users, {} products, {} orders, {} order items",
                USER_COUNT, PRODUCT_COUNT, ORDER_COUNT, ORDER_COUNT * ITEMS_PER_ORDER);
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    private static Instant[] seedUsers(Connection conn, Random rng,
                                        Instant twoYearsAgo, long timeRangeSeconds)
            throws SQLException {
        Instant[] createdAts = new Instant[USER_COUNT];
        String sql = "INSERT INTO users (name, email, department, created_at) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < USER_COUNT; i++) {
                String first = FIRST_NAMES[i / LAST_NAMES.length % FIRST_NAMES.length];
                String last = LAST_NAMES[i % LAST_NAMES.length];
                String name = first + " " + last;
                String email = first.toLowerCase() + "." + last.toLowerCase() + "." + i + "@bench.test";
                String dept = DEPARTMENTS[i % DEPARTMENTS.length];
                long offsetSeconds = (long) (rng.nextDouble() * timeRangeSeconds);
                Instant createdAt = twoYearsAgo.plusSeconds(offsetSeconds);
                createdAts[i] = createdAt;

                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, dept);
                ps.setTimestamp(4, Timestamp.from(createdAt));
                ps.addBatch();

                if ((i + 1) % BATCH_SIZE == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
        }

        log.info("Seeded {} users", USER_COUNT);
        return createdAts;
    }

    private static BigDecimal[] seedProducts(Connection conn, Random rng) throws SQLException {
        BigDecimal[] prices = new BigDecimal[PRODUCT_COUNT];
        String sql = "INSERT INTO products (name, category, price, attributes) VALUES (?, ?, ?, ?::jsonb)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < PRODUCT_COUNT; i++) {
                String adj = PRODUCT_ADJECTIVES[i / PRODUCT_NOUNS.length % PRODUCT_ADJECTIVES.length];
                String noun = PRODUCT_NOUNS[i % PRODUCT_NOUNS.length];
                String name = adj + " " + noun;
                String category = CATEGORIES[i % CATEGORIES.length];
                double rawPrice = rng.nextDouble() * 998.99 + 1.00;
                BigDecimal price = BigDecimal.valueOf(rawPrice).setScale(2, RoundingMode.HALF_UP);
                prices[i] = price;

                String attrs = generateAttributes(rng);

                ps.setString(1, name);
                ps.setString(2, category);
                ps.setBigDecimal(3, price);
                ps.setString(4, attrs);
                ps.addBatch();

                if ((i + 1) % BATCH_SIZE == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
        }

        log.info("Seeded {} products", PRODUCT_COUNT);
        return prices;
    }

    private static String generateAttributes(Random rng) {
        int keyCount = rng.nextInt(4) + 2; // 2-5 keys
        boolean[] used = new boolean[ATTR_KEYS.length];
        StringBuilder sb = new StringBuilder("{");
        int added = 0;

        while (added < keyCount) {
            int idx = rng.nextInt(ATTR_KEYS.length);
            if (used[idx]) continue;
            used[idx] = true;

            if (added > 0) sb.append(",");
            String value = ATTR_VALUES[idx][rng.nextInt(ATTR_VALUES[idx].length)];
            sb.append("\"").append(ATTR_KEYS[idx]).append("\":\"").append(value).append("\"");
            added++;
        }

        sb.append("}");
        return sb.toString();
    }

    private static void seedOrdersAndItems(Connection conn, Random rng,
                                            Instant[] userCreatedAt,
                                            BigDecimal[] productPrices,
                                            Instant now) throws SQLException {
        // Phase 1: Generate order metadata and insert all orders.
        // We must also consume RNG calls for items in the same loop to keep
        // the PRNG sequence deterministic, so we store item data in memory.
        int[][] itemProductIdx = new int[ORDER_COUNT][ITEMS_PER_ORDER];
        int[][] itemQuantity = new int[ORDER_COUNT][ITEMS_PER_ORDER];
        BigDecimal[] orderTotals = new BigDecimal[ORDER_COUNT];

        String orderSql = "INSERT INTO orders (user_id, total, status, placed_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement orderPs = conn.prepareStatement(orderSql)) {
            for (int i = 0; i < ORDER_COUNT; i++) {
                int userIdx = rng.nextInt(USER_COUNT);
                long userId = userIdx + 1;
                String status = pickStatus(rng);
                Instant userCreated = userCreatedAt[userIdx];
                long maxOffset = now.getEpochSecond() - userCreated.getEpochSecond();
                long orderOffset = maxOffset > 0 ? (long) (rng.nextDouble() * maxOffset) : 0;
                Instant placedAt = userCreated.plusSeconds(orderOffset);

                // Pre-generate items for this order (consumes RNG in deterministic order)
                BigDecimal orderTotal = BigDecimal.ZERO;
                for (int j = 0; j < ITEMS_PER_ORDER; j++) {
                    int productIdx = rng.nextInt(PRODUCT_COUNT);
                    int quantity = rng.nextInt(10) + 1;
                    itemProductIdx[i][j] = productIdx;
                    itemQuantity[i][j] = quantity;
                    orderTotal = orderTotal.add(
                        productPrices[productIdx].multiply(BigDecimal.valueOf(quantity)));
                }
                orderTotals[i] = orderTotal.setScale(2, RoundingMode.HALF_UP);

                orderPs.setLong(1, userId);
                orderPs.setBigDecimal(2, orderTotals[i]);
                orderPs.setString(3, status);
                orderPs.setTimestamp(4, Timestamp.from(placedAt));
                orderPs.addBatch();

                if ((i + 1) % BATCH_SIZE == 0) {
                    orderPs.executeBatch();
                }
            }
            orderPs.executeBatch();
        }
        conn.commit();
        log.info("Seeded {} orders", ORDER_COUNT);

        // Phase 2: Insert all order items (orders now exist in DB).
        String itemSql = "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement itemPs = conn.prepareStatement(itemSql)) {
            int batchCount = 0;
            for (int i = 0; i < ORDER_COUNT; i++) {
                long orderId = i + 1;
                for (int j = 0; j < ITEMS_PER_ORDER; j++) {
                    int productIdx = itemProductIdx[i][j];
                    itemPs.setLong(1, orderId);
                    itemPs.setLong(2, productIdx + 1);
                    itemPs.setInt(3, itemQuantity[i][j]);
                    itemPs.setBigDecimal(4, productPrices[productIdx]);
                    itemPs.addBatch();
                    batchCount++;

                    if (batchCount % BATCH_SIZE == 0) {
                        itemPs.executeBatch();
                    }
                }
            }
            itemPs.executeBatch();
        }

        log.info("Seeded {} order items", ORDER_COUNT * ITEMS_PER_ORDER);
    }

    private static String pickStatus(Random rng) {
        int roll = rng.nextInt(100);
        for (int i = 0; i < STATUS_THRESHOLDS.length; i++) {
            if (roll < STATUS_THRESHOLDS[i]) return STATUSES[i];
        }
        return STATUSES[STATUSES.length - 1];
    }

    private static void analyze(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ANALYZE users");
            stmt.execute("ANALYZE products");
            stmt.execute("ANALYZE orders");
            stmt.execute("ANALYZE order_items");
            stmt.execute("ANALYZE bench_users");
        }
        log.info("ANALYZE complete on all tables");
    }
}
