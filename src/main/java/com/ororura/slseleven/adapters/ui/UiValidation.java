package com.ororura.slseleven.adapters.ui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;
import javafx.scene.control.TextInputControl;

public final class UiValidation {
    /**
     * Метод UiValidation.
     */
    private UiValidation() {}

    /**
     * Метод requireNotBlank.
     */
    public static boolean requireNotBlank(
        TextInputControl field,
        String message,
        Consumer<String> onError
    ) {
        if (field == null || field.getText() == null) {
            onError.accept(message);
            return false;
        }
        if (field.getText().trim().isEmpty()) {
            onError.accept(message);
            return false;
        }
        return true;
    }

    /**
     * Метод parseTime.
     */
    public static LocalTime parseTime(
        TextInputControl field,
        DateTimeFormatter formatter,
        String invalidMessage,
        Consumer<String> onError
    ) {
        try {
            return LocalTime.parse(field.getText().trim(), formatter);
        } catch (DateTimeParseException e) {
            onError.accept(invalidMessage);
            return null;
        }
    }

    /**
     * Метод parseInt.
     */
    public static Integer parseInt(
        TextInputControl field,
        String invalidMessage,
        Consumer<String> onError
    ) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            onError.accept(invalidMessage);
            return null;
        }
    }
}
