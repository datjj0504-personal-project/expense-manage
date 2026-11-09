package com.moneytrack.controller;

import com.moneytrack.service.DashboardService;
import com.moneytrack.service.DashboardService.CategoryTotal;
import com.moneytrack.service.DashboardService.DashboardData;
import com.moneytrack.util.CurrencyFormatter;
import com.moneytrack.util.UiTasks;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;

public class ReportsController {
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private YearMonth month = YearMonth.now();
    private long requestId;
    private DashboardService service;
    @FXML private Label monthLabel;
    @FXML private Label statusLabel;
    @FXML private SummaryCardController incomeCardController;
    @FXML private SummaryCardController expenseCardController;
    @FXML private SummaryCardController balanceCardController;
    @FXML private BarChart<String, Number> comparisonChart;
    @FXML private Label comparisonEmpty;
    @FXML private TableView<CategoryTotal> categoryTable;
    @FXML private TableColumn<CategoryTotal, String> categoryColumn;
    @FXML private TableColumn<CategoryTotal, java.math.BigInteger> totalColumn;

    @FXML private void initialize() {
        categoryColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().name()));
        totalColumn.setCellValueFactory(value -> new ReadOnlyObjectWrapper<>(value.getValue().amount()));
        totalColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override protected void updateItem(java.math.BigInteger amount, boolean empty) {
                super.updateItem(amount, empty);
                setText(empty || amount == null ? null : CurrencyFormatter.format(amount));
            }
        });
        categoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        categoryTable.setPlaceholder(new Label("No expenses for this month."));
        clear();
    }

    public void setService(DashboardService service) {
        this.service = Objects.requireNonNull(service);
    }

    public void refresh() {
        if (service == null) return;
        YearMonth requestedMonth = month;
        long request = ++requestId;
        monthLabel.setText(month.format(MONTH_FORMAT));
        clear();
        statusLabel.setText("Loading report…");
        UiTasks.run(() -> service.load(requestedMonth), data -> {
            if (request != requestId) return;
            render(data);
            statusLabel.setText("");
        }, error -> {
            if (request != requestId) return;
            clear();
            statusLabel.setText("Could not load report: " + error.getMessage());
        });
    }

    @FXML private void previousMonth() { month = month.minusMonths(1); refresh(); }
    @FXML private void nextMonth() { month = month.plusMonths(1); refresh(); }

    private void clear() {
        incomeCardController.configure("Total Income", "+", "—", "Selected month", null);
        expenseCardController.configure("Total Expenses", "−", "—", "Selected month", "expense-icon");
        balanceCardController.configure("Balance", "=", "—", "Income minus expenses", "balance-icon");
        comparisonChart.getData().clear();
        comparisonChart.setVisible(false);
        comparisonChart.setManaged(false);
        comparisonEmpty.setVisible(false);
        comparisonEmpty.setManaged(false);
        categoryTable.getItems().clear();
    }

    private void render(DashboardData data) {
        incomeCardController.configure("Total Income", "+", CurrencyFormatter.format(data.income()), "Selected month", null);
        expenseCardController.configure("Total Expenses", "−", CurrencyFormatter.format(data.expenses()), "Selected month", "expense-icon");
        balanceCardController.configure("Balance", "=", CurrencyFormatter.format(data.balance()), "Income minus expenses", "balance-icon");
        categoryTable.getItems().setAll(data.categoryTotals());
        comparisonChart.setManaged(data.hasMonthlyTransactions());
        comparisonChart.setVisible(data.hasMonthlyTransactions());
        comparisonEmpty.setManaged(!data.hasMonthlyTransactions());
        comparisonEmpty.setVisible(!data.hasMonthlyTransactions());
        if (data.hasMonthlyTransactions()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.getData().add(new XYChart.Data<>("Income", data.income().doubleValue()));
            series.getData().add(new XYChart.Data<>("Expenses", data.expenses().doubleValue()));
            comparisonChart.getData().add(series);
            Tooltip.install(series.getData().get(0).getNode(), new Tooltip(CurrencyFormatter.format(data.income())));
            Tooltip.install(series.getData().get(1).getNode(), new Tooltip(CurrencyFormatter.format(data.expenses())));
        }
    }
}
