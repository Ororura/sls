package com.ororura.slseleven.controller;

import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.ui.UiFormatters;
import com.ororura.slseleven.usecase.LessonUseCase;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Контроллер диалога добавления/редактирования занятия
 */
public class LessonDialogController {

    @FXML
    private TextField topicField;

    @FXML
    private TextField lessonNameField;

    @FXML
    private TextField classNameField;

    @FXML
    private TextField timeField;

    @FXML
    private TextField locationField;

    @FXML
    private TextField instructorField;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField durationHoursField;

    private Lesson lesson;
    private LessonUseCase lessonUseCase;

    @FXML
    public void initialize() {
        // Инициализация при загрузке FXML
    }

    public void setLesson(
        Lesson lesson,
        LocalDate date,
        LessonUseCase lessonUseCase,
        Dialog<ButtonType> dialog
    ) {
        this.lesson = lesson;
        this.lessonUseCase = lessonUseCase;

        datePicker.setValue(date);

        if (lesson != null) {
            // Редактирование существующего занятия
            topicField.setText(lesson.getTopic());
            lessonNameField.setText(lesson.getLessonName());
            classNameField.setText(lesson.getClassName());
            timeField.setText(
                lesson.getTime().format(UiFormatters.TIME_FORMATTER)
            );
            locationField.setText(lesson.getLocation());
            instructorField.setText(lesson.getInstructor());
            datePicker.setValue(lesson.getDate());
            durationHoursField.setText(String.valueOf(lesson.getDurationHours()));
        } else {
            durationHoursField.setText("1");
        }

        // Обработка результата диалога
        if (dialog != null) {
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    if (!saveLesson()) {
                        return null; // Предотвратить закрытие диалога при ошибке
                    }
                }
                return dialogButton;
            });
        }
    }

    private boolean saveLesson() {
        try {
            if (lessonUseCase == null) {
                showError("Ошибка: Use case не инициализирован");
                return false;
            }
            // Валидация
            if (topicField.getText().trim().isEmpty()) {
                showError("Предмет не может быть пустым");
                return false;
            }
            if (lessonNameField.getText().trim().isEmpty()) {
                showError("Тема не может быть пустой");
                return false;
            }
            if (classNameField.getText().trim().isEmpty()) {
                showError("Занятие не может быть пустым");
                return false;
            }
            if (timeField.getText().trim().isEmpty()) {
                showError("Время не может быть пустым");
                return false;
            }
            if (locationField.getText().trim().isEmpty()) {
                showError("Место проведения не может быть пустым");
                return false;
            }
            if (instructorField.getText().trim().isEmpty()) {
                showError("Преподаватель не может быть пустым");
                return false;
            }
            if (datePicker.getValue() == null) {
                showError("Дата не может быть пустой");
                return false;
            }
            if (durationHoursField.getText().trim().isEmpty()) {
                showError("Длительность не может быть пустой");
                return false;
            }

            // Парсинг времени
            LocalTime time;
            try {
                time = LocalTime.parse(
                    timeField.getText().trim(),
                    UiFormatters.TIME_FORMATTER
                );
            } catch (DateTimeParseException e) {
                showError(
                    "Неверный формат времени. Используйте формат HH:mm (например, 14:30)"
                );
                return false;
            }
            int durationHours;
            try {
                durationHours = Integer.parseInt(
                    durationHoursField.getText().trim()
                );
            } catch (NumberFormatException e) {
                showError("Длительность должна быть целым числом");
                return false;
            }
            if (durationHours <= 0) {
                showError("Длительность должна быть больше 0");
                return false;
            }

            // Создание или обновление занятия
            if (lesson == null) {
                lesson = new Lesson();
            }

            lesson.setTopic(topicField.getText().trim());
            lesson.setLessonName(lessonNameField.getText().trim());
            lesson.setClassName(classNameField.getText().trim());
            lesson.setTime(time);
            lesson.setLocation(locationField.getText().trim());
            lesson.setInstructor(instructorField.getText().trim());
            lesson.setDate(datePicker.getValue());
            lesson.setDurationHours(durationHours);

            if (
                lesson.getId() == null ||
                !lessonUseCase.getLessonById(lesson.getId()).isPresent()
            ) {
                // Создание нового занятия
                lessonUseCase.createLesson(lesson);
            } else {
                // Обновление существующего занятия
                lessonUseCase.updateLesson(lesson);
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
