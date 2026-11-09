package com.moneytrack.service;

import com.moneytrack.model.Category;
import com.moneytrack.model.Transaction;
import com.moneytrack.model.TransactionType;
import com.moneytrack.repository.CategoryRepository;
import com.moneytrack.repository.TransactionRepository;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class TransactionService {
    private final TransactionRepository transactions;
    private final CategoryRepository categories;

    public TransactionService(TransactionRepository transactions, CategoryRepository categories) {
        this.transactions = Objects.requireNonNull(transactions);
        this.categories = Objects.requireNonNull(categories);
    }

    public List<Transaction> findAll() throws SQLException {
        return transactions.findAll();
    }

    public List<Category> findCategories() throws SQLException {
        return categories.findAll();
    }

    public Transaction save(long id, TransactionType type, Category category, String amountText,
                            String description, LocalDate date) throws SQLException {
        if (type == null) {
            throw new IllegalArgumentException("Select a transaction type.");
        }
        if (category == null || category.type() != type) {
            throw new IllegalArgumentException("Select a category matching the transaction type.");
        }
        if (date == null) {
            throw new IllegalArgumentException("Select a date.");
        }
        if (amountText == null || !amountText.matches("[0-9]+")) {
            throw new IllegalArgumentException("Amount must be a whole number in VND.");
        }
        long amount;
        try {
            amount = Long.parseLong(amountText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount is too large.", e);
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        Transaction transaction = new Transaction(id, category.id(), type, amount, description, date);
        if (id == 0) {
            return transactions.create(transaction);
        }
        if (!transactions.update(transaction)) {
            throw new IllegalStateException("This transaction no longer exists. Refresh and try again.");
        }
        return transaction;
    }

    public void delete(long id) throws SQLException {
        if (!transactions.delete(id)) {
            throw new IllegalStateException("This transaction no longer exists. Refresh and try again.");
        }
    }
}
