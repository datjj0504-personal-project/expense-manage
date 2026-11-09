package com.moneytrack.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public final class DatabaseManager {
    private final String url;
    private final Path databaseFile;

    public DatabaseManager() throws IOException, SQLException {
        this(Path.of(System.getProperty("user.home"), ".moneytrack", "moneytrack.db"));
    }

    public DatabaseManager(Path databaseFile) throws IOException, SQLException {
        Path path = Objects.requireNonNull(databaseFile, "databaseFile").toAbsolutePath().normalize();
        this.databaseFile = path;
        Files.createDirectories(path.getParent());
        url = "jdbc:sqlite:" + path;
        try (Connection connection = getConnection()) {
            DatabaseInitializer.initialize(connection);
        }

    }

    public Path getDatabaseFile() {
        return databaseFile;
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            return connection;
        } catch (SQLException e) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                e.addSuppressed(closeException);
            }
            throw e;
        }
    }
}
