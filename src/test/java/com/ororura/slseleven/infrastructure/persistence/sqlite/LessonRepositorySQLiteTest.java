package com.ororura.slseleven.infrastructure.persistence.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.infrastructure.persistence.migrations.SchemaInitializer;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LessonRepositorySQLiteTest {

    @TempDir
    Path tempDir;

    private LessonRepositorySQLite repository;

    @BeforeEach
    void setUp() {
        SQLiteConnectionProvider provider = new SQLiteConnectionProvider(tempDir.resolve("test.db"));
        SchemaInitializer.init(provider);
        repository = new LessonRepositorySQLite(provider);
    }

    @Test
    void shouldSupportCrudAndDateQueries() {
        LocalDate date = LocalDate.of(2026, 2, 3);
        Lesson lesson = new Lesson("Math", "L1", LocalTime.of(11, 0), "A1", "Ivanov", date);

        repository.save(lesson);

        assertTrue(repository.existsById(lesson.getId()));
        assertTrue(repository.findById(lesson.getId()).isPresent());
        assertEquals(1, repository.findByDate(date).size());

        repository.save(new Lesson("Math", "L2", LocalTime.of(9, 0), "A1", "Ivanov", date.plusDays(1)));
        repository.save(new Lesson("Math", "L0", LocalTime.of(9, 0), "A1", "Ivanov", date.minusDays(1)));

        List<Lesson> range = repository.findByDateRange(date, date.plusDays(1));
        assertEquals(2, range.size());
        assertEquals(date, range.get(0).getDate());
        assertEquals(date.plusDays(1), range.get(1).getDate());

        repository.deleteById(lesson.getId());
        assertFalse(repository.existsById(lesson.getId()));

        repository.deleteAll();
        assertTrue(repository.findAll().isEmpty());
    }
}
