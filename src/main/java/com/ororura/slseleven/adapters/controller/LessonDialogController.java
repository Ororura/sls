package com.ororura.slseleven.adapters.controller;

import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.adapters.ui.UiAlerts;
import com.ororura.slseleven.adapters.ui.UiFormatters;
import com.ororura.slseleven.adapters.ui.UiValidation;
import com.ororura.slseleven.application.usecase.LessonUseCase;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.Consumer;
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

    /**
     * Метод initialize.
     */
    @FXML
    public void initialize() {
        // Инициализация при загрузке FXML
    }

    /**
     * Метод setLesson.
     */
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

    /**
     * Метод saveLesson.
     */
    private boolean saveLesson() {
        try {
            if (lessonUseCase == null) {
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
                    timeField,
                    "Время не может быть пустым",
                    onError
                )
            ) {
                return false;
            }
            if (
                !UiValidation.requireNotBlank(
                    locationField,
                    "Место проведения не может быть пустым",
                    onError
                )
            ) {
                return false;
            }
            if (
                !UiValidation.requireNotBlank(
                    instructorField,
                    "Преподаватель не может быть пустым",
                    onError
                )
            ) {
                return false;
            }
            if (datePicker.getValue() == null) {
                onError.accept("Дата не может быть пустой");
                return false;
            }
            if (
                !UiValidation.requireNotBlank(
                    durationHoursField,
                    "Длительность не может быть пустой",
                    onError
                )
            ) {
                return false;
            }

            LocalTime time = UiValidation.parseTime(
                timeField,
                UiFormatters.TIME_FORMATTER,
                "Неверный формат времени. Используйте формат HH:mm (например, 14:30)",
                onError
            );
            if (time == null) {
                return false;
            }

            Integer durationHours = UiValidation.parseInt(
                durationHoursField,
                "Длительность должна быть целым числом",
                onError
            );
            if (durationHours == null) {
                return false;
            }
            if (durationHours <= 0) {
                onError.accept("Длительность должна быть больше 0");
                return false;
            }

            boolean isNew = lesson == null;
            if (isNew) {
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

            if (isNew) {
                lessonUseCase.createLesson(lesson);
            } else {
                lessonUseCase.updateLesson(lesson);
            }
            return true;
        } catch (Exception e) {
            UiAlerts.showError("Ошибка", "Ошибка при сохранении: " + e.getMessage());
            return false;
        }
    }
}
