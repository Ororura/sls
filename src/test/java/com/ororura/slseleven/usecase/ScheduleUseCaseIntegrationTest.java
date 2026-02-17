package com.ororura.slseleven.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.domain.model.ScheduleItem;
import com.ororura.slseleven.domain.model.SubjectScheduleRule;
import com.ororura.slseleven.domain.repository.LessonRepository;
import com.ororura.slseleven.domain.repository.ScheduleHistoryRepository;
import com.ororura.slseleven.domain.repository.ScheduleItemRepository;
import com.ororura.slseleven.domain.repository.ScheduleSettingsRepository;
import com.ororura.slseleven.infrastructure.persistence.migrations.SchemaInitializer;
import com.ororura.slseleven.infrastructure.persistence.sqlite.LessonRepositorySQLite;
import com.ororura.slseleven.infrastructure.persistence.sqlite.SQLiteConnectionProvider;
import com.ororura.slseleven.infrastructure.persistence.sqlite.ScheduleHistoryRepositorySQLite;
import com.ororura.slseleven.infrastructure.persistence.sqlite.ScheduleItemRepositorySQLite;
import com.ororura.slseleven.infrastructure.persistence.sqlite.ScheduleSettingsRepositorySQLite;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScheduleUseCaseIntegrationTest {
    private static final String DEFAULT_CALENDAR_ID = "default";

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
        ScheduleHistoryRepository scheduleHistoryRepository =
            new ScheduleHistoryRepositorySQLite(provider);

        scheduleUseCase = new ScheduleUseCase(
            lessonRepository,
            scheduleItemRepository,
            scheduleSettingsRepository,
            scheduleHistoryRepository
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

        List<Lesson> mondayLessons = lessonRepository.findByDate(
            monday,
            DEFAULT_CALENDAR_ID
        );
        assertEquals(2, mondayLessons.size());
        assertEquals(LocalTime.of(9, 0), mondayLessons.get(0).getTime());
        assertEquals(LocalTime.of(9, 50), mondayLessons.get(1).getTime());

        List<ScheduleItem> remainingQueue = scheduleItemRepository.findAll(
            DEFAULT_CALENDAR_ID
        );
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

        List<Lesson> mondayLessons = lessonRepository.findByDate(
            monday,
            DEFAULT_CALENDAR_ID
        );
        assertEquals(3, mondayLessons.size());
        assertEquals(
            1,
            lessonRepository.findByDate(tuesday, DEFAULT_CALENDAR_ID).size(),
            "outside range lessons must stay untouched"
        );
        assertTrue(
            scheduleItemRepository.findAll(DEFAULT_CALENDAR_ID).isEmpty(),
            "queue must be consumed after successful reschedule"
        );
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
        assertTrue(
            lessonRepository.findByDate(tuesday, DEFAULT_CALENDAR_ID).isEmpty()
        );
        assertEquals(1, scheduleItemRepository.findAll(DEFAULT_CALENDAR_ID).size());
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
        assertTrue(lessonRepository.findAll(DEFAULT_CALENDAR_ID).isEmpty());
        List<ScheduleItem> items = scheduleItemRepository.findAll(
            DEFAULT_CALENDAR_ID
        );
        assertEquals(1, items.size());
        assertEquals(2, items.get(0).getHours());
    }

    @Test
    void autoSchedule_shouldCreateConsecutiveBlockWhenConfigured() {
        LocalDate monday = LocalDate.of(2026, 2, 2);
        scheduleUseCase.saveMaxHoursByDay(onlyDayCapacity(DayOfWeek.MONDAY, 3));

        ScheduleItem item = new ScheduleItem("Math", "Block", "A", "A1", "Ivanov", 3);
        item.setConsecutiveHours(2);
        scheduleUseCase.createItem(item);

        AutoScheduleResult result = scheduleUseCase.autoSchedule(monday, monday);

        assertEquals(2, result.getCreatedLessons());
        assertEquals(0, result.getRemainingHours());

        List<Lesson> mondayLessons = lessonRepository.findByDate(
            monday,
            DEFAULT_CALENDAR_ID
        );
        assertEquals(2, mondayLessons.size());
        assertEquals(2, mondayLessons.get(0).getDurationHours());
        assertEquals(1, mondayLessons.get(1).getDurationHours());
    }

    @Test
    void autoSchedule_shouldAlternateSubjectsWhenPossible() {
        LocalDate monday = LocalDate.of(2026, 2, 2);
        scheduleUseCase.saveMaxHoursByDay(onlyDayCapacity(DayOfWeek.MONDAY, 4));

        scheduleUseCase.createItem(new ScheduleItem("Math", "Algebra", "A1", "Ivanov", 2));
        scheduleUseCase.createItem(new ScheduleItem("Physics", "Mechanics", "B1", "Petrov", 2));

        AutoScheduleResult result = scheduleUseCase.autoSchedule(monday, monday);

        assertEquals(4, result.getCreatedLessons());
        List<Lesson> mondayLessons = lessonRepository.findByDate(
            monday,
            DEFAULT_CALENDAR_ID
        );
        assertEquals(4, mondayLessons.size());
        assertEquals("Math", mondayLessons.get(0).getTopic());
        assertEquals("Physics", mondayLessons.get(1).getTopic());
        assertEquals("Math", mondayLessons.get(2).getTopic());
        assertEquals("Physics", mondayLessons.get(3).getTopic());
    }

    @Test
    void autoSchedule_shouldAvoidForbiddenConsecutiveSubjects() {
        LocalDate monday = LocalDate.of(2026, 2, 2);
        scheduleUseCase.saveMaxHoursByDay(
            dayCapacities(Map.of(DayOfWeek.MONDAY, 4, DayOfWeek.TUESDAY, 4))
        );

        scheduleUseCase.createItem(new ScheduleItem("Строевые занятия", "S1", "A1", "Ivanov", 2));
        scheduleUseCase.createItem(new ScheduleItem("Физическая подготовка", "P1", "A1", "Petrov", 2));

        scheduleUseCase.saveSubjectRules(
            List.of(
                new SubjectScheduleRule(
                    "Строевые занятия",
                    EnumSet.allOf(DayOfWeek.class),
                    EnumSet.noneOf(DayOfWeek.class),
                    1,
                    "",
                    Set.of("физическая подготовка"),
                    Set.of()
                )
            )
        );

        AutoScheduleResult result = scheduleUseCase.autoSchedule(monday, monday.plusDays(1));

        assertEquals(4, result.getCreatedLessons());
        List<Lesson> mondayLessons = lessonRepository.findByDate(
            monday,
            DEFAULT_CALENDAR_ID
        );
        List<Lesson> tuesdayLessons = lessonRepository.findByDate(
            monday.plusDays(1),
            DEFAULT_CALENDAR_ID
        );
        assertNoForbiddenNeighbors(
            mondayLessons,
            "Строевые занятия",
            "Физическая подготовка"
        );
        assertNoForbiddenNeighbors(
            tuesdayLessons,
            "Строевые занятия",
            "Физическая подготовка"
        );
    }

    @Test
    void autoSchedule_shouldAvoidForbiddenSameDaySubjects() {
        LocalDate monday = LocalDate.of(2026, 2, 2);
        scheduleUseCase.saveMaxHoursByDay(
            dayCapacities(Map.of(DayOfWeek.MONDAY, 4, DayOfWeek.TUESDAY, 4))
        );

        scheduleUseCase.createItem(new ScheduleItem("Строевые занятия", "S1", "A1", "Ivanov", 2));
        scheduleUseCase.createItem(new ScheduleItem("Физическая подготовка", "P1", "A1", "Petrov", 2));

        scheduleUseCase.saveSubjectRules(
            List.of(
                new SubjectScheduleRule(
                    "Строевые занятия",
                    EnumSet.allOf(DayOfWeek.class),
                    EnumSet.noneOf(DayOfWeek.class),
                    1,
                    "",
                    Set.of(),
                    Set.of("физическая подготовка")
                )
            )
        );

        AutoScheduleResult result = scheduleUseCase.autoSchedule(monday, monday.plusDays(1));

        assertEquals(4, result.getCreatedLessons());
        List<Lesson> mondayLessons = lessonRepository.findByDate(
            monday,
            DEFAULT_CALENDAR_ID
        );
        assertEquals(2, mondayLessons.size());
        assertEquals("Строевые занятия", mondayLessons.get(0).getTopic());
        assertEquals("Строевые занятия", mondayLessons.get(1).getTopic());

        List<Lesson> tuesdayLessons = lessonRepository.findByDate(
            monday.plusDays(1),
            DEFAULT_CALENDAR_ID
        );
        assertEquals(2, tuesdayLessons.size());
        assertEquals("Физическая подготовка", tuesdayLessons.get(0).getTopic());
        assertEquals("Физическая подготовка", tuesdayLessons.get(1).getTopic());
    }

    @Test
    void autoSchedule_shouldRespectMaxLessonsPerDayBySubjectRule() {
        LocalDate monday = LocalDate.of(2026, 2, 2);
        scheduleUseCase.saveMaxHoursByDay(
            dayCapacities(Map.of(DayOfWeek.MONDAY, 4, DayOfWeek.TUESDAY, 4))
        );

        scheduleUseCase.createItem(new ScheduleItem("Строевые занятия", "S1", "A1", "Ivanov", 4));

        scheduleUseCase.saveSubjectRules(
            List.of(
                new SubjectScheduleRule(
                    "Строевые занятия",
                    EnumSet.allOf(DayOfWeek.class),
                    EnumSet.noneOf(DayOfWeek.class),
                    1,
                    1,
                    "",
                    Set.of(),
                    Set.of()
                )
            )
        );

        AutoScheduleResult result = scheduleUseCase.autoSchedule(
            monday,
            monday.plusDays(1)
        );

        assertEquals(2, result.getCreatedLessons());
        assertEquals(2, result.getRemainingHours());
        List<Lesson> mondayLessons = lessonRepository.findByDate(
            monday,
            DEFAULT_CALENDAR_ID
        );
        List<Lesson> tuesdayLessons = lessonRepository.findByDate(
            monday.plusDays(1),
            DEFAULT_CALENDAR_ID
        );
        assertEquals(1, mondayLessons.size());
        assertEquals(1, tuesdayLessons.size());
        assertEquals("Строевые занятия", mondayLessons.get(0).getTopic());
        assertEquals("Строевые занятия", tuesdayLessons.get(0).getTopic());
    }

    @Test
    void autoSchedule_maxLessonsPerDayShouldLimitOnlyTargetSubject() {
        LocalDate monday = LocalDate.of(2026, 2, 2);
        scheduleUseCase.saveMaxHoursByDay(onlyDayCapacity(DayOfWeek.MONDAY, 4));

        scheduleUseCase.createItem(new ScheduleItem("Строевые занятия", "S1", "A1", "Ivanov", 3));
        scheduleUseCase.createItem(new ScheduleItem("Огневая подготовка", "F1", "A1", "Petrov", 2));

        scheduleUseCase.saveSubjectRules(
            List.of(
                new SubjectScheduleRule(
                    "Строевые занятия",
                    EnumSet.allOf(DayOfWeek.class),
                    EnumSet.noneOf(DayOfWeek.class),
                    1,
                    1,
                    "",
                    Set.of(),
                    Set.of()
                )
            )
        );

        AutoScheduleResult result = scheduleUseCase.autoSchedule(monday, monday);

        assertEquals(3, result.getCreatedLessons());
        List<Lesson> mondayLessons = lessonRepository.findByDate(
            monday,
            DEFAULT_CALENDAR_ID
        );
        long drillsCount = mondayLessons
            .stream()
            .filter(lesson -> "Строевые занятия".equals(lesson.getTopic()))
            .count();
        long fireCount = mondayLessons
            .stream()
            .filter(lesson -> "Огневая подготовка".equals(lesson.getTopic()))
            .count();
        assertEquals(1, drillsCount);
        assertEquals(2, fireCount);
    }

    private Map<DayOfWeek, Integer> onlyDayCapacity(DayOfWeek day, int capacity) {
        Map<DayOfWeek, Integer> map = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek value : DayOfWeek.values()) {
            map.put(value, 0);
        }
        map.put(day, capacity);
        return map;
    }

    private Map<DayOfWeek, Integer> dayCapacities(
        Map<DayOfWeek, Integer> capacities
    ) {
        Map<DayOfWeek, Integer> map = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek value : DayOfWeek.values()) {
            map.put(value, 0);
        }
        map.putAll(capacities);
        return map;
    }

    private void assertNoForbiddenNeighbors(
        List<Lesson> lessons,
        String firstSubject,
        String secondSubject
    ) {
        List<Lesson> sorted = new ArrayList<>(lessons);
        sorted.sort(Comparator.comparing(Lesson::getTime));
        for (int i = 0; i < sorted.size() - 1; i++) {
            Lesson current = sorted.get(i);
            Lesson next = sorted.get(i + 1);
            if (!areNeighborSlots(current.getTime(), next.getTime())) {
                continue;
            }
            String pair = current.getTopic() + "|" + next.getTopic();
            boolean forbiddenForward = pair.equals(firstSubject + "|" + secondSubject);
            boolean forbiddenBackward = pair.equals(secondSubject + "|" + firstSubject);
            assertTrue(
                !forbiddenForward && !forbiddenBackward,
                "forbidden consecutive subjects found: " + pair
            );
        }
    }

    private boolean areNeighborSlots(LocalTime first, LocalTime second) {
        List<LocalTime> slots = List.of(
            LocalTime.of(9, 0),
            LocalTime.of(9, 50),
            LocalTime.of(10, 50),
            LocalTime.of(11, 40),
            LocalTime.of(12, 40),
            LocalTime.of(13, 30),
            LocalTime.of(16, 0),
            LocalTime.of(16, 50)
        );
        int firstIndex = slots.indexOf(first);
        int secondIndex = slots.indexOf(second);
        return firstIndex >= 0 && secondIndex == firstIndex + 1;
    }
}
