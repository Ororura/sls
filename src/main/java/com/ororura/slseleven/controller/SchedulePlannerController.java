package com.ororura.slseleven.controller;

import com.ororura.slseleven.domain.model.ScheduleItem;
import com.ororura.slseleven.ui.UiStyles;
import com.ororura.slseleven.usecase.AutoScheduleResult;
import com.ororura.slseleven.usecase.ScheduleUseCase;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

public class SchedulePlannerController {

    @FXML
    private TableView<ScheduleItem> scheduleTable;

    @FXML
    private TableColumn<ScheduleItem, String> topicColumn;

    @FXML
    private TableColumn<ScheduleItem, String> lessonNameColumn;

    @FXML
    private TableColumn<ScheduleItem, String> locationColumn;

    @FXML
    private TableColumn<ScheduleItem, String> instructorColumn;

    @FXML
    private TableColumn<ScheduleItem, Integer> hoursColumn;

    @FXML
    private Label totalHoursLabel;

    @FXML
    private DatePicker startDatePicker;

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

    private final ObservableList<ScheduleItem> data =
        FXCollections.observableArrayList();

    private ScheduleUseCase scheduleUseCase;

    @FXML
    public void initialize() {
        scheduleTable.setItems(data);
        topicColumn.setCellValueFactory(new PropertyValueFactory<>("topic"));
        lessonNameColumn.setCellValueFactory(
            new PropertyValueFactory<>("lessonName")
        );
        locationColumn.setCellValueFactory(
            new PropertyValueFactory<>("location")
        );
        instructorColumn.setCellValueFactory(
            new PropertyValueFactory<>("instructor")
        );
        hoursColumn.setCellValueFactory(new PropertyValueFactory<>("hours"));

        setupSpinner(mondayHours);
        setupSpinner(tuesdayHours);
        setupSpinner(wednesdayHours);
        setupSpinner(thursdayHours);
        setupSpinner(fridayHours);
        setupSpinner(saturdayHours);
        setupSpinner(sundayHours);

        startDatePicker.setValue(LocalDate.now());
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
        ScheduleItem selected = scheduleTable
            .getSelectionModel()
            .getSelectedItem();
        if (selected == null) {
            showInfo("Выберите строку для удаления.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение");
        alert.setHeaderText("Удалить запись?");
        alert.setContentText(
            "Вы уверены, что хотите удалить выбранный элемент?"
        );
        alert
            .showAndWait()
            .ifPresent(response -> {
                if (response == ButtonType.OK) {
                    scheduleUseCase.deleteItem(selected.getId());
                    reload();
                }
            });
    }

    @FXML
    private void onSaveSettings() {
        saveSettings();
        showInfo("Настройки сохранены.");
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

        saveSettings();

        try {
            AutoScheduleResult result = scheduleUseCase.autoSchedule(
                startDatePicker.getValue()
            );
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

            showInfo(message.toString());
        } catch (Exception e) {
            showError("Ошибка при распределении: " + e.getMessage());
        }
    }

    private void setupSpinner(Spinner<Integer> spinner) {
        SpinnerValueFactory<Integer> factory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 24, 0);
        spinner.setValueFactory(factory);
        spinner.setEditable(true);
    }

    private void reload() {
        if (scheduleUseCase == null) {
            return;
        }
        List<ScheduleItem> items = scheduleUseCase.getAllItems();
        data.setAll(items);
        int total = items.stream().mapToInt(ScheduleItem::getHours).sum();
        totalHoursLabel.setText("Всего часов: " + total);
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
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Сообщение");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
