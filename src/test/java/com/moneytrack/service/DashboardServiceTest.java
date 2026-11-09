package com.moneytrack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.moneytrack.database.DatabaseManager;
import com.moneytrack.model.Category;
import com.moneytrack.model.Transaction;
import com.moneytrack.model.TransactionType;
import com.moneytrack.repository.CategoryRepository;
import com.moneytrack.repository.TransactionRepository;
import java.math.BigInteger;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DashboardServiceTest {
    @TempDir Path directory;
    private TransactionRepository transactions;
    private DashboardService dashboard;
    private Category food;
    private Category shopping;
    private Category salary;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager database = new DatabaseManager(directory.resolve("dashboard.db"));
        transactions = new TransactionRepository(database);
        dashboard = new DashboardService(transactions);
        CategoryRepository categories = new CategoryRepository(database);
        food = categories.findAll().stream().filter(c -> c.name().equals("Food")).findFirst().orElseThrow();
        shopping = categories.findAll().stream().filter(c -> c.name().equals("Shopping")).findFirst().orElseThrow();
        salary = categories.findAll().stream().filter(c -> c.name().equals("Salary")).findFirst().orElseThrow();
    }

    @Test
    void emptyDatabaseHasZeroTotalsAndEmptySections() throws Exception {
        var data = dashboard.load(YearMonth.of(2026, 11));
        assertEquals(BigInteger.ZERO, data.income());
        assertEquals(BigInteger.ZERO, data.expenses());
        assertEquals(BigInteger.ZERO, data.balance());
        assertTrue(data.categoryTotals().isEmpty());
        assertTrue(data.recentTransactions().isEmpty());
        assertFalse(data.hasMonthlyTransactions());
    }

    @Test
    void calculatesMonthAndMostRecentFiveAcrossDates() throws Exception {
        add(salary, 1000, "salary", "2026-11-01");
        add(food, 100, "food", "2026-11-30");
        add(food, 150, "lunch", "2026-11-15");
        add(shopping, 200, "shop", "2026-11-15");
        add(food, 20, "prior", "2026-10-31");
        add(salary, 700, "next", "2026-12-01");
        var data = dashboard.load(YearMonth.of(2026, 11));
        assertEquals(BigInteger.valueOf(1000), data.income());
        assertEquals(BigInteger.valueOf(450), data.expenses());
        assertEquals(BigInteger.valueOf(550), data.balance());
        assertTrue(data.hasMonthlyTransactions());
        assertEquals(2, data.categoryTotals().size());
        assertEquals(food.id(), data.categoryTotals().get(0).categoryId());
        assertEquals(BigInteger.valueOf(250), data.categoryTotals().get(0).amount());
        assertEquals(BigInteger.valueOf(200), data.categoryTotals().get(1).amount());
        assertEquals(java.util.List.of("next", "food", "shop", "lunch", "salary"),
                data.recentTransactions().stream().map(e -> e.transaction().description()).toList());
        var october = dashboard.load(YearMonth.of(2026, 10));
        assertEquals(BigInteger.valueOf(20), october.expenses());
        assertEquals(BigInteger.valueOf(-20), october.balance());
    }

    @Test
    void totalsRemainExactBeyondLongRangeAndReflectCrud() throws Exception {
        Transaction first = add(food, Long.MAX_VALUE, "first", "2026-02-28");
        add(food, Long.MAX_VALUE, "second", "2026-02-01");
        BigInteger doubled = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO);
        var data = dashboard.load(YearMonth.of(2026, 2));
        assertEquals(doubled, data.expenses());
        assertEquals(doubled.negate(), data.balance());
        assertEquals(doubled, data.categoryTotals().getFirst().amount());
        transactions.update(new Transaction(first.id(), salary.id(), TransactionType.INCOME, 5, "edited", LocalDate.of(2026, 2, 28)));
        assertEquals(BigInteger.valueOf(5), dashboard.load(YearMonth.of(2026, 2)).income());
        transactions.delete(first.id());
        assertEquals(BigInteger.ZERO, dashboard.load(YearMonth.of(2026, 2)).income());
    }

    @Test
    void rejectsNegativeAmountsInsteadOfDisplayingMisleadingTotals() throws Exception {
        add(food, -1, "invalid", "2026-11-09");
        assertThrows(IllegalStateException.class, () -> dashboard.load(YearMonth.of(2026, 11)));
    }

    @Test
    void emptySelectedMonthStillShowsRecentActivity() throws Exception {
        add(salary, 123, "salary", "2026-11-09");
        var emptyMonth = dashboard.load(YearMonth.of(2026, 12));
        assertFalse(emptyMonth.hasMonthlyTransactions());
        assertEquals(BigInteger.ZERO, emptyMonth.income());
        assertTrue(emptyMonth.categoryTotals().isEmpty());
        assertEquals("salary", emptyMonth.recentTransactions().getFirst().transaction().description());
        var incomeMonth = dashboard.load(YearMonth.of(2026, 11));
        assertTrue(incomeMonth.hasMonthlyTransactions());
        assertEquals(BigInteger.valueOf(123), incomeMonth.income());
        assertTrue(incomeMonth.categoryTotals().isEmpty());
    }

    private Transaction add(Category category, long amount, String description, String date) throws Exception {
        return transactions.create(new Transaction(category.id(), category.type(), amount, description, LocalDate.parse(date)));
    }
}
