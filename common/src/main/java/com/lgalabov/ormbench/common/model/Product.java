package com.lgalabov.ormbench.common.model;

import java.math.BigDecimal;

public record Product(
    Long id,
    String name,
    String category,
    BigDecimal price,
    String attributes
) {}
