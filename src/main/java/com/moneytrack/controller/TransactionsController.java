package com.moneytrack.controller;

import com.moneytrack.model.Category;
import com.moneytrack.model.Transaction;
import com.moneytrack.model.TransactionType;
import com.moneytrack.service.TransactionService;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

public class TransactionsController {
    @FXML private TableView<Transaction> table;
    @FXML private TableColumn<Transaction, String> dateColumn;
    @FXML private TableColumn<Transaction, String> descriptionColumn;
    @FXML private TableColumn<Transaction, String> categoryColumn;
    @FXML private TableColumn<Transaction, String> typeColumn;
    @FXML private TableColumn<Transaction, Long> amountColumn;
    @FXML private ComboBox<String> typeFilter;
    @FXML private TextField searchField;
    @FXML private DatePicker dateFilter;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Label statusLabel;
    private final ObservableList<Transaction> rows = FXCollections.observableArrayList();
    private final FilteredList<Transaction> filtered = new FilteredList<>(rows);
    private List<Category> categories = List.of();
    private Map<Long, String> categoryNames = Map.of();
    private TransactionService service;
    private boolean busy;
    private Runnable onChanged = () -> { };

    @FXML
    private void initialize() {
        SortedList<Transaction> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No transactions found."));
        dateColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().transactionDate().toString()));
        descriptionColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().description() == null ? "" : cell.getValue().description()));
        categoryColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                categoryNames.getOrDefault(cell.getValue().categoryId(), "Unknown category")));
        typeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(typeConverter().toString(cell.getValue().type())));
        amountColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().amount()));
        amountColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Long amount, boolean empty) {
                super.updateItem(amount, empty);
                setText(empty || amount == null ? null : NumberFormat.getIntegerInstance(Locale.US).format(amount));
            }
        });
        typeFilter.setItems(FXCollections.observableArrayList("All types", "Income", "Expense"));
        typeFilter.setValue("All types");
        typeFilter.valueProperty().addListener((o, old, value) -> applyFilters());
        searchField.textProperty().addListener((o, old, value) -> applyFilters());
        dateFilter.valueProperty().addListener((o, old, value) -> applyFilters());
        table.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> updateButtons());
        updateButtons();
    }

    public void setService(TransactionService service) {
        this.service = service;
    }

    public void setOnChanged(Runnable onChanged) {
        this.onChanged = java.util.Objects.requireNonNull(onChanged);
    }

    public void refresh() {
        refresh(null);
    }

    private void refresh(String successMessage) {
        if (busy) {
            return;
        }
        runAsync(() -> new Snapshot(service.findCategories(), service.findAll()), snapshot -> {
            categories = snapshot.categories();
            categoryNames = categories.stream().collect(Collectors.toMap(Category::id, Category::name));
            rows.setAll(snapshot.transactions());
            table.refresh();
            applyFilters();
            if (successMessage != null) {
                statusLabel.setText(successMessage);
                statusLabel.getStyleClass().add("status-success");
            }
        }, error -> showError("Could not load transactions", error));
    }

    private record Snapshot(List<Category> categories, List<Transaction> transactions) { }

    private void applyFilters() {
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        String type = typeFilter.getValue();
        LocalDate date = dateFilter.getValue();
        filtered.setPredicate(transaction ->
                (type == null || type.equals("All types") || type.equalsIgnoreCase(transaction.type().name()))
                && (transaction.description() == null ? query.isEmpty()
                    : transaction.description().toLowerCase(Locale.ROOT).contains(query))
                && (date == null || date.equals(transaction.transactionDate())));
        statusLabel.setText(filtered.size() + " transaction(s)");
    }

    @FXML
    private void clearFilters() {
        searchField.clear();
        typeFilter.setValue("All types");
        dateFilter.setValue(null);
    }

    @FXML private void addTransaction() { openForm(null); }
    @FXML private void editTransaction() {
        Transaction selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) openForm(selected);
    }

    private void openForm(Transaction transaction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/transaction-form.fxml"));
            Parent content = loader.load();
            TransactionFormController form = loader.getController();
            form.configure(categories, transaction);
            Dialog<Void> dialog = new Dialog<>();
            dialog.initOwner(table.getScene().getWindow());
            dialog.setTitle(transaction == null ? "Add transaction" : "Edit transaction");
            dialog.setHeaderText(transaction == null ? "Add a transaction" : "Edit transaction");
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getStylesheets().addAll(table.getScene().getStylesheets());
            ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
            Button save = (Button) dialog.getDialogPane().lookupButton(saveType);
            save.getStyleClass().add("primary-button");
            save.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume();
                long id = transaction == null ? 0 : transaction.id();
                TransactionType type = form.type();
                Category category = form.category();
                String amount = form.amount();
                String description = form.description();
                LocalDate date = form.date();
                save.setDisable(true);
                content.setDisable(true);
                dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setDisable(true);
                dialog.setOnCloseRequest(close -> close.consume());
                runAsync(() -> service.save(id, type, category, amount, description, date), saved -> {
                    dialog.setOnCloseRequest(null);
                    dialog.close();
                    onChanged.run();
                    refresh("Transaction saved successfully.");
                }, error -> {
                    save.setDisable(false);
                    content.setDisable(false);
                    dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setDisable(false);
                    dialog.setOnCloseRequest(null);
                    form.setError(errorMessage(error));
                });
            });
            dialog.show();
        } catch (IOException e) {
            showError("Could not open transaction form", e);
        }
    }

    @FXML
    private void deleteTransaction() {
        Transaction selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.initOwner(table.getScene().getWindow());
        confirmation.setTitle("Delete transaction");
        confirmation.setHeaderText("Delete this transaction?");
        confirmation.setContentText("This action cannot be undone.");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            runAsync(() -> { service.delete(selected.id()); return null; },
                    ignored -> { onChanged.run(); refresh("Transaction deleted successfully."); },
                    error -> showError("Could not delete transaction", error));
        }
    }

    private <T> void runAsync(Callable<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        if (busy) return;
        busy = true;
        updateButtons();
        statusLabel.getStyleClass().remove("status-success");
        statusLabel.setText("Working…");
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(event -> {
            busy = false;
            updateButtons();
            success.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            busy = false;
            updateButtons();
            applyFilters();
            failure.accept(task.getException());
        });
        Thread thread = new Thread(task, "moneytrack-transactions");
        thread.setDaemon(true);
        thread.start();
    }

    private void updateButtons() {
        boolean noSelection = table.getSelectionModel().getSelectedItem() == null;
        addButton.setDisable(busy);
        editButton.setDisable(busy || noSelection);
        deleteButton.setDisable(busy || noSelection);
    }

    private void showError(String title, Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(table.getScene().getWindow());
        alert.setHeaderText(title);
        alert.setContentText(errorMessage(error));
        alert.show();
    }

    private static String errorMessage(Throwable error) {
        if (error instanceof IllegalArgumentException || error instanceof IllegalStateException) {
            return error.getMessage();
        }
        return "Database operation failed: " + error.getMessage();
    }

    public static StringConverter<TransactionType> typeConverter() {
        return new StringConverter<>() {
            @Override public String toString(TransactionType type) {
                return type == null ? "" : type == TransactionType.INCOME ? "Income" : "Expense";
            }
            @Override public TransactionType fromString(String value) { throw new UnsupportedOperationException(); }
        };
    }
}
