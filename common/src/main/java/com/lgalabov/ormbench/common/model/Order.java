package com.lgalabov.ormbench.common.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Order(
    Long id,
    Long userId,
    BigDecimal total,
    String status,
    Instant placedAt
) {}
