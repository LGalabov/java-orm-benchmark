package com.lgalabov.ormbench.common.model;

import java.time.Instant;

public record User(
    Long id,
    String name,
    String email,
    String department,
    Instant createdAt
) {}
