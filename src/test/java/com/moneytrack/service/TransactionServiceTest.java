package com.moneytrack.service;

import static org.junit.jupiter.api.Assertions.*;

import com.moneytrack.database.DatabaseManager;
import com.moneytrack.model.Category;
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

class TransactionServiceTest {
    @TempDir Path directory;
    private TransactionService service;
    private Category food;
    private Category salary;
    private final LocalDate date = LocalDate.of(2026, 11, 9);

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager database = new DatabaseManager(directory.resolve("test.db"));
        service = new TransactionService(new TransactionRepository(database), new CategoryRepository(database));
        food = service.findCategories().stream().filter(c -> c.name().equals("Food")).findFirst().orElseThrow();
        salary = service.findCategories().stream().filter(c -> c.name().equals("Salary")).findFirst().orElseThrow();
    }

    @Test
    void persistsCreateEditAndDelete() throws Exception {
        Transaction saved = service.save(0, TransactionType.EXPENSE, food, "9000000000", "Lunch", date);
        assertEquals(saved, service.findAll().getFirst());
        DatabaseManager reopened = new DatabaseManager(directory.resolve("test.db"));
        TransactionService secondService = new TransactionService(
                new TransactionRepository(reopened), new CategoryRepository(reopened));
        assertEquals(saved, secondService.findAll().getFirst());
        Transaction edited = service.save(saved.id(), TransactionType.INCOME, salary, "42", "Pay", date.plusDays(1));
        assertEquals(edited, service.findAll().getFirst());
        service.delete(saved.id());
        assertTrue(service.findAll().isEmpty());
        assertThrows(IllegalStateException.class, () -> service.delete(saved.id()));
        assertThrows(IllegalStateException.class,
                () -> service.save(saved.id(), TransactionType.INCOME, salary, "42", "Pay", date));
    }

    @Test
    void validatesRequiredValuesAndIntegerVnd() throws Exception {
        for (String invalid : new String[] {null, "", "0", "-1", "1.5", "1,000", "abc", "9223372036854775808"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.save(0, TransactionType.EXPENSE, food, invalid, "", date), invalid);
        }
        assertThrows(IllegalArgumentException.class, () -> service.save(0, null, food, "1", "", date));
        assertThrows(IllegalArgumentException.class, () -> service.save(0, TransactionType.EXPENSE, null, "1", "", date));
        assertThrows(IllegalArgumentException.class, () -> service.save(0, TransactionType.EXPENSE, salary, "1", "", date));
        assertThrows(IllegalArgumentException.class, () -> service.save(0, TransactionType.EXPENSE, food, "1", "", null));
        assertTrue(service.findAll().isEmpty());
        assertEquals(Long.MAX_VALUE, service.save(0, TransactionType.EXPENSE, food,
                Long.toString(Long.MAX_VALUE), "", date).amount());
    }

    @Test
    void rejectsDeletedCategoryThroughForeignKey() throws Exception {
        CategoryRepository categories = new CategoryRepository(new DatabaseManager(directory.resolve("test.db")));
        assertTrue(categories.delete(food.id()));
        assertThrows(SQLException.class, () -> service.save(0, TransactionType.EXPENSE, food, "1", "", date));
        assertTrue(service.findAll().isEmpty());
    }

    @Test
    void reopeningAfterEditAndDeleteReflectsPersistedChanges() throws Exception {
        Transaction saved = service.save(0, TransactionType.EXPENSE, food, "1000", "Lunch", date);
        service.save(saved.id(), TransactionType.INCOME, salary, "2500", "Salary", date.plusDays(1));
        DatabaseManager reopened = new DatabaseManager(directory.resolve("test.db"));
        TransactionService secondService = new TransactionService(
                new TransactionRepository(reopened), new CategoryRepository(reopened));
        Transaction edited = secondService.findAll().getFirst();
        assertEquals(2500, edited.amount());
        assertEquals(TransactionType.INCOME, edited.type());
        assertEquals("Salary", edited.description());
        secondService.delete(edited.id());
        DatabaseManager reopenedAgain = new DatabaseManager(directory.resolve("test.db"));
        assertTrue(new TransactionRepository(reopenedAgain).findAll().isEmpty());
    }
}
