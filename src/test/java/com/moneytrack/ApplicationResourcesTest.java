package com.moneytrack;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ApplicationResourcesTest {
    @Test
    void applicationResourcesAreOnClasspath() {
        assertNotNull(Main.class.getResource("/fxml/main.fxml"));
        assertNotNull(Main.class.getResource("/fxml/sidebar.fxml"));
        assertNotNull(Main.class.getResource("/fxml/dashboard.fxml"));
        assertNotNull(Main.class.getResource("/fxml/summary-card.fxml"));
        assertNotNull(Main.class.getResource("/fxml/transactions.fxml"));
        assertNotNull(Main.class.getResource("/fxml/transaction-form.fxml"));
        assertNotNull(Main.class.getResource("/fxml/categories.fxml"));
        assertNotNull(Main.class.getResource("/fxml/category-form.fxml"));
        assertNotNull(Main.class.getResource("/fxml/reports.fxml"));
        assertNotNull(Main.class.getResource("/fxml/settings.fxml"));
        assertNotNull(Main.class.getResource("/application.properties"));
        assertNotNull(Main.class.getResource("/css/style.css"));
    }
}
