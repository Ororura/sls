package com.ororura.slseleven.controller;

import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.ui.UiFormatters;
import com.ororura.slseleven.ui.UiStyles;
import com.ororura.slseleven.usecase.LessonUseCase;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

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
    private TableColumn<Lesson, String> locationColumn;

    @FXML
    private TableColumn<Lesson, String> instructorColumn;

    @FXML
    private Label countLabel;

    private LessonUseCase lessonUseCase;

    private final ObservableList<Lesson> data =
        FXCollections.observableArrayList();

    private final DateTimeFormatter dateFormatter = UiFormatters.DATE_FORMATTER;
    private final DateTimeFormatter timeFormatter = UiFormatters.TIME_FORMATTER;

    @FXML
    public void initialize() {
        lessonTable.setItems(data);
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
    }

    public void setLessonUseCase(LessonUseCase lessonUseCase) {
        this.lessonUseCase = lessonUseCase;
        reload();
    }

    private void reload() {
        List<Lesson> lessons = lessonUseCase.getAllLessons();
        data.setAll(lessons);
        countLabel.setText("Всего занятий: " + data.size());
    }

    @FXML
    private void onRefresh() {
        reload();
    }

    @FXML
    private void onSave() {
        showLessonDialog(null, LocalDate.now());
    }

    @FXML
    private void onDelete() {
        Lesson selected = lessonTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Удаление");
            alert.setHeaderText(null);
            alert.setContentText("Выберите занятие для удаления.");
            alert.showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение");
        alert.setHeaderText("Удалить занятие?");
        alert.setContentText(
            "Вы уверены, что хотите удалить выбранное занятие?"
        );
        alert
            .showAndWait()
            .ifPresent(response -> {
                if (response == ButtonType.OK) {
                    lessonUseCase.deleteLesson(selected.getId());
                    reload();
                }
            });
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
}
