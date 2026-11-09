package com.moneytrack.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {
    private static final int SCHEMA_VERSION = 1;

    private DatabaseInitializer() {
    }

    public static void initialize(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) {
            throw new SQLException("Database initialization requires an auto-commit connection");
        }

        connection.setAutoCommit(false);
        try {
            int version;
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("PRAGMA user_version")) {
                if (!result.next()) {
                    throw new SQLException("Unable to read database schema version");
                }
                version = result.getInt(1);
            }
            if (version > SCHEMA_VERSION) {
                throw new SQLException("Unsupported database schema version: " + version);
            }
            if (version == 0) {
                createSchema(connection);
                insertDefaultCategories(connection);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("PRAGMA user_version = " + SCHEMA_VERSION);
                }
            }
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException rollbackException) {
                e.addSuppressed(rollbackException);
            }
            throw e;
        }
        connection.setAutoCommit(true);
    }

    private static void createSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE categories (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL CHECK (length(trim(name)) > 0),
                        type TEXT NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
                        UNIQUE (name, type),
                        UNIQUE (id, type)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE transactions (
                        id INTEGER PRIMARY KEY,
                        category_id INTEGER NOT NULL,
                        type TEXT NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
                        amount INTEGER NOT NULL CHECK (typeof(amount) = 'integer'),
                        description TEXT,
                        transaction_date TEXT NOT NULL,
                        FOREIGN KEY (category_id, type) REFERENCES categories (id, type)
                    )
                    """);
        }
    }

    private static void insertDefaultCategories(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO categories (name, type) VALUES (?, ?)")) {
            for (String name : new String[] {"Food", "Transportation", "Shopping", "Entertainment", "Other"}) {
                addCategory(statement, name, "EXPENSE");
            }
            for (String name : new String[] {"Salary", "Bonus", "Investment", "Other"}) {
                addCategory(statement, name, "INCOME");
            }
            statement.executeBatch();
        }
    }

    private static void addCategory(PreparedStatement statement, String name, String type) throws SQLException {
        statement.setString(1, name);
        statement.setString(2, type);
        statement.addBatch();
    }
}
