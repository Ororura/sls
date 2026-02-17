package com.ororura.slseleven.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import com.ororura.slseleven.domain.model.AppCalendar;
import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.ui.UiAlerts;
import com.ororura.slseleven.ui.UiFormatters;
import com.ororura.slseleven.ui.UiStyles;
import com.ororura.slseleven.usecase.LessonUseCase;
import com.ororura.slseleven.usecase.ScheduleUseCase;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Контроллер календаря
 */
public class CalendarController {
    private static final String ALL_CALENDARS_ID = "__all__";
    private static final String DIR_ALL = "Все директории";
    private static final String DIR_ROOT = "Без директории";
    private static final AppCalendar ALL_CALENDARS_OPTION = new AppCalendar(
        ALL_CALENDARS_ID,
        "Общий (все календари)"
    );

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

    @FXML
    private ComboBox<AppCalendar> calendarComboBox;

    @FXML
    private ComboBox<String> directoryComboBox;

    @FXML
    private Label currentDateLabel;

    @FXML
    private Label currentTimeLabel;

    @FXML
    private Label currentLessonLabel;

    private YearMonth currentYearMonth;
    private LocalDate selectedDate;
    private LessonUseCase lessonUseCase;
    private ScheduleUseCase scheduleUseCase;
    private final ObservableList<AppCalendar> calendars =
        FXCollections.observableArrayList();
    private final ObservableList<String> directories =
        FXCollections.observableArrayList();
    private final List<AppCalendar> allCalendarsLoaded = new ArrayList<>();
    private boolean updatingCalendarSelection;
    private boolean allCalendarsMode;
    private final Map<LocalDate, List<Lesson>> monthLessons = new HashMap<>();
    private final DateTimeFormatter monthYearFormatter =
        UiFormatters.MONTH_YEAR_FORMATTER;
    private final DateTimeFormatter statusDateFormatter =
        UiFormatters.LONG_DATE_FORMATTER;
    private final DateTimeFormatter statusTimeFormatter =
        UiFormatters.TIME_FORMATTER;
    private Timeline headerTimeline;
    private static final String STATUS_LESSON_CURRENT_CLASS =
        "status-lesson-current";
    private static final String STATUS_LESSON_NEXT_CLASS = "status-lesson-next";
    private static final String STATUS_LESSON_NONE_CLASS = "status-lesson-none";
    private static final PseudoClass PSEUDO_TODAY = PseudoClass.getPseudoClass(
        "today"
    );
    private static final PseudoClass PSEUDO_SELECTED =
        PseudoClass.getPseudoClass("selected");
    private static final List<LocalTime> LESSON_SLOT_START_TIMES_ORDERED =
        List.of(
            LocalTime.of(9, 0),
            LocalTime.of(9, 50),
            LocalTime.of(10, 50),
            LocalTime.of(11, 40),
            LocalTime.of(12, 40),
            LocalTime.of(13, 30),
            LocalTime.of(16, 0),
            LocalTime.of(16, 50)
        );
    private static final Map<LocalTime, LocalTime> LESSON_END_BY_START =
        Map.of(
            LocalTime.of(9, 0),
            LocalTime.of(9, 50),
            LocalTime.of(9, 50),
            LocalTime.of(10, 40),
            LocalTime.of(10, 50),
            LocalTime.of(11, 40),
            LocalTime.of(11, 40),
            LocalTime.of(12, 30),
            LocalTime.of(12, 40),
            LocalTime.of(13, 30),
            LocalTime.of(13, 30),
            LocalTime.of(14, 20),
            LocalTime.of(16, 0),
            LocalTime.of(16, 50),
            LocalTime.of(16, 50),
            LocalTime.of(17, 40)
        );
    private static final int PRINT_A4_WIDTH = 2480;
    private static final int PRINT_A4_HEIGHT = 3508;

    public CalendarController() {
        this.currentYearMonth = YearMonth.now();
    }

    /**
     * Установить use case (вызывается из Application)
     */
    public void setLessonUseCase(LessonUseCase lessonUseCase) {
        this.lessonUseCase = lessonUseCase;
        applyCalendarSelection();
        refreshCalendar();
        updateHeaderStatus();
    }

    public void setScheduleUseCase(ScheduleUseCase scheduleUseCase) {
        this.scheduleUseCase = scheduleUseCase;
        loadCalendars();
    }

    @FXML
    private void onOpenLessonsList() {
        if (allCalendarsMode) {
            showInfo("В режиме общего календаря выберите конкретный календарь для списка занятий.");
            return;
        }
        applyCalendarSelection();
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
            controller.setScheduleUseCase(scheduleUseCase);

            Stage stage = new Stage();
            stage.setTitle("Все занятия");
            stage.setScene(scene);
            stage.setOnHidden(event -> refreshCalendar());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onOpenSchedulePlanner() {
        if (allCalendarsMode) {
            showInfo("В режиме общего календаря выберите конкретный календарь для авторасписания.");
            return;
        }
        applyCalendarSelection();
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
            stage.setOnHidden(event -> refreshCalendar());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onAboutApp() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("О приложении");
        dialog.setHeaderText(null);

        Label title = new Label("Авторы проекта");
        title.getStyleClass().add("about-title");

        Label ideaLabel = new Label("Автор идеи");
        ideaLabel.getStyleClass().add("about-label");
        Label ideaValue = new Label("капитан 2 ранга Киселев О. А.");
        ideaValue.getStyleClass().add("about-line");

        Label devLabel = new Label("Разработчик");
        devLabel.getStyleClass().add("about-label");
        Label devValue = new Label("старший матрос Гладких Е. Ю.");
        devValue.getStyleClass().add("about-line");

        VBox content = new VBox(
            10,
            title,
            new Separator(),
            ideaLabel,
            ideaValue,
            devLabel,
            devValue
        );
        content.getStyleClass().add("about-card");

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        UiStyles.apply(dialog.getDialogPane());
        dialog.showAndWait();
    }

    @FXML
    public void initialize() {
        previousMonthButton.setOnAction(e -> previousMonth());
        nextMonthButton.setOnAction(e -> nextMonth());
        directoryComboBox.setItems(directories);
        directoryComboBox
            .getSelectionModel()
            .selectedItemProperty()
            .addListener((obs, oldValue, newValue) -> {
                if (updatingCalendarSelection) {
                    return;
                }
                applyDirectoryFilter();
            });
        configureDirectoryComboCells();
        calendarComboBox.setItems(calendars);
        configureCalendarComboCells();
        calendarComboBox
            .getSelectionModel()
            .selectedItemProperty()
            .addListener((obs, oldValue, newValue) -> {
                if (updatingCalendarSelection || newValue == null) {
                    return;
                }
                switchCalendar(newValue);
            });
        startHeaderTicker();
        buildCalendar();
    }

    private void configureCalendarComboCells() {
        calendarComboBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AppCalendar item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(formatCalendarForCombo(item));
            }
        });
        calendarComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AppCalendar item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(formatCalendarForCombo(item));
            }
        });
    }

    private void configureDirectoryComboCells() {
        directoryComboBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatDirectoryLabel(item));
            }
        });
        directoryComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatDirectoryLabel(item));
            }
        });
    }

    private String formatDirectoryLabel(String directory) {
        if (directory == null || directory.isBlank()) {
            return DIR_ALL;
        }
        if (DIR_ALL.equals(directory) || DIR_ROOT.equals(directory)) {
            return directory;
        }
        return directory.replace("/", " / ");
    }

    private String formatCalendarForCombo(AppCalendar calendar) {
        if (calendar == null) {
            return "";
        }
        if (ALL_CALENDARS_ID.equals(calendar.getId())) {
            return "Все календари";
        }
        String path = calendar.getDirectoryPath();
        if (path == null || path.isBlank()) {
            return calendar.getName() + "  [Корень]";
        }
        return calendar.getName() + "  [" + path.replace("/", " / ") + "]";
    }

    @FXML
    private void onCreateCalendar() {
        if (scheduleUseCase == null) {
            showError("Ошибка: модуль календарей не инициализирован");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Новый календарь");
        dialog.setHeaderText("Создание календаря");
        TextField directoryField = new TextField();
        directoryField.setPromptText("Например: 2 бат/2 рота/ОВП");
        TextField nameField = new TextField();
        nameField.setPromptText("Название календаря");
        VBox content = new VBox(
            8,
            new Label("Директория (необязательно):"),
            directoryField,
            new Label("Название календаря:"),
            nameField
        );
        VBox.setVgrow(directoryField, Priority.NEVER);
        VBox.setVgrow(nameField, Priority.NEVER);
        dialog.getDialogPane().setContent(content);
        dialog
            .getDialogPane()
            .getButtonTypes()
            .addAll(ButtonType.OK, ButtonType.CANCEL);
        UiStyles.apply(dialog.getDialogPane());

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            AppCalendar created = scheduleUseCase.createCalendar(
                nameField.getText(),
                directoryField.getText()
            );
            scheduleUseCase.setCurrentCalendarId(created.getId());
            allCalendarsMode = false;
            applyCalendarSelection();
            loadCalendars();
        } catch (Exception ex) {
            showError("Не удалось создать календарь: " + ex.getMessage());
        }
    }

    @FXML
    private void onMoveCalendarToDirectory() {
        AppCalendar selected = calendarComboBox
            .getSelectionModel()
            .getSelectedItem();
        if (selected == null || scheduleUseCase == null) {
            showError("Выберите календарь");
            return;
        }
        if (ALL_CALENDARS_ID.equals(selected.getId())) {
            showError("Общий календарь нельзя перенести в папку");
            return;
        }

        List<String> knownDirectories = new ArrayList<>(
            scheduleUseCase.getCalendarDirectories()
        );
        if (
            selected.getDirectoryPath() != null &&
            !selected.getDirectoryPath().isBlank() &&
            !knownDirectories.contains(selected.getDirectoryPath())
        ) {
            knownDirectories.add(selected.getDirectoryPath());
        }
        knownDirectories.sort(String.CASE_INSENSITIVE_ORDER);

        TextInputDialog dialog = new TextInputDialog(selected.getDirectoryPath());
        dialog.setTitle("Папка календаря");
        dialog.setHeaderText(
            "Укажите директорию для \"" + selected.getName() + "\""
        );
        dialog.setContentText("Путь (например 2 бат/2 рота/ОВП, пусто = корень):");
        UiStyles.apply(dialog.getDialogPane());
        if (!knownDirectories.isEmpty()) {
            dialog
                .getEditor()
                .setPromptText("Доступные: " + String.join(", ", knownDirectories));
        }

        dialog
            .showAndWait()
            .ifPresent(path -> {
                try {
                    scheduleUseCase.moveCalendarToDirectory(selected.getId(), path);
                    loadCalendars();
                } catch (Exception ex) {
                    showError(
                        "Не удалось перенести календарь: " + ex.getMessage()
                    );
                }
            });
    }

    @FXML
    private void onRenameCalendar() {
        AppCalendar selected = calendarComboBox
            .getSelectionModel()
            .getSelectedItem();
        if (selected == null || scheduleUseCase == null) {
            showError("Выберите календарь для переименования");
            return;
        }
        if (ALL_CALENDARS_ID.equals(selected.getId())) {
            showError("Общий календарь переименовать нельзя");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("Переименование календаря");
        dialog.setHeaderText("Изменение названия");
        dialog.setContentText("Новое название:");
        UiStyles.apply(dialog.getDialogPane());

        dialog
            .showAndWait()
            .ifPresent(name -> {
                try {
                    scheduleUseCase.renameCalendar(selected.getId(), name);
                    loadCalendars();
                } catch (Exception ex) {
                    showError(
                        "Не удалось переименовать календарь: " + ex.getMessage()
                    );
                }
            });
    }

    @FXML
    private void onDeleteCalendar() {
        AppCalendar selected = calendarComboBox
            .getSelectionModel()
            .getSelectedItem();
        if (selected == null || scheduleUseCase == null) {
            showError("Выберите календарь для удаления");
            return;
        }
        if (ALL_CALENDARS_ID.equals(selected.getId())) {
            showError("Общий календарь удалить нельзя");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Удаление календаря");
        confirm.setHeaderText(
            "Удалить календарь \"" + selected.getName() + "\"?"
        );
        confirm.setContentText("Будут удалены все занятия и часы этого календаря.");
        UiStyles.apply(confirm.getDialogPane());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            scheduleUseCase.deleteCalendar(selected.getId());
            applyCalendarSelection();
            loadCalendars();
        } catch (Exception ex) {
            showError("Не удалось удалить календарь: " + ex.getMessage());
        }
    }

    private void previousMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        buildCalendar();
    }

    private void nextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        buildCalendar();
    }

    @FXML
    private void onCurrentMonth() {
        currentYearMonth = YearMonth.now();
        buildCalendar();
    }

    @FXML
    private void onExportPrintImage() {
        if (lessonUseCase == null) {
            showError("Ошибка: модуль занятий не инициализирован");
            return;
        }
        preloadMonthLessons();
        Set<LocalDate> selectedDates = chooseDaysForPngPrint();
        if (selectedDates == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить календарь для печати");
        chooser
            .getExtensionFilters()
            .add(new FileChooser.ExtensionFilter("PNG (*.png)", "*.png"));
        chooser.setInitialFileName(
            "calendar_print_" + currentYearMonth + ".png"
        );
        File file = chooser.showSaveDialog(
            calendarGrid.getScene() == null ? null : calendarGrid.getScene().getWindow()
        );
        if (file == null) {
            return;
        }

        try {
            Canvas canvas = buildPrintCanvas(selectedDates);
            WritableImage image = canvas.snapshot(new SnapshotParameters(), null);
            BufferedImage bufferedImage = toBufferedImage(image);
            ImageIO.write(bufferedImage, "png", file);
            showInfo("Файл сохранён: " + file.getName());
        } catch (Exception ex) {
            showError("Не удалось сформировать изображение: " + ex.getMessage());
        }
    }

    @FXML
    private void onExportPrintExcel() {
        if (lessonUseCase == null) {
            showError("Ошибка: модуль занятий не инициализирован");
            return;
        }
        preloadMonthLessons();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить календарь для печати (Excel)");
        chooser
            .getExtensionFilters()
            .add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        chooser.setInitialFileName(
            "calendar_print_" + currentYearMonth + ".xlsx"
        );
        File file = chooser.showSaveDialog(
            calendarGrid.getScene() == null ? null : calendarGrid.getScene().getWindow()
        );
        if (file == null) {
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            buildPrintSheet(workbook);
            try (java.io.FileOutputStream output = new java.io.FileOutputStream(file)) {
                workbook.write(output);
            }
            showInfo("Файл сохранён: " + file.getName());
        } catch (Exception ex) {
            showError("Не удалось сформировать Excel: " + ex.getMessage());
        }
    }

    private void buildCalendar() {
        if (lessonUseCase != null && !allCalendarsMode) {
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

        ensureSelectedDateForCurrentMonth();
        showLessonsForDate(selectedDate);
        updateHeaderStatus();
    }

    private Canvas buildPrintCanvas(Set<LocalDate> selectedDates) {
        Canvas canvas = new Canvas(PRINT_A4_WIDTH, PRINT_A4_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, PRINT_A4_WIDTH, PRINT_A4_HEIGHT);

        Font titleFont = Font.font("System", FontWeight.BOLD, 64);
        Font subtitleFont = Font.font("System", FontWeight.NORMAL, 34);
        Font dayHeaderFont = Font.font("System", FontWeight.BOLD, 24);
        Font dayNumberFont = Font.font("System", FontWeight.BOLD, 24);
        Font lessonFont = Font.font("System", FontWeight.NORMAL, 16);

        double marginX = 80;
        double headerTop = 78;
        double dayHeaderTop = 200;
        double gridTop = 246;
        double footerBottom = 90;
        double gridHeight = PRINT_A4_HEIGHT - gridTop - footerBottom;
        double cellWidth = (PRINT_A4_WIDTH - marginX * 2) / 7.0;
        double cellHeight = gridHeight / 6.0;

        String calendarName = allCalendarsMode
            ? "Все календари"
            : resolveCalendarName(
                scheduleUseCase == null ? null : scheduleUseCase.getCurrentCalendarId()
            );
        String title = "Календарь занятий";
        String subtitle = currentYearMonth.format(monthYearFormatter) +
        " • " + safe(calendarName);

        gc.setFill(Color.BLACK);
        gc.setFont(titleFont);
        gc.fillText(
            title,
            centerX(title, titleFont),
            headerTop
        );

        gc.setFont(subtitleFont);
        gc.setFill(Color.rgb(50, 50, 50));
        gc.fillText(
            subtitle,
            centerX(subtitle, subtitleFont),
            headerTop + 52
        );

        LocalDate firstDay = currentYearMonth.atDay(1);
        int startOffset = firstDay.getDayOfWeek().getValue() - 1;
        int daysInMonth = currentYearMonth.lengthOfMonth();
        boolean compactMode = selectedDates != null && selectedDates.size() < daysInMonth;
        if (!compactMode) {
            String[] dayNames = { "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье" };
            gc.setFont(dayHeaderFont);
            gc.setFill(Color.rgb(38, 38, 38));
            for (int col = 0; col < 7; col++) {
                double x = marginX + col * cellWidth;
                gc.setStroke(Color.rgb(190, 190, 190));
                gc.strokeRect(x, dayHeaderTop, cellWidth, 36);
                gc.fillText(dayNames[col], x + 10, dayHeaderTop + 24);
            }

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date = currentYearMonth.atDay(day);
                int index = startOffset + day - 1;
                int row = index / 7;
                int col = index % 7;
                double x = marginX + col * cellWidth;
                double y = gridTop + row * cellHeight;

                gc.setStroke(Color.rgb(210, 210, 210));
                gc.strokeRect(x, y, cellWidth, cellHeight);

                gc.setFont(dayNumberFont);
                gc.setFill(Color.rgb(25, 25, 25));
                gc.fillText(String.valueOf(day), x + 8, y + 22);

                List<Lesson> dayLessons = new ArrayList<>(
                    monthLessons.getOrDefault(date, List.of())
                );
                dayLessons.sort(Comparator.comparing(Lesson::getTime));

                double cursorY = y + 40;
                double maxY = y + cellHeight - 8;
                gc.setFont(lessonFont);
                gc.setFill(Color.rgb(32, 32, 32));
                for (Lesson lesson : dayLessons) {
                    List<String> block = buildLessonPrintLines(lesson);
                    for (String line : block) {
                        List<String> wrapped = wrapText(line, lessonFont, cellWidth - 14);
                        for (String wrappedLine : wrapped) {
                            if (cursorY + 16 > maxY) {
                                gc.setFill(Color.rgb(90, 90, 90));
                                gc.fillText("...", x + 7, maxY);
                                cursorY = maxY + 1;
                                break;
                            }
                            gc.setFill(Color.rgb(32, 32, 32));
                            gc.fillText(wrappedLine, x + 7, cursorY);
                            cursorY += 16;
                        }
                        if (cursorY > maxY) {
                            break;
                        }
                    }
                    cursorY += 4;
                    if (cursorY > maxY) {
                        break;
                    }
                }
            }
        } else {
            List<LocalDate> compactDates = new ArrayList<>(selectedDates);
            compactDates.sort(LocalDate::compareTo);
            int columns = compactDates.size() <= 8 ? 2 : 3;
            double cardWidth = (PRINT_A4_WIDTH - marginX * 2 - (columns - 1) * 20) / columns;
            int rows = (int) Math.ceil(compactDates.size() / (double) columns);
            double cardsTop = 200;
            double cardsHeight = PRINT_A4_HEIGHT - cardsTop - footerBottom;
            double cardHeight = cardsHeight / Math.max(1, rows);
            DateTimeFormatter dayTitleFormatter = DateTimeFormatter.ofPattern(
                "dd.MM.yyyy (EE)",
                UiFormatters.RU_LOCALE
            );
            Font compactTitleFont = Font.font(
                "System",
                FontWeight.BOLD,
                compactDates.size() <= 6 ? 20 : 18
            );
            Font compactLessonFont = Font.font(
                "System",
                FontWeight.NORMAL,
                compactDates.size() <= 6 ? 18 : 16
            );

            gc.setFont(Font.font("System", FontWeight.BOLD, 24));
            gc.setFill(Color.rgb(38, 38, 38));
            gc.fillText("Выбранные даты (" + compactDates.size() + ")", marginX, 188);

            for (int i = 0; i < compactDates.size(); i++) {
                LocalDate date = compactDates.get(i);
                int col = i % columns;
                int row = i / columns;
                double x = marginX + col * (cardWidth + 20);
                double y = cardsTop + row * cardHeight;

                gc.setStroke(Color.rgb(190, 190, 190));
                gc.strokeRect(x, y, cardWidth, cardHeight - 8);

                gc.setFont(compactTitleFont);
                gc.setFill(Color.rgb(30, 30, 30));
                gc.fillText(dayTitleFormatter.format(date), x + 8, y + 22);

                List<Lesson> dayLessons = new ArrayList<>(
                    monthLessons.getOrDefault(date, List.of())
                );
                dayLessons.sort(Comparator.comparing(Lesson::getTime));
                double cursorY = y + 40;
                double maxY = y + cardHeight - 18;
                gc.setFont(compactLessonFont);
                if (dayLessons.isEmpty()) {
                    gc.setFill(Color.rgb(120, 120, 120));
                    gc.fillText("Занятий нет", x + 8, cursorY);
                    continue;
                }
                for (Lesson lesson : dayLessons) {
                    for (String line : buildLessonPrintLines(lesson)) {
                        for (String wrappedLine : wrapText(line, compactLessonFont, cardWidth - 16)) {
                            if (cursorY + 17 > maxY) {
                                gc.setFill(Color.rgb(90, 90, 90));
                                gc.fillText("...", x + 8, maxY);
                                cursorY = maxY + 1;
                                break;
                            }
                            gc.setFill(Color.rgb(32, 32, 32));
                            gc.fillText(wrappedLine, x + 8, cursorY);
                            cursorY += 17;
                        }
                        if (cursorY > maxY) {
                            break;
                        }
                    }
                    cursorY += 4;
                    if (cursorY > maxY) {
                        break;
                    }
                }
            }
        }

        gc.setFont(Font.font("System", FontWeight.NORMAL, 18));
        gc.setFill(Color.rgb(80, 80, 80));
        String footer = "Сформировано: " + LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        ) + " • Формат: A4 (PNG)";
        gc.fillText(footer, marginX, PRINT_A4_HEIGHT - 40);

        return canvas;
    }

    private Set<LocalDate> chooseDaysForPngPrint() {
        List<LocalDate> monthDates = new ArrayList<>();
        LocalDate first = currentYearMonth.atDay(1);
        for (int i = 0; i < currentYearMonth.lengthOfMonth(); i++) {
            monthDates.add(first.plusDays(i));
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Печать PNG");
        dialog.setHeaderText("Выберите дни для печати");

        ListView<LocalDate> listView = new ListView<>(
            FXCollections.observableArrayList(monthDates)
        );
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setPrefHeight(420);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy (EE)");
        listView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                int lessonsCount = monthLessons.getOrDefault(item, List.of()).size();
                setText(item.format(dateFormatter) + " • занятий: " + lessonsCount);
            }
        });

        Button selectAllButton = new Button("Все дни");
        Button selectWithLessonsButton = new Button("Только с занятиями");
        Button clearButton = new Button("Очистить");
        selectAllButton.setOnAction(e -> listView.getSelectionModel().selectAll());
        selectWithLessonsButton.setOnAction(e -> {
            listView.getSelectionModel().clearSelection();
            for (int i = 0; i < monthDates.size(); i++) {
                LocalDate date = monthDates.get(i);
                if (!monthLessons.getOrDefault(date, List.of()).isEmpty()) {
                    listView.getSelectionModel().select(i);
                }
            }
        });
        clearButton.setOnAction(e -> listView.getSelectionModel().clearSelection());

        listView.getSelectionModel().selectAll();

        VBox content = new VBox(
            8,
            new Label("Если выбраны не все дни, PNG будет сформирован в компактном режиме только по выбранным датам."),
            new HBox(8, selectAllButton, selectWithLessonsButton, clearButton),
            listView
        );
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        UiStyles.apply(dialog.getDialogPane());

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return null;
        }
        Set<LocalDate> selected = new HashSet<>(
            listView.getSelectionModel().getSelectedItems()
        );
        if (selected.isEmpty()) {
            showError("Выберите хотя бы один день для печати.");
            return null;
        }
        return selected;
    }

    private void buildPrintSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Календарь " + currentYearMonth);
        sheet.setDisplayGridlines(true);
        sheet.setFitToPage(true);
        sheet.setHorizontallyCenter(true);
        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
        printSetup.setLandscape(false);
        printSetup.setFitWidth((short) 1);
        printSetup.setFitHeight((short) 1);

        CellStyle titleStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 20);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle subtitleStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font subtitleFont = workbook.createFont();
        subtitleFont.setFontHeightInPoints((short) 13);
        subtitleStyle.setFont(subtitleFont);
        subtitleStyle.setAlignment(HorizontalAlignment.CENTER);
        subtitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle dayHeaderStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font dayHeaderFont = workbook.createFont();
        dayHeaderFont.setBold(true);
        dayHeaderFont.setFontHeightInPoints((short) 12);
        dayHeaderStyle.setFont(dayHeaderFont);
        dayHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        dayHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dayHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        dayHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        dayHeaderStyle.setBorderBottom(BorderStyle.THIN);
        dayHeaderStyle.setBorderTop(BorderStyle.THIN);
        dayHeaderStyle.setBorderLeft(BorderStyle.THIN);
        dayHeaderStyle.setBorderRight(BorderStyle.THIN);

        CellStyle dayCellStyle = workbook.createCellStyle();
        dayCellStyle.setWrapText(true);
        dayCellStyle.setVerticalAlignment(VerticalAlignment.TOP);
        dayCellStyle.setAlignment(HorizontalAlignment.LEFT);
        dayCellStyle.setBorderBottom(BorderStyle.THIN);
        dayCellStyle.setBorderTop(BorderStyle.THIN);
        dayCellStyle.setBorderLeft(BorderStyle.THIN);
        dayCellStyle.setBorderRight(BorderStyle.THIN);

        CellStyle dayNumberStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font dayNumberFont = workbook.createFont();
        dayNumberFont.setBold(true);
        dayNumberFont.setFontHeightInPoints((short) 11);
        dayNumberStyle.setFont(dayNumberFont);
        dayNumberStyle.setAlignment(HorizontalAlignment.LEFT);
        dayNumberStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dayNumberStyle.setBorderBottom(BorderStyle.THIN);
        dayNumberStyle.setBorderTop(BorderStyle.THIN);
        dayNumberStyle.setBorderLeft(BorderStyle.THIN);
        dayNumberStyle.setBorderRight(BorderStyle.THIN);
        dayNumberStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        dayNumberStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle lessonCellStyle = workbook.createCellStyle();
        lessonCellStyle.cloneStyleFrom(dayCellStyle);
        org.apache.poi.ss.usermodel.Font lessonFont = workbook.createFont();
        lessonFont.setFontHeightInPoints((short) 11);
        lessonCellStyle.setFont(lessonFont);

        CellStyle emptyLessonStyle = workbook.createCellStyle();
        emptyLessonStyle.cloneStyleFrom(dayCellStyle);
        org.apache.poi.ss.usermodel.Font emptyFont = workbook.createFont();
        emptyFont.setItalic(true);
        emptyFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        emptyFont.setFontHeightInPoints((short) 11);
        emptyLessonStyle.setFont(emptyFont);
        emptyLessonStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle footerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font footerFont = workbook.createFont();
        footerFont.setItalic(true);
        footerFont.setFontHeightInPoints((short) 9);
        footerStyle.setFont(footerFont);
        footerStyle.setAlignment(HorizontalAlignment.LEFT);
        footerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        for (int col = 0; col < 7; col++) {
            sheet.setColumnWidth(col, 31 * 256);
        }

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(32);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Календарь занятий");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        String calendarName = allCalendarsMode
            ? "Все календари"
            : resolveCalendarName(
                scheduleUseCase == null ? null : scheduleUseCase.getCurrentCalendarId()
            );
        Row subtitleRow = sheet.createRow(1);
        subtitleRow.setHeightInPoints(24);
        Cell subtitleCell = subtitleRow.createCell(0);
        subtitleCell.setCellValue(currentYearMonth.format(monthYearFormatter) + " • " + safe(calendarName));
        subtitleCell.setCellStyle(subtitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

        int headerRowIndex = 3;
        Row dayHeaderRow = sheet.createRow(headerRowIndex);
        dayHeaderRow.setHeightInPoints(24);
        String[] dayNames = { "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье" };
        for (int col = 0; col < 7; col++) {
            Cell cell = dayHeaderRow.createCell(col);
            cell.setCellValue(dayNames[col]);
            cell.setCellStyle(dayHeaderStyle);
        }

        LocalDate firstDay = currentYearMonth.atDay(1);
        int startOffset = firstDay.getDayOfWeek().getValue() - 1;
        int daysInMonth = currentYearMonth.lengthOfMonth();
        int currentRowIndex = headerRowIndex + 1;
        DateTimeFormatter dayLabelFormatter = DateTimeFormatter.ofPattern(
            "dd.MM (EE)",
            UiFormatters.RU_LOCALE
        );

        for (int week = 0; week < 6; week++) {
            int maxLessonsInWeek = 0;
            List<List<Lesson>> lessonsByColumn = new ArrayList<>();
            for (int col = 0; col < 7; col++) {
                int dayNumber = week * 7 + col - startOffset + 1;
                if (dayNumber < 1 || dayNumber > daysInMonth) {
                    lessonsByColumn.add(List.of());
                    continue;
                }
                LocalDate date = currentYearMonth.atDay(dayNumber);
                List<Lesson> dayLessons = new ArrayList<>(
                    monthLessons.getOrDefault(date, List.of())
                );
                dayLessons.sort(Comparator.comparing(Lesson::getTime));
                lessonsByColumn.add(dayLessons);
                maxLessonsInWeek = Math.max(maxLessonsInWeek, dayLessons.size());
            }

            Row dayRow = sheet.createRow(currentRowIndex++);
            dayRow.setHeightInPoints(22);
            for (int col = 0; col < 7; col++) {
                Cell cell = dayRow.createCell(col);
                int dayNumber = week * 7 + col - startOffset + 1;
                if (dayNumber < 1 || dayNumber > daysInMonth) {
                    cell.setCellStyle(dayCellStyle);
                    cell.setCellValue("");
                    continue;
                }
                LocalDate date = currentYearMonth.atDay(dayNumber);
                cell.setCellStyle(dayNumberStyle);
                cell.setCellValue(dayLabelFormatter.format(date));
            }

            int lessonRows = Math.max(1, maxLessonsInWeek);
            for (int lessonIndex = 0; lessonIndex < lessonRows; lessonIndex++) {
                Row lessonRow = sheet.createRow(currentRowIndex++);
                lessonRow.setHeightInPoints(76);
                for (int col = 0; col < 7; col++) {
                    Cell cell = lessonRow.createCell(col);
                    List<Lesson> dayLessons = lessonsByColumn.get(col);
                    if (dayLessons.isEmpty() || lessonIndex >= dayLessons.size()) {
                        cell.setCellStyle(emptyLessonStyle);
                        cell.setCellValue("—");
                        continue;
                    }
                    cell.setCellStyle(lessonCellStyle);
                    cell.setCellValue(buildLessonCellText(dayLessons.get(lessonIndex)));
                }
            }
        }

        int footerRowIndex = currentRowIndex + 1;
        Row footerRow = sheet.createRow(footerRowIndex);
        footerRow.setHeightInPoints(18);
        Cell footerCell = footerRow.createCell(0);
        footerCell.setCellValue(
            "Сформировано: " +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) +
            " • Формат печати: A4"
        );
        footerCell.setCellStyle(footerStyle);
        sheet.addMergedRegion(new CellRangeAddress(footerRowIndex, footerRowIndex, 0, 6));

        workbook.setPrintArea(
            workbook.getSheetIndex(sheet),
            0,
            6,
            0,
            footerRowIndex
        );
    }

    private String buildLessonCellText(Lesson lesson) {
        if (lesson == null) {
            return "";
        }
        String time = lesson.getTime() == null ? "--:--" : lesson.getTime().format(statusTimeFormatter);
        return time +
        " • " + safe(lesson.getTopic()) +
        "\nТема: " + safe(lesson.getLessonName()) +
        "\nЗанятие: " + safe(lesson.getClassName()) +
        "\nМесто: " + safe(lesson.getLocation()) +
        "\nПреподаватель: " + safe(lesson.getInstructor());
    }

    private List<String> buildLessonPrintLines(Lesson lesson) {
        List<String> lines = new ArrayList<>();
        String time = lesson.getTime() == null ? "--:--" : lesson.getTime().format(statusTimeFormatter);
        lines.add(time + " • " + safe(lesson.getTopic()));
        lines.add("Тема: " + safe(lesson.getLessonName()));
        lines.add(
            "Занятие: " + safe(lesson.getClassName()) +
            " | Место: " + safe(lesson.getLocation())
        );
        lines.add("Преподаватель: " + safe(lesson.getInstructor()));
        return lines;
    }

    private List<String> wrapText(String text, Font font, double maxWidth) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        String[] words = text.trim().split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.length() == 0
                ? word
                : current + " " + word;
            if (measureWidth(candidate, font) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (current.length() > 0) {
                result.add(current.toString());
                current.setLength(0);
            }
            if (measureWidth(word, font) <= maxWidth) {
                current.append(word);
                continue;
            }
            String chopped = chopWord(word, font, maxWidth);
            result.add(chopped);
            String remainder = word.substring(Math.min(chopped.length(), word.length()));
            if (!remainder.isBlank()) {
                current.append(remainder);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    private String chopWord(String word, Font font, double maxWidth) {
        if (word == null || word.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            String candidate = builder.toString() + word.charAt(i);
            if (measureWidth(candidate, font) > maxWidth) {
                break;
            }
            builder.append(word.charAt(i));
        }
        return builder.length() == 0
            ? word.substring(0, 1)
            : builder.toString();
    }

    private double measureWidth(String value, Font font) {
        Text text = new Text(value);
        text.setFont(font);
        return text.getLayoutBounds().getWidth();
    }

    private double centerX(String value, Font font) {
        return (PRINT_A4_WIDTH - measureWidth(value, font)) / 2.0;
    }

    private BufferedImage toBufferedImage(WritableImage image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        BufferedImage buffered = new BufferedImage(
            width,
            height,
            BufferedImage.TYPE_INT_ARGB
        );
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffered.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        return buffered;
    }

    private void preloadMonthLessons() {
        monthLessons.clear();
        if (lessonUseCase == null) {
            return;
        }

        LocalDate start = currentYearMonth.atDay(1);
        LocalDate end = currentYearMonth.atEndOfMonth();
        List<Lesson> lessons = allCalendarsMode
            ? lessonUseCase.getLessonsByDateRangeAllCalendars(start, end)
            : lessonUseCase.getLessonsByDateRange(start, end);
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
            lessons = allCalendarsMode
                ? lessonUseCase.getLessonsByDateAllCalendars(date)
                : lessonUseCase.getLessonsByDate(date);
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
            formatLessonTimeRange(lesson)
        );
        timeLabel.getStyleClass().add("lesson-time");

        Label topicLabel = new Label("Предмет: " + lesson.getTopic());
        Label lessonNameLabel = new Label("Тема: " + lesson.getLessonName());
        Label classNameLabel = new Label("Занятие: " + lesson.getClassName());
        Label locationLabel = new Label("Место: " + lesson.getLocation());
        Label instructorLabel = new Label(
            "Преподаватель: " + lesson.getInstructor()
        );
        Label calendarLabel = new Label(
            "Календарь: " + resolveCalendarName(lesson.getCalendarId())
        );
        topicLabel.getStyleClass().add("lesson-meta");
        lessonNameLabel.getStyleClass().add("lesson-meta");
        classNameLabel.getStyleClass().add("lesson-meta");
        locationLabel.getStyleClass().add("lesson-meta");
        instructorLabel.getStyleClass().add("lesson-meta");
        calendarLabel.getStyleClass().add("lesson-meta");

        card
            .getChildren()
            .addAll(
                timeLabel,
                topicLabel,
                lessonNameLabel,
                classNameLabel,
                locationLabel,
                instructorLabel,
                calendarLabel
            );
        applyLessonStateStyles(lesson, card, timeLabel);

        // Кнопки действий
        HBox actionsBox = new HBox(5);
        actionsBox.getStyleClass().add("card-actions");
        Button editButton = new Button("Редактировать");
        Button deleteButton = new Button("Удалить");
        editButton.getStyleClass().add("button-secondary");
        deleteButton.getStyleClass().add("button-danger");

        editButton.setOnAction(event -> showEditLessonDialog(lesson));
        deleteButton.setOnAction(event -> {
            if (allCalendarsMode) {
                showInfo("Для редактирования выберите конкретный календарь.");
                return;
            }
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

    private void applyLessonStateStyles(
        Lesson lesson,
        VBox card,
        Label timeLabel
    ) {
        LocalDate today = LocalDate.now();
        LocalDate lessonDate = lesson.getDate();
        if (lessonDate == null || lesson.getTime() == null) {
            return;
        }

        if (lessonDate.isBefore(today)) {
            card.getStyleClass().add("lesson-card-past");
            Label status = new Label("Уже прошло");
            status.getStyleClass().add("lesson-status-past");
            card.getChildren().add(1, status);
            return;
        }
        if (!lessonDate.equals(today)) {
            return;
        }

        LocalTime now = LocalTime.now();
        LocalTime start = lesson.getTime();
        LocalTime end = calculateLessonEndTime(lesson);

        if (!now.isBefore(start) && now.isBefore(end)) {
            card.getStyleClass().add("lesson-card-current");
            timeLabel.getStyleClass().add("lesson-time-current");
            Label status = new Label("Идет сейчас");
            status.getStyleClass().add("lesson-status-current");
            card.getChildren().add(1, status);
        } else if (now.isAfter(end) || now.equals(end)) {
            card.getStyleClass().add("lesson-card-past");
            Label status = new Label("Уже прошло");
            status.getStyleClass().add("lesson-status-past");
            card.getChildren().add(1, status);
        }
    }

    private void showAddLessonDialog(LocalDate date) {
        if (allCalendarsMode) {
            showInfo("Добавление доступно только в конкретном календаре.");
            return;
        }
        showLessonDialog(null, date);
    }

    private void showEditLessonDialog(Lesson lesson) {
        if (allCalendarsMode) {
            showInfo("Редактирование доступно только в конкретном календаре.");
            return;
        }
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

    private void loadCalendars() {
        if (scheduleUseCase == null) {
            return;
        }
        allCalendarsLoaded.clear();
        allCalendarsLoaded.addAll(scheduleUseCase.getCalendars());
        rebuildDirectoryOptions();
        if (directoryComboBox.getSelectionModel().getSelectedItem() == null) {
            updatingCalendarSelection = true;
            directoryComboBox.getSelectionModel().select(DIR_ALL);
            updatingCalendarSelection = false;
        }
        applyDirectoryFilter();

        List<AppCalendar> loaded = new ArrayList<>(calendars);
        String activeId = scheduleUseCase.getCurrentCalendarId();
        AppCalendar selected = allCalendarsMode
            ? ALL_CALENDARS_OPTION
            : loaded
            .stream()
            .filter(calendar -> calendar.getId().equals(activeId))
            .findFirst()
            .orElse(loaded.isEmpty() ? null : loaded.get(0));

        updatingCalendarSelection = true;
        calendarComboBox.getSelectionModel().select(selected);
        updatingCalendarSelection = false;

        applyCalendarSelection();
    }

    private void rebuildDirectoryOptions() {
        List<String> loaded = new ArrayList<>();
        loaded.add(DIR_ALL);
        loaded.add(DIR_ROOT);
        for (AppCalendar calendar : allCalendarsLoaded) {
            String path = calendar.getDirectoryPath();
            if (path == null || path.isBlank()) {
                continue;
            }
            if (!loaded.contains(path)) {
                loaded.add(path);
            }
        }
        loaded.subList(2, loaded.size()).sort(String.CASE_INSENSITIVE_ORDER);
        directories.setAll(loaded);
    }

    private void applyDirectoryFilter() {
        String directory = directoryComboBox
            .getSelectionModel()
            .getSelectedItem();
        List<AppCalendar> filtered = new ArrayList<>();
        filtered.add(ALL_CALENDARS_OPTION);
        for (AppCalendar calendar : allCalendarsLoaded) {
            if (DIR_ALL.equals(directory) || directory == null) {
                filtered.add(calendar);
                continue;
            }
            if (DIR_ROOT.equals(directory)) {
                if (
                    calendar.getDirectoryPath() == null ||
                    calendar.getDirectoryPath().isBlank()
                ) {
                    filtered.add(calendar);
                }
                continue;
            }
            if (directory.equals(calendar.getDirectoryPath())) {
                filtered.add(calendar);
            }
        }
        AppCalendar selected = calendarComboBox.getSelectionModel().getSelectedItem();
        String selectedId = selected == null ? null : selected.getId();
        calendars.setAll(filtered);
        if (selectedId != null) {
            AppCalendar same = filtered
                .stream()
                .filter(item -> selectedId.equals(item.getId()))
                .findFirst()
                .orElse(null);
            if (same != null) {
                updatingCalendarSelection = true;
                calendarComboBox.getSelectionModel().select(same);
                updatingCalendarSelection = false;
            }
        }
    }

    private void switchCalendar(AppCalendar calendar) {
        if (scheduleUseCase == null || calendar == null) {
            return;
        }
        allCalendarsMode = ALL_CALENDARS_ID.equals(calendar.getId());
        if (!allCalendarsMode) {
            scheduleUseCase.setCurrentCalendarId(calendar.getId());
        }
        applyCalendarSelection();
        selectedDate = null;
        buildCalendar();
        updateHeaderStatus();
    }

    private void ensureSelectedDateForCurrentMonth() {
        if (selectedDate != null && YearMonth.from(selectedDate).equals(currentYearMonth)) {
            return;
        }

        LocalDate today = LocalDate.now();
        if (YearMonth.from(today).equals(currentYearMonth)) {
            selectedDate = today;
            return;
        }

        selectedDate = currentYearMonth.atDay(1);
    }

    private String formatLessonTimeRange(Lesson lesson) {
        LocalTime start = lesson.getTime();
        if (start == null) {
            return "";
        }
        LocalTime end = calculateLessonEndTime(lesson);
        if (end == null || end.equals(start)) {
            return start.format(UiFormatters.TIME_FORMATTER);
        }
        return (
            start.format(UiFormatters.TIME_FORMATTER) +
            " - " +
            end.format(UiFormatters.TIME_FORMATTER)
        );
    }

    private LocalTime calculateLessonEndTime(Lesson lesson) {
        if (lesson == null || lesson.getTime() == null) {
            return null;
        }

        int duration = Math.max(1, lesson.getDurationHours());
        LocalTime start = lesson.getTime();
        int startIndex = lessonStartSlotIndex(start);
        if (startIndex < 0) {
            return start.plusMinutes(50L * duration);
        }
        int endSlotIndex = Math.min(
            startIndex + duration - 1,
            LESSON_SLOT_START_TIMES_ORDERED.size() - 1
        );
        LocalTime endSlotStart = LESSON_SLOT_START_TIMES_ORDERED.get(endSlotIndex);
        return LESSON_END_BY_START.getOrDefault(endSlotStart, endSlotStart.plusMinutes(50));
    }

    private int lessonStartSlotIndex(LocalTime start) {
        for (int i = 0; i < LESSON_SLOT_START_TIMES_ORDERED.size(); i++) {
            if (LESSON_SLOT_START_TIMES_ORDERED.get(i).equals(start)) {
                return i;
            }
        }
        return -1;
    }

    private void applyCalendarSelection() {
        if (allCalendarsMode) {
            return;
        }
        if (scheduleUseCase != null && lessonUseCase != null) {
            lessonUseCase.setCurrentCalendarId(scheduleUseCase.getCurrentCalendarId());
            lessonUseCase.archivePastLessons(LocalDate.now());
        }
    }

    private String resolveCalendarName(String calendarId) {
        if (calendarId == null || calendarId.isBlank()) {
            return "Неизвестно";
        }
        for (AppCalendar calendar : calendars) {
            if (calendarId.equals(calendar.getId())) {
                return calendar.getName();
            }
        }
        return calendarId;
    }

    private void showError(String message) {
        UiAlerts.showError("Ошибка", message);
    }

    private void showInfo(String message) {
        UiAlerts.showInfo("Информация", message);
    }

    private void startHeaderTicker() {
        if (headerTimeline != null) {
            headerTimeline.stop();
        }
        headerTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, event -> updateHeaderStatus()),
            new KeyFrame(Duration.seconds(1))
        );
        headerTimeline.setCycleCount(Timeline.INDEFINITE);
        headerTimeline.play();
    }

    private void updateHeaderStatus() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        currentDateLabel.setText("Дата: " + statusDateFormatter.format(today));
        currentTimeLabel.setText("Время: " + statusTimeFormatter.format(now));

        if (lessonUseCase == null) {
            currentLessonLabel.setText("Текущее занятие: -");
            setLessonStatusStyle(STATUS_LESSON_NONE_CLASS);
            return;
        }

        List<Lesson> sourceLessons = allCalendarsMode
            ? lessonUseCase.getLessonsByDateAllCalendars(today)
            : lessonUseCase.getLessonsByDate(today);
        List<Lesson> todayLessons = new ArrayList<>(sourceLessons);
        todayLessons.sort(
            Comparator.comparing(
                Lesson::getTime,
                Comparator.nullsLast(LocalTime::compareTo)
            )
        );
        if (todayLessons.isEmpty()) {
            currentLessonLabel.setText("Текущее занятие: сегодня занятий нет");
            setLessonStatusStyle(STATUS_LESSON_NONE_CLASS);
            return;
        }

        Lesson currentLesson = null;
        Lesson nextLesson = null;
        for (Lesson lesson : todayLessons) {
            LocalTime start = lesson.getTime();
            LocalTime end = calculateLessonEndTime(lesson);
            if (start == null || end == null) {
                continue;
            }

            if (!now.isBefore(start) && now.isBefore(end)) {
                currentLesson = lesson;
                break;
            }
            if (now.isBefore(start) && (nextLesson == null || start.isBefore(nextLesson.getTime()))) {
                nextLesson = lesson;
            }
        }

        if (currentLesson != null) {
            currentLessonLabel.setText(
                "Текущее занятие: " +
                safe(currentLesson.getTopic()) +
                " (" +
                formatLessonTimeRange(currentLesson) +
                ")"
            );
            setLessonStatusStyle(STATUS_LESSON_CURRENT_CLASS);
            return;
        }
        if (nextLesson != null) {
            currentLessonLabel.setText(
                "Следующее занятие: " +
                safe(nextLesson.getTopic()) +
                " в " +
                nextLesson.getTime().format(statusTimeFormatter)
            );
            setLessonStatusStyle(STATUS_LESSON_NEXT_CLASS);
            return;
        }
        currentLessonLabel.setText("Текущее занятие: занятий больше нет");
        setLessonStatusStyle(STATUS_LESSON_NONE_CLASS);
    }

    private void setLessonStatusStyle(String statusClass) {
        currentLessonLabel
            .getStyleClass()
            .removeAll(
                STATUS_LESSON_CURRENT_CLASS,
                STATUS_LESSON_NEXT_CLASS,
                STATUS_LESSON_NONE_CLASS
            );
        if (statusClass != null && !statusClass.isBlank()) {
            currentLessonLabel.getStyleClass().add(statusClass);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
