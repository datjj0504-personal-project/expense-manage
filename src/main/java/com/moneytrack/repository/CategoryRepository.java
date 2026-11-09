package com.moneytrack.repository;

import com.moneytrack.database.DatabaseManager;
import com.moneytrack.model.Category;
import com.moneytrack.model.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CategoryRepository {
    private final DatabaseManager database;

    public CategoryRepository(DatabaseManager database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public Category create(Category category) throws SQLException {
        Objects.requireNonNull(category, "category");
        if (category.id() != 0) {
            throw new IllegalArgumentException("New category must not have an id");
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO categories (name, type) VALUES (?, ?)")) {
            statement.setString(1, category.name());
            statement.setString(2, category.type().name());
            statement.executeUpdate();
            try (PreparedStatement idStatement = connection.prepareStatement("SELECT last_insert_rowid()");
                 ResultSet result = idStatement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Unable to retrieve the new category id");
                }
                return new Category(result.getLong(1), category.name(), category.type());
            }
        }
    }

    public Optional<Category> findById(long id) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, name, type FROM categories WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readCategory(result)) : Optional.empty();
            }
        }
    }

    public List<Category> findAll() throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, name, type FROM categories ORDER BY id");
             ResultSet result = statement.executeQuery()) {
            List<Category> categories = new ArrayList<>();
            while (result.next()) {
                categories.add(readCategory(result));
            }
            return categories;
        }
    }

    public boolean update(Category category) throws SQLException {
        Objects.requireNonNull(category, "category");
        if (category.id() <= 0) {
            throw new IllegalArgumentException("Category id must be positive");
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE categories SET name = ?, type = ? WHERE id = ?")) {
            statement.setString(1, category.name());
            statement.setString(2, category.type().name());
            statement.setLong(3, category.id());
            return statement.executeUpdate() == 1;
        }
    }

    public boolean delete(long id) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM categories WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    private static Category readCategory(ResultSet result) throws SQLException {
        return new Category(result.getLong("id"), result.getString("name"),
                TransactionType.valueOf(result.getString("type")));
    }
}
