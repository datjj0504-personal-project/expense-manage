package com.moneytrack.service;

import com.moneytrack.model.Transaction;
import com.moneytrack.model.TransactionType;
import com.moneytrack.repository.TransactionRepository;
import com.moneytrack.repository.TransactionRepository.CategoryTransaction;
import java.math.BigInteger;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DashboardService {
    private final TransactionRepository repository;

    public DashboardService(TransactionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public record CategoryTotal(long categoryId, String name, BigInteger amount) { }
    public record DashboardData(BigInteger income, BigInteger expenses, BigInteger balance,
                                List<CategoryTotal> categoryTotals, List<CategoryTransaction> recentTransactions,
                                boolean hasMonthlyTransactions) { }

    public DashboardData load(YearMonth month) throws SQLException {
        Objects.requireNonNull(month, "month");
        List<CategoryTransaction> entries = repository.findDashboardTransactions(month);
        BigInteger income = BigInteger.ZERO;
        BigInteger expenses = BigInteger.ZERO;
        Map<Long, CategoryTotal> grouped = new LinkedHashMap<>();
        boolean hasMonthlyTransactions = false;
        for (CategoryTransaction entry : entries) {
            Transaction transaction = entry.transaction();
            if (!YearMonth.from(transaction.transactionDate()).equals(month)) {
                continue;
            }
            if (transaction.amount() < 0) {
                throw new IllegalStateException("A transaction has a negative amount. Correct it in Transactions.");
            }
            hasMonthlyTransactions = true;
            BigInteger amount = BigInteger.valueOf(transaction.amount());
            if (transaction.type() == TransactionType.INCOME) {
                income = income.add(amount);
            } else {
                expenses = expenses.add(amount);
                CategoryTotal previous = grouped.get(transaction.categoryId());
                grouped.put(transaction.categoryId(), new CategoryTotal(transaction.categoryId(), entry.categoryName(),
                        previous == null ? amount : previous.amount().add(amount)));
            }
        }
        List<CategoryTotal> totals = new ArrayList<>(grouped.values());
        totals.removeIf(total -> total.amount().signum() == 0);
        totals.sort((a, b) -> {
            int amountOrder = b.amount().compareTo(a.amount());
            return amountOrder != 0 ? amountOrder : a.name().compareToIgnoreCase(b.name());
        });
        return new DashboardData(income, expenses, income.subtract(expenses), List.copyOf(totals),
                List.copyOf(entries.subList(0, Math.min(5, entries.size()))), hasMonthlyTransactions);
    }
}
