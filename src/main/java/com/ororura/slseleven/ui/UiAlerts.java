package com.ororura.slseleven.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class UiAlerts {
    private UiAlerts() {}

    public static void showError(String title, String message) {
        show(Alert.AlertType.ERROR, title, null, message);
    }

    public static void showInfo(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, null, message);
    }

    public static boolean confirm(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private static void show(
        Alert.AlertType type,
        String title,
        String header,
        String message
    ) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        UiStyles.apply(alert.getDialogPane());
        alert.showAndWait();
    }
}
