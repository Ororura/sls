package com.ororura.slseleven.infrastructure.persistence.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ororura.slseleven.infrastructure.persistence.migrations.SchemaInitializer;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScheduleSettingsRepositorySQLiteTest {

    @TempDir
    Path tempDir;

    private ScheduleSettingsRepositorySQLite repository;

    @BeforeEach
    void setUp() {
        SQLiteConnectionProvider provider = new SQLiteConnectionProvider(tempDir.resolve("test.db"));
        SchemaInitializer.init(provider);
        repository = new ScheduleSettingsRepositorySQLite(provider);
    }

    @Test
    void shouldReturnDefaultsAndSaveUpdates() {
        Map<DayOfWeek, Integer> initial = repository.getMaxHoursByDay();
        assertEquals(2, initial.get(DayOfWeek.MONDAY));
        assertEquals(2, initial.get(DayOfWeek.FRIDAY));
        assertEquals(0, initial.get(DayOfWeek.SATURDAY));

        Map<DayOfWeek, Integer> custom = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            custom.put(day, 1);
        }
        custom.put(DayOfWeek.SUNDAY, 4);

        repository.saveAll(custom);
        Map<DayOfWeek, Integer> reloaded = repository.getMaxHoursByDay();

        assertEquals(1, reloaded.get(DayOfWeek.MONDAY));
        assertEquals(4, reloaded.get(DayOfWeek.SUNDAY));
    }
}
