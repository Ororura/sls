package com.ororura.slseleven.adapters.ui;

import java.util.List;
import java.util.StringJoiner;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class UiAlerts {
    /**
     * Метод UiAlerts.
     */
    private UiAlerts() {}

    /**
     * Метод showError.
     */
    public static void showError(String title, String message) {
        show(Alert.AlertType.ERROR, title, null, message);
    }

    /**
     * Метод showInfo.
     */
    public static void showInfo(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, null, message);
    }

    /**
     * Метод confirm.
     */
    public static boolean confirm(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        UiStyles.apply(alert.getDialogPane());
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    public static void showImportResult(
        String title,
        int imported,
        List<String> errors,
        int previewLimit
    ) {
        StringJoiner joiner = new StringJoiner(System.lineSeparator());
        joiner.add("Импортировано строк: " + imported);
        if (errors != null && !errors.isEmpty()) {
            int limit = Math.min(previewLimit, errors.size());
            for (int i = 0; i < limit; i++) {
                joiner.add(errors.get(i));
            }
            if (errors.size() > limit) {
                joiner.add("Ошибок ещё: " + (errors.size() - limit));
            }
        }
        showInfo(title, joiner.toString());
    }

    /**
     * Метод show.
     */
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
