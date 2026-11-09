package com.moneytrack.model;

import java.util.Objects;

public record Category(long id, String name, TransactionType type) {
    public Category {
        if (id < 0) {
            throw new IllegalArgumentException("Category id cannot be negative");
        }
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be blank");
        }
        Objects.requireNonNull(type, "type");
    }

    public Category(String name, TransactionType type) {
        this(0, name, type);
    }
}
