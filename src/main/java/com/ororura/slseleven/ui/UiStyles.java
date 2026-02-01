package com.ororura.slseleven.ui;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

public final class UiStyles {
    private static final String STYLESHEET =
            "/com/ororura/slseleven/styles.css";

    private UiStyles() {
    }

    public static void apply(Scene scene) {
        if (scene == null) {
            return;
        }
        String url = UiStyles.class.getResource(STYLESHEET).toExternalForm();
        if (!scene.getStylesheets().contains(url)) {
            scene.getStylesheets().add(url);
        }
    }

    public static void apply(DialogPane pane) {
        if (pane == null) {
            return;
        }
        String url = UiStyles.class.getResource(STYLESHEET).toExternalForm();
        if (!pane.getStylesheets().contains(url)) {
            pane.getStylesheets().add(url);
        }
        if (!pane.getStyleClass().contains("app-dialog")) {
            pane.getStyleClass().add("app-dialog");
        }
    }
}
