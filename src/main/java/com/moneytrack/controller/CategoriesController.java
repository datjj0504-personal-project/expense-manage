package com.moneytrack.controller;

import com.moneytrack.model.Category;
import com.moneytrack.model.TransactionType;
import com.moneytrack.service.CategoryService;
import com.moneytrack.util.UiTasks;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class CategoriesController {
    @FXML private TableView<Category> expenseTable;
    @FXML private TableView<Category> incomeTable;
    @FXML private TableColumn<Category, String> expenseName;
    @FXML private TableColumn<Category, String> incomeName;
    @FXML private TabPane tabs;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Label statusLabel;
    private CategoryService service;
    private Runnable onChanged = () -> { };
    private boolean busy;

    @FXML private void initialize() {
        expenseName.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().name()));
        incomeName.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().name()));
        expenseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        incomeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        expenseTable.setPlaceholder(new Label("No expense categories yet."));
        incomeTable.setPlaceholder(new Label("No income categories yet."));
        expenseTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateButtons());
        incomeTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateButtons());
        tabs.getSelectionModel().selectedIndexProperty().addListener((o, a, b) -> updateButtons());
        updateButtons();
    }

    public void configure(CategoryService service, Runnable onChanged) {
        this.service = Objects.requireNonNull(service);
        this.onChanged = Objects.requireNonNull(onChanged);
    }

    public void refresh() {
        refresh(null);
    }

    private void refresh(String successMessage) {
        if (busy) return;
        busy = true;
        updateButtons();
        statusLabel.getStyleClass().removeAll("status-success", "status-error");
        statusLabel.setText("Loading categories…");
        UiTasks.run(service::findAll, categories -> {
            expenseTable.getItems().setAll(categories.stream().filter(c -> c.type() == TransactionType.EXPENSE).toList());
            incomeTable.getItems().setAll(categories.stream().filter(c -> c.type() == TransactionType.INCOME).toList());
            busy = false;
            updateButtons();
            statusLabel.setText(successMessage == null ? categories.size() + " categories" : successMessage);
            if (successMessage != null) statusLabel.getStyleClass().add("status-success");
        }, error -> {
            busy = false;
            updateButtons();
            statusLabel.setText("Could not load categories: " + error.getMessage());
            statusLabel.getStyleClass().add("status-error");
        });
    }

    private TableView<Category> selectedTable() {
        return tabs.getSelectionModel().getSelectedIndex() == 0 ? expenseTable : incomeTable;
    }

    private void updateButtons() {
        addButton.setDisable(busy);
        boolean disabled = busy || selectedTable().getSelectionModel().isEmpty();
        editButton.setDisable(disabled);
        deleteButton.setDisable(disabled);
    }

    @FXML private void add() { openForm(null); }
    @FXML private void edit() {
        Category category = selectedTable().getSelectionModel().getSelectedItem();
        if (category != null) openForm(category);
    }

    private void openForm(Category category) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/category-form.fxml"));
            Parent content = loader.load();
            CategoryFormController form = loader.getController();
            form.configure(category, selectedTable() == expenseTable ? TransactionType.EXPENSE : TransactionType.INCOME);
            Dialog<Void> dialog = new Dialog<>();
            dialog.initOwner(tabs.getScene().getWindow());
            dialog.setTitle(category == null ? "Add category" : "Edit category");
            dialog.setHeaderText(dialog.getTitle());
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getStylesheets().addAll(tabs.getScene().getStylesheets());
            ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
            Button save = (Button) dialog.getDialogPane().lookupButton(saveType);
            save.getStyleClass().add("primary-button");
            save.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume();
                String name = form.name();
                TransactionType type = form.type();
                save.setDisable(true);
                content.setDisable(true);
                dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setDisable(true);
                dialog.setOnCloseRequest(close -> close.consume());
                busy = true;
                updateButtons();
                UiTasks.run(() -> service.save(category == null ? 0 : category.id(), name, type), result -> {
                    busy = false;
                    dialog.setOnCloseRequest(null);
                    dialog.close();
                    onChanged.run();
                    refresh("Category saved successfully.");
                }, error -> {
                    busy = false;
                    updateButtons();
                    save.setDisable(false);
                    content.setDisable(false);
                    dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setDisable(false);
                    dialog.setOnCloseRequest(null);
                    form.error(message(error));
                });
            });
            dialog.show();
        } catch (IOException e) {
            showError("Could not open category form", e);
        }
    }

    @FXML private void delete() {
        Category category = selectedTable().getSelectionModel().getSelectedItem();
        if (category == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.initOwner(tabs.getScene().getWindow());
        confirmation.setTitle("Delete category");
        confirmation.setHeaderText("Delete “" + category.name() + "”?");
        confirmation.setContentText("Categories used by transactions cannot be deleted.");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        busy = true;
        updateButtons();
        UiTasks.run(() -> { service.delete(category.id()); return null; }, ignored -> {
            busy = false;
            onChanged.run();
            refresh("Category deleted successfully.");
        }, error -> {
            busy = false;
            updateButtons();
            showError("Could not delete category", error);
        });
    }

    private void showError(String title, Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(tabs.getScene().getWindow());
        alert.setHeaderText(title);
        alert.setContentText(message(error));
        alert.show();
    }

    private static String message(Throwable error) {
        if (error instanceof SQLException sql && sql.getMessage().contains("FOREIGN KEY")) {
            return "This category is used by transactions and cannot be deleted or changed to another type.";
        }
        if (error instanceof SQLException sql && sql.getMessage().contains("UNIQUE")) {
            return "A category with this name and type already exists.";
        }
        return error.getMessage() == null ? "The operation failed." : error.getMessage();
    }
}
