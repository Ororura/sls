package com.ororura.slseleven.controller;

import com.ororura.slseleven.domain.model.ScheduleItem;
import com.ororura.slseleven.domain.model.InstructorDuty;
import com.ororura.slseleven.domain.model.InstructorProfile;
import com.ororura.slseleven.domain.model.RoomProfile;
import com.ororura.slseleven.domain.model.SubjectScheduleRule;
import com.ororura.slseleven.ui.UiAlerts;
import com.ororura.slseleven.ui.UiStyles;
import com.ororura.slseleven.usecase.AutoScheduleResult;
import com.ororura.slseleven.usecase.ScheduleUseCase;
import com.ororura.slseleven.util.DelimitedText;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class SchedulePlannerController {

    @FXML
    private TableView<ScheduleItem> scheduleTable;

    @FXML
    private TableColumn<ScheduleItem, String> topicColumn;

    @FXML
    private TableColumn<ScheduleItem, String> lessonNameColumn;

    @FXML
    private TableColumn<ScheduleItem, String> classNameColumn;

    @FXML
    private TableColumn<ScheduleItem, String> locationColumn;

    @FXML
    private TableColumn<ScheduleItem, String> instructorColumn;

    @FXML
    private TableColumn<ScheduleItem, Integer> hoursColumn;

    @FXML
    private TableColumn<ScheduleItem, Integer> consecutiveHoursColumn;

    @FXML
    private Label totalHoursLabel;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Spinner<Integer> mondayHours;

    @FXML
    private Spinner<Integer> tuesdayHours;

    @FXML
    private Spinner<Integer> wednesdayHours;

    @FXML
    private Spinner<Integer> thursdayHours;

    @FXML
    private Spinner<Integer> fridayHours;

    @FXML
    private Spinner<Integer> saturdayHours;

    @FXML
    private Spinner<Integer> sundayHours;

    @FXML
    private TextField searchField;

    private final ObservableList<ScheduleItem> data =
        FXCollections.observableArrayList();
    private FilteredList<ScheduleItem> filteredData;

    private ScheduleUseCase scheduleUseCase;

    @FXML
    public void initialize() {
        filteredData = new FilteredList<>(data, item -> true);
        scheduleTable.setItems(filteredData);
        scheduleTable.setPlaceholder(
            new Label("Пул пуст. Добавьте записи или измените фильтр.")
        );
        scheduleTable
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
        hoursColumn.setCellValueFactory(new PropertyValueFactory<>("hours"));
        consecutiveHoursColumn.setCellValueFactory(
            new PropertyValueFactory<>("consecutiveHours")
        );

        setupSpinner(mondayHours);
        setupSpinner(tuesdayHours);
        setupSpinner(wednesdayHours);
        setupSpinner(thursdayHours);
        setupSpinner(fridayHours);
        setupSpinner(saturdayHours);
        setupSpinner(sundayHours);

        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(null);
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            applySearchFilter(newValue);
            updateTotalLabel();
        });
    }

    public void setScheduleUseCase(ScheduleUseCase scheduleUseCase) {
        this.scheduleUseCase = scheduleUseCase;
        reload();
        loadSettings();
    }

    @FXML
    private void onAddItem() {
        showItemDialog(null);
    }

    @FXML
    private void onEditItem() {
        ScheduleItem selected = scheduleTable
            .getSelectionModel()
            .getSelectedItem();
        if (selected == null) {
            showInfo("Выберите строку для редактирования.");
            return;
        }
        showItemDialog(selected);
    }

    @FXML
    private void onDeleteItem() {
        List<ScheduleItem> selected = List.copyOf(
            scheduleTable.getSelectionModel().getSelectedItems()
        );
        if (selected.isEmpty()) {
            showInfo("Выберите строки для удаления.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение");
        alert.setHeaderText("Удалить выбранные записи?");
        alert.setContentText(
            "Количество выбранных записей: " + selected.size()
        );
        alert
            .showAndWait()
            .ifPresent(response -> {
                if (response == ButtonType.OK) {
                    for (ScheduleItem item : selected) {
                        scheduleUseCase.deleteItem(item.getId());
                    }
                    reload();
                }
            });
    }

    @FXML
    private void onQuickImport() {
        if (scheduleUseCase == null) {
            showError("Ошибка: Use case не инициализирован");
            return;
        }
        String clipboardText = Clipboard.getSystemClipboard().getString();
        if (clipboardText == null || clipboardText.trim().isEmpty()) {
            showInfo("Буфер обмена пуст.");
            return;
        }

        ImportResult result = importItemsFromText(clipboardText);
        showImportResult(result);
    }

    @FXML
    private void onImport() {
        if (scheduleUseCase == null) {
            showError("Ошибка: Use case не инициализирован");
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
            "Ожидаемые колонки: Предмет, Тема, Часы. Поля Занятие/Место/Преподаватель можно не передавать."
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
            ImportResult result = importItemsFromText(textArea.getText());
            showImportResult(result);
            event.consume();
        });

        dialog.showAndWait();
    }

    @FXML
    private void onImportXlsx() {
        if (scheduleUseCase == null) {
            showError("Ошибка: Use case не инициализирован");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Импорт XLSX");
        chooser
            .getExtensionFilters()
            .add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        java.io.File file = chooser.showOpenDialog(
            scheduleTable.getScene().getWindow()
        );
        if (file == null) {
            return;
        }

        List<String> errors = new ArrayList<>();
        List<List<String>> rows = readXlsxRows(file, errors);
        ImportResult result = importItemsFromRows(rows, errors);
        showImportResult(result);
    }

    @FXML
    private void onExportXlsx() {
        if (data.isEmpty()) {
            showInfo("Нет данных для экспорта.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт XLSX");
        chooser
            .getExtensionFilters()
            .add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        chooser.setInitialFileName(
            "schedule_export_" + LocalDate.now() + ".xlsx"
        );
        java.io.File file = chooser.showSaveDialog(
            scheduleTable.getScene().getWindow()
        );
        if (file == null) {
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Авторасписание");
            Row header = sheet.createRow(0);
            String[] headers = {
                "Предмет",
                "Тема",
                "Занятие",
                "Место",
                "Преподаватель",
                "Часы",
                "Подряд (предмет)",
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            int rowIndex = 1;
            for (ScheduleItem item : data) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(safeValue(item.getTopic()));
                row.createCell(1).setCellValue(safeValue(item.getLessonName()));
                row.createCell(2).setCellValue(safeValue(item.getClassName()));
                row.createCell(3).setCellValue(safeValue(item.getLocation()));
                row.createCell(4).setCellValue(safeValue(item.getInstructor()));
                row.createCell(5).setCellValue(item.getHours());
                row.createCell(6).setCellValue(item.getConsecutiveHours());
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }

            showInfo("Файл сохранён: " + file.getName());
        } catch (Exception ex) {
            showError("Не удалось сохранить файл: " + ex.getMessage());
        }
    }

    @FXML
    private void onImportAndSchedule() {
        if (scheduleUseCase == null) {
            showError("Ошибка: Use case не инициализирован");
            return;
        }
        if (startDatePicker.getValue() == null) {
            showError("Укажите дату начала");
            return;
        }
        if (!validateDateRange()) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Импорт и распределение");
        dialog.setHeaderText("Вставьте данные для импорта");

        TextArea textArea = new TextArea();
        textArea.setWrapText(false);
        textArea.setEditable(true);
        textArea.setPrefRowCount(18);

        Label hint = new Label(
            "Ожидаемые колонки: Предмет, Тема, Часы. Поля Занятие/Место/Преподаватель можно не передавать."
        );
        Label hint2 = new Label(
            "Поддерживаются табуляции (TSV) и CSV с ';' или ','. Заголовок необязателен."
        );

        VBox content = new VBox(8, hint, hint2, textArea);
        dialog.getDialogPane().setContent(content);

        ButtonType importButtonType = new ButtonType(
            "Импортировать и распределить",
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
            ImportResult importResult = importItemsFromText(textArea.getText());
            if (importResult.added <= 0) {
                showImportResult(importResult);
                event.consume();
                return;
            }

            saveSettings();

            try {
                saveHistorySnapshot("Перед импортом и распределением");
                AutoScheduleResult scheduleResult =
                    scheduleUseCase.autoSchedule(
                        startDatePicker.getValue(),
                        endDatePicker.getValue()
                    );
                reload();
                showImportAndScheduleResult(importResult, scheduleResult);
            } catch (Exception ex) {
                showError("Ошибка при распределении: " + ex.getMessage());
            }

            event.consume();
        });

        dialog.showAndWait();
    }

    @FXML
    private void onQuickExport() {
        if (data.isEmpty()) {
            showInfo("Нет данных для экспорта.");
            return;
        }

        String exportText = buildTsvExport(List.copyOf(data));
        ClipboardContent contentCopy = new ClipboardContent();
        contentCopy.putString(exportText);
        Clipboard.getSystemClipboard().setContent(contentCopy);
        showInfo("Экспорт скопирован в буфер обмена (TSV).");
    }

    @FXML
    private void onDeleteAll() {
        if (data.isEmpty()) {
            showInfo("Список пуст.");
            return;
        }

        if (!confirmDoubleDelete("Удалить все записи авторасписания?")) {
            return;
        }

        scheduleUseCase.deleteAllItems();
        reload();
    }

    @FXML
    private void onSaveSettings() {
        saveSettings();
        showInfo("Настройки сохранены.");
    }

    @FXML
    private void onSubjectRules() {
        if (scheduleUseCase == null) {
            showError("Ошибка: Use case не инициализирован");
            return;
        }

        List<String> poolSubjects = data
            .stream()
            .map(ScheduleItem::getTopic)
            .filter(value -> value != null && !value.trim().isEmpty())
            .map(String::trim)
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(Collectors.toList());
        if (poolSubjects.isEmpty()) {
            showInfo("Сначала добавьте предметы в пул автораспределения.");
            return;
        }

        Map<String, SubjectScheduleRule> existingBySubject =
            new LinkedHashMap<>();
        for (SubjectScheduleRule rule : scheduleUseCase.getSubjectRules()) {
            existingBySubject.put(normalizeSubjectKey(rule.getSubject()), rule);
        }
        List<String> roomNames = scheduleUseCase
            .getRooms()
            .stream()
            .map(RoomProfile::getName)
            .filter(name -> name != null && !name.isBlank())
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(Collectors.toList());

        List<SubjectRuleEditModel> models = new ArrayList<>();
        for (String subject : poolSubjects) {
            SubjectScheduleRule current = existingBySubject.get(
                normalizeSubjectKey(subject)
            );
            Set<DayOfWeek> allowed = current == null
                ? EnumSet.allOf(DayOfWeek.class)
                : current.getAllowedDays();
            Set<DayOfWeek> exclusive = current == null
                ? EnumSet.noneOf(DayOfWeek.class)
                : current.getExclusiveDays();
            int consecutiveHours = current == null
                ? detectSubjectConsecutiveHours(subject)
                : current.getConsecutiveHours();
            String fixedRoom = current == null ? "" : current.getFixedRoom();
            models.add(
                new SubjectRuleEditModel(
                    subject,
                    allowed,
                    exclusive,
                    consecutiveHours,
                    fixedRoom
                )
            );
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Правила предметов");
        dialog.setHeaderText("Настройка правил для предметов из текущего пула");

        ListView<SubjectRuleEditModel> subjectList = new ListView<>(
            FXCollections.observableArrayList(models)
        );
        subjectList.setPrefWidth(280);
        subjectList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(SubjectRuleEditModel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.subject);
            }
        });

        Label selectedSubjectLabel = new Label("Выберите предмет");
        selectedSubjectLabel.getStyleClass().add("section-title");
        Spinner<Integer> consecutiveHoursSpinner = new Spinner<>();
        consecutiveHoursSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 8, 1)
        );
        consecutiveHoursSpinner.setEditable(true);
        ComboBox<String> fixedRoomComboBox = new ComboBox<>();
        fixedRoomComboBox
            .getItems()
            .addAll(roomNames);
        fixedRoomComboBox.getItems().add(0, "Без фиксации");
        fixedRoomComboBox.setValue("Без фиксации");

        FlowPane allowedPane = new FlowPane(8, 8);
        FlowPane exclusivePane = new FlowPane(8, 8);
        Map<DayOfWeek, CheckBox> allowedBoxes = new EnumMap<>(DayOfWeek.class);
        Map<DayOfWeek, CheckBox> exclusiveBoxes = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            CheckBox allow = new CheckBox(dayToShort(day));
            CheckBox exclusive = new CheckBox(dayToShort(day));
            allowedBoxes.put(day, allow);
            exclusiveBoxes.put(day, exclusive);
            allowedPane.getChildren().add(allow);
            exclusivePane.getChildren().add(exclusive);
        }

        Label hint = new Label(
            "Часы подряд задаются на уровне предмета и применяются ко всем темам этого предмета.\n" +
            "Разрешенные дни: когда предмет можно ставить.\n" +
            "Эксклюзивные дни: в эти дни можно ставить только этот предмет."
        );
        Button allDaysButton = new Button("Разрешить все");
        Button clearExclusiveButton = new Button("Снять эксклюзив");

        VBox editor = new VBox(
            10,
            selectedSubjectLabel,
            new Label("Часы подряд (для предмета):"),
            consecutiveHoursSpinner,
            new Label("Фиксированный кабинет:"),
            fixedRoomComboBox,
            new Label("Разрешенные дни:"),
            allowedPane,
            new Label("Эксклюзивные дни:"),
            exclusivePane,
            new HBox(8, allDaysButton, clearExclusiveButton),
            hint
        );

        HBox layout = new HBox(14, subjectList, editor);
        dialog.getDialogPane().setContent(layout);
        dialog
            .getDialogPane()
            .getButtonTypes()
            .addAll(ButtonType.OK, ButtonType.CANCEL);
        UiStyles.apply(dialog.getDialogPane());

        final SubjectRuleEditModel[] currentModel = new SubjectRuleEditModel[] {
            null,
        };

        Runnable refreshEditor = () -> {
            SubjectRuleEditModel model = currentModel[0];
            boolean hasSubject = model != null;
            selectedSubjectLabel.setText(
                hasSubject ? model.subject : "Выберите предмет"
            );
            consecutiveHoursSpinner.setDisable(!hasSubject);
            consecutiveHoursSpinner
                .getValueFactory()
                .setValue(hasSubject ? Math.max(1, model.consecutiveHours) : 1);
            fixedRoomComboBox.setDisable(!hasSubject);
            if (hasSubject && model.fixedRoom != null && !model.fixedRoom.isBlank()) {
                fixedRoomComboBox.setValue(model.fixedRoom);
            } else {
                fixedRoomComboBox.setValue("Без фиксации");
            }
            for (DayOfWeek day : DayOfWeek.values()) {
                CheckBox allow = allowedBoxes.get(day);
                CheckBox exclusive = exclusiveBoxes.get(day);
                allow.setDisable(!hasSubject);
                exclusive.setDisable(!hasSubject);
                allow.setSelected(hasSubject && model.allowedDays.contains(day));
                exclusive.setSelected(
                    hasSubject && model.exclusiveDays.contains(day)
                );
            }
            allDaysButton.setDisable(!hasSubject);
            clearExclusiveButton.setDisable(!hasSubject);
        };

        consecutiveHoursSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            SubjectRuleEditModel model = currentModel[0];
            if (model == null || newV == null) {
                return;
            }
            model.consecutiveHours = Math.max(1, newV);
        });
        fixedRoomComboBox.valueProperty().addListener((obs, oldV, newV) -> {
            SubjectRuleEditModel model = currentModel[0];
            if (model == null) {
                return;
            }
            if (newV == null || "Без фиксации".equals(newV)) {
                model.fixedRoom = "";
            } else {
                model.fixedRoom = newV;
            }
        });

        for (DayOfWeek day : DayOfWeek.values()) {
            CheckBox allow = allowedBoxes.get(day);
            CheckBox exclusive = exclusiveBoxes.get(day);
            allow.selectedProperty().addListener((obs, oldV, newV) -> {
                SubjectRuleEditModel model = currentModel[0];
                if (model == null) {
                    return;
                }
                if (newV) {
                    model.allowedDays.add(day);
                } else {
                    model.allowedDays.remove(day);
                    model.exclusiveDays.remove(day);
                    exclusive.setSelected(false);
                }
            });
            exclusive.selectedProperty().addListener((obs, oldV, newV) -> {
                SubjectRuleEditModel model = currentModel[0];
                if (model == null) {
                    return;
                }
                if (newV) {
                    model.exclusiveDays.add(day);
                    model.allowedDays.add(day);
                    allow.setSelected(true);
                } else {
                    model.exclusiveDays.remove(day);
                }
            });
        }

        allDaysButton.setOnAction(event -> {
            SubjectRuleEditModel model = currentModel[0];
            if (model == null) {
                return;
            }
            model.allowedDays.clear();
            model.allowedDays.addAll(EnumSet.allOf(DayOfWeek.class));
            refreshEditor.run();
        });
        clearExclusiveButton.setOnAction(event -> {
            SubjectRuleEditModel model = currentModel[0];
            if (model == null) {
                return;
            }
            model.exclusiveDays.clear();
            refreshEditor.run();
        });

        subjectList
            .getSelectionModel()
            .selectedItemProperty()
            .addListener((obs, oldV, newV) -> {
                currentModel[0] = newV;
                refreshEditor.run();
            });
        if (!models.isEmpty()) {
            subjectList.getSelectionModel().select(0);
        } else {
            refreshEditor.run();
        }

        dialog
            .showAndWait()
            .ifPresent(button -> {
                if (button != ButtonType.OK) {
                    return;
                }

                Map<String, SubjectScheduleRule> merged = new LinkedHashMap<>();
                for (SubjectScheduleRule rule : scheduleUseCase.getSubjectRules()) {
                    merged.put(normalizeSubjectKey(rule.getSubject()), rule);
                }
                for (SubjectRuleEditModel model : models) {
                    if (model.isDefaultRule()) {
                        merged.remove(normalizeSubjectKey(model.subject));
                    } else {
                        merged.put(
                            normalizeSubjectKey(model.subject),
                            new SubjectScheduleRule(
                                model.subject,
                                model.allowedDays,
                                model.exclusiveDays,
                                model.consecutiveHours,
                                model.fixedRoom
                            )
                        );
                    }
                }
                List<SubjectScheduleRule> toSave = new ArrayList<>(
                    merged.values()
                );
                toSave.sort(Comparator.comparing(SubjectScheduleRule::getSubject, String.CASE_INSENSITIVE_ORDER));
                scheduleUseCase.saveSubjectRules(toSave);
                showInfo("Правила предметов сохранены.");
            });
    }

    @FXML
    private void onManageInstructors() {
        if (scheduleUseCase == null) {
            showError("Ошибка: Use case не инициализирован");
            return;
        }
        while (true) {
            List<InstructorProfile> instructors = scheduleUseCase.getInstructors();
            List<String> options = new ArrayList<>();
            options.add("Добавить преподавателя");
            options.add("Изменить рабочие дни");
            options.add("Переименовать преподавателя");
            options.add("Удалить преподавателя");
            options.add("Закрыть");

            ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
            dialog.setTitle("Преподаватели");
            dialog.setHeaderText(
                "Преподаватели: " +
                (instructors.isEmpty()
                        ? "нет"
                        : instructors
                            .stream()
                            .map(InstructorProfile::getName)
                            .collect(Collectors.joining(", ")))
            );
            dialog.setContentText("Действие:");
            UiStyles.apply(dialog.getDialogPane());
            String action = dialog.showAndWait().orElse("Закрыть");
            if ("Закрыть".equals(action)) {
                return;
            }
            try {
                if ("Добавить преподавателя".equals(action)) {
                    TextInputDialog input = new TextInputDialog();
                    input.setTitle("Преподаватели");
                    input.setHeaderText("Добавить преподавателя");
                    input.setContentText("ФИО:");
                    UiStyles.apply(input.getDialogPane());
                    String value = input.showAndWait().orElse("").trim();
                    if (!value.isEmpty()) {
                        scheduleUseCase.createInstructor(value);
                    }
                } else if ("Переименовать преподавателя".equals(action)) {
                    InstructorProfile selected = selectInstructor(instructors, "Переименовать преподавателя");
                    if (selected == null) {
                        continue;
                    }
                    TextInputDialog input = new TextInputDialog(selected.getName());
                    input.setTitle("Преподаватели");
                    input.setHeaderText("Переименовать преподавателя");
                    input.setContentText("Новое имя:");
                    UiStyles.apply(input.getDialogPane());
                    String value = input.showAndWait().orElse("").trim();
                    if (!value.isEmpty()) {
                        selected.setName(value);
                        scheduleUseCase.updateInstructor(selected);
                    }
                } else if ("Удалить преподавателя".equals(action)) {
                    InstructorProfile selected = selectInstructor(instructors, "Удалить преподавателя");
                    if (selected == null) {
                        continue;
                    }
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Преподаватели");
                    confirm.setHeaderText("Удалить \"" + selected.getName() + "\"?");
                    confirm.setContentText("Связанные наряды будут удалены.");
                    UiStyles.apply(confirm.getDialogPane());
                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        scheduleUseCase.deleteInstructor(selected.getId());
                    }
                } else if ("Изменить рабочие дни".equals(action)) {
                    InstructorProfile selected = selectInstructor(instructors, "Рабочие дни преподавателя");
                    if (selected == null) {
                        continue;
                    }
                    Dialog<ButtonType> daysDialog = new Dialog<>();
                    daysDialog.setTitle("Рабочие дни");
                    daysDialog.setHeaderText(selected.getName());
                    FlowPane pane = new FlowPane(8, 8);
                    Map<DayOfWeek, CheckBox> boxes = new EnumMap<>(DayOfWeek.class);
                    for (DayOfWeek day : DayOfWeek.values()) {
                        CheckBox box = new CheckBox(dayToShort(day));
                        box.setSelected(selected.getAllowedDays().contains(day));
                        boxes.put(day, box);
                        pane.getChildren().add(box);
                    }
                    daysDialog.getDialogPane().setContent(pane);
                    daysDialog
                        .getDialogPane()
                        .getButtonTypes()
                        .addAll(ButtonType.OK, ButtonType.CANCEL);
                    UiStyles.apply(daysDialog.getDialogPane());
                    if (daysDialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        Set<DayOfWeek> allowed = EnumSet.noneOf(DayOfWeek.class);
                        for (Map.Entry<DayOfWeek, CheckBox> entry : boxes.entrySet()) {
                            if (entry.getValue().isSelected()) {
                                allowed.add(entry.getKey());
                            }
                        }
                        if (allowed.isEmpty()) {
                            showError("Выберите хотя бы один рабочий день.");
                            continue;
                        }
                        selected.setAllowedDays(allowed);
                        scheduleUseCase.updateInstructor(selected);
                    }
                }
            } catch (Exception e) {
                showError("Ошибка: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onManageRooms() {
        if (scheduleUseCase == null) {
            showError("Ошибка: Use case не инициализирован");
            return;
        }
        while (true) {
            List<RoomProfile> rooms = scheduleUseCase.getRooms();
            List<String> options = List.of(
                "Добавить кабинет",
                "Переименовать кабинет",
                "Удалить кабинет",
                "Закрыть"
            );
            ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
            dialog.setTitle("Кабинеты");
            dialog.setHeaderText(
                "Кабинеты: " +
                (rooms.isEmpty()
                        ? "нет"
                        : rooms.stream().map(RoomProfile::getName).collect(Collectors.joining(", ")))
            );
            dialog.setContentText("Действие:");
            UiStyles.apply(dialog.getDialogPane());
            String action = dialog.showAndWait().orElse("Закрыть");
            if ("Закрыть".equals(action)) {
                return;
            }
            try {
                if ("Добавить кабинет".equals(action)) {
                    TextInputDialog input = new TextInputDialog();
                    input.setTitle("Кабинеты");
                    input.setHeaderText("Добавить кабинет");
                    input.setContentText("Название:");
                    UiStyles.apply(input.getDialogPane());
                    String value = input.showAndWait().orElse("").trim();
                    if (!value.isEmpty()) {
                        scheduleUseCase.createRoom(value);
                    }
                } else if ("Переименовать кабинет".equals(action)) {
                    RoomProfile selected = selectRoom(rooms, "Переименовать кабинет");
                    if (selected == null) {
                        continue;
                    }
                    TextInputDialog input = new TextInputDialog(selected.getName());
                    input.setTitle("Кабинеты");
                    input.setHeaderText("Переименовать кабинет");
                    input.setContentText("Новое название:");
                    UiStyles.apply(input.getDialogPane());
                    String value = input.showAndWait().orElse("").trim();
                    if (!value.isEmpty()) {
                        scheduleUseCase.renameRoom(selected.getId(), value);
                    }
                } else if ("Удалить кабинет".equals(action)) {
                    RoomProfile selected = selectRoom(rooms, "Удалить кабинет");
                    if (selected == null) {
                        continue;
                    }
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Кабинеты");
                    confirm.setHeaderText("Удалить \"" + selected.getName() + "\"?");
                    confirm.setContentText("Фиксации кабинетов в правилах предметов стоит проверить вручную.");
                    UiStyles.apply(confirm.getDialogPane());
                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        scheduleUseCase.deleteRoom(selected.getId());
                    }
                }
            } catch (Exception e) {
                showError("Ошибка: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onInstructorDuties() {
        if (scheduleUseCase == null) {
            showError("Ошибка: Use case не инициализирован");
            return;
        }
        List<InstructorProfile> instructors = scheduleUseCase.getInstructors();
        InstructorProfile selectedInstructor = selectInstructor(instructors, "Наряды преподавателей");
        if (selectedInstructor == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Наряды");
        dialog.setHeaderText(selectedInstructor.getName());
        DatePicker datePicker = new DatePicker(LocalDate.now());
        ListView<String> list = new ListView<>();
        Runnable reloadList = () -> {
            List<String> dutyDates = scheduleUseCase
                .getInstructorDuties()
                .stream()
                .filter(duty -> selectedInstructor.getId().equals(duty.getInstructorId()))
                .map(duty -> duty.getDutyDate().toString())
                .sorted()
                .collect(Collectors.toList());
            list.setItems(FXCollections.observableArrayList(dutyDates));
        };
        reloadList.run();

        Button addButton = new Button("Добавить наряд");
        addButton.setOnAction(event -> {
            try {
                if (datePicker.getValue() == null) {
                    showError("Выберите дату наряда.");
                    return;
                }
                scheduleUseCase.addInstructorDuty(selectedInstructor.getId(), datePicker.getValue());
                reloadList.run();
            } catch (Exception e) {
                showError("Ошибка: " + e.getMessage());
            }
        });
        Button removeButton = new Button("Удалить наряд");
        removeButton.setOnAction(event -> {
            String selectedDate = list.getSelectionModel().getSelectedItem();
            if (selectedDate == null) {
                showError("Выберите наряд из списка.");
                return;
            }
            try {
                scheduleUseCase.removeInstructorDuty(
                    selectedInstructor.getId(),
                    LocalDate.parse(selectedDate)
                );
                reloadList.run();
            } catch (Exception e) {
                showError("Ошибка: " + e.getMessage());
            }
        });

        VBox content = new VBox(
            10,
            new Label("Дата заступления в 17:00:"),
            datePicker,
            new HBox(8, addButton, removeButton),
            new Label("Текущие наряды:"),
            list
        );
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        UiStyles.apply(dialog.getDialogPane());
        dialog.showAndWait();
    }

    @FXML
    private void onHistory() {
        if (scheduleUseCase == null) {
            showError("Ошибка: Use case не инициализирован");
            return;
        }
        List<ScheduleUseCase.HistoryEntry> entries =
            scheduleUseCase.getHistoryEntries();
        if (entries.isEmpty()) {
            showInfo("История распределений пуста.");
            return;
        }

        ChoiceDialog<ScheduleUseCase.HistoryEntry> dialog = new ChoiceDialog<>(
            entries.get(0),
            entries
        );
        dialog.setTitle("История распределений");
        dialog.setHeaderText("Выберите снимок для восстановления");
        dialog.setContentText("Снимок:");
        UiStyles.apply(dialog.getDialogPane());

        ScheduleUseCase.HistoryEntry selected = dialog
            .showAndWait()
            .orElse(null);
        if (selected == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Восстановить выбранный снимок?");
        confirm.setContentText(
            "Текущие занятия и пул нераспределённых будут заменены."
        );
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        boolean restored = scheduleUseCase.restoreFromHistory(selected.getId());
        if (!restored) {
            showError("Не удалось восстановить выбранный снимок.");
            return;
        }
        reload();
        showInfo("Снимок успешно восстановлен.");
    }

    @FXML
    private void onLoadArchiveToPool() {
        if (scheduleUseCase == null) {
            showError("Ошибка: Use case не инициализирован");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Архив");
        confirm.setHeaderText("Загрузить архивные занятия в пул?");
        confirm.setContentText(
            "Архивные занятия будут перенесены в авторасписание для нового периода."
        );
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        int moved = scheduleUseCase.moveArchivedLessonsToPool();
        reload();
        if (moved <= 0) {
            showInfo("Архив пуст.");
            return;
        }
        showInfo("Перенесено из архива: " + moved + " занятий.");
    }

    @FXML
    private void onAutoSchedule() {
        if (scheduleUseCase == null) {
            showError("Ошибка: Use case не инициализирован");
            return;
        }
        if (startDatePicker.getValue() == null) {
            showError("Укажите дату начала");
            return;
        }
        if (!validateDateRange()) {
            return;
        }

        saveSettings();

        try {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            AutoScheduleResult result;
            saveHistorySnapshot("Перед распределением");

            int autoLessonsInRange = scheduleUseCase.countAutoScheduledLessonsForReschedule(
                startDate,
                endDate
            );
            boolean canReschedule = autoLessonsInRange > 0;
            if (canReschedule && confirmReschedule(autoLessonsInRange)) {
                result = scheduleUseCase.reschedule(startDate, endDate);
            } else if (canReschedule) {
                return;
            } else {
                result = scheduleUseCase.autoSchedule(startDate, endDate);
            }

            reload();

            if (result.getCreatedLessons() == 0) {
                showInfo("Нет часов для распределения.");
                return;
            }

            StringBuilder message = new StringBuilder();
            message
                .append("Создано занятий: ")
                .append(result.getCreatedLessons());
            if (result.getLastScheduledDate() != null) {
                message
                    .append("\nПоследняя дата: ")
                    .append(result.getLastScheduledDate());
            }
            if (endDatePicker.getValue() != null) {
                message
                    .append("\nОграничение по дате: до ")
                    .append(endDatePicker.getValue());
            }
            appendRemainingHours(message, result);

            showInfo(message.toString());
        } catch (Exception e) {
            showError("Ошибка при распределении: " + e.getMessage());
        }
    }

    @FXML
    private void onReschedule() {
        if (scheduleUseCase == null) {
            showError("Ошибка: Use case не инициализирован");
            return;
        }
        if (startDatePicker.getValue() == null) {
            showError("Укажите дату начала");
            return;
        }
        if (!validateDateRange()) {
            return;
        }

        saveSettings();

        try {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            saveHistorySnapshot("Перед перераспределением");
            int autoLessonsInRange =
                scheduleUseCase.countAutoScheduledLessonsForReschedule(
                    startDate,
                    endDate
                );
            if (autoLessonsInRange <= 0) {
                showInfo(
                    "В выбранном диапазоне нет авто-распределённых занятий для переформирования."
                );
                return;
            }
            if (!confirmReschedule(autoLessonsInRange)) {
                return;
            }

            AutoScheduleResult result = scheduleUseCase.reschedule(
                startDate,
                endDate
            );
            reload();

            StringBuilder message = new StringBuilder();
            message
                .append("Перераспределено занятий: ")
                .append(result.getCreatedLessons());
            if (result.getLastScheduledDate() != null) {
                message
                    .append("\nПоследняя дата: ")
                    .append(result.getLastScheduledDate());
            }
            appendRemainingHours(message, result);
            showInfo(message.toString());
        } catch (Exception e) {
            showError("Ошибка при перераспределении: " + e.getMessage());
        }
    }

    private void setupSpinner(Spinner<Integer> spinner) {
        SpinnerValueFactory<Integer> factory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 8, 0);
        spinner.setValueFactory(factory);
        spinner.setEditable(true);
    }

    private void saveHistorySnapshot(String label) {
        if (scheduleUseCase != null) {
            scheduleUseCase.createHistorySnapshot(label);
        }
    }

    private InstructorProfile selectInstructor(
        List<InstructorProfile> instructors,
        String title
    ) {
        if (instructors == null || instructors.isEmpty()) {
            showInfo("Список преподавателей пуст.");
            return null;
        }
        Map<String, InstructorProfile> byLabel = new LinkedHashMap<>();
        for (InstructorProfile instructor : instructors) {
            byLabel.put(instructor.getName(), instructor);
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(
            byLabel.keySet().iterator().next(),
            byLabel.keySet()
        );
        dialog.setTitle("Преподаватели");
        dialog.setHeaderText(title);
        dialog.setContentText("Преподаватель:");
        UiStyles.apply(dialog.getDialogPane());
        String selected = dialog.showAndWait().orElse(null);
        return selected == null ? null : byLabel.get(selected);
    }

    private RoomProfile selectRoom(List<RoomProfile> rooms, String title) {
        if (rooms == null || rooms.isEmpty()) {
            showInfo("Список кабинетов пуст.");
            return null;
        }
        Map<String, RoomProfile> byLabel = new LinkedHashMap<>();
        for (RoomProfile room : rooms) {
            byLabel.put(room.getName(), room);
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(
            byLabel.keySet().iterator().next(),
            byLabel.keySet()
        );
        dialog.setTitle("Кабинеты");
        dialog.setHeaderText(title);
        dialog.setContentText("Кабинет:");
        UiStyles.apply(dialog.getDialogPane());
        String selected = dialog.showAndWait().orElse(null);
        return selected == null ? null : byLabel.get(selected);
    }

    private String normalizeSubjectKey(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private int resolveConsecutiveHoursForImportedSubject(
        String topic,
        Map<String, Integer> subjectConsecutiveByRule
    ) {
        String subjectKey = normalizeSubjectKey(topic);
        if (subjectConsecutiveByRule != null) {
            Integer fromRules = subjectConsecutiveByRule.get(subjectKey);
            if (fromRules != null) {
                return Math.max(1, fromRules);
            }
        }
        return detectSubjectConsecutiveHours(topic);
    }

    private int detectSubjectConsecutiveHours(String subject) {
        String key = normalizeSubjectKey(subject);
        int result = 1;
        for (ScheduleItem item : data) {
            if (!key.equals(normalizeSubjectKey(item.getTopic()))) {
                continue;
            }
            result = Math.max(result, Math.max(1, item.getConsecutiveHours()));
        }
        return result;
    }

    private String dayToShort(DayOfWeek day) {
        switch (day) {
            case MONDAY:
                return "ПН";
            case TUESDAY:
                return "ВТ";
            case WEDNESDAY:
                return "СР";
            case THURSDAY:
                return "ЧТ";
            case FRIDAY:
                return "ПТ";
            case SATURDAY:
                return "СБ";
            case SUNDAY:
                return "ВС";
            default:
                return day.toString();
        }
    }

    private static final class SubjectRuleEditModel {
        private final String subject;
        private final EnumSet<DayOfWeek> allowedDays;
        private final EnumSet<DayOfWeek> exclusiveDays;
        private int consecutiveHours;
        private String fixedRoom;

        private SubjectRuleEditModel(
            String subject,
            Set<DayOfWeek> allowedDays,
            Set<DayOfWeek> exclusiveDays,
            int consecutiveHours,
            String fixedRoom
        ) {
            this.subject = subject;
            this.allowedDays = allowedDays == null || allowedDays.isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class)
                : EnumSet.copyOf(allowedDays);
            this.exclusiveDays = exclusiveDays == null || exclusiveDays.isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class)
                : EnumSet.copyOf(exclusiveDays);
            this.consecutiveHours = Math.max(1, consecutiveHours);
            this.fixedRoom = fixedRoom == null ? "" : fixedRoom.trim();
        }

        private boolean isDefaultRule() {
            return (
                allowedDays.size() == DayOfWeek.values().length &&
                exclusiveDays.isEmpty() &&
                consecutiveHours == 1 &&
                fixedRoom.isBlank()
            );
        }
    }

    private void reload() {
        if (scheduleUseCase == null) {
            return;
        }
        List<ScheduleItem> items = scheduleUseCase.getAllItems();
        data.setAll(items);
        applySearchFilter(searchField.getText());
        updateTotalLabel();
    }

    private void loadSettings() {
        if (scheduleUseCase == null) {
            return;
        }
        Map<DayOfWeek, Integer> settings = scheduleUseCase.getMaxHoursByDay();
        mondayHours
            .getValueFactory()
            .setValue(settings.getOrDefault(DayOfWeek.MONDAY, 0));
        tuesdayHours
            .getValueFactory()
            .setValue(settings.getOrDefault(DayOfWeek.TUESDAY, 0));
        wednesdayHours
            .getValueFactory()
            .setValue(settings.getOrDefault(DayOfWeek.WEDNESDAY, 0));
        thursdayHours
            .getValueFactory()
            .setValue(settings.getOrDefault(DayOfWeek.THURSDAY, 0));
        fridayHours
            .getValueFactory()
            .setValue(settings.getOrDefault(DayOfWeek.FRIDAY, 0));
        saturdayHours
            .getValueFactory()
            .setValue(settings.getOrDefault(DayOfWeek.SATURDAY, 0));
        sundayHours
            .getValueFactory()
            .setValue(settings.getOrDefault(DayOfWeek.SUNDAY, 0));
    }

    private void saveSettings() {
        Map<DayOfWeek, Integer> maxHours = new EnumMap<>(DayOfWeek.class);
        maxHours.put(DayOfWeek.MONDAY, mondayHours.getValue());
        maxHours.put(DayOfWeek.TUESDAY, tuesdayHours.getValue());
        maxHours.put(DayOfWeek.WEDNESDAY, wednesdayHours.getValue());
        maxHours.put(DayOfWeek.THURSDAY, thursdayHours.getValue());
        maxHours.put(DayOfWeek.FRIDAY, fridayHours.getValue());
        maxHours.put(DayOfWeek.SATURDAY, saturdayHours.getValue());
        maxHours.put(DayOfWeek.SUNDAY, sundayHours.getValue());
        scheduleUseCase.saveMaxHoursByDay(maxHours);
    }

    private void showItemDialog(ScheduleItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                    "/com/ororura/slseleven/schedule-item-dialog.fxml"
                )
            );
            GridPane dialogContent = loader.load();
            ScheduleItemDialogController controller = loader.getController();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(
                item == null ? "Добавить часы" : "Редактировать часы"
            );
            dialog.getDialogPane().setContent(dialogContent);
            dialog
                .getDialogPane()
                .getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);
            UiStyles.apply(dialog.getDialogPane());

            controller.setItem(item, scheduleUseCase, dialog);

            dialog
                .showAndWait()
                .ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        reload();
                    }
                });
        } catch (Exception e) {
            showError("Не удалось открыть диалог: " + e.getMessage());
        }
    }

    private void showError(String message) {
        UiAlerts.showError("Ошибка", message);
    }

    private void showInfo(String message) {
        UiAlerts.showInfo("Сообщение", message);
    }

    private ImportResult importItemsFromText(String text) {
        List<String> errors = new ArrayList<>();
        List<List<String>> rows = DelimitedText.parse(text, errors);
        return importItemsFromRows(rows, errors);
    }

    private ImportResult importItemsFromRows(
        List<List<String>> rows,
        List<String> errors
    ) {
        if (rows.isEmpty()) {
            return new ImportResult(0, errors);
        }

        HeaderMapping mapping = HeaderMapping.forItems(rows);
        if (mapping == null) {
            errors.add("Не удалось определить заголовки или порядок колонок.");
            return new ImportResult(0, errors);
        }

        int added = 0;
        Map<String, Integer> subjectConsecutiveByRule = new HashMap<>();
        if (scheduleUseCase != null) {
            for (SubjectScheduleRule rule : scheduleUseCase.getSubjectRules()) {
                if (rule == null || isBlank(rule.getSubject())) {
                    continue;
                }
                subjectConsecutiveByRule.put(
                    normalizeSubjectKey(rule.getSubject()),
                    Math.max(1, rule.getConsecutiveHours())
                );
            }
        }
        for (int i = mapping.startRowIndex; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            int rowNumber = i + 1;
            try {
                String topic = mapping.get(row, "topic");
                String lessonName = mapping.get(row, "lesson");
                String className = mapping.get(row, "class");
                String location = mapping.get(row, "location");
                String instructor = mapping.get(row, "instructor");
                String hoursRaw = mapping.get(row, "hours");
                if (isBlank(className)) {
                    className = lessonName;
                }

                if (
                    isBlank(topic) ||
                    isBlank(lessonName) ||
                    isBlank(hoursRaw)
                ) {
                    errors.add(
                        "Строка " + rowNumber + ": пропущены обязательные поля."
                    );
                    continue;
                }

                int hours;
                try {
                    hours = Integer.parseInt(hoursRaw.trim());
                } catch (NumberFormatException ex) {
                    errors.add(
                        "Строка " + rowNumber + ": неверный формат часов."
                    );
                    continue;
                }
                ScheduleItem item = new ScheduleItem(
                    topic,
                    lessonName,
                    className,
                    location,
                    instructor,
                    hours
                );
                item.setConsecutiveHours(
                    resolveConsecutiveHoursForImportedSubject(
                        topic,
                        subjectConsecutiveByRule
                    )
                );
                scheduleUseCase.createItem(item);
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

    private void showImportAndScheduleResult(
        ImportResult importResult,
        AutoScheduleResult scheduleResult
    ) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Импорт и распределение");
        alert.setHeaderText(null);
        StringJoiner joiner = new StringJoiner(System.lineSeparator());
        joiner.add("Импортировано строк: " + importResult.added);
        joiner.add("Создано занятий: " + scheduleResult.getCreatedLessons());
        if (scheduleResult.getLastScheduledDate() != null) {
            joiner.add(
                "Последняя дата: " + scheduleResult.getLastScheduledDate()
            );
        }
        if (endDatePicker.getValue() != null) {
            joiner.add("Ограничение по дате: до " + endDatePicker.getValue());
        }
        appendRemainingHours(joiner, scheduleResult);
        if (!importResult.errors.isEmpty()) {
            int limit = Math.min(4, importResult.errors.size());
            for (int i = 0; i < limit; i++) {
                joiner.add(importResult.errors.get(i));
            }
            if (importResult.errors.size() > limit) {
                joiner.add(
                    "Ошибок ещё: " + (importResult.errors.size() - limit)
                );
            }
        }
        alert.setContentText(joiner.toString());
        alert.showAndWait();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private void applySearchFilter(String query) {
        if (filteredData == null) {
            return;
        }
        String normalized = query == null
            ? ""
            : query.trim().toLowerCase();
        if (normalized.isEmpty()) {
            filteredData.setPredicate(item -> true);
            return;
        }
        filteredData.setPredicate(item ->
            safeValue(item.getTopic()).toLowerCase().contains(normalized) ||
            safeValue(item.getLessonName()).toLowerCase().contains(normalized) ||
            safeValue(item.getClassName()).toLowerCase().contains(normalized) ||
            safeValue(item.getLocation()).toLowerCase().contains(normalized) ||
            safeValue(item.getInstructor()).toLowerCase().contains(normalized)
        );
    }

    private void updateTotalLabel() {
        int total = data.stream().mapToInt(ScheduleItem::getHours).sum();
        int visible = filteredData == null
            ? data.size()
            : filteredData.stream().mapToInt(ScheduleItem::getHours).sum();
        if (searchField == null || searchField.getText().isBlank()) {
            totalHoursLabel.setText("Всего часов: " + total);
            return;
        }
        totalHoursLabel.setText(
            "Показано часов: " + visible + " из " + total
        );
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
            showError("Не удалось прочитать файл: " + ex.getMessage());
        }
    }

    private String buildTsvExport(List<ScheduleItem> items) {
        StringJoiner joiner = new StringJoiner(System.lineSeparator());
        joiner.add(
            "Предмет\tТема\tЗанятие\tМесто\tПреподаватель\tЧасы\tПодряд (предмет)"
        );
        for (ScheduleItem item : items) {
            joiner.add(
                safe(item.getTopic()) +
                    "\t" +
                    safe(item.getLessonName()) +
                    "\t" +
                    safe(item.getClassName()) +
                    "\t" +
                    safe(item.getLocation()) +
                    "\t" +
                    safe(item.getInstructor()) +
                    "\t" +
                    item.getHours() +
                    "\t" +
                    item.getConsecutiveHours()
            );
        }
        return joiner.toString();
    }

    private String safe(String value) {
        return value == null
            ? ""
            : value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
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

    private boolean confirmReschedule(int lessonsInRange) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Переформировать расписание");
        alert.setHeaderText("Авторасписание уже распределено");
        alert.setContentText(
            "В выбранном диапазоне авто-занятий: " +
            lessonsInRange +
            ".\nПереформировать расписание?\n" +
            "Авто-занятия будут возвращены в пул и распределены заново."
        );

        ButtonType reformButton = new ButtonType(
            "Переформировать",
            ButtonBar.ButtonData.OK_DONE
        );
        ButtonType cancelButton = new ButtonType(
            "Отмена",
            ButtonBar.ButtonData.CANCEL_CLOSE
        );
        alert.getButtonTypes().setAll(reformButton, cancelButton);
        return alert.showAndWait().orElse(cancelButton) == reformButton;
    }

    private boolean validateDateRange() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            showError("Дата окончания не может быть раньше даты начала.");
            return false;
        }
        return true;
    }

    private void appendRemainingHours(
        StringBuilder builder,
        AutoScheduleResult result
    ) {
        if (result.getRemainingHours() <= 0) {
            return;
        }

        builder
            .append("\nНераспределённые часы: ")
            .append(result.getRemainingHours());
        int limit = Math.min(5, result.getRemainingItems().size());
        for (int i = 0; i < limit; i++) {
            AutoScheduleResult.RemainingScheduleItem item = result
                .getRemainingItems()
                .get(i);
            builder
                .append("\n• ")
                .append(item.getTopic())
                .append(" / ")
                .append(item.getLessonName())
                .append(" / ")
                .append(item.getClassName())
                .append(": ")
                .append(item.getHours());
        }
        if (result.getRemainingItems().size() > limit) {
            builder
                .append("\nЕщё позиций: ")
                .append(result.getRemainingItems().size() - limit);
        }
    }

    private void appendRemainingHours(
        StringJoiner joiner,
        AutoScheduleResult result
    ) {
        if (result.getRemainingHours() <= 0) {
            return;
        }

        joiner.add("Нераспределённые часы: " + result.getRemainingHours());
        int limit = Math.min(5, result.getRemainingItems().size());
        for (int i = 0; i < limit; i++) {
            AutoScheduleResult.RemainingScheduleItem item = result
                .getRemainingItems()
                .get(i);
            joiner.add(
                "• " +
                item.getTopic() +
                " / " +
                item.getLessonName() +
                " / " +
                item.getClassName() +
                ": " +
                item.getHours()
            );
        }
        if (result.getRemainingItems().size() > limit) {
            joiner.add(
                "Ещё позиций: " + (result.getRemainingItems().size() - limit)
            );
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

        private static HeaderMapping forItems(List<List<String>> rows) {
            if (rows.isEmpty()) {
                return null;
            }

            Map<String, String> aliases = new HashMap<>();
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
            aliases.put("часы", "hours");
            aliases.put("hours", "hours");
            aliases.put("hour", "hours");

            List<String> required = List.of(
                "topic",
                "lesson",
                "hours"
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
            int offset = row.size() == keys.size() + 1 && row.get(0).isBlank()
                ? 1
                : 0;
            if (row.size() - offset >= 6) {
                defaultMap.put("topic", 0 + offset);
                defaultMap.put("lesson", 1 + offset);
                defaultMap.put("class", 2 + offset);
                defaultMap.put("location", 3 + offset);
                defaultMap.put("instructor", 4 + offset);
                defaultMap.put("hours", 5 + offset);
                return defaultMap;
            }
            if (row.size() - offset == 5) {
                defaultMap.put("topic", 0 + offset);
                defaultMap.put("lesson", 1 + offset);
                defaultMap.put("class", 2 + offset);
                defaultMap.put("location", 3 + offset);
                defaultMap.put("instructor", -1);
                defaultMap.put("hours", 4 + offset);
                return defaultMap;
            }
            if (row.size() - offset == 4) {
                defaultMap.put("topic", 0 + offset);
                defaultMap.put("lesson", 1 + offset);
                defaultMap.put("class", 2 + offset);
                defaultMap.put("location", -1);
                defaultMap.put("instructor", -1);
                defaultMap.put("hours", 3 + offset);
                return defaultMap;
            }
            defaultMap.put("topic", 0 + offset);
            defaultMap.put("lesson", 1 + offset);
            defaultMap.put("class", -1);
            defaultMap.put("location", -1);
            defaultMap.put("instructor", -1);
            defaultMap.put("hours", 2 + offset);
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
