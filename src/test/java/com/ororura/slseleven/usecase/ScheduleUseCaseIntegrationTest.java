package com.ororura.slseleven.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.domain.model.ScheduleItem;
import com.ororura.slseleven.domain.repository.LessonRepository;
import com.ororura.slseleven.domain.repository.ScheduleItemRepository;
import com.ororura.slseleven.domain.repository.ScheduleSettingsRepository;
import com.ororura.slseleven.infrastructure.persistence.migrations.SchemaInitializer;
import com.ororura.slseleven.infrastructure.persistence.sqlite.LessonRepositorySQLite;
import com.ororura.slseleven.infrastructure.persistence.sqlite.SQLiteConnectionProvider;
import com.ororura.slseleven.infrastructure.persistence.sqlite.ScheduleItemRepositorySQLite;
import com.ororura.slseleven.infrastructure.persistence.sqlite.ScheduleSettingsRepositorySQLite;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScheduleUseCaseIntegrationTest {

    @TempDir
    Path tempDir;

    private LessonRepository lessonRepository;
    private ScheduleItemRepository scheduleItemRepository;
    private ScheduleUseCase scheduleUseCase;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test.db");
        SQLiteConnectionProvider provider = new SQLiteConnectionProvider(dbPath);
        SchemaInitializer.init(provider);

        lessonRepository = new LessonRepositorySQLite(provider);
        scheduleItemRepository = new ScheduleItemRepositorySQLite(provider);
        ScheduleSettingsRepository scheduleSettingsRepository =
            new ScheduleSettingsRepositorySQLite(provider);

        scheduleUseCase = new ScheduleUseCase(
            lessonRepository,
            scheduleItemRepository,
            scheduleSettingsRepository
        );
    }

    @Test
    void autoSchedule_shouldRespectEndDateAndKeepRemainingHours() {
        LocalDate monday = LocalDate.of(2026, 2, 2);
        scheduleUseCase.saveMaxHoursByDay(onlyDayCapacity(DayOfWeek.MONDAY, 2));

        scheduleUseCase.createItem(new ScheduleItem("Math", "Lesson 1", "A1", "Ivanov", 3));

        AutoScheduleResult result = scheduleUseCase.autoSchedule(monday, monday);

        assertEquals(2, result.getCreatedLessons());
        assertEquals(1, result.getRemainingHours());
        assertEquals(monday, result.getLastScheduledDate());
        assertEquals(1, result.getRemainingItems().size());
        assertEquals(1, result.getRemainingItems().get(0).getHours());

        List<Lesson> mondayLessons = lessonRepository.findByDate(monday);
        assertEquals(2, mondayLessons.size());
        assertEquals(LocalTime.of(9, 0), mondayLessons.get(0).getTime());
        assertEquals(LocalTime.of(9, 50), mondayLessons.get(1).getTime());

        List<ScheduleItem> remainingQueue = scheduleItemRepository.findAll();
        assertEquals(1, remainingQueue.size());
        assertEquals(1, remainingQueue.get(0).getHours());
    }

    @Test
    void reschedule_shouldRebuildLessonsFromRangeAndQueue() {
        LocalDate monday = LocalDate.of(2026, 2, 2);
        LocalDate tuesday = monday.plusDays(1);
        scheduleUseCase.saveMaxHoursByDay(onlyDayCapacity(DayOfWeek.MONDAY, 4));

        Lesson firstAuto = new Lesson("Math", "L1", LocalTime.of(9, 0), "A1", "Ivanov", monday);
        firstAuto.setAutoScheduled(true);
        lessonRepository.save(firstAuto);
        Lesson secondAuto = new Lesson("Math", "L1", LocalTime.of(10, 0), "A1", "Ivanov", monday);
        secondAuto.setAutoScheduled(true);
        lessonRepository.save(secondAuto);
        lessonRepository.save(new Lesson("Physics", "Lab", LocalTime.of(9, 0), "B1", "Petrov", tuesday));
        scheduleItemRepository.save(new ScheduleItem("Math", "L1", "A1", "Ivanov", 1));

        AutoScheduleResult result = scheduleUseCase.reschedule(monday, monday);

        assertEquals(3, result.getCreatedLessons());
        assertEquals(0, result.getRemainingHours());
        assertTrue(result.getRemainingItems().isEmpty());

        List<Lesson> mondayLessons = lessonRepository.findByDate(monday);
        assertEquals(3, mondayLessons.size());
        assertEquals(1, lessonRepository.findByDate(tuesday).size(), "outside range lessons must stay untouched");
        assertTrue(scheduleItemRepository.findAll().isEmpty(), "queue must be consumed after successful reschedule");
    }

    @Test
    void countLessonsInRange_shouldSupportOpenEnd() {
        LocalDate start = LocalDate.of(2026, 2, 2);

        lessonRepository.save(new Lesson("A", "Before", LocalTime.of(9, 0), "X", "T", start.minusDays(1)));
        lessonRepository.save(new Lesson("A", "In", LocalTime.of(9, 0), "X", "T", start));
        lessonRepository.save(new Lesson("A", "After", LocalTime.of(10, 0), "X", "T", start.plusDays(1)));

        int count = scheduleUseCase.countLessonsInRange(start, null);

        assertEquals(2, count);
    }

    @Test
    void autoSchedule_shouldRejectInvalidDateRange() {
        LocalDate monday = LocalDate.of(2026, 2, 2);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> scheduleUseCase.autoSchedule(monday, monday.minusDays(1))
        );

        assertTrue(ex.getMessage().contains("Дата окончания"));
    }

    @Test
    void reschedule_shouldReturnRemainingAutoLessonsToPool() {
        LocalDate monday = LocalDate.of(2026, 2, 2);
        LocalDate tuesday = monday.plusDays(1);
        scheduleUseCase.saveMaxHoursByDay(onlyDayCapacity(DayOfWeek.MONDAY, 1));

        Lesson inRange = new Lesson(
            "Math",
            "T1",
            "Class A",
            LocalTime.of(9, 0),
            "A1",
            "Ivanov",
            monday
        );
        inRange.setAutoScheduled(true);
        lessonRepository.save(inRange);

        Lesson afterRange = new Lesson(
            "Math",
            "T2",
            "Class B",
            LocalTime.of(10, 0),
            "A1",
            "Ivanov",
            tuesday
        );
        afterRange.setAutoScheduled(true);
        lessonRepository.save(afterRange);

        AutoScheduleResult result = scheduleUseCase.reschedule(monday, monday);

        assertEquals(1, result.getCreatedLessons());
        assertEquals(1, result.getRemainingHours());
        assertTrue(lessonRepository.findByDate(tuesday).isEmpty());
        assertEquals(1, scheduleItemRepository.findAll().size());
    }

    @Test
    void moveArchivedLessonsToPool_shouldMoveAndAggregate() {
        LocalDate day = LocalDate.of(2026, 2, 2);
        Lesson archivedFirst = new Lesson(
            "Math",
            "Topic",
            "Class",
            LocalTime.of(9, 0),
            "A1",
            "Ivanov",
            day
        );
        archivedFirst.setArchived(true);
        lessonRepository.save(archivedFirst);

        Lesson archivedSecond = new Lesson(
            "Math",
            "Topic",
            "Class",
            LocalTime.of(9, 50),
            "A1",
            "Ivanov",
            day
        );
        archivedSecond.setArchived(true);
        lessonRepository.save(archivedSecond);

        int moved = scheduleUseCase.moveArchivedLessonsToPool();

        assertEquals(2, moved);
        assertTrue(lessonRepository.findAll().isEmpty());
        List<ScheduleItem> items = scheduleItemRepository.findAll();
        assertEquals(1, items.size());
        assertEquals(2, items.get(0).getHours());
    }

    private Map<DayOfWeek, Integer> onlyDayCapacity(DayOfWeek day, int capacity) {
        Map<DayOfWeek, Integer> map = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek value : DayOfWeek.values()) {
            map.put(value, 0);
        }
        map.put(day, capacity);
        return map;
    }
}
