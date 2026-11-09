package com.moneytrack.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SummaryCardController {
    @FXML private Label titleLabel;
    @FXML private Label iconLabel;
    @FXML private Label amountLabel;
    @FXML private Label detailLabel;

    public void configure(String title, String icon, String amount, String detail, String iconStyle) {
        titleLabel.setText(title);
        iconLabel.setText(icon);
        if (iconStyle != null && !iconLabel.getStyleClass().contains(iconStyle)) {
            iconLabel.getStyleClass().add(iconStyle);
        }
        amountLabel.setText(amount);
        detailLabel.setText(detail);
    }
}
