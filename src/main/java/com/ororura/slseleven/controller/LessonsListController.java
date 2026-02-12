package com.ororura.slseleven.controller;

import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.ui.UiFormatters;
import com.ororura.slseleven.ui.UiStyles;
import com.ororura.slseleven.usecase.LessonUseCase;
import com.ororura.slseleven.util.DelimitedText;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class LessonsListController {

    @FXML
    private TableView<Lesson> lessonTable;

    @FXML
    private TableColumn<Lesson, LocalDate> dateColumn;

    @FXML
    private TableColumn<Lesson, LocalTime> timeColumn;

    @FXML
    private TableColumn<Lesson, String> topicColumn;

    @FXML
    private TableColumn<Lesson, String> lessonNameColumn;

    @FXML
    private TableColumn<Lesson, String> classNameColumn;

    @FXML
    private TableColumn<Lesson, String> locationColumn;

    @FXML
    private TableColumn<Lesson, String> instructorColumn;

    @FXML
    private Label countLabel;

    @FXML
    private TextField searchField;

    private LessonUseCase lessonUseCase;

    private final ObservableList<Lesson> data =
        FXCollections.observableArrayList();
    private FilteredList<Lesson> filteredData;

    private final DateTimeFormatter dateFormatter = UiFormatters.DATE_FORMATTER;
    private final DateTimeFormatter timeFormatter = UiFormatters.TIME_FORMATTER;
    private static final String[] EXPORT_HEADERS = {
        "Дата",
        "Время",
        "Предмет",
        "Тема",
        "Занятие",
        "Место",
        "Преподаватель",
    };

    @FXML
    public void initialize() {
        filteredData = new FilteredList<>(data, lesson -> true);
        lessonTable.setItems(filteredData);
        lessonTable.setPlaceholder(
            new Label("Нет занятий. Добавьте запись или измените фильтр.")
        );
        lessonTable
            .getSelectionModel()
            .setSelectionMode(SelectionMode.MULTIPLE);
        topicColumn.setCellValueFactory(new PropertyValueFactory<>("topic"));
        lessonNameColumn.setCellValueFactory(
            new PropertyValueFactory<>("lessonName")
        );
        classNameColumn.setCellValueFactory(
            new PropertyValueFactory<>("className")
        );
        locationColumn.setCellValueFactory(
            new PropertyValueFactory<>("location")
        );
        instructorColumn.setCellValueFactory(
            new PropertyValueFactory<>("instructor")
        );
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));

        // Форматирование даты
        dateColumn.setCellFactory(col ->
            new TableCell<Lesson, LocalDate>() {
                @Override
                protected void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(
                        empty || item == null
                            ? null
                            : dateFormatter.format(item)
                    );
                }
            }
        );

        // Форматирование времени
        timeColumn.setCellFactory(col ->
            new TableCell<Lesson, LocalTime>() {
                @Override
                protected void updateItem(LocalTime item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(
                        empty || item == null
                            ? null
                            : timeFormatter.format(item)
                    );
                }
            }
        );
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            applySearchFilter(newValue);
            updateCountLabel();
        });
    }

    public void setLessonUseCase(LessonUseCase lessonUseCase) {
        this.lessonUseCase = lessonUseCase;
        reload();
    }

    private void reload() {
        lessonUseCase.archivePastLessons(LocalDate.now());
        List<Lesson> lessons = lessonUseCase.getAllLessons();
        data.setAll(lessons);
        applySearchFilter(searchField.getText());
        updateCountLabel();
    }

    @FXML
    private void onRefresh() {
        reload();
    }

    @FXML
    private void onShowArchive() {
        if (lessonUseCase == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Архив");
            alert.setHeaderText(null);
            alert.setContentText("Ошибка: Use case не инициализирован");
            alert.showAndWait();
            return;
        }

        List<Lesson> archived = lessonUseCase.getArchivedLessons();
        if (archived.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Архив");
            alert.setHeaderText(null);
            alert.setContentText("Архив пуст.");
            alert.showAndWait();
            return;
        }

        Map<String, TopicSummary> summaryByTopic = new LinkedHashMap<>();
        for (Lesson lesson : archived) {
            String key = topicThemeKey(lesson);
            TopicSummary summary = summaryByTopic.computeIfAbsent(
                key,
                ignored -> new TopicSummary(topicThemeLabel(lesson))
            );
            summary.archivedCount++;
        }

        List<Lesson> activeLessons = lessonUseCase.getAllLessons();
        for (Lesson lesson : activeLessons) {
            String key = topicThemeKey(lesson);
            TopicSummary summary = summaryByTopic.computeIfAbsent(
                key,
                ignored -> new TopicSummary(topicThemeLabel(lesson))
            );
            summary.activeCount++;
        }

        Set<String> completed = new HashSet<>();
        Set<String> notCompleted = new HashSet<>();
        for (TopicSummary summary : summaryByTopic.values()) {
            if (summary.activeCount > 0) {
                notCompleted.add(summary.label);
            } else if (summary.archivedCount > 0) {
                completed.add(summary.label);
            }
        }

        List<String> completedList = new ArrayList<>(completed);
        completedList.sort(String::compareToIgnoreCase);
        List<String> notCompletedList = new ArrayList<>(notCompleted);
        notCompletedList.sort(String::compareToIgnoreCase);

        StringJoiner joiner = new StringJoiner(System.lineSeparator());
        joiner.add("Статус тем и предметов:");
        joiner.add("");
        joiner.add("Завершено:");
        if (completedList.isEmpty()) {
            joiner.add("  - Нет завершённых тем");
        } else {
            for (String label : completedList) {
                joiner.add("  - " + label);
            }
        }
        joiner.add("");
        joiner.add("Не завершено:");
        if (notCompletedList.isEmpty()) {
            joiner.add("  - Нет незавершённых тем");
        } else {
            for (String label : notCompletedList) {
                joiner.add("  - " + label);
            }
        }
        joiner.add("");
        joiner.add("Архивные занятия:");
        for (Lesson lesson : archived) {
            joiner.add(
                formatDateValue(lesson.getDate()) +
                " " +
                formatTimeValue(lesson.getTime()) +
                " | " +
                safeValue(lesson.getTopic()) +
                " | " +
                safeValue(lesson.getLessonName()) +
                " | " +
                safeValue(lesson.getClassName()) +
                " | " +
                safeValue(lesson.getLocation()) +
                " | " +
                safeValue(lesson.getInstructor())
            );
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Архив занятий");
        dialog.setHeaderText(
            "Архивных занятий: " +
            archived.size() +
            " | Завершено тем: " +
            completedList.size() +
            " | Не завершено тем: " +
            notCompletedList.size()
        );
        TextArea archiveArea = new TextArea(joiner.toString());
        archiveArea.setEditable(false);
        archiveArea.setWrapText(false);
        archiveArea.setPrefRowCount(20);
        dialog.getDialogPane().setContent(archiveArea);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        UiStyles.apply(dialog.getDialogPane());
        dialog.showAndWait();
    }

    private String topicThemeKey(Lesson lesson) {
        return (
            safeValue(lesson.getTopic()).trim().toLowerCase(Locale.ROOT) +
            "||" +
            safeValue(lesson.getLessonName()).trim().toLowerCase(Locale.ROOT)
        );
    }

    private String topicThemeLabel(Lesson lesson) {
        return (
            safeValue(lesson.getTopic()).trim() +
            " / " +
            safeValue(lesson.getLessonName()).trim()
        );
    }

    @FXML
    private void onSave() {
        showLessonDialog(null, LocalDate.now());
    }

    @FXML
    private void onDelete() {
        List<Lesson> selected = List.copyOf(
            lessonTable.getSelectionModel().getSelectedItems()
        );
        if (selected.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Удаление");
            alert.setHeaderText(null);
            alert.setContentText("Выберите занятия для удаления.");
            alert.showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение");
        alert.setHeaderText("Удалить выбранные занятия?");
        alert.setContentText(
            "Количество выбранных занятий: " + selected.size()
        );
        alert
            .showAndWait()
            .ifPresent(response -> {
                if (response == ButtonType.OK) {
                    for (Lesson lesson : selected) {
                        lessonUseCase.deleteLesson(lesson.getId());
                    }
                    reload();
                }
            });
    }

    @FXML
    private void onDeleteOneAuto() {
        Lesson selected = lessonTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Автораспределение");
            alert.setHeaderText(null);
            alert.setContentText("Выберите занятие для удаления.");
            alert.showAndWait();
            return;
        }

        try {
            lessonUseCase.deleteAutoScheduledLesson(selected.getId());
            reload();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Автораспределение");
            alert.setHeaderText(null);
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onDeleteAllAuto() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Автораспределение");
        confirm.setHeaderText("Удалить все занятия автораспределения?");
        confirm.setContentText("Будут удалены только авто-созданные занятия.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        int deleted = lessonUseCase.deleteAllAutoScheduledLessons();
        reload();

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Автораспределение");
        info.setHeaderText(null);
        info.setContentText("Удалено авто-занятий: " + deleted);
        info.showAndWait();
    }

    @FXML
    private void onDeleteAll() {
        if (data.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Удаление");
            alert.setHeaderText(null);
            alert.setContentText("Список занятий пуст.");
            alert.showAndWait();
            return;
        }

        if (!confirmDoubleDelete("Удалить все занятия?")) {
            return;
        }

        lessonUseCase.deleteAllLessons();
        reload();
    }

    private boolean confirmDoubleDelete(String headerText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение");
        alert.setHeaderText(headerText);
        alert.setContentText("Это действие нельзя отменить.");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return false;
        }

        Alert second = new Alert(Alert.AlertType.CONFIRMATION);
        second.setTitle("Подтверждение");
        second.setHeaderText("Подтвердите ещё раз");
        second.setContentText("Удалить данные без возможности восстановления?");
        return second.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    @FXML
    private void onQuickImport() {
        if (lessonUseCase == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Импорт");
            alert.setHeaderText(null);
            alert.setContentText("Ошибка: Use case не инициализирован");
            alert.showAndWait();
            return;
        }
        String clipboardText = Clipboard.getSystemClipboard().getString();
        if (clipboardText == null || clipboardText.trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Импорт");
            alert.setHeaderText(null);
            alert.setContentText("Буфер обмена пуст.");
            alert.showAndWait();
            return;
        }

        ImportResult result = importLessonsFromText(clipboardText);
        showImportResult(result);
    }

    @FXML
    private void onImport() {
        if (lessonUseCase == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Импорт");
            alert.setHeaderText(null);
            alert.setContentText("Ошибка: Use case не инициализирован");
            alert.showAndWait();
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Импорт");
        dialog.setHeaderText("Вставьте данные для импорта");

        TextArea textArea = new TextArea();
        textArea.setWrapText(false);
        textArea.setEditable(true);
        textArea.setPrefRowCount(18);

        Label hint = new Label(
            "Ожидаемые колонки: Дата (необязательно), Время, Предмет, Тема, Занятие, Место, Преподаватель."
        );
        Label hint2 = new Label(
            "Поддерживаются табуляции (TSV) и CSV с ';' или ','. Заголовок необязателен."
        );

        VBox content = new VBox(8, hint, hint2, textArea);
        dialog.getDialogPane().setContent(content);

        ButtonType importButtonType = new ButtonType(
            "Импортировать",
            ButtonBar.ButtonData.LEFT
        );
        ButtonType loadFileButtonType = new ButtonType(
            "Загрузить файл",
            ButtonBar.ButtonData.LEFT
        );
        dialog
            .getDialogPane()
            .getButtonTypes()
            .addAll(loadFileButtonType, importButtonType, ButtonType.CLOSE);
        UiStyles.apply(dialog.getDialogPane());

        Button loadFileButton = (Button) dialog
            .getDialogPane()
            .lookupButton(loadFileButtonType);
        loadFileButton.addEventFilter(
            javafx.event.ActionEvent.ACTION,
            event -> {
                loadDelimitedFromFile(
                    dialog.getDialogPane().getScene().getWindow(),
                    textArea
                );
                event.consume();
            }
        );

        Button importButton = (Button) dialog
            .getDialogPane()
            .lookupButton(importButtonType);
        importButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            ImportResult result = importLessonsFromText(textArea.getText());
            showImportResult(result);
            event.consume();
        });

        dialog.showAndWait();
    }

    @FXML
    private void onImportXlsx() {
        if (lessonUseCase == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Импорт");
            alert.setHeaderText(null);
            alert.setContentText("Ошибка: Use case не инициализирован");
            alert.showAndWait();
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Импорт XLSX");
        chooser
            .getExtensionFilters()
            .add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        java.io.File file = chooser.showOpenDialog(
            lessonTable.getScene().getWindow()
        );
        if (file == null) {
            return;
        }

        List<String> errors = new ArrayList<>();
        List<List<String>> rows = readXlsxRows(file, errors);
        ImportResult result = importLessonsFromRows(rows, errors);
        showImportResult(result);
    }

    @FXML
    private void onExportXlsx() {
        if (data.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Экспорт");
            alert.setHeaderText(null);
            alert.setContentText("Нет данных для экспорта.");
            alert.showAndWait();
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт XLSX");
        chooser
            .getExtensionFilters()
            .add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        chooser.setInitialFileName(
            "lessons_export_" + LocalDate.now() + ".xlsx"
        );
        java.io.File file = chooser.showSaveDialog(
            lessonTable.getScene().getWindow()
        );
        if (file == null) {
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Занятия");
            Row header = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                header.createCell(i).setCellValue(EXPORT_HEADERS[i]);
            }
            int rowIndex = 1;
            for (Lesson lesson : data) {
                Row row = sheet.createRow(rowIndex++);
                row
                    .createCell(0)
                    .setCellValue(formatDateValue(lesson.getDate()));
                row
                    .createCell(1)
                    .setCellValue(formatTimeValue(lesson.getTime()));
                row.createCell(2).setCellValue(safeValue(lesson.getTopic()));
                row
                    .createCell(3)
                    .setCellValue(safeValue(lesson.getLessonName()));
                row.createCell(4).setCellValue(safeValue(lesson.getClassName()));
                row
                    .createCell(5)
                    .setCellValue(safeValue(lesson.getLocation()));
                row
                    .createCell(6)
                    .setCellValue(safeValue(lesson.getInstructor()));
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Экспорт");
            alert.setHeaderText(null);
            alert.setContentText("Файл сохранён: " + file.getName());
            alert.showAndWait();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText(null);
            alert.setContentText(
                "Не удалось сохранить файл: " + ex.getMessage()
            );
            alert.showAndWait();
        }
    }

    @FXML
    private void onQuickExport() {
        if (data.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Экспорт");
            alert.setHeaderText(null);
            alert.setContentText("Нет данных для экспорта.");
            alert.showAndWait();
            return;
        }

        String exportText = ExportFormat.TSV.buildExport(
            List.copyOf(data),
            dateFormatter,
            timeFormatter
        );
        ClipboardContent contentCopy = new ClipboardContent();
        contentCopy.putString(exportText);
        Clipboard.getSystemClipboard().setContent(contentCopy);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Экспорт");
        alert.setHeaderText(null);
        alert.setContentText("Экспорт скопирован в буфер обмена (TSV).");
        alert.showAndWait();
    }

    @FXML
    private void onExport() {
        if (data.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Экспорт");
            alert.setHeaderText(null);
            alert.setContentText("Нет данных для экспорта.");
            alert.showAndWait();
            return;
        }

        List<Lesson> lessons = List.copyOf(data);
        ExportFormat format = chooseExportFormat();
        if (format == null) {
            return;
        }

        String exportText = format.buildExport(
            lessons,
            dateFormatter,
            timeFormatter
        );
        showExportDialog(format, exportText);
    }

    @FXML
    private void onWeeklyReport() {
        if (lessonUseCase == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Отчет");
            alert.setHeaderText(null);
            alert.setContentText("Ошибка: Use case не инициализирован");
            alert.showAndWait();
            return;
        }

        List<Lesson> lessons = lessonUseCase.getAllLessons();
        if (lessons.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Отчет");
            alert.setHeaderText(null);
            alert.setContentText("Нет данных для отчета.");
            alert.showAndWait();
            return;
        }

        WeeklyReportData reportData = buildWeeklyReportData(lessons);

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить отчет по неделям");
        chooser
            .getExtensionFilters()
            .add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        chooser.setInitialFileName(
            "weekly_report_" + LocalDate.now() + ".xlsx"
        );
        java.io.File file = chooser.showSaveDialog(
            lessonTable.getScene().getWindow()
        );
        if (file == null) {
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по неделям");

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setWrapText(true);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle contentStyle = workbook.createCellStyle();
            contentStyle.setWrapText(true);
            contentStyle.setVerticalAlignment(VerticalAlignment.TOP);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < reportData.headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(reportData.headers.get(i));
                cell.setCellStyle(headerStyle);
            }
            headerRow.setHeightInPoints(42);

            for (int r = 0; r < reportData.rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<String> values = reportData.rows.get(r);
                int maxLines = 1;
                for (int c = 0; c < values.size(); c++) {
                    String value = values.get(c);
                    Cell cell = row.createCell(c);
                    cell.setCellValue(value);
                    cell.setCellStyle(contentStyle);
                    int lines = value == null || value.isBlank()
                        ? 1
                        : value.split("\\R").length;
                    if (lines > maxLines) {
                        maxLines = lines;
                    }
                }
                row.setHeightInPoints(Math.max(20, maxLines * 14));
            }

            for (int i = 0; i < reportData.headers.size(); i++) {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(width + 800, 18000));
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Отчет");
            alert.setHeaderText(null);
            alert.setContentText("Файл сохранён: " + file.getName());
            alert.showAndWait();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText(null);
            alert.setContentText(
                "Не удалось сохранить отчет: " + ex.getMessage()
            );
            alert.showAndWait();
        }
    }

    private ExportFormat chooseExportFormat() {
        ChoiceDialog<ExportFormat> dialog = new ChoiceDialog<>(
            ExportFormat.TSV,
            ExportFormat.values()
        );
        dialog.setTitle("Экспорт");
        dialog.setHeaderText("Выберите формат экспорта");
        dialog.setContentText("Формат:");
        UiStyles.apply(dialog.getDialogPane());
        return dialog.showAndWait().orElse(null);
    }

    private void showExportDialog(ExportFormat format, String exportText) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Экспорт");
        dialog.setHeaderText(format.getLabel());

        TextArea textArea = new TextArea(exportText);
        textArea.setWrapText(false);
        textArea.setEditable(true);
        textArea.setPrefRowCount(18);

        Label hint = new Label(
            "Скопируйте текст и вставьте в Excel. " +
                "Табличный формат использует табуляции, CSV — точку с запятой."
        );

        VBox content = new VBox(8, hint, textArea);
        dialog.getDialogPane().setContent(content);

        ButtonType saveButtonType = new ButtonType(
            "Сохранить в файл",
            ButtonBar.ButtonData.LEFT
        );
        ButtonType copyButtonType = new ButtonType(
            "Скопировать",
            ButtonBar.ButtonData.LEFT
        );
        dialog
            .getDialogPane()
            .getButtonTypes()
            .addAll(saveButtonType, copyButtonType, ButtonType.CLOSE);
        UiStyles.apply(dialog.getDialogPane());

        Button saveButton = (Button) dialog
            .getDialogPane()
            .lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            saveExportToFile(
                dialog.getDialogPane().getScene().getWindow(),
                format,
                exportText
            );
            event.consume();
        });

        Button copyButton = (Button) dialog
            .getDialogPane()
            .lookupButton(copyButtonType);
        copyButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            ClipboardContent contentCopy = new ClipboardContent();
            contentCopy.putString(exportText);
            Clipboard.getSystemClipboard().setContent(contentCopy);
            event.consume();
        });

        dialog.showAndWait();
    }

    private void saveExportToFile(
        Window owner,
        ExportFormat format,
        String exportText
    ) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить экспорт");
        chooser
            .getExtensionFilters()
            .add(
                new FileChooser.ExtensionFilter(
                    format.getExtensionLabel(),
                    "*" + format.getExtension()
                )
            );
        chooser.setInitialFileName(format.buildDefaultFileName());

        java.io.File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }

        try {
            Files.writeString(
                file.toPath(),
                exportText,
                StandardCharsets.UTF_8
            );
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Экспорт");
            alert.setHeaderText(null);
            alert.setContentText("Файл сохранён: " + file.getName());
            alert.showAndWait();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText(null);
            alert.setContentText(
                "Не удалось сохранить файл: " + ex.getMessage()
            );
            alert.showAndWait();
        }
    }

    private ImportResult importLessonsFromText(String text) {
        List<String> errors = new ArrayList<>();
        List<List<String>> rows = DelimitedText.parse(text, errors);
        return importLessonsFromRows(rows, errors);
    }

    private ImportResult importLessonsFromRows(
        List<List<String>> rows,
        List<String> errors
    ) {
        if (rows.isEmpty()) {
            return new ImportResult(0, errors);
        }

        HeaderMapping mapping = HeaderMapping.forLessons(rows);
        if (mapping == null) {
            errors.add("Не удалось определить заголовки или порядок колонок.");
            return new ImportResult(0, errors);
        }

        int added = 0;
        for (int i = mapping.startRowIndex; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            int rowNumber = i + 1;
            try {
                String dateRaw = mapping.get(row, "date");
                String timeRaw = mapping.get(row, "time");
                String topic = mapping.get(row, "topic");
                String lessonName = mapping.get(row, "lesson");
                String className = mapping.get(row, "class");
                String location = mapping.get(row, "location");
                String instructor = mapping.get(row, "instructor");
                if (isBlank(className)) {
                    className = lessonName;
                }

                if (
                    isBlank(timeRaw) ||
                    isBlank(topic) ||
                    isBlank(lessonName) ||
                    isBlank(location) ||
                    isBlank(instructor)
                ) {
                    errors.add(
                        "Строка " + rowNumber + ": пропущены обязательные поля."
                    );
                    continue;
                }

                LocalDate date = isBlank(dateRaw)
                    ? LocalDate.now()
                    : parseDate(dateRaw);
                LocalTime time = parseTime(timeRaw);
                if (date == null || time == null) {
                    errors.add(
                        "Строка " +
                            rowNumber +
                            ": неверный формат даты/времени."
                    );
                    continue;
                }

                Lesson lesson = new Lesson(
                    topic,
                    lessonName,
                    className,
                    time,
                    location,
                    instructor,
                    date
                );
                lessonUseCase.createLesson(lesson);
                added++;
            } catch (Exception ex) {
                errors.add("Строка " + rowNumber + ": " + ex.getMessage());
            }
        }

        reload();
        return new ImportResult(added, errors);
    }

    private void showImportResult(ImportResult result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Импорт");
        alert.setHeaderText(null);
        if (result.errors.isEmpty()) {
            alert.setContentText("Импортировано строк: " + result.added);
        } else {
            StringJoiner joiner = new StringJoiner(System.lineSeparator());
            joiner.add("Импортировано строк: " + result.added);
            int limit = Math.min(6, result.errors.size());
            for (int i = 0; i < limit; i++) {
                joiner.add(result.errors.get(i));
            }
            if (result.errors.size() > limit) {
                joiner.add("Ошибок ещё: " + (result.errors.size() - limit));
            }
            alert.setContentText(joiner.toString());
        }
        alert.showAndWait();
    }

    private LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw, dateFormatter);
        } catch (Exception ignored) {
            try {
                DateTimeFormatter shortSlash = DateTimeFormatter.ofPattern(
                    "dd/MM/yy"
                );
                return LocalDate.parse(raw, shortSlash);
            } catch (Exception ignoredAlt) {
                try {
                    return LocalDate.parse(raw);
                } catch (Exception ignored2) {
                    return null;
                }
            }
        }
    }

    private LocalTime parseTime(String raw) {
        try {
            return LocalTime.parse(raw, timeFormatter);
        } catch (Exception ignored) {
            try {
                return LocalTime.parse(raw);
            } catch (Exception ignored2) {
                return null;
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String formatDateValue(LocalDate date) {
        return date == null ? "" : dateFormatter.format(date);
    }

    private String formatTimeValue(LocalTime time) {
        return time == null ? "" : timeFormatter.format(time);
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private void applySearchFilter(String query) {
        if (filteredData == null) {
            return;
        }
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            filteredData.setPredicate(lesson -> true);
            return;
        }
        filteredData.setPredicate(lesson -> lessonMatchesQuery(lesson, normalized));
    }

    private boolean lessonMatchesQuery(Lesson lesson, String query) {
        return (
            safeValue(lesson.getTopic()).toLowerCase(Locale.ROOT).contains(query) ||
            safeValue(lesson.getLessonName()).toLowerCase(Locale.ROOT).contains(query) ||
            safeValue(lesson.getClassName()).toLowerCase(Locale.ROOT).contains(query) ||
            safeValue(lesson.getLocation()).toLowerCase(Locale.ROOT).contains(query) ||
            safeValue(lesson.getInstructor()).toLowerCase(Locale.ROOT).contains(query)
        );
    }

    private void updateCountLabel() {
        if (filteredData == null) {
            countLabel.setText("Всего занятий: " + data.size());
            return;
        }
        int visible = filteredData.size();
        int total = data.size();
        if (searchField.getText() == null || searchField.getText().isBlank()) {
            countLabel.setText("Всего занятий: " + total);
            return;
        }
        countLabel.setText("Показано: " + visible + " из " + total);
    }

    private List<List<String>> readXlsxRows(
        java.io.File file,
        List<String> errors
    ) {
        try (
            FileInputStream input = new FileInputStream(file);
            Workbook workbook = new XSSFWorkbook(input)
        ) {
            Sheet sheet =
                workbook.getNumberOfSheets() > 0
                    ? workbook.getSheetAt(0)
                    : null;
            if (sheet == null) {
                errors.add("XLSX файл не содержит листов.");
                return List.of();
            }

            DataFormatter formatter = new DataFormatter();
            List<List<String>> rows = new ArrayList<>();
            int lastRow = sheet.getLastRowNum();
            for (int i = 0; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                int lastCell = row.getLastCellNum();
                if (lastCell <= 0) {
                    continue;
                }
                List<String> values = new ArrayList<>();
                boolean hasContent = false;
                for (int c = 0; c < lastCell; c++) {
                    Cell cell = row.getCell(c);
                    String value =
                        cell == null ? "" : formatter.formatCellValue(cell);
                    if (!value.isBlank()) {
                        hasContent = true;
                    }
                    values.add(value.trim());
                }
                if (hasContent) {
                    rows.add(values);
                }
            }
            return rows;
        } catch (Exception ex) {
            errors.add("Не удалось прочитать XLSX: " + ex.getMessage());
            return List.of();
        }
    }

    private void loadDelimitedFromFile(Window owner, TextArea target) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Загрузить TSV/CSV");
        chooser
            .getExtensionFilters()
            .addAll(
                new FileChooser.ExtensionFilter("TSV (*.tsv)", "*.tsv"),
                new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"),
                new FileChooser.ExtensionFilter("Все файлы (*.*)", "*.*")
            );
        java.io.File file = chooser.showOpenDialog(owner);
        if (file == null) {
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String content = new String(bytes, StandardCharsets.UTF_8);
            if (content.indexOf('\uFFFD') >= 0) {
                content = new String(bytes, Charset.forName("Windows-1251"));
            }
            target.setText(content);
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Импорт");
            alert.setHeaderText(null);
            alert.setContentText(
                "Не удалось прочитать файл: " + ex.getMessage()
            );
            alert.showAndWait();
        }
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
                        reload();
                    }
                });
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setContentText(
                "Не удалось открыть диалог: " + e.getMessage()
            );
            alert.showAndWait();
        }
    }

    private enum ExportFormat {
        TSV("Табличный (TSV, табуляции)", "\t", false),
        CSV("CSV (;) для Excel", ";", true);

        private final String label;
        private final String delimiter;
        private final boolean csvQuote;

        ExportFormat(String label, String delimiter, boolean csvQuote) {
            this.label = label;
            this.delimiter = delimiter;
            this.csvQuote = csvQuote;
        }

        String getLabel() {
            return label;
        }

        String getExtension() {
            return this == TSV ? ".tsv" : ".csv";
        }

        String getExtensionLabel() {
            return this == TSV ? "TSV (*.tsv)" : "CSV (*.csv)";
        }

        String buildDefaultFileName() {
            return "schedule_export_" + LocalDate.now() + getExtension();
        }

        @Override
        public String toString() {
            return label;
        }

        String buildExport(
            List<Lesson> lessons,
            DateTimeFormatter dateFormatter,
            DateTimeFormatter timeFormatter
        ) {
            StringJoiner joiner = new StringJoiner(System.lineSeparator());
            joiner.add(buildHeader());
            for (Lesson lesson : lessons) {
                joiner.add(
                    formatRow(
                        List.of(
                            formatDate(lesson.getDate(), dateFormatter),
                            formatTime(lesson.getTime(), timeFormatter),
                            safe(lesson.getTopic()),
                            safe(lesson.getLessonName()),
                            safe(lesson.getClassName()),
                            safe(lesson.getLocation()),
                            safe(lesson.getInstructor())
                        )
                    )
                );
            }
            return joiner.toString();
        }

        private String buildHeader() {
            return formatRow(List.of(EXPORT_HEADERS));
        }

        private String formatRow(List<String> values) {
            StringJoiner row = new StringJoiner(delimiter);
            for (String value : values) {
                if (csvQuote) {
                    row.add(escapeCsv(value));
                } else {
                    row.add(escapeTsv(value));
                }
            }
            return row.toString();
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }

        private static String formatDate(
            LocalDate date,
            DateTimeFormatter formatter
        ) {
            return date == null ? "" : formatter.format(date);
        }

        private static String formatTime(
            LocalTime time,
            DateTimeFormatter formatter
        ) {
            return time == null ? "" : formatter.format(time);
        }

        private String escapeTsv(String value) {
            return value
                .replace('\t', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ');
        }

        private String escapeCsv(String value) {
            boolean needsQuotes =
                value.indexOf('"') >= 0 ||
                value.indexOf(delimiter) >= 0 ||
                value.indexOf('\n') >= 0 ||
                value.indexOf('\r') >= 0;
            String escaped = value.replace("\"", "\"\"");
            return needsQuotes ? "\"" + escaped + "\"" : escaped;
        }
    }

    private WeeklyReportData buildWeeklyReportData(List<Lesson> sourceLessons) {
        List<Lesson> lessons = new ArrayList<>(sourceLessons);
        lessons.sort(
            Comparator.comparing(Lesson::getDate).thenComparing(Lesson::getTime)
        );

        LocalDate minDate = lessons.get(0).getDate();
        LocalDate maxDate = lessons.get(lessons.size() - 1).getDate();
        LocalDate weekStart = minDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate finalWeekStart = maxDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));

        List<WeekRange> weeks = new ArrayList<>();
        LocalDate cursor = weekStart;
        while (!cursor.isAfter(finalWeekStart)) {
            weeks.add(new WeekRange(cursor, cursor.plusDays(4)));
            cursor = cursor.plusWeeks(1);
        }

        Map<String, List<Lesson>> bySubject = new LinkedHashMap<>();
        for (Lesson lesson : lessons) {
            bySubject
                .computeIfAbsent(lesson.getTopic(), key -> new ArrayList<>())
                .add(lesson);
        }

        List<String> headers = new ArrayList<>();
        headers.add("№ п/п");
        headers.add("Предмет обучения");
        headers.add("Количество часов");
        for (WeekRange week : weeks) {
            headers.add(week.title());
        }
        List<List<String>> rows = new ArrayList<>();

        int index = 1;
        for (Map.Entry<String, List<Lesson>> entry : bySubject.entrySet()) {
            String subject = entry.getKey();
            List<Lesson> subjectLessons = entry.getValue();
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(index++));
            row.add(safeValue(subject));
            row.add(String.valueOf(subjectLessons.size()));

            for (WeekRange week : weeks) {
                Map<String, Integer> grouped = new LinkedHashMap<>();
                for (Lesson lesson : subjectLessons) {
                    if (week.contains(lesson.getDate())) {
                        String key =
                            safeValue(lesson.getLessonName()) +
                            "/" +
                            safeValue(lesson.getClassName());
                        grouped.merge(key, 1, Integer::sum);
                    }
                }

                if (grouped.isEmpty()) {
                    row.add("");
                    continue;
                }

                StringJoiner cell = new StringJoiner(System.lineSeparator());
                for (Map.Entry<String, Integer> group : grouped.entrySet()) {
                    cell.add(group.getKey());
                    cell.add(String.valueOf(group.getValue()));
                }
                row.add(cell.toString());
            }

            rows.add(row);
        }

        return new WeeklyReportData(headers, rows);
    }

    private static final class TopicSummary {
        private final String label;
        private int archivedCount;
        private int activeCount;

        private TopicSummary(String label) {
            this.label = label;
        }
    }

    private static final class WeekRange {
        private final LocalDate start;
        private final LocalDate end;

        private WeekRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }

        private boolean contains(LocalDate date) {
            return !date.isBefore(start) && !date.isAfter(end);
        }

        private String title() {
            YearMonth ym = YearMonth.from(start);
            int weekInMonth = ((start.getDayOfMonth() - 1) / 7) + 1;
            String monthName = start
                .format(DateTimeFormatter.ofPattern("LLLL", new Locale("ru")))
                .substring(0, 1)
                .toUpperCase(new Locale("ru")) +
                start
                    .format(DateTimeFormatter.ofPattern("LLLL", new Locale("ru")))
                    .substring(1);
            return (
                monthName +
                " " +
                ym.getYear() +
                " (" +
                weekInMonth +
                " нед: " +
                start.format(DateTimeFormatter.ofPattern("dd.MM")) +
                "-" +
                end.format(DateTimeFormatter.ofPattern("dd.MM")) +
                ")"
            );
        }
    }

    private static final class WeeklyReportData {
        private final List<String> headers;
        private final List<List<String>> rows;

        private WeeklyReportData(List<String> headers, List<List<String>> rows) {
            this.headers = headers;
            this.rows = rows;
        }
    }

    private static final class ImportResult {

        private final int added;
        private final List<String> errors;

        private ImportResult(int added, List<String> errors) {
            this.added = added;
            this.errors = errors;
        }
    }

    private static final class HeaderMapping {

        private final Map<String, Integer> indexByKey;
        private final int startRowIndex;

        private HeaderMapping(
            Map<String, Integer> indexByKey,
            int startRowIndex
        ) {
            this.indexByKey = indexByKey;
            this.startRowIndex = startRowIndex;
        }

        private String get(List<String> row, String key) {
            Integer index = indexByKey.get(key);
            if (index == null || index < 0 || index >= row.size()) {
                return "";
            }
            return row.get(index);
        }

        private static HeaderMapping forLessons(List<List<String>> rows) {
            if (rows.isEmpty()) {
                return null;
            }

            Map<String, String> aliases = new HashMap<>();
            aliases.put("дата", "date");
            aliases.put("date", "date");
            aliases.put("время", "time");
            aliases.put("time", "time");
            aliases.put("предмет", "topic");
            aliases.put("subject", "topic");
            aliases.put("topic", "topic");
            aliases.put("тема", "lesson");
            aliases.put("lesson", "lesson");
            aliases.put("lessonname", "lesson");
            aliases.put("class", "class");
            aliases.put("classname", "class");
            aliases.put("занятие", "class");
            aliases.put("место", "location");
            aliases.put("location", "location");
            aliases.put("преподаватель", "instructor");
            aliases.put("инструктор", "instructor");
            aliases.put("instructor", "instructor");
            aliases.put("teacher", "instructor");

            List<String> required = List.of(
                "time",
                "topic",
                "lesson",
                "location",
                "instructor"
            );
            Map<String, Integer> headerMap = resolveHeaderMap(
                rows.get(0),
                aliases
            );
            if (!headerMap.isEmpty()) {
                if (headerMap.keySet().containsAll(required)) {
                    if (!headerMap.containsKey("class")) {
                        headerMap.put("class", -1);
                    }
                    if (!headerMap.containsKey("date")) {
                        headerMap.put("date", -1);
                    }
                    return new HeaderMapping(headerMap, 1);
                }
                if (rows.get(0).size() >= required.size()) {
                    return new HeaderMapping(
                        buildDefaultMap(rows.get(0), required),
                        1
                    );
                }
                return null;
            }

            return new HeaderMapping(buildDefaultMap(rows.get(0), required), 0);
        }

        private static Map<String, Integer> buildDefaultMap(
            List<String> row,
            List<String> keys
        ) {
            Map<String, Integer> defaultMap = new HashMap<>();
            int size = row.size();
            if (size >= 7) {
                defaultMap.put("date", 0);
                defaultMap.put("time", 1);
                defaultMap.put("topic", 2);
                defaultMap.put("lesson", 3);
                defaultMap.put("class", 4);
                defaultMap.put("location", 5);
                defaultMap.put("instructor", 6);
                return defaultMap;
            }
            if (size >= 6) {
                boolean firstLooksLikeTime = row
                    .get(0)
                    .contains(":");
                if (firstLooksLikeTime) {
                    defaultMap.put("date", -1);
                    defaultMap.put("time", 0);
                    defaultMap.put("topic", 1);
                    defaultMap.put("lesson", 2);
                    defaultMap.put("class", 3);
                    defaultMap.put("location", 4);
                    defaultMap.put("instructor", 5);
                } else {
                    defaultMap.put("date", 0);
                    defaultMap.put("time", 1);
                    defaultMap.put("topic", 2);
                    defaultMap.put("lesson", 3);
                    defaultMap.put("class", -1);
                    defaultMap.put("location", 4);
                    defaultMap.put("instructor", 5);
                }
                return defaultMap;
            }
            defaultMap.put("date", -1);
            defaultMap.put("time", 0);
            defaultMap.put("topic", 1);
            defaultMap.put("lesson", 2);
            if (size >= 6) {
                defaultMap.put("class", 3);
                defaultMap.put("location", 4);
                defaultMap.put("instructor", 5);
            } else {
                defaultMap.put("class", -1);
                defaultMap.put("location", 3);
                defaultMap.put("instructor", 4);
            }
            return defaultMap;
        }

        private static Map<String, Integer> resolveHeaderMap(
            List<String> row,
            Map<String, String> aliases
        ) {
            Map<String, Integer> map = new HashMap<>();
            for (int i = 0; i < row.size(); i++) {
                String normalized = DelimitedText.normalizeHeader(row.get(i));
                if (normalized.isEmpty()) {
                    continue;
                }
                String key = aliases.get(normalized);
                if (key != null && !map.containsKey(key)) {
                    map.put(key, i);
                }
            }

            if (map.size() < 2) {
                return Map.of();
            }
            return map;
        }
    }
}
