package com.moneytrack.controller;

import com.moneytrack.model.Category;
import com.moneytrack.model.TransactionType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class CategoryFormController {
    @FXML private TextField nameField;
    @FXML private ComboBox<TransactionType> typeField;
    @FXML private Label errorLabel;

    @FXML private void initialize() {
        typeField.setItems(FXCollections.observableArrayList(TransactionType.values()));
        typeField.setConverter(TransactionsController.typeConverter());
    }

    public void configure(Category category, TransactionType defaultType) {
        typeField.setValue(category == null ? defaultType : category.type());
        if (category != null) nameField.setText(category.name());
    }

    public String name() { return nameField.getText(); }
    public TransactionType type() { return typeField.getValue(); }
    public void error(String message) { errorLabel.setText(message); }
}
