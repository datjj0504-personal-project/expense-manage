package com.moneytrack.service;

import static org.junit.jupiter.api.Assertions.*;

import com.moneytrack.database.DatabaseManager;
import com.moneytrack.model.Transaction;
import com.moneytrack.model.TransactionType;
import com.moneytrack.repository.CategoryRepository;
import com.moneytrack.repository.TransactionRepository;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CategoryServiceTest {
    @TempDir Path directory;
    private DatabaseManager database;
    private CategoryService service;

    @BeforeEach void setUp() throws Exception {
        database = new DatabaseManager(directory.resolve("test.db"));
        service = new CategoryService(new CategoryRepository(database));
    }

    @Test void categoryCrudPersistsAndValidates() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.save(0, "  ", TransactionType.EXPENSE));
        assertThrows(IllegalArgumentException.class, () -> service.save(0, "Test", null));
        var saved = service.save(0, "  Rent  ", TransactionType.EXPENSE);
        assertEquals("Rent", saved.name());
        assertThrows(SQLException.class, () -> service.save(0, "Rent", TransactionType.EXPENSE));
        var changed = service.save(saved.id(), "Rent updated", TransactionType.INCOME);
        assertEquals(changed, new CategoryRepository(new DatabaseManager(database.getDatabaseFile()))
                .findById(saved.id()).orElseThrow());
        service.delete(saved.id());
        assertThrows(IllegalStateException.class, () -> service.delete(saved.id()));
        assertThrows(IllegalStateException.class,
                () -> service.save(saved.id(), "Missing", TransactionType.INCOME));
    }

    @Test void referencedCategoriesCannotBeDeletedOrChangeType() throws Exception {
        var category = service.save(0, "Travel", TransactionType.EXPENSE);
        new TransactionRepository(database).create(new Transaction(category.id(), category.type(), 100, "", LocalDate.now()));
        assertThrows(SQLException.class, () -> service.delete(category.id()));
        assertThrows(SQLException.class, () -> service.save(category.id(), "Travel", TransactionType.INCOME));
        assertEquals("Transport", service.save(category.id(), "Transport", TransactionType.EXPENSE).name());
    }
}
