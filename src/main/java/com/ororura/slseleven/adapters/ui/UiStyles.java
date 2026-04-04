package com.ororura.slseleven.adapters.ui;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

public final class UiStyles {

    private static final String STYLESHEET =
        "/com/ororura/slseleven/styles.css";

    /**
     * Метод UiStyles.
     */
    private UiStyles() {}

    /**
     * Метод apply.
     */
    public static void apply(Scene scene) {
        if (scene == null) {
            return;
        }
        String url = UiStyles.class.getResource(STYLESHEET).toExternalForm();
        if (!scene.getStylesheets().contains(url)) {
            scene.getStylesheets().add(url);
        }
        applyAnimations(scene.getRoot());
    }

    /**
     * Метод apply.
     */
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
        applyAnimations(pane);
    }

    /**
     * Метод applyInteractiveAnimations.
     */
    public static void applyInteractiveAnimations(Node node) {
        if (node == null) {
            return;
        }
        if (Boolean.TRUE.equals(node.getProperties().get("ui-anim-applied"))) {
            return;
        }

        if (
            /**
             * Метод hasAnyStyleClass.
             */
            hasAnyStyleClass(
                node,
                "button-primary",
                "button-secondary",
                "button-ghost",
                "button-danger"
            )
        ) {
            attachHoverScale(node, 1.03, 120);
        }

        if (hasAnyStyleClass(node, "lesson-card", "day-cell")) {
            attachHoverLift(node, 1.02, -3, 160);
        }

        node.getProperties().put("ui-anim-applied", true);
    }

    /**
     * Метод applyAnimations.
     */
    private static void applyAnimations(Parent root) {
        if (root == null) {
            return;
        }
        if (!Boolean.TRUE.equals(root.getProperties().get("ui-fade-applied"))) {
            root.setOpacity(0);
            FadeTransition fade = new FadeTransition(
                Duration.millis(260),
                root
            );
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
            root.getProperties().put("ui-fade-applied", true);
        }

        walk(root);
    }

    /**
     * Метод walk.
     */
    private static void walk(Parent parent) {
        for (Node node : parent.getChildrenUnmodifiable()) {
            applyInteractiveAnimations(node);
            if (node instanceof Parent) {
                walk((Parent) node);
            }
        }
    }

    /**
     * Метод attachHoverScale.
     */
    private static void attachHoverScale(
        Node node,
        double scale,
        int durationMs
    ) {
        ScaleTransition scaleUp = new ScaleTransition(
            Duration.millis(durationMs),
            node
        );
        scaleUp.setToX(scale);
        scaleUp.setToY(scale);

        ScaleTransition scaleDown = new ScaleTransition(
            Duration.millis(durationMs),
            node
        );
        scaleDown.setToX(1);
        scaleDown.setToY(1);

        node.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            scaleDown.stop();
            scaleUp.playFromStart();
        });
        node.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            scaleUp.stop();
            scaleDown.playFromStart();
        });
    }

    /**
     * Метод attachHoverLift.
     */
    private static void attachHoverLift(
        Node node,
        double scale,
        double translateY,
        int durationMs
    ) {
        ScaleTransition scaleUp = new ScaleTransition(
            Duration.millis(durationMs),
            node
        );
        scaleUp.setToX(scale);
        scaleUp.setToY(scale);

        ScaleTransition scaleDown = new ScaleTransition(
            Duration.millis(durationMs),
            node
        );
        scaleDown.setToX(1);
        scaleDown.setToY(1);

        TranslateTransition liftUp = new TranslateTransition(
            Duration.millis(durationMs),
            node
        );
        liftUp.setToY(translateY);

        TranslateTransition liftDown = new TranslateTransition(
            Duration.millis(durationMs),
            node
        );
        liftDown.setToY(0);

        node.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            scaleDown.stop();
            liftDown.stop();
            scaleUp.playFromStart();
            liftUp.playFromStart();
        });
        node.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            scaleUp.stop();
            liftUp.stop();
            scaleDown.playFromStart();
            liftDown.playFromStart();
        });
    }

    /**
     * Метод hasAnyStyleClass.
     */
    private static boolean hasAnyStyleClass(Node node, String... styles) {
        for (String style : styles) {
            if (node.getStyleClass().contains(style)) {
                return true;
            }
        }
        return false;
    }
}
