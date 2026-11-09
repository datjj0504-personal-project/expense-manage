package com.moneytrack.controller;

import com.moneytrack.model.Transaction;
import com.moneytrack.model.TransactionType;
import com.moneytrack.repository.TransactionRepository.CategoryTransaction;
import com.moneytrack.service.DashboardService;
import com.moneytrack.service.DashboardService.DashboardData;
import com.moneytrack.util.CurrencyFormatter;
import java.math.BigInteger;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DashboardController {
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private YearMonth selectedMonth = YearMonth.now();
    private DashboardService service;
    private long requestId;

    @FXML private Label monthLabel;
    @FXML private Label statusLabel;
    @FXML private SummaryCardController incomeCardController;
    @FXML private SummaryCardController expenseCardController;
    @FXML private SummaryCardController balanceCardController;
    @FXML private PieChart expenseChart;
    @FXML private Label expenseEmpty;
    @FXML private BarChart<String, Number> monthlyChart;
    @FXML private Label monthlyEmpty;
    @FXML private VBox transactionRows;
    @FXML private Label recentEmpty;

    @FXML
    private void initialize() {
        monthLabel.setText(selectedMonth.format(MONTH_FORMAT));
        clearData();
        statusLabel.setText("Loading dashboard…");
    }

    public void setService(DashboardService service) {
        this.service = Objects.requireNonNull(service);
    }

    public void refresh() {
        if (service == null) {
            return;
        }
        YearMonth month = selectedMonth;
        long request = ++requestId;
        monthLabel.setText(month.format(MONTH_FORMAT));
        clearData();
        statusLabel.setText("Loading dashboard…");
        Task<DashboardData> task = new Task<>() {
            @Override protected DashboardData call() throws Exception {
                return service.load(month);
            }
        };
        task.setOnSucceeded(event -> {
            if (request == requestId) {
                render(task.getValue());
                statusLabel.setText("");
            }
        });
        task.setOnFailed(event -> {
            if (request == requestId) {
                clearData();
                Throwable error = task.getException();
                statusLabel.setText("Could not load dashboard: " + error.getMessage());
            }
        });
        Thread thread = new Thread(task, "moneytrack-dashboard");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML private void previousMonth() {
        selectedMonth = selectedMonth.minusMonths(1);
        refresh();
    }

    @FXML private void nextMonth() {
        selectedMonth = selectedMonth.plusMonths(1);
        refresh();
    }

    private void clearData() {
        incomeCardController.configure("Total Income", "+", "—", "Selected month", null);
        expenseCardController.configure("Total Expenses", "−", "—", "Selected month", "expense-icon");
        balanceCardController.configure("Balance", "=", "—", "Income minus expenses", "balance-icon");
        expenseChart.getData().clear();
        monthlyChart.getData().clear();
        transactionRows.getChildren().clear();
        show(expenseChart, false);
        show(expenseEmpty, false);
        show(monthlyChart, false);
        show(monthlyEmpty, false);
        show(recentEmpty, false);
    }

    private void render(DashboardData data) {
        incomeCardController.configure("Total Income", "+", formatAmount(data.income()), "Selected month", null);
        expenseCardController.configure("Total Expenses", "−", formatAmount(data.expenses()), "Selected month", "expense-icon");
        balanceCardController.configure("Balance", "=", formatAmount(data.balance()), "Income minus expenses", "balance-icon");

        data.categoryTotals().forEach(category -> {
            PieChart.Data slice = new PieChart.Data(category.name(), category.amount().doubleValue());
            expenseChart.getData().add(slice);
            Tooltip.install(slice.getNode(), new Tooltip(category.name() + ": " + formatAmount(category.amount())));
        });
        show(expenseChart, !data.categoryTotals().isEmpty());
        show(expenseEmpty, data.categoryTotals().isEmpty());

        if (data.hasMonthlyTransactions()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.getData().add(new XYChart.Data<>("Income", data.income().doubleValue()));
            series.getData().add(new XYChart.Data<>("Expenses", data.expenses().doubleValue()));
            monthlyChart.getData().add(series);
            Tooltip.install(series.getData().get(0).getNode(), new Tooltip(formatAmount(data.income())));
            Tooltip.install(series.getData().get(1).getNode(), new Tooltip(formatAmount(data.expenses())));
        }
        show(monthlyChart, data.hasMonthlyTransactions());
        show(monthlyEmpty, !data.hasMonthlyTransactions());
        data.recentTransactions().forEach(entry -> transactionRows.getChildren().add(transactionRow(entry)));
        show(recentEmpty, data.recentTransactions().isEmpty());
    }

    private static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private HBox transactionRow(CategoryTransaction entry) {
        Transaction transaction = entry.transaction();
        String description = transaction.description();
        Label nameLabel = label(description == null || description.isBlank() ? "No description" : description,
                "transaction-name");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
        Label categoryLabel = label(entry.categoryName(), "transaction-category");
        categoryLabel.setMinWidth(110);
        Label dateLabel = label(transaction.transactionDate().format(DATE_FORMAT), "transaction-date");
        dateLabel.setMinWidth(100);
        boolean income = transaction.type() == TransactionType.INCOME;
        Label amountLabel = label((income ? "+ " : "− ") + formatAmount(BigInteger.valueOf(transaction.amount())),
                "transaction-amount");
        amountLabel.setMinWidth(120);
        amountLabel.setAlignment(Pos.CENTER_RIGHT);
        if (income) {
            amountLabel.getStyleClass().add("transaction-income");
        }
        HBox row = new HBox(12, nameLabel, categoryLabel, dateLabel, amountLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("transaction-row");
        return row;
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private static String formatAmount(BigInteger amount) {
        return CurrencyFormatter.format(amount);
    }
}
