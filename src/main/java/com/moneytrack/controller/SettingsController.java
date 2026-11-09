package com.moneytrack.controller;

import com.moneytrack.database.DatabaseManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class SettingsController {
    @FXML private Label versionLabel;
    @FXML private TextField databasePath;

    public void configure(DatabaseManager database) throws IOException {
        databasePath.setText(database.getDatabaseFile().toString());
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application.properties")) {
            if (input == null) throw new IOException("Application version information is missing.");
            properties.load(input);
        }
        String version = properties.getProperty("application.version");
        if (version == null || version.isBlank()) throw new IOException("Application version is missing.");
        versionLabel.setText(version);
    }
}
