package com.ororura.slseleven;

import com.ororura.slseleven.controller.CalendarController;
import com.ororura.slseleven.domain.repository.LessonRepository;
import com.ororura.slseleven.domain.repository.ScheduleHistoryRepository;
import com.ororura.slseleven.domain.repository.ScheduleItemRepository;
import com.ororura.slseleven.domain.repository.ScheduleSettingsRepository;
import com.ororura.slseleven.infrastructure.persistence.migrations.SchemaInitializer;
import com.ororura.slseleven.infrastructure.persistence.sqlite.LessonRepositorySQLite;
import com.ororura.slseleven.infrastructure.persistence.sqlite.SQLiteConnectionProvider;
import com.ororura.slseleven.infrastructure.persistence.sqlite.ScheduleHistoryRepositorySQLite;
import com.ororura.slseleven.infrastructure.persistence.sqlite.ScheduleItemRepositorySQLite;
import com.ororura.slseleven.infrastructure.persistence.sqlite.ScheduleSettingsRepositorySQLite;
import com.ororura.slseleven.ui.UiStyles;
import com.ororura.slseleven.usecase.LessonUseCase;
import com.ororura.slseleven.usecase.ScheduleUseCase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Инициализация базы данных
        Path dbDirectory = Path.of(
            System.getProperty("user.home"),
            ".slseleven"
        );
        if (!Files.exists(dbDirectory)) {
            Files.createDirectories(dbDirectory);
        }

        Path dbPath = dbDirectory.resolve("lessons.db");
        SQLiteConnectionProvider provider = new SQLiteConnectionProvider(
            dbPath
        );
        SchemaInitializer.init(provider);
        // Инициализация слоев архитектуры
        LessonRepository lessonRepository = new LessonRepositorySQLite(
            provider
        );
        LessonUseCase lessonUseCase = new LessonUseCase(lessonRepository);
        ScheduleItemRepository scheduleItemRepository =
            new ScheduleItemRepositorySQLite(provider);
        ScheduleSettingsRepository scheduleSettingsRepository =
            new ScheduleSettingsRepositorySQLite(provider);
        ScheduleHistoryRepository scheduleHistoryRepository =
            new ScheduleHistoryRepositorySQLite(provider);
        ScheduleUseCase scheduleUseCase = new ScheduleUseCase(
            lessonRepository,
            scheduleItemRepository,
            scheduleSettingsRepository,
            scheduleHistoryRepository
        );

        // Загрузка FXML и установка контроллера
        FXMLLoader fxmlLoader = new FXMLLoader(
            HelloApplication.class.getResource("calendar-view.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
        UiStyles.apply(scene);

        // Передача зависимостей в контроллер
        CalendarController controller = fxmlLoader.getController();
        controller.setLessonUseCase(lessonUseCase);
        controller.setScheduleUseCase(scheduleUseCase);

        stage.setTitle("Календарь занятий");
        stage.setScene(scene);
        stage.show();
    }
}
