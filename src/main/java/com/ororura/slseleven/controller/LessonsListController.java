package com.ororura.slseleven.controller;

import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.usecase.LessonUseCase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        lessonTable.setItems(data);

        // ⚠️ ВАЖНО: имена — это ИМЕНА GETTER'ОВ
        topicColumn.setCellValueFactory(
                new PropertyValueFactory<>("topic"));
        lessonNameColumn.setCellValueFactory(
                new PropertyValueFactory<>("lessonName"));
        locationColumn.setCellValueFactory(
                new PropertyValueFactory<>("location"));
        instructorColumn.setCellValueFactory(
                new PropertyValueFactory<>("instructor"));
        dateColumn.setCellValueFactory(
                new PropertyValueFactory<>("date"));
        timeColumn.setCellValueFactory(
                new PropertyValueFactory<>("time"));

        // Форматирование даты
        dateColumn.setCellFactory(col -> new TableCell<Lesson, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : dateFormatter.format(item));
            }
        });

        // Форматирование времени
        timeColumn.setCellFactory(col -> new TableCell<Lesson, LocalTime>() {
            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : timeFormatter.format(item));
            }
        });
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
    private void onDelete() {
        Lesson selected = lessonTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        lessonUseCase.deleteLesson(selected.getId());
        reload();
    }
}
