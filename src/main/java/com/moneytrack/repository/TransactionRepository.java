package com.moneytrack.repository;

import com.moneytrack.database.DatabaseManager;
import com.moneytrack.model.Transaction;
import com.moneytrack.model.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TransactionRepository {
    private final DatabaseManager database;

    public TransactionRepository(DatabaseManager database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public Transaction create(Transaction transaction) throws SQLException {
        Objects.requireNonNull(transaction, "transaction");
        if (transaction.id() != 0) {
            throw new IllegalArgumentException("New transaction must not have an id");
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO transactions (category_id, type, amount, description, transaction_date)
                     VALUES (?, ?, ?, ?, ?)
                     """)) {
            bindValues(statement, transaction);
            statement.executeUpdate();
            try (PreparedStatement idStatement = connection.prepareStatement("SELECT last_insert_rowid()");
                 ResultSet result = idStatement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Unable to retrieve the new transaction id");
                }
                return new Transaction(result.getLong(1), transaction.categoryId(), transaction.type(),
                        transaction.amount(), transaction.description(), transaction.transactionDate());
            }
        }
    }

    public Optional<Transaction> findById(long id) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, category_id, type, amount, description, transaction_date
                     FROM transactions WHERE id = ?
                     """)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readTransaction(result)) : Optional.empty();
            }
        }
    }

    public List<Transaction> findAll() throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, category_id, type, amount, description, transaction_date
                     FROM transactions ORDER BY id
                     """);
             ResultSet result = statement.executeQuery()) {
            List<Transaction> transactions = new ArrayList<>();
            while (result.next()) {
                transactions.add(readTransaction(result));
            }
            return transactions;
        }
    }

    public record CategoryTransaction(Transaction transaction, String categoryName) { }

    public List<CategoryTransaction> findDashboardTransactions(YearMonth month) throws SQLException {
        Objects.requireNonNull(month, "month");
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT t.id, t.category_id, t.type, t.amount, t.description, t.transaction_date,
                            c.name AS category_name
                     FROM transactions t JOIN categories c ON c.id = t.category_id AND c.type = t.type
                     WHERE (t.transaction_date >= ? AND t.transaction_date <= ?)
                        OR t.id IN (SELECT id FROM transactions ORDER BY transaction_date DESC, id DESC LIMIT 5)
                     ORDER BY t.transaction_date DESC, t.id DESC
                     """)) {
            statement.setString(1, month.atDay(1).toString());
            statement.setString(2, month.atEndOfMonth().toString());
            try (ResultSet result = statement.executeQuery()) {
                List<CategoryTransaction> entries = new ArrayList<>();
                while (result.next()) {
                    entries.add(new CategoryTransaction(readTransaction(result), result.getString("category_name")));
                }
                return entries;
            }
        }
    }

    public boolean update(Transaction transaction) throws SQLException {
        Objects.requireNonNull(transaction, "transaction");
        if (transaction.id() <= 0) {
            throw new IllegalArgumentException("Transaction id must be positive");
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE transactions SET category_id = ?, type = ?, amount = ?,
                         description = ?, transaction_date = ? WHERE id = ?
                     """)) {
            bindValues(statement, transaction);
            statement.setLong(6, transaction.id());
            return statement.executeUpdate() == 1;
        }
    }

    public boolean delete(long id) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM transactions WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    private static void bindValues(PreparedStatement statement, Transaction transaction) throws SQLException {
        statement.setLong(1, transaction.categoryId());
        statement.setString(2, transaction.type().name());
        statement.setLong(3, transaction.amount());
        statement.setString(4, transaction.description());
        statement.setString(5, transaction.transactionDate().toString());
    }

    private static Transaction readTransaction(ResultSet result) throws SQLException {
        return new Transaction(result.getLong("id"), result.getLong("category_id"),
                TransactionType.valueOf(result.getString("type")), result.getLong("amount"),
                result.getString("description"), LocalDate.parse(result.getString("transaction_date")));
    }
}
