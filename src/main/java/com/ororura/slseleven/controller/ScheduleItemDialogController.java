package com.ororura.slseleven.controller;

import com.ororura.slseleven.domain.model.ScheduleItem;
import com.ororura.slseleven.ui.UiAlerts;
import com.ororura.slseleven.ui.UiValidation;
import com.ororura.slseleven.usecase.ScheduleUseCase;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;

public class ScheduleItemDialogController {

    @FXML
    private TextField topicField;

    @FXML
    private TextField lessonNameField;

    @FXML
    private TextField classNameField;

    @FXML
    private TextField locationField;

    @FXML
    private TextField instructorField;

    @FXML
    private TextField hoursField;

    @FXML
    private TextField consecutiveHoursField;

    private ScheduleItem item;
    private ScheduleUseCase scheduleUseCase;

    /**
     * Метод setItem.
     */
    public void setItem(
        ScheduleItem item,
        ScheduleUseCase scheduleUseCase,
        Dialog<ButtonType> dialog
    ) {
        this.item = item;
        this.scheduleUseCase = scheduleUseCase;

        if (item != null) {
            topicField.setText(item.getTopic());
            lessonNameField.setText(item.getLessonName());
            classNameField.setText(item.getClassName());
            locationField.setText(item.getLocation());
            instructorField.setText(item.getInstructor());
            hoursField.setText(String.valueOf(item.getHours()));
            consecutiveHoursField.setText(
                String.valueOf(item.getConsecutiveHours())
            );
        } else {
            consecutiveHoursField.setText("1");
        }

        if (dialog != null) {
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    if (!saveItem()) {
                        return null;
                    }
                }
                return dialogButton;
            });
        }
    }

    /**
     * Метод saveItem.
     */
    private boolean saveItem() {
        try {
            if (scheduleUseCase == null) {
                UiAlerts.showError("Ошибка", "Ошибка: Use case не инициализирован");
                return false;
            }

            Consumer<String> onError = message ->
                UiAlerts.showError("Ошибка", message);

            if (
                !UiValidation.requireNotBlank(
                    topicField,
                    "Предмет не может быть пустым",
                    onError
                )
            ) {
                return false;
            }
            if (
                !UiValidation.requireNotBlank(
                    lessonNameField,
                    "Тема не может быть пустой",
                    onError
                )
            ) {
                return false;
            }
            if (
                !UiValidation.requireNotBlank(
                    classNameField,
                    "Занятие не может быть пустым",
                    onError
                )
            ) {
                return false;
            }
            if (
                !UiValidation.requireNotBlank(
                    hoursField,
                    "Часы не могут быть пустыми",
                    onError
                )
            ) {
                return false;
            }
            if (
                !UiValidation.requireNotBlank(
                    consecutiveHoursField,
                    "Часы подряд для предмета не могут быть пустыми",
                    onError
                )
            ) {
                return false;
            }

            Integer hours = UiValidation.parseInt(
                hoursField,
                "Часы должны быть целым числом",
                onError
            );
            if (hours == null) {
                return false;
            }
            Integer consecutiveHours = UiValidation.parseInt(
                consecutiveHoursField,
                "Часы подряд для предмета должны быть целым числом",
                onError
            );
            if (consecutiveHours == null) {
                return false;
            }
            if (consecutiveHours <= 0) {
                onError.accept("Часы подряд для предмета должны быть больше 0");
                return false;
            }

            boolean isNew = item == null;
            if (isNew) {
                item = new ScheduleItem();
            }

            item.setTopic(topicField.getText().trim());
            item.setLessonName(lessonNameField.getText().trim());
            item.setClassName(classNameField.getText().trim());
            item.setLocation(locationField.getText().trim());
            item.setInstructor(instructorField.getText().trim());
            item.setHours(hours);
            item.setConsecutiveHours(consecutiveHours);

            if (isNew) {
                scheduleUseCase.createItem(item);
            } else {
                scheduleUseCase.updateItem(item);
            }

            return true;
        } catch (Exception e) {
            UiAlerts.showError("Ошибка", "Ошибка при сохранении: " + e.getMessage());
            return false;
        }
    }
}
