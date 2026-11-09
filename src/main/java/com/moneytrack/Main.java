package com.moneytrack;

import com.moneytrack.database.DatabaseManager;
import com.moneytrack.controller.MainController;
import java.io.IOException;
import java.util.Objects;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private DatabaseManager database;

    @Override
    public void init() throws Exception {
        database = new DatabaseManager();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                Main.class.getResource("/fxml/main.fxml"), "Missing main.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 750);
        ((MainController) loader.getController()).setDatabase(database);
        scene.getStylesheets().add(Objects.requireNonNull(
                Main.class.getResource("/css/style.css"), "Missing style.css").toExternalForm());

        stage.setTitle("MoneyTrack");
        stage.setMinWidth(950);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.setWidth(1200);
        stage.setHeight(750);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
