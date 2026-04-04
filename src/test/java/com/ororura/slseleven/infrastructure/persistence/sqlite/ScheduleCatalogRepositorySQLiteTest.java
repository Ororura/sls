package com.ororura.slseleven.infrastructure.persistence.sqlite;

import com.ororura.slseleven.domain.repository.CalendarRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ororura.slseleven.domain.model.SubjectScheduleRule;
import com.ororura.slseleven.infrastructure.persistence.migrations.SchemaInitializer;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScheduleCatalogRepositorySQLiteTest {

    @TempDir
    Path tempDir;

    private ScheduleCatalogRepositorySQLite repository;
    private String defaultCalendarId;

    @BeforeEach
    void setUp() {
        SQLiteConnectionProvider provider = new SQLiteConnectionProvider(tempDir.resolve("test.db"));
        SchemaInitializer.init(provider);
        repository = new ScheduleCatalogRepositorySQLite(provider);
        CalendarRepository calendarRepository = new CalendarRepositorySQLite(
            provider
        );
        defaultCalendarId = calendarRepository
            .findAllCalendars()
            .get(0)
            .getId();
    }

    @Test
    void shouldReturnDefaultsAndSaveUpdates() {
        Map<DayOfWeek, Integer> initial = repository.getMaxHoursByDay(defaultCalendarId);
        assertEquals(2, initial.get(DayOfWeek.MONDAY));
        assertEquals(2, initial.get(DayOfWeek.FRIDAY));
        assertEquals(0, initial.get(DayOfWeek.SATURDAY));

        Map<DayOfWeek, Integer> custom = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            custom.put(day, 1);
        }
        custom.put(DayOfWeek.SUNDAY, 4);

        repository.saveMaxHoursByDay(defaultCalendarId, custom);
        Map<DayOfWeek, Integer> reloaded = repository.getMaxHoursByDay(defaultCalendarId);

        assertEquals(1, reloaded.get(DayOfWeek.MONDAY));
        assertEquals(4, reloaded.get(DayOfWeek.SUNDAY));
    }

    @Test
    void shouldPersistSubjectCompatibilityRules() {
        repository.saveSubjectRules(
            defaultCalendarId,
            List.of(
                new SubjectScheduleRule(
                    "Строевые занятия",
                    EnumSet.allOf(DayOfWeek.class),
                    EnumSet.noneOf(DayOfWeek.class),
                    1,
                    2,
                    "",
                    Set.of("физическая подготовка"),
                    Set.of("огневая подготовка")
                )
            )
        );

        List<SubjectScheduleRule> rules = repository.getSubjectRules(
            defaultCalendarId
        );

        assertEquals(1, rules.size());
        SubjectScheduleRule rule = rules.get(0);
        assertEquals(2, rule.getMaxLessonsPerDay());
        assertEquals(Set.of("физическая подготовка"), rule.getNoConsecutiveWithSubjects());
        assertEquals(Set.of("огневая подготовка"), rule.getNoSameDayWithSubjects());
    }
}
