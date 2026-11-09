package com.moneytrack.controller;

import com.moneytrack.model.Category;
import com.moneytrack.model.Transaction;
import com.moneytrack.model.TransactionType;
import java.time.LocalDate;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

public class TransactionFormController {
    @FXML private ComboBox<TransactionType> typeField;
    @FXML private ComboBox<Category> categoryField;
    @FXML private TextField amountField;
    @FXML private TextField descriptionField;
    @FXML private DatePicker dateField;
    @FXML private Label errorLabel;
    private List<Category> categories;

    @FXML
    private void initialize() {
        typeField.setItems(FXCollections.observableArrayList(TransactionType.values()));
        typeField.setConverter(TransactionsController.typeConverter());
        categoryField.setConverter(new StringConverter<>() {
            @Override public String toString(Category category) { return category == null ? "" : category.name(); }
            @Override public Category fromString(String value) { throw new UnsupportedOperationException(); }
        });
        typeField.valueProperty().addListener((observable, oldValue, type) -> updateCategories(type));
    }

    public void configure(List<Category> categories, Transaction transaction) {
        this.categories = List.copyOf(categories);
        typeField.setValue(transaction == null ? TransactionType.EXPENSE : transaction.type());
        dateField.setValue(transaction == null ? LocalDate.now() : transaction.transactionDate());
        if (transaction != null) {
            categoryField.getItems().stream().filter(c -> c.id() == transaction.categoryId())
                    .findFirst().ifPresent(categoryField::setValue);
            amountField.setText(Long.toString(transaction.amount()));
            descriptionField.setText(transaction.description());
        }
    }

    private void updateCategories(TransactionType type) {
        if (categories != null) {
            categoryField.setItems(FXCollections.observableArrayList(
                    categories.stream().filter(category -> category.type() == type).toList()));
            categoryField.setValue(null);
        }
    }

    public TransactionType type() { return typeField.getValue(); }
    public Category category() { return categoryField.getValue(); }
    public String amount() { return amountField.getText().trim(); }
    public String description() { return descriptionField.getText(); }
    public LocalDate date() { return dateField.getValue(); }
    public void setError(String error) { errorLabel.setText(error); }
}
