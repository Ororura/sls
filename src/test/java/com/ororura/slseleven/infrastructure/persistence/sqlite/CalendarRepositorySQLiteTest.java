package com.ororura.slseleven.infrastructure.persistence.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ororura.slseleven.domain.model.AppCalendar;
import com.ororura.slseleven.infrastructure.persistence.migrations.SchemaInitializer;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CalendarRepositorySQLiteTest {

    @TempDir
    Path tempDir;

    private CalendarRepositorySQLite repository;

    @BeforeEach
    void setUp() {
        SQLiteConnectionProvider provider = new SQLiteConnectionProvider(
            tempDir.resolve("test.db")
        );
        SchemaInitializer.init(provider);
        repository = new CalendarRepositorySQLite(provider);
    }

    @Test
    void shouldCreateRenameMoveAndDeleteCalendar() {
        AppCalendar created = repository.createCalendar("Учебный", "2026/Весна");

        repository.renameCalendar(created.getId(), "Учебный-2");
        repository.moveCalendarToDirectory(created.getId(), "2026/Осень");

        List<AppCalendar> calendars = repository.findAllCalendars();
        AppCalendar updated = calendars
            .stream()
            .filter(calendar -> calendar.getId().equals(created.getId()))
            .findFirst()
            .orElseThrow();

        assertEquals("Учебный-2", updated.getName());
        assertEquals("2026/Осень", updated.getDirectoryPath());
        assertTrue(repository.getCalendarDirectories().contains("2026/Осень"));

        repository.deleteCalendar(created.getId());

        assertFalse(
            repository
                .findAllCalendars()
                .stream()
                .anyMatch(calendar -> calendar.getId().equals(created.getId()))
        );
    }
}
