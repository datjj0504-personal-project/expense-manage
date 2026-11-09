package com.moneytrack.model;

import java.time.LocalDate;
import java.util.Objects;

public record Transaction(long id, long categoryId, TransactionType type, long amount,
                          String description, LocalDate transactionDate) {
    public Transaction {
        if (id < 0) {
            throw new IllegalArgumentException("Transaction id cannot be negative");
        }
        if (categoryId <= 0) {
            throw new IllegalArgumentException("Category id must be positive");
        }
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(transactionDate, "transactionDate");
    }

    public Transaction(long categoryId, TransactionType type, long amount,
                       String description, LocalDate transactionDate) {
        this(0, categoryId, type, amount, description, transactionDate);
    }
}
