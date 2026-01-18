package com.ororura.slseleven;

import com.ororura.slseleven.controller.CalendarController;
import com.ororura.slseleven.domain.repository.LessonRepository;
import com.ororura.slseleven.infrastructure.persistence.migrations.SchemaInitializer;
import com.ororura.slseleven.infrastructure.persistence.sqlite.LessonRepositorySQLite;
import com.ororura.slseleven.infrastructure.persistence.sqlite.SQLiteConnectionProvider;
import com.ororura.slseleven.usecase.LessonUseCase;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Главный класс приложения
 * Инициализирует зависимости и запускает JavaFX приложение
 */
public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Инициализация базы данных
        Path dbDirectory = Path.of(System.getProperty("user.home"), ".slseleven");
        if (!Files.exists(dbDirectory)) {
            Files.createDirectories(dbDirectory);
        }
        
        Path dbPath = dbDirectory.resolve("lessons.db");
        SQLiteConnectionProvider provider = new SQLiteConnectionProvider(dbPath);
        SchemaInitializer.init(provider);

        // Инициализация слоев архитектуры
        LessonRepository lessonRepository = new LessonRepositorySQLite(provider);
        LessonUseCase lessonUseCase = new LessonUseCase(lessonRepository);

        // Загрузка FXML и установка контроллера
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("calendar-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
        
        // Передача зависимостей в контроллер
        CalendarController controller = fxmlLoader.getController();
        controller.setLessonUseCase(lessonUseCase);
        
        stage.setTitle("Календарь занятий");
        stage.setScene(scene);
        stage.show();
    }
}
