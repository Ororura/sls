package com.ororura.slseleven.infrastructure.persistence.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ororura.slseleven.infrastructure.persistence.migrations.SchemaInitializer;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ActiveCalendarRepositorySQLiteTest {

    @TempDir
    Path tempDir;

    private ActiveCalendarRepositorySQLite repository;
    private CalendarRepositorySQLite calendarRepository;

    @BeforeEach
    void setUp() {
        SQLiteConnectionProvider provider = new SQLiteConnectionProvider(
            tempDir.resolve("test.db")
        );
        SchemaInitializer.init(provider);
        repository = new ActiveCalendarRepositorySQLite(provider);
        calendarRepository = new CalendarRepositorySQLite(provider);
    }

    @Test
    void shouldPersistSelectedActiveCalendar() {
        String createdCalendarId = calendarRepository
            .createCalendar("Второй")
            .getId();

        repository.setActiveCalendarId(createdCalendarId);

        assertEquals(createdCalendarId, repository.getActiveCalendarId());
    }
}
