package com.ororura.slseleven.controller;

import com.ororura.slseleven.domain.model.ScheduleItem;
import com.ororura.slseleven.usecase.ScheduleUseCase;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;

public class ScheduleItemDialogController {

    @FXML
    private TextField topicField;

    @FXML
    private TextField lessonNameField;

    @FXML
    private TextField locationField;

    @FXML
    private TextField instructorField;

    @FXML
    private TextField hoursField;

    private ScheduleItem item;
    private ScheduleUseCase scheduleUseCase;

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
            locationField.setText(item.getLocation());
            instructorField.setText(item.getInstructor());
            hoursField.setText(String.valueOf(item.getHours()));
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

    private boolean saveItem() {
        try {
            if (scheduleUseCase == null) {
                showError("Ошибка: Use case не инициализирован");
                return false;
            }
            if (topicField.getText().trim().isEmpty()) {
                showError("Предмет не может быть пустым");
                return false;
            }
            if (lessonNameField.getText().trim().isEmpty()) {
                showError("Тема не может быть пустой");
                return false;
            }
            if (locationField.getText().trim().isEmpty()) {
                showError("Место не может быть пустым");
                return false;
            }
            if (instructorField.getText().trim().isEmpty()) {
                showError("Преподаватель не может быть пустым");
                return false;
            }
            if (hoursField.getText().trim().isEmpty()) {
                showError("Часы не могут быть пустыми");
                return false;
            }

            int hours;
            try {
                hours = Integer.parseInt(hoursField.getText().trim());
            } catch (NumberFormatException e) {
                showError("Часы должны быть целым числом");
                return false;
            }

            boolean isNew = item == null;
            if (isNew) {
                item = new ScheduleItem();
            }

            item.setTopic(topicField.getText().trim());
            item.setLessonName(lessonNameField.getText().trim());
            item.setLocation(locationField.getText().trim());
            item.setInstructor(instructorField.getText().trim());
            item.setHours(hours);

            if (isNew) {
                scheduleUseCase.createItem(item);
            } else {
                scheduleUseCase.updateItem(item);
            }

            return true;
        } catch (Exception e) {
            showError("Ошибка при сохранении: " + e.getMessage());
            return false;
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
