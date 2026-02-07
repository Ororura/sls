package com.ororura.slseleven.infrastructure.persistence.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ororura.slseleven.domain.model.ScheduleItem;
import com.ororura.slseleven.infrastructure.persistence.migrations.SchemaInitializer;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScheduleItemRepositorySQLiteTest {
    private static final String DEFAULT_CALENDAR_ID = "default";

    @TempDir
    Path tempDir;

    private ScheduleItemRepositorySQLite repository;

    @BeforeEach
    void setUp() {
        SQLiteConnectionProvider provider = new SQLiteConnectionProvider(tempDir.resolve("test.db"));
        SchemaInitializer.init(provider);
        repository = new ScheduleItemRepositorySQLite(provider);
    }

    @Test
    void shouldSupportCrudAndBatchDelete() {
        ScheduleItem first = new ScheduleItem("Math", "L1", "A1", "Ivanov", 2);
        ScheduleItem second = new ScheduleItem("Physics", "L2", "B1", "Petrov", 3);
        ScheduleItem third = new ScheduleItem("Chem", "L3", "C1", "Sidorov", 1);

        repository.save(first);
        repository.save(second);
        repository.save(third);

        assertEquals(3, repository.findAll(DEFAULT_CALENDAR_ID).size());
        assertTrue(repository.findById(first.getId()).isPresent());

        second.setHours(5);
        repository.save(second);
        assertEquals(5, repository.findById(second.getId()).orElseThrow().getHours());

        repository.deleteAllByIds(List.of(first.getId(), second.getId()));
        assertFalse(repository.existsById(first.getId()));
        assertFalse(repository.existsById(second.getId()));
        assertTrue(repository.existsById(third.getId()));

        repository.deleteAll(DEFAULT_CALENDAR_ID);
        assertTrue(repository.findAll(DEFAULT_CALENDAR_ID).isEmpty());
    }
}
