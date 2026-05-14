package com.ororura.slseleven;

import com.ororura.slseleven.adapters.controller.CalendarController;
import com.ororura.slseleven.adapters.ui.UiStyles;
import com.ororura.slseleven.infrastructure.bootstrap.ApplicationBootstrap;
import com.ororura.slseleven.infrastructure.bootstrap.ApplicationServices;
import java.io.IOException;
import java.nio.file.Path;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClassCalendarApplication extends Application {

    /**
     * Метод start.
     */
    @Override
    public void start(Stage stage) throws IOException {
        ApplicationServices services = new ApplicationBootstrap().bootstrap(
            Path.of(System.getProperty("user.home"))
        );

        // 3) Инициализация UI.
        FXMLLoader fxmlLoader = new FXMLLoader(
            ClassCalendarApplication.class.getResource("calendar-view.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        UiStyles.apply(scene);

        // 4) Внедрение use-case в главный контроллер.
        CalendarController controller = fxmlLoader.getController();
        controller.setLessonUseCase(services.getLessonUseCase());
        controller.setScheduleUseCase(services.getScheduleUseCase());

        stage.setTitle("Календарь занятий");
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }
}
