package com.lgalabov.ormbench.common.model;

import java.math.BigDecimal;

public record OrderWithItems(
    Long orderId,
    BigDecimal total,
    String status,
    String userName,
    String productName,
    int quantity,
    BigDecimal itemPrice
) {}
