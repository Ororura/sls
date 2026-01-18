package com.ororura.slseleven.controller;

import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.usecase.LessonUseCase;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Контроллер календаря
 */
public class CalendarController {
    @FXML
    private Label monthYearLabel;
    @FXML
    private GridPane calendarGrid;
    @FXML
    private VBox lessonsList;
    @FXML
    private Button previousMonthButton;
    @FXML
    private Button nextMonthButton;

    private YearMonth currentYearMonth;
    private LocalDate selectedDate;
    private LessonUseCase lessonUseCase;
    private final DateTimeFormatter monthYearFormatter =
            DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("ru"));

    public CalendarController() {
        this.currentYearMonth = YearMonth.now();
    }

    /**
     * Установить use case (вызывается из Application)
     */
    public void setLessonUseCase(LessonUseCase lessonUseCase) {
        this.lessonUseCase = lessonUseCase;
    }

    @FXML
    private void onOpenLessonsList() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/ororura/slseleven/lessons-list.fxml")
            );

            Scene scene = new Scene(loader.load(), 1000, 700);

            LessonsListController controller = loader.getController();
            controller.setLessonUseCase(lessonUseCase);

            Stage stage = new Stage();
            stage.setTitle("Все занятия");
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        previousMonthButton.setOnAction(e -> previousMonth());
        nextMonthButton.setOnAction(e -> nextMonth());
        buildCalendar();
    }

    private void previousMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        buildCalendar();
    }

    private void nextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        buildCalendar();
    }

    private void buildCalendar() {
        monthYearLabel.setText(currentYearMonth.format(monthYearFormatter));
        calendarGrid.getChildren().clear();

        // Заголовки дней недели
        String[] dayNames = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(dayNames[i]);
            dayLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            dayLabel.setStyle("-fx-alignment: center;");
            calendarGrid.add(dayLabel, i, 0);
        }

        // Первый день месяца
        LocalDate firstDay = currentYearMonth.atDay(1);
        int dayOfWeek = firstDay.getDayOfWeek().getValue() - 1; // Понедельник = 0

        // Дни месяца
        int daysInMonth = currentYearMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();

        int currentDayOfWeek = dayOfWeek;
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentYearMonth.atDay(day);
            VBox dayCell = createDayCell(date, day, date.equals(today));
            int row = (day - 1 + dayOfWeek) / 7 + 1;
            int col = currentDayOfWeek;
            calendarGrid.add(dayCell, col, row);
            currentDayOfWeek = (currentDayOfWeek + 1) % 7;
        }

        // Выделить выбранную дату
        if (selectedDate != null && selectedDate.getMonth() == currentYearMonth.getMonth()) {
            showLessonsForDate(selectedDate);
        }
    }

    private VBox createDayCell(LocalDate date, int day, boolean isToday) {
        VBox cell = new VBox(2);
        cell.setPrefSize(100, 80);

        boolean isSelected = date.equals(selectedDate);
        if (isToday && isSelected) {
            cell.setStyle("-fx-border-color: #0066cc; -fx-border-width: 3; -fx-padding: 5; -fx-background-color: #cce6ff;");
        } else if (isToday) {
            cell.setStyle("-fx-border-color: #0066cc; -fx-border-width: 2; -fx-padding: 5; -fx-background-color: #e6f2ff;");
        } else if (isSelected) {
            cell.setStyle("-fx-border-color: #0066cc; -fx-border-width: 2; -fx-padding: 5; -fx-background-color: #f0f8ff;");
        } else {
            cell.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-padding: 5;");
        }

        Label dayLabel = new Label(String.valueOf(day));
        dayLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        cell.getChildren().add(dayLabel);

        // Показать количество занятий на этот день
        if (lessonUseCase != null) {
            List<Lesson> dayLessons = lessonUseCase.getLessonsByDate(date);
            if (!dayLessons.isEmpty()) {
                Label lessonsCount = new Label(dayLessons.size() + " занятий");
                lessonsCount.setFont(Font.font("System", 10));
                lessonsCount.setTextFill(Color.BLUE);
                cell.getChildren().add(lessonsCount);
            }
        }

        // Обработка кликов
        cell.setOnMouseClicked(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                selectDate(date);
            } else if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                showAddLessonDialog(date);
            }
        });

        // Подсветка при наведении
        cell.setOnMouseEntered(e -> {
            if (!isToday && !isSelected) {
                cell.setStyle("-fx-border-color: #0066cc; -fx-border-width: 1; -fx-padding: 5; -fx-background-color: #f0f8ff;");
            }
        });
        cell.setOnMouseExited(e -> {
            if (!isToday && !isSelected) {
                cell.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-padding: 5;");
            } else if (isToday && !isSelected) {
                cell.setStyle("-fx-border-color: #0066cc; -fx-border-width: 2; -fx-padding: 5; -fx-background-color: #e6f2ff;");
            } else if (!isToday) {
                cell.setStyle("-fx-border-color: #0066cc; -fx-border-width: 2; -fx-padding: 5; -fx-background-color: #f0f8ff;");
            } else {
                cell.setStyle("-fx-border-color: #0066cc; -fx-border-width: 3; -fx-padding: 5; -fx-background-color: #cce6ff;");
            }
        });

        return cell;
    }

    private void selectDate(LocalDate date) {
        selectedDate = date;
        showLessonsForDate(date);
        buildCalendar(); // Перестроить для выделения
    }

    private void showLessonsForDate(LocalDate date) {
        lessonsList.getChildren().clear();

        Label dateLabel = new Label(date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru"))));
        dateLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        lessonsList.getChildren().add(dateLabel);

        if (lessonUseCase == null) {
            Label errorLabel = new Label("Ошибка: Use case не инициализирован");
            errorLabel.setStyle("-fx-text-fill: red;");
            lessonsList.getChildren().add(errorLabel);
            return;
        }

        List<Lesson> lessons = lessonUseCase.getLessonsByDate(date);
        if (lessons.isEmpty()) {
            Label noLessonsLabel = new Label("Нет занятий на этот день");
            noLessonsLabel.setStyle("-fx-text-fill: gray;");
            lessonsList.getChildren().add(noLessonsLabel);
        } else {
            for (Lesson lesson : lessons) {
                VBox lessonCard = createLessonCard(lesson);
                lessonsList.getChildren().add(lessonCard);
            }
        }
    }

    private VBox createLessonCard(Lesson lesson) {
        VBox card = new VBox(5);
        card.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5; " +
                "-fx-padding: 10; -fx-background-color: #f9f9f9; -fx-spacing: 5;");
        card.setPrefWidth(300);

        Label timeLabel = new Label(lesson.getTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        timeLabel.setTextFill(Color.BLUE);

        Label topicLabel = new Label("Тема: " + lesson.getTopic());
        Label lessonNameLabel = new Label("Занятие: " + lesson.getLessonName());
        Label locationLabel = new Label("Место: " + lesson.getLocation());
        Label instructorLabel = new Label("Преподаватель: " + lesson.getInstructor());

        card.getChildren().addAll(timeLabel, topicLabel, lessonNameLabel, locationLabel, instructorLabel);

        // Кнопки действий
        HBox actionsBox = new HBox(5);
        Button editButton = new Button("Редактировать");
        Button deleteButton = new Button("Удалить");

        editButton.setOnAction(event -> showEditLessonDialog(lesson));
        deleteButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Подтверждение");
            alert.setHeaderText("Удалить занятие?");
            alert.setContentText("Вы уверены, что хотите удалить это занятие?");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK && lessonUseCase != null) {
                    try {
                        lessonUseCase.deleteLesson(lesson.getId());
                        refreshCalendar();
                    } catch (Exception ex) {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Ошибка");
                        errorAlert.setContentText("Не удалось удалить занятие: " + ex.getMessage());
                        errorAlert.showAndWait();
                    }
                }
            });
        });

        actionsBox.getChildren().addAll(editButton, deleteButton);
        card.getChildren().add(actionsBox);

        return card;
    }

    private void showAddLessonDialog(LocalDate date) {
        showLessonDialog(null, date);
    }

    private void showEditLessonDialog(Lesson lesson) {
        showLessonDialog(lesson, lesson.getDate());
    }

    private void showLessonDialog(Lesson lesson, LocalDate date) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ororura/slseleven/lesson-dialog.fxml"));
            GridPane dialogContent = loader.load();
            LessonDialogController controller = loader.getController();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(lesson == null ? "Добавить занятие" : "Редактировать занятие");
            dialog.getDialogPane().setContent(dialogContent);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            controller.setLesson(lesson, date, lessonUseCase, dialog);

            dialog.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    // Сохранение уже обработано в контроллере
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setContentText("Не удалось открыть диалог: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void refreshCalendar() {
        buildCalendar();
        if (selectedDate != null) {
            showLessonsForDate(selectedDate);
        }
    }
}
