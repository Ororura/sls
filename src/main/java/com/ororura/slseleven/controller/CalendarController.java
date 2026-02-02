package com.ororura.slseleven.controller;

import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.ui.UiFormatters;
import com.ororura.slseleven.ui.UiStyles;
import com.ororura.slseleven.usecase.LessonUseCase;
import com.ororura.slseleven.usecase.ScheduleUseCase;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
    private ScheduleUseCase scheduleUseCase;
    private final Map<LocalDate, List<Lesson>> monthLessons = new HashMap<>();
    private final DateTimeFormatter monthYearFormatter =
        UiFormatters.MONTH_YEAR_FORMATTER;
    private static final PseudoClass PSEUDO_TODAY = PseudoClass.getPseudoClass(
        "today"
    );
    private static final PseudoClass PSEUDO_SELECTED =
        PseudoClass.getPseudoClass("selected");

    public CalendarController() {
        this.currentYearMonth = YearMonth.now();
    }

    /**
     * Установить use case (вызывается из Application)
     */
    public void setLessonUseCase(LessonUseCase lessonUseCase) {
        this.lessonUseCase = lessonUseCase;
        if (this.lessonUseCase != null) {
            this.lessonUseCase.archivePastLessons(LocalDate.now());
        }
    }

    public void setScheduleUseCase(ScheduleUseCase scheduleUseCase) {
        this.scheduleUseCase = scheduleUseCase;
    }

    @FXML
    private void onOpenLessonsList() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                    "/com/ororura/slseleven/lessons-list.fxml"
                )
            );

            Scene scene = new Scene(loader.load(), 1000, 700);
            UiStyles.apply(scene);

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
    private void onOpenSchedulePlanner() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                    "/com/ororura/slseleven/schedule-planner.fxml"
                )
            );

            Scene scene = new Scene(loader.load(), 1100, 700);
            UiStyles.apply(scene);

            SchedulePlannerController controller = loader.getController();
            controller.setScheduleUseCase(scheduleUseCase);

            Stage stage = new Stage();
            stage.setTitle("Авторасписание");
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
        if (lessonUseCase != null) {
            lessonUseCase.archivePastLessons(LocalDate.now());
        }
        monthYearLabel.setText(currentYearMonth.format(monthYearFormatter));
        calendarGrid.getChildren().clear();
        preloadMonthLessons();

        // Заголовки дней недели
        String[] dayNames = { "Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс" };
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(dayNames[i]);
            dayLabel.getStyleClass().add("dow-label");
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);
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
        if (
            selectedDate != null &&
            YearMonth.from(selectedDate).equals(currentYearMonth)
        ) {
            showLessonsForDate(selectedDate);
        }
    }

    private void preloadMonthLessons() {
        monthLessons.clear();
        if (lessonUseCase == null) {
            return;
        }

        LocalDate start = currentYearMonth.atDay(1);
        LocalDate end = currentYearMonth.atEndOfMonth();
        List<Lesson> lessons = lessonUseCase.getLessonsByDateRange(start, end);
        for (Lesson lesson : lessons) {
            monthLessons
                .computeIfAbsent(lesson.getDate(), key -> new ArrayList<>())
                .add(lesson);
        }
    }

    private VBox createDayCell(LocalDate date, int day, boolean isToday) {
        VBox cell = new VBox(2);
        cell.getStyleClass().add("day-cell");
        cell.setPrefSize(100, 80);
        UiStyles.applyInteractiveAnimations(cell);

        boolean isSelected = date.equals(selectedDate);
        cell.pseudoClassStateChanged(PSEUDO_TODAY, isToday);
        cell.pseudoClassStateChanged(PSEUDO_SELECTED, isSelected);

        Label dayLabel = new Label(String.valueOf(day));
        dayLabel.getStyleClass().add("day-number");
        cell.getChildren().add(dayLabel);

        // Показать количество занятий на этот день
        if (!monthLessons.isEmpty()) {
            List<Lesson> dayLessons = monthLessons.get(date);
            if (dayLessons != null && !dayLessons.isEmpty()) {
                Label lessonsCount = new Label(dayLessons.size() + " занятий");
                lessonsCount.getStyleClass().add("lesson-count");
                cell.getChildren().add(lessonsCount);
            }
        }

        // Обработка кликов
        cell.setOnMouseClicked(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                selectDate(date);
            } else if (
                event.getButton() == javafx.scene.input.MouseButton.SECONDARY
            ) {
                showAddLessonDialog(date);
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

        Label dateLabel = new Label(
            date.format(UiFormatters.LONG_DATE_FORMATTER)
        );
        dateLabel.getStyleClass().add("section-title");
        lessonsList.getChildren().add(dateLabel);

        if (lessonUseCase == null) {
            Label errorLabel = new Label("Ошибка: Use case не инициализирован");
            errorLabel.getStyleClass().add("error-text");
            lessonsList.getChildren().add(errorLabel);
            return;
        }

        List<Lesson> lessons;
        if (
            YearMonth.from(date).equals(currentYearMonth) &&
            !monthLessons.isEmpty()
        ) {
            lessons = monthLessons.getOrDefault(date, List.of());
        } else if (lessonUseCase != null) {
            lessons = lessonUseCase.getLessonsByDate(date);
        } else {
            lessons = List.of();
        }
        if (lessons.isEmpty()) {
            Label noLessonsLabel = new Label("Нет занятий на этот день");
            noLessonsLabel.getStyleClass().add("muted");
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
        card.getStyleClass().add("lesson-card");
        card.setPrefWidth(300);
        UiStyles.applyInteractiveAnimations(card);

        Label timeLabel = new Label(
            lesson.getTime().format(UiFormatters.TIME_FORMATTER)
        );
        timeLabel.getStyleClass().add("lesson-time");

        Label topicLabel = new Label("Предмет: " + lesson.getTopic());
        Label lessonNameLabel = new Label("Тема: " + lesson.getLessonName());
        Label classNameLabel = new Label("Занятие: " + lesson.getClassName());
        Label locationLabel = new Label("Место: " + lesson.getLocation());
        Label instructorLabel = new Label(
            "Преподаватель: " + lesson.getInstructor()
        );
        topicLabel.getStyleClass().add("lesson-meta");
        lessonNameLabel.getStyleClass().add("lesson-meta");
        classNameLabel.getStyleClass().add("lesson-meta");
        locationLabel.getStyleClass().add("lesson-meta");
        instructorLabel.getStyleClass().add("lesson-meta");

        card
            .getChildren()
            .addAll(
                timeLabel,
                topicLabel,
                lessonNameLabel,
                classNameLabel,
                locationLabel,
                instructorLabel
            );

        // Кнопки действий
        HBox actionsBox = new HBox(5);
        actionsBox.getStyleClass().add("card-actions");
        Button editButton = new Button("Редактировать");
        Button deleteButton = new Button("Удалить");
        editButton.getStyleClass().add("button-secondary");
        deleteButton.getStyleClass().add("button-danger");

        editButton.setOnAction(event -> showEditLessonDialog(lesson));
        deleteButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Подтверждение");
            alert.setHeaderText("Удалить занятие?");
            alert.setContentText("Вы уверены, что хотите удалить это занятие?");
            alert
                .showAndWait()
                .ifPresent(response -> {
                    if (response == ButtonType.OK && lessonUseCase != null) {
                        try {
                            lessonUseCase.deleteLesson(lesson.getId());
                            refreshCalendar();
                        } catch (Exception ex) {
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Ошибка");
                            errorAlert.setContentText(
                                "Не удалось удалить занятие: " + ex.getMessage()
                            );
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
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                    "/com/ororura/slseleven/lesson-dialog.fxml"
                )
            );
            GridPane dialogContent = loader.load();
            LessonDialogController controller = loader.getController();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(
                lesson == null ? "Добавить занятие" : "Редактировать занятие"
            );
            dialog.getDialogPane().setContent(dialogContent);
            dialog
                .getDialogPane()
                .getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);
            UiStyles.apply(dialog.getDialogPane());

            controller.setLesson(lesson, date, lessonUseCase, dialog);

            dialog
                .showAndWait()
                .ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        // Сохранение уже обработано в контроллере
                        refreshCalendar();
                    }
                });
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setContentText(
                "Не удалось открыть диалог: " + e.getMessage()
            );
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
