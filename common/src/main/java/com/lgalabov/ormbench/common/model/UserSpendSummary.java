package com.lgalabov.ormbench.common.model;

import java.math.BigDecimal;
import java.time.Instant;

public record UserSpendSummary(
    Long id,
    String name,
    String email,
    int orderCount,
    BigDecimal totalSpend,
    Instant lastOrder
) {}
