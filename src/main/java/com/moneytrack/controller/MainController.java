package com.moneytrack.controller;

import com.moneytrack.controller.SidebarController.Screen;
import com.moneytrack.database.DatabaseManager;
import com.moneytrack.repository.CategoryRepository;
import com.moneytrack.repository.TransactionRepository;
import com.moneytrack.service.CategoryService;
import com.moneytrack.service.DashboardService;
import com.moneytrack.service.TransactionService;
import java.io.IOException;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;

public class MainController {
    @FXML private BorderPane root;
    @FXML private ScrollPane dashboardView;
    @FXML private SidebarController sidebarController;
    @FXML private DashboardController dashboardController;
    private DatabaseManager database;
    private TransactionService transactionService;
    private CategoryService categoryService;
    private DashboardService dashboardService;
    private Parent transactionsView;
    private TransactionsController transactionsController;
    private Parent categoriesView;
    private CategoriesController categoriesController;
    private Parent reportsView;
    private ReportsController reportsController;
    private Parent settingsView;

    @FXML private void initialize() {
        sidebarController.setNavigation(this::navigate);
    }

    public void setDatabase(DatabaseManager database) {
        this.database = Objects.requireNonNull(database);
        TransactionRepository transactions = new TransactionRepository(database);
        CategoryRepository categories = new CategoryRepository(database);
        transactionService = new TransactionService(transactions, categories);
        categoryService = new CategoryService(categories);
        dashboardService = new DashboardService(transactions);
        dashboardController.setService(dashboardService);
        dashboardController.refresh();
    }

    private void navigate(Screen screen) {
        if (database == null) {
            showError("The database is not initialized.");
            return;
        }
        try {
            switch (screen) {
                case DASHBOARD -> {
                    root.setCenter(dashboardView);
                    dashboardController.refresh();
                }
                case TRANSACTIONS -> {
                    if (transactionsView == null) {
                        FXMLLoader loader = loader("transactions.fxml");
                        transactionsView = loader.load();
                        transactionsController = loader.getController();
                        transactionsController.setService(transactionService);
                        transactionsController.setOnChanged(dashboardController::refresh);
                    }
                    root.setCenter(transactionsView);
                    transactionsController.refresh();
                }
                case CATEGORIES -> {
                    if (categoriesView == null) {
                        FXMLLoader loader = loader("categories.fxml");
                        categoriesView = loader.load();
                        categoriesController = loader.getController();
                        categoriesController.configure(categoryService, dashboardController::refresh);
                    }
                    root.setCenter(categoriesView);
                    categoriesController.refresh();
                }
                case REPORTS -> {
                    if (reportsView == null) {
                        FXMLLoader loader = loader("reports.fxml");
                        reportsView = loader.load();
                        reportsController = loader.getController();
                        reportsController.setService(dashboardService);
                    }
                    root.setCenter(reportsView);
                    reportsController.refresh();
                }
                case SETTINGS -> {
                    if (settingsView == null) {
                        FXMLLoader loader = loader("settings.fxml");
                        Parent view = loader.load();
                        SettingsController settings = loader.getController();
                        settings.configure(database);
                        settingsView = view;
                    }
                    root.setCenter(settingsView);
                }
            }
            sidebarController.setActive(screen);
        } catch (IOException e) {
            showError("Could not open " + screen.name().toLowerCase() + ": " + e.getMessage());
        }
    }

    private FXMLLoader loader(String name) throws IOException {
        var resource = getClass().getResource("/fxml/" + name);
        if (resource == null) throw new IOException("Missing view: " + name);
        return new FXMLLoader(resource);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(root.getScene().getWindow());
        alert.setHeaderText("Unable to open screen");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
