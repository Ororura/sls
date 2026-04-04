package com.ororura.slseleven.infrastructure.bootstrap;

import com.ororura.slseleven.application.usecase.LessonUseCase;
import com.ororura.slseleven.application.usecase.ScheduleUseCase;
import com.ororura.slseleven.domain.repository.ActiveCalendarRepository;
import com.ororura.slseleven.domain.repository.CalendarRepository;
import com.ororura.slseleven.domain.repository.LessonRepository;
import com.ororura.slseleven.domain.repository.ScheduleCatalogRepository;
import com.ororura.slseleven.domain.repository.ScheduleHistoryRepository;
import com.ororura.slseleven.domain.repository.ScheduleItemRepository;
import com.ororura.slseleven.infrastructure.context.PersistentCalendarContext;
import com.ororura.slseleven.infrastructure.persistence.migrations.SchemaInitializer;
import com.ororura.slseleven.infrastructure.persistence.sqlite.ActiveCalendarRepositorySQLite;
import com.ororura.slseleven.infrastructure.persistence.sqlite.CalendarRepositorySQLite;
import com.ororura.slseleven.infrastructure.persistence.sqlite.LessonRepositorySQLite;
import com.ororura.slseleven.infrastructure.persistence.sqlite.SQLiteConnectionProvider;
import com.ororura.slseleven.infrastructure.persistence.sqlite.ScheduleCatalogRepositorySQLite;
import com.ororura.slseleven.infrastructure.persistence.sqlite.ScheduleHistoryRepositorySQLite;
import com.ororura.slseleven.infrastructure.persistence.sqlite.ScheduleItemRepositorySQLite;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ApplicationBootstrap {

    public ApplicationServices bootstrap(Path applicationHome) throws IOException {
        Path databaseDirectory = applicationHome.resolve(".slseleven");
        Files.createDirectories(databaseDirectory);

        SQLiteConnectionProvider connectionProvider = new SQLiteConnectionProvider(
            databaseDirectory.resolve("lessons.db")
        );
        SchemaInitializer.init(connectionProvider);

        LessonRepository lessonRepository = new LessonRepositorySQLite(connectionProvider);
        ScheduleItemRepository scheduleItemRepository =
            new ScheduleItemRepositorySQLite(connectionProvider);
        ScheduleHistoryRepository scheduleHistoryRepository =
            new ScheduleHistoryRepositorySQLite(connectionProvider);
        ScheduleCatalogRepository scheduleCatalogRepository =
            new ScheduleCatalogRepositorySQLite(connectionProvider);
        CalendarRepository calendarRepository =
            new CalendarRepositorySQLite(connectionProvider);
        ActiveCalendarRepository activeCalendarRepository =
            new ActiveCalendarRepositorySQLite(connectionProvider);

        PersistentCalendarContext calendarContext = new PersistentCalendarContext(
            activeCalendarRepository
        );

        LessonUseCase lessonUseCase = new LessonUseCase(
            lessonRepository,
            calendarContext
        );
        ScheduleUseCase scheduleUseCase = new ScheduleUseCase(
            lessonRepository,
            scheduleItemRepository,
            scheduleHistoryRepository,
            scheduleCatalogRepository,
            calendarRepository,
            calendarContext
        );

        return new ApplicationServices(lessonUseCase, scheduleUseCase);
    }
}
