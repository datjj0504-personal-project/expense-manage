package com.moneytrack.controller;

import java.util.List;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class SidebarController {
    public enum Screen { DASHBOARD, TRANSACTIONS, CATEGORIES, REPORTS, SETTINGS }

    @FXML private Label navigationMessage;
    @FXML private Button dashboardButton;
    @FXML private Button transactionsButton;
    @FXML private Button categoriesButton;
    @FXML private Button reportsButton;
    @FXML private Button settingsButton;
    private Consumer<Screen> navigation;

    public void setNavigation(Consumer<Screen> navigation) {
        this.navigation = navigation;
    }

    public void setActive(Screen screen) {
        List<Button> buttons = List.of(dashboardButton, transactionsButton, categoriesButton, reportsButton, settingsButton);
        buttons.forEach(button -> button.getStyleClass().remove("nav-active"));
        Button active = buttons.get(screen.ordinal());
        active.getStyleClass().add("nav-active");
        navigationMessage.setText(active.getText());
    }

    @FXML private void showDashboard() { navigation.accept(Screen.DASHBOARD); }
    @FXML private void showTransactions() { navigation.accept(Screen.TRANSACTIONS); }
    @FXML private void showCategories() { navigation.accept(Screen.CATEGORIES); }
    @FXML private void showReports() { navigation.accept(Screen.REPORTS); }
    @FXML private void showSettings() { navigation.accept(Screen.SETTINGS); }
}
