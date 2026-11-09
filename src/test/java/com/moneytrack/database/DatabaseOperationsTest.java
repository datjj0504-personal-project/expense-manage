package com.moneytrack.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.moneytrack.model.Category;
import com.moneytrack.model.Transaction;
import com.moneytrack.model.TransactionType;
import com.moneytrack.repository.CategoryRepository;
import com.moneytrack.repository.TransactionRepository;
import java.nio.file.Path;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseOperationsTest {
    @TempDir
    Path directory;

    private Path file;
    private DatabaseManager database;
    private CategoryRepository categories;
    private TransactionRepository transactions;

    @BeforeEach
    void setUp() throws Exception {
        file = directory.resolve("nested").resolve("moneytrack.db");
        database = new DatabaseManager(file);
        categories = new CategoryRepository(database);
        transactions = new TransactionRepository(database);
    }

    @Test
    void initializesDefaultsOnceAndEnablesForeignKeysOnEveryConnection() throws Exception {
        Set<String> defaults = categories.findAll().stream()
                .map(category -> category.type() + ":" + category.name())
                .collect(Collectors.toSet());
        assertEquals(Set.of("EXPENSE:Food", "EXPENSE:Transportation", "EXPENSE:Shopping",
                "EXPENSE:Entertainment", "EXPENSE:Other", "INCOME:Salary",
                "INCOME:Bonus", "INCOME:Investment", "INCOME:Other"), defaults);
        assertEquals(9, categories.findAll().size());

        Category food = categories.findAll().stream().filter(c -> c.name().equals("Food")).findFirst().orElseThrow();
        assertTrue(categories.delete(food.id()));
        CategoryRepository reopened = new CategoryRepository(new DatabaseManager(file));
        assertEquals(8, reopened.findAll().size());
        assertTrue(reopened.findById(food.id()).isEmpty());

        for (int i = 0; i < 2; i++) {
            try (Connection connection = database.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("PRAGMA foreign_keys")) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }

        }
    }

    @Test
    void defaultDatabaseIsCreatedUnderUserHome() throws Exception {
        String originalHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", directory.toString());
            DatabaseManager defaultDatabase = new DatabaseManager();
            assertTrue(Files.isRegularFile(directory.resolve(".moneytrack").resolve("moneytrack.db")));
            assertEquals(9, new CategoryRepository(defaultDatabase).findAll().size());
        } finally {
            if (originalHome == null) {
                System.clearProperty("user.home");
            } else {
                System.setProperty("user.home", originalHome);
            }
        }
    }

    @Test
    void categoryCrudAndConstraints() throws Exception {
        Category created = categories.create(new Category("Rent", TransactionType.EXPENSE));
        assertTrue(created.id() > 0);
        assertEquals(created, categories.findById(created.id()).orElseThrow());
        assertTrue(categories.findAll().contains(created));
        assertThrows(SQLException.class, () -> categories.create(new Category("Rent", TransactionType.EXPENSE)));
        assertNotEquals(created.id(), categories.create(new Category("Rent", TransactionType.INCOME)).id());

        Category updated = new Category(created.id(), "Rent updated", TransactionType.EXPENSE);
        assertTrue(categories.update(updated));
        assertEquals(updated, categories.findById(created.id()).orElseThrow());
        assertTrue(categories.delete(created.id()));
        assertFalse(categories.delete(created.id()));
        assertFalse(categories.update(updated));
        assertTrue(categories.findById(created.id()).isEmpty());
    }

    @Test
    void transactionCrudPreservesIntegerVndAndDate() throws Exception {
        Category food = categories.findAll().stream().filter(c -> c.name().equals("Food")).findFirst().orElseThrow();
        Transaction created = transactions.create(new Transaction(food.id(), TransactionType.EXPENSE,
                9_000_000_000L, "Lunch 'special'", LocalDate.of(2026, 11, 9)));
        assertTrue(created.id() > 0);
        assertEquals(created, transactions.findById(created.id()).orElseThrow());
        assertEquals(java.util.List.of(created), transactions.findAll());
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT typeof(amount), amount FROM transactions WHERE id = ?")) {
            statement.setLong(1, created.id());
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals("integer", result.getString(1));
                assertEquals(9_000_000_000L, result.getLong(2));
            }
        }

        Transaction updated = new Transaction(created.id(), food.id(), TransactionType.EXPENSE,
                42, null, LocalDate.of(2024, 2, 29));
        assertTrue(transactions.update(updated));
        assertEquals(updated, transactions.findById(created.id()).orElseThrow());
        assertNull(transactions.findById(created.id()).orElseThrow().description());
        assertTrue(transactions.delete(created.id()));
        assertFalse(transactions.delete(created.id()));
        assertFalse(transactions.update(updated));
        assertTrue(transactions.findById(created.id()).isEmpty());
        assertTrue(transactions.findAll().isEmpty());
    }

    @Test
    void enforcesCategoryReferencesAndMatchingType() throws Exception {
        Category food = categories.findAll().stream().filter(c -> c.name().equals("Food")).findFirst().orElseThrow();
        LocalDate date = LocalDate.of(2026, 1, 1);
        assertThrows(SQLException.class, () -> transactions.create(
                new Transaction(99999, TransactionType.EXPENSE, 100, null, date)));
        assertThrows(SQLException.class, () -> transactions.create(
                new Transaction(food.id(), TransactionType.INCOME, 100, null, date)));
        Transaction saved = transactions.create(new Transaction(food.id(), TransactionType.EXPENSE, 100, null, date));
        assertThrows(SQLException.class, () -> categories.delete(food.id()));
        assertThrows(SQLException.class, () -> categories.update(
                new Category(food.id(), food.name(), TransactionType.INCOME)));
        assertThrows(SQLException.class, () -> transactions.update(
                new Transaction(saved.id(), food.id(), TransactionType.INCOME, 100, null, date)));
        assertEquals(saved, transactions.findById(saved.id()).orElseThrow());
        assertEquals(food, categories.findById(food.id()).orElseThrow());
    }

    @Test
    void failedInitializationRollsBackSchemaAndVersion() throws Exception {
        Path brokenFile = directory.resolve("broken.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + brokenFile);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE transactions (id INTEGER)");
        }
        assertThrows(SQLException.class, () -> new DatabaseManager(brokenFile));
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + brokenFile);
             Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery(
                    "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'categories'")) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }
            try (ResultSet result = statement.executeQuery("PRAGMA user_version")) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }

        }
    }

    @Test
    void rejectsFutureSchemaVersionWithoutChangingDatabase() throws Exception {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA user_version = 99");
        }
        assertThrows(SQLException.class, () -> new DatabaseManager(file));
        assertEquals(9, categories.findAll().size());
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA user_version")) {
            assertTrue(result.next());
            assertEquals(99, result.getInt(1));
        }
    }
}
