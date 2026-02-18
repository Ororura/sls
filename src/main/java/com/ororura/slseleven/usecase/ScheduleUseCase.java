package com.ororura.slseleven.usecase;

import com.ororura.slseleven.domain.model.CalendarDefaults;
import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.domain.model.AppCalendar;
import com.ororura.slseleven.domain.model.InstructorDuty;
import com.ororura.slseleven.domain.model.InstructorProfile;
import com.ororura.slseleven.domain.model.RoomProfile;
import com.ororura.slseleven.domain.model.ScheduleHistorySnapshot;
import com.ororura.slseleven.domain.model.ScheduleItem;
import com.ororura.slseleven.domain.model.SubjectScheduleRule;
import com.ororura.slseleven.domain.repository.ScheduleHistoryRepository;
import com.ororura.slseleven.domain.repository.LessonRepository;
import com.ororura.slseleven.domain.repository.ScheduleItemRepository;
import com.ororura.slseleven.domain.repository.ScheduleSettingsRepository;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Бизнес-логика планировщика:
 * - управление пулом предметов/часов для автораспределения;
 * - генерация авто-занятий с учётом правил по предметам, кабинетам и преподавателям;
 * - восстановление/сохранение снимков состояния расписания.
 */
public class ScheduleUseCase {

    private static final List<LocalTime> LESSON_SLOT_START_TIMES = List.of(
        LocalTime.of(9, 0),
        LocalTime.of(9, 50),
        LocalTime.of(10, 50),
        LocalTime.of(11, 40),
        LocalTime.of(12, 40),
        LocalTime.of(13, 30),
        LocalTime.of(16, 0),
        LocalTime.of(16, 50)
    );
    private static final Map<LocalTime, Integer> SLOT_INDEX_BY_START_TIME =
        buildSlotIndexByStartTime();

    private final LessonRepository lessonRepository;
    private final ScheduleItemRepository scheduleItemRepository;
    private final ScheduleSettingsRepository scheduleSettingsRepository;
    private final ScheduleHistoryRepository scheduleHistoryRepository;
    private String currentCalendarId = CalendarDefaults.DEFAULT_ID;

    private static Map<LocalTime, Integer> buildSlotIndexByStartTime() {
        Map<LocalTime, Integer> indexByTime = new HashMap<>();
        for (int i = 0; i < LESSON_SLOT_START_TIMES.size(); i++) {
            indexByTime.put(LESSON_SLOT_START_TIMES.get(i), i);
        }
        return indexByTime;
    }

    /**
     * Создаёт use-case планировщика и инициализирует активный календарь из настроек.
     */
    public ScheduleUseCase(
        LessonRepository lessonRepository,
        ScheduleItemRepository scheduleItemRepository,
        ScheduleSettingsRepository scheduleSettingsRepository,
        ScheduleHistoryRepository scheduleHistoryRepository
    ) {
        this.lessonRepository = lessonRepository;
        this.scheduleItemRepository = scheduleItemRepository;
        this.scheduleSettingsRepository = scheduleSettingsRepository;
        this.scheduleHistoryRepository = scheduleHistoryRepository;
        this.currentCalendarId = scheduleSettingsRepository.getActiveCalendarId();
    }

    /**
     * Устанавливает активный календарь и синхронизирует его в хранилище настроек.
     */
    public void setCurrentCalendarId(String calendarId) {
        if (calendarId == null || calendarId.isBlank()) {
            throw new IllegalArgumentException("ID календаря не может быть пустым");
        }
        scheduleSettingsRepository.setActiveCalendarId(calendarId);
        this.currentCalendarId = calendarId;
    }

    /**
     * Возвращает ID активного календаря.
     */
    public String getCurrentCalendarId() {
        return currentCalendarId;
    }

    /**
     * Возвращает список всех календарей.
     */
    public List<AppCalendar> getCalendars() {
        return scheduleSettingsRepository.findAllCalendars();
    }

    /**
     * Создаёт новый календарь.
     */
    public AppCalendar createCalendar(String name) {
        return scheduleSettingsRepository.createCalendar(name);
    }

    /**
     * Создаёт новый календарь.
     */
    public AppCalendar createCalendar(String name, String directoryPath) {
        return scheduleSettingsRepository.createCalendar(name, directoryPath);
    }

    /**
     * Переименовывает календарь по ID.
     */
    public void renameCalendar(String calendarId, String newName) {
        scheduleSettingsRepository.renameCalendar(calendarId, newName);
    }

    /**
     * Перемещает календарь в указанную директорию.
     */
    public void moveCalendarToDirectory(String calendarId, String directoryPath) {
        scheduleSettingsRepository.moveCalendarToDirectory(
            calendarId,
            directoryPath
        );
    }

    /**
     * Возвращает список доступных директорий календарей.
     */
    public List<String> getCalendarDirectories() {
        return scheduleSettingsRepository.getCalendarDirectories();
    }

    /**
     * Удаляет календарь и переключает активный календарь на доступный.
     */
    public void deleteCalendar(String calendarId) {
        scheduleSettingsRepository.deleteCalendar(calendarId);
        this.currentCalendarId = scheduleSettingsRepository.getActiveCalendarId();
    }

    /**
     * Возвращает пул нераспределённых элементов расписания активного календаря.
     */
    public List<ScheduleItem> getAllItems() {
        return scheduleItemRepository.findAll(currentCalendarId);
    }

    /**
     * Добавляет элемент в пул распределения и синхронизирует правила подрядности по предмету.
     */
    public void createItem(ScheduleItem item) {
        validateItem(item);
        item.setCalendarId(currentCalendarId);
        scheduleItemRepository.save(item);
        syncSubjectConsecutiveHours(item.getTopic(), item.getConsecutiveHours(), item.getId());
    }

    /**
     * Обновляет элемент пула распределения с проверкой принадлежности активному календарю.
     */
    public void updateItem(ScheduleItem item) {
        if (item == null || item.getId() == null) {
            throw new IllegalArgumentException(
                "Элемент списка или ID не может быть null"
            );
        }
        validateItem(item);
        if (!scheduleItemRepository.existsById(item.getId())) {
            throw new IllegalArgumentException(
                "Элемент списка с ID " + item.getId() + " не найден"
            );
        }
        ScheduleItem existing = scheduleItemRepository.findById(item.getId()).orElse(null);
        if (existing == null || !currentCalendarId.equals(existing.getCalendarId())) {
            throw new IllegalArgumentException(
                "Элемент списка с ID " + item.getId() + " не найден"
            );
        }
        item.setCalendarId(currentCalendarId);
        scheduleItemRepository.save(item);
        syncSubjectConsecutiveHours(item.getTopic(), item.getConsecutiveHours(), item.getId());
    }

    /**
     * Удаляет элемент пула распределения по ID.
     */
    public void deleteItem(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "ID элемента списка не может быть пустым"
            );
        }
        ScheduleItem existing = scheduleItemRepository.findById(id).orElse(null);
        if (existing == null || !currentCalendarId.equals(existing.getCalendarId())) {
            throw new IllegalArgumentException(
                "Элемент списка с ID " + id + " не найден"
            );
        }
        scheduleItemRepository.deleteById(id);
    }

    /**
     * Очищает весь пул распределения активного календаря.
     */
    public void deleteAllItems() {
        scheduleItemRepository.deleteAll(currentCalendarId);
    }

    public Map<DayOfWeek, Integer> getMaxHoursByDay() {
        return scheduleSettingsRepository.getMaxHoursByDay();
    }

    /**
     * Возвращает правила распределения по предметам.
     */
    public List<SubjectScheduleRule> getSubjectRules() {
        return scheduleSettingsRepository.getSubjectRules();
    }

    /**
     * Сохраняет правила предметов и синхронизирует подрядные часы у элементов пула.
     */
    public void saveSubjectRules(List<SubjectScheduleRule> rules) {
        scheduleSettingsRepository.saveSubjectRules(rules);
        if (rules == null || rules.isEmpty()) {
            return;
        }
        Map<String, Integer> consecutiveBySubject = new HashMap<>();
        for (SubjectScheduleRule rule : rules) {
            if (rule == null || rule.getSubject().isBlank()) {
                continue;
            }
            consecutiveBySubject.put(
                normalizeSubject(rule.getSubject()),
                Math.max(1, rule.getConsecutiveHours())
            );
        }
        if (consecutiveBySubject.isEmpty()) {
            return;
        }
        for (ScheduleItem item : scheduleItemRepository.findAll(currentCalendarId)) {
            Integer subjectConsecutive = consecutiveBySubject.get(
                normalizeSubject(item.getTopic())
            );
            if (subjectConsecutive == null) {
                continue;
            }
            if (item.getConsecutiveHours() == subjectConsecutive) {
                continue;
            }
            item.setConsecutiveHours(subjectConsecutive);
            scheduleItemRepository.save(item);
        }
    }

    /**
     * Возвращает список преподавателей активного календаря.
     */
    public List<InstructorProfile> getInstructors() {
        return scheduleSettingsRepository.getInstructors();
    }

    /**
     * Создаёт нового преподавателя.
     */
    public InstructorProfile createInstructor(String name) {
        return scheduleSettingsRepository.createInstructor(name);
    }

    /**
     * Обновляет карточку преподавателя.
     */
    public void updateInstructor(InstructorProfile instructor) {
        scheduleSettingsRepository.updateInstructor(instructor);
    }

    /**
     * Удаляет преподавателя по ID.
     */
    public void deleteInstructor(String instructorId) {
        scheduleSettingsRepository.deleteInstructor(instructorId);
    }

    /**
     * Возвращает список кабинетов активного календаря.
     */
    public List<RoomProfile> getRooms() {
        return scheduleSettingsRepository.getRooms();
    }

    /**
     * Создаёт новый кабинет.
     */
    public RoomProfile createRoom(String name) {
        return scheduleSettingsRepository.createRoom(name);
    }

    /**
     * Переименовывает кабинет по ID.
     */
    public void renameRoom(String roomId, String newName) {
        scheduleSettingsRepository.renameRoom(roomId, newName);
    }

    /**
     * Удаляет кабинет по ID.
     */
    public void deleteRoom(String roomId) {
        scheduleSettingsRepository.deleteRoom(roomId);
    }

    /**
     * Возвращает список дежурств преподавателей.
     */
    public List<InstructorDuty> getInstructorDuties() {
        return scheduleSettingsRepository.getInstructorDuties();
    }

    /**
     * Добавляет дату дежурства преподавателя.
     */
    public void addInstructorDuty(String instructorId, LocalDate dutyDate) {
        scheduleSettingsRepository.addInstructorDuty(instructorId, dutyDate);
    }

    /**
     * Удаляет дату дежурства преподавателя.
     */
    public void removeInstructorDuty(String instructorId, LocalDate dutyDate) {
        scheduleSettingsRepository.removeInstructorDuty(instructorId, dutyDate);
    }

    /**
     * Возвращает список снимков истории расписания для UI.
     */
    public List<HistoryEntry> getHistoryEntries() {
        List<ScheduleHistorySnapshot> snapshots = scheduleHistoryRepository.findAll(currentCalendarId);
        List<HistoryEntry> entries = new ArrayList<>();
        for (ScheduleHistorySnapshot snapshot : snapshots) {
            entries.add(
                new HistoryEntry(
                    snapshot.getId(),
                    snapshot.getCreatedAt(),
                    snapshot.getLabel()
                )
            );
        }
        return entries;
    }

    /**
     * Создаёт снимок текущего состояния занятий и пула распределения.
     */
    public boolean createHistorySnapshot(String label) {
        List<Lesson> lessons = lessonRepository.findAll(currentCalendarId);
        List<ScheduleItem> items = scheduleItemRepository.findAll(currentCalendarId);
        if (lessons.isEmpty() && items.isEmpty()) {
            return false;
        }

        String snapshotId = java.util.UUID.randomUUID().toString();
        ScheduleHistorySnapshot snapshot = new ScheduleHistorySnapshot(
            snapshotId,
            currentCalendarId,
            LocalDateTime.now(),
            label == null || label.isBlank() ? "Ручной снимок" : label,
            serializeLessons(lessons),
            serializeScheduleItems(items)
        );
        scheduleHistoryRepository.save(snapshot);
        return true;
    }

    /**
     * Восстанавливает занятия и пул из выбранного снимка истории.
     */
    public boolean restoreFromHistory(String historyId) {
        if (historyId == null || historyId.isBlank()) {
            throw new IllegalArgumentException("ID снимка не может быть пустым");
        }
        ScheduleHistorySnapshot snapshot = scheduleHistoryRepository
            .findById(historyId, currentCalendarId)
            .orElse(null);
        if (snapshot == null) {
            return false;
        }

        List<Lesson> lessons = deserializeLessons(snapshot.getLessonsBlob());
        List<ScheduleItem> items = deserializeScheduleItems(
            snapshot.getScheduleItemsBlob()
        );

        lessonRepository.deleteAll(currentCalendarId);
        scheduleItemRepository.deleteAll(currentCalendarId);

        for (Lesson lesson : lessons) {
            lesson.setCalendarId(currentCalendarId);
            lessonRepository.save(lesson);
        }
        for (ScheduleItem item : items) {
            item.setCalendarId(currentCalendarId);
            scheduleItemRepository.save(item);
        }
        return true;
    }

    /**
     * Сохраняет лимиты часов по дням недели.
     */
    public void saveMaxHoursByDay(Map<DayOfWeek, Integer> maxHoursByDay) {
        scheduleSettingsRepository.saveAll(maxHoursByDay);
    }

    /**
     * Подсчитывает количество занятий в заданном диапазоне дат.
     */
    public int countLessonsInRange(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return findLessonsInRange(startDate, endDate).size();
    }

    /**
     * Возвращает все архивные занятия в пул распределения с агрегацией часов.
     */
    public int moveArchivedLessonsToPool() {
        List<Lesson> allLessons = lessonRepository.findAll(currentCalendarId);
        Map<ScheduleKey, Integer> aggregatedHours = new LinkedHashMap<>();
        Map<String, Integer> preferredConsecutiveBySubject = new HashMap<>();
        List<String> archivedLessonIds = new ArrayList<>();

        for (Lesson lesson : allLessons) {
            if (!lesson.isArchived()) {
                continue;
            }
            ScheduleKey key = scheduleKeyOfLesson(lesson);
            int durationHours = Math.max(1, lesson.getDurationHours());
            aggregatedHours.merge(key, durationHours, Integer::sum);
            preferredConsecutiveBySubject.merge(
                normalizeSubject(lesson.getTopic()),
                durationHours,
                Math::max
            );
            archivedLessonIds.add(lesson.getId());
        }

        if (aggregatedHours.isEmpty()) {
            return 0;
        }

        Map<ScheduleKey, ScheduleItem> existingItemsByKey = new HashMap<>();
        for (ScheduleItem item : scheduleItemRepository.findAll(currentCalendarId)) {
            preferredConsecutiveBySubject.merge(
                normalizeSubject(item.getTopic()),
                Math.max(1, item.getConsecutiveHours()),
                Math::max
            );
            existingItemsByKey.put(scheduleKeyOfItem(item), item);
        }

        for (Map.Entry<ScheduleKey, Integer> entry : aggregatedHours.entrySet()) {
            ScheduleKey key = entry.getKey();
            int hoursToAdd = entry.getValue();
            ScheduleItem existing = existingItemsByKey.get(key);
            int subjectConsecutive = preferredConsecutiveBySubject.getOrDefault(
                normalizeSubject(key.topic),
                1
            );
            if (existing != null) {
                existing.setHours(existing.getHours() + hoursToAdd);
                existing.setConsecutiveHours(
                    Math.max(
                        existing.getConsecutiveHours(),
                        subjectConsecutive
                    )
                );
                scheduleItemRepository.save(existing);
                continue;
            }
            ScheduleItem created = new ScheduleItem(
                key.topic,
                key.lessonName,
                key.className,
                key.location,
                key.instructor,
                hoursToAdd
            );
            created.setCalendarId(currentCalendarId);
            created.setConsecutiveHours(subjectConsecutive);
            scheduleItemRepository.save(created);
        }

        for (String lessonId : archivedLessonIds) {
            lessonRepository.deleteById(lessonId);
        }
        return archivedLessonIds.size();
    }

    /**
     * Возвращает одно архивное занятие в пул распределения.
     */
    public void moveArchivedLessonToPool(String lessonId) {
        if (lessonId == null || lessonId.isBlank()) {
            throw new IllegalArgumentException("ID занятия не может быть пустым");
        }

        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null || !currentCalendarId.equals(lesson.getCalendarId())) {
            throw new IllegalArgumentException("Занятие не найдено");
        }
        if (!lesson.isArchived()) {
            throw new IllegalArgumentException("Занятие не находится в архиве");
        }

        moveLessonToPoolAndDelete(lesson);
    }

    /**
     * Возвращает одно авто-созданное занятие обратно в пул распределения.
     */
    public void moveAutoScheduledLessonToPool(String lessonId) {
        if (lessonId == null || lessonId.isBlank()) {
            throw new IllegalArgumentException("ID занятия не может быть пустым");
        }

        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null || !currentCalendarId.equals(lesson.getCalendarId())) {
            throw new IllegalArgumentException("Занятие не найдено");
        }
        if (lesson.isArchived()) {
            throw new IllegalArgumentException("Архивное занятие верните через кнопку архива");
        }
        if (!lesson.isAutoScheduled()) {
            throw new IllegalArgumentException(
                "Можно вернуть в пул только занятие, созданное автораспределением"
            );
        }

        moveLessonToPoolAndDelete(lesson);
    }

    /**
     * Возвращает все авто-созданные занятия в пул и удаляет их из расписания.
     */
    public int moveAllAutoScheduledLessonsToPool() {
        List<Lesson> allLessons = lessonRepository.findAll(currentCalendarId);
        int moved = 0;
        for (Lesson lesson : allLessons) {
            if (!lesson.isAutoScheduled() || lesson.isArchived()) {
                continue;
            }
            moveLessonToPoolAndDelete(lesson);
            moved++;
        }
        return moved;
    }

    /**
     * Служебно переносит занятие в пул распределения и удаляет исходную запись.
     */
    private void moveLessonToPoolAndDelete(Lesson lesson) {
        if (lesson == null) {
            return;
        }

        ScheduleKey key = scheduleKeyOfLesson(lesson);
        int durationHours = Math.max(1, lesson.getDurationHours());
        ScheduleItem existing = null;
        for (ScheduleItem item : scheduleItemRepository.findAll(currentCalendarId)) {
            if (scheduleKeyOfItem(item).equals(key)) {
                existing = item;
                break;
            }
        }

        if (existing != null) {
            existing.setHours(existing.getHours() + durationHours);
            existing.setConsecutiveHours(
                Math.max(existing.getConsecutiveHours(), durationHours)
            );
            scheduleItemRepository.save(existing);
        } else {
            ScheduleItem created = new ScheduleItem(
                lesson.getTopic(),
                lesson.getLessonName(),
                lesson.getClassName(),
                lesson.getLocation(),
                lesson.getInstructor(),
                durationHours
            );
            created.setCalendarId(currentCalendarId);
            created.setConsecutiveHours(durationHours);
            scheduleItemRepository.save(created);
        }

        int subjectConsecutive = resolveSubjectConsecutiveHours(lesson.getTopic());
        syncSubjectConsecutiveHours(lesson.getTopic(), subjectConsecutive, null);
        lessonRepository.deleteById(lesson.getId());
    }

    /**
     * Подсчитывает авто-созданные занятия в диапазоне дат.
     */
    public int countAutoScheduledLessonsInRange(
        LocalDate startDate,
        LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);
        return findAutoScheduledLessonsInRange(startDate, endDate).size();
    }

    /**
     * Подсчитывает авто-занятия, которые будут затронуты перераспределением.
     */
    public int countAutoScheduledLessonsForReschedule(
        LocalDate startDate,
        LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);
        return findAutoScheduledLessonsForReschedule(startDate, endDate).size();
    }

    /**
     * Пересобирает пул из текущих данных и выполняет новое автораспределение.
     */
    public AutoScheduleResult reschedule(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        List<Lesson> lessonsToReschedule = findAutoScheduledLessonsForReschedule(
            startDate,
            endDate
        );
        if (lessonsToReschedule.isEmpty()) {
            throw new IllegalStateException(
                "В выбранном диапазоне нет авто-распределённых занятий для переформирования"
            );
        }

        List<ScheduleItem> queuedItems = scheduleItemRepository.findAll(currentCalendarId);
        Map<ScheduleKey, Integer> aggregatedHours = new LinkedHashMap<>();
        Map<String, Integer> preferredConsecutiveBySubject = new HashMap<>();

        for (ScheduleItem item : queuedItems) {
            ScheduleKey key = new ScheduleKey(
                item.getTopic(),
                item.getLessonName(),
                item.getClassName(),
                item.getLocation(),
                item.getInstructor()
            );
            aggregatedHours.merge(key, item.getHours(), Integer::sum);
            preferredConsecutiveBySubject.merge(
                normalizeSubject(item.getTopic()),
                Math.max(1, item.getConsecutiveHours()),
                Math::max
            );
        }

        for (Lesson lesson : lessonsToReschedule) {
            ScheduleKey key = new ScheduleKey(
                lesson.getTopic(),
                lesson.getLessonName(),
                lesson.getClassName(),
                lesson.getLocation(),
                lesson.getInstructor()
            );
            int durationHours = Math.max(1, lesson.getDurationHours());
            aggregatedHours.merge(key, durationHours, Integer::sum);
            preferredConsecutiveBySubject.merge(
                normalizeSubject(lesson.getTopic()),
                durationHours,
                Math::max
            );
        }

        scheduleItemRepository.deleteAll(currentCalendarId);
        for (Map.Entry<ScheduleKey, Integer> entry : aggregatedHours.entrySet()) {
            ScheduleKey key = entry.getKey();
            ScheduleItem created = new ScheduleItem(
                key.topic,
                key.lessonName,
                key.className,
                key.location,
                key.instructor,
                entry.getValue()
            );
            created.setCalendarId(currentCalendarId);
            created.setConsecutiveHours(
                preferredConsecutiveBySubject.getOrDefault(
                    normalizeSubject(key.topic),
                    1
                )
            );
            scheduleItemRepository.save(created);
        }

        for (Lesson lesson : lessonsToReschedule) {
            lessonRepository.deleteById(lesson.getId());
        }

        return autoSchedule(startDate, endDate);
    }

    /**
     * Выполняет автоматическое распределение часов в расписание по ограничениям и слотам.
     */
    public AutoScheduleResult autoSchedule(LocalDate startDate) {
        return autoSchedule(startDate, null);
    }

    /**
     * Выполняет автоматическое распределение часов в расписание по ограничениям и слотам.
     */
    public AutoScheduleResult autoSchedule(
        LocalDate startDate,
        LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);

        List<ScheduleItem> items = scheduleItemRepository.findAll(currentCalendarId);
        if (items.isEmpty()) {
            return new AutoScheduleResult(0, null, 0, List.of());
        }

        Map<DayOfWeek, Integer> maxHoursByDay = new EnumMap<>(
            scheduleSettingsRepository.getMaxHoursByDay()
        );
        Map<String, SubjectScheduleRule> rulesBySubject = toRuleMap(
            scheduleSettingsRepository.getSubjectRules()
        );
        List<InstructorProfile> instructors = new ArrayList<>(
            scheduleSettingsRepository.getInstructors()
        );
        instructors.sort(Comparator.comparing(InstructorProfile::getName, String.CASE_INSENSITIVE_ORDER));
        List<RoomProfile> rooms = new ArrayList<>(scheduleSettingsRepository.getRooms());
        rooms.sort(Comparator.comparing(RoomProfile::getName, String.CASE_INSENSITIVE_ORDER));
        Map<String, Set<LocalDate>> dutyDatesByInstructor = buildDutyDatesByInstructor(
            scheduleSettingsRepository.getInstructorDuties()
        );
        boolean hasCapacity = maxHoursByDay
            .values()
            .stream()
            .anyMatch(hours -> hours != null && hours > 0);
        if (!hasCapacity) {
            throw new IllegalStateException(
                "Нужно задать часы занятий хотя бы для одного дня недели"
            );
        }

        int totalHours = items.stream().mapToInt(ScheduleItem::getHours).sum();
        Map<String, Integer> subjectConsecutiveHours =
            buildSubjectConsecutiveHours(items, rulesBySubject);
        int createdLessons = 0;
        LocalDate currentDate = startDate;
        LocalDate lastDate = null;
        Map<String, Integer> remainingHoursByItemId = new HashMap<>();
        for (ScheduleItem item : items) {
            remainingHoursByItemId.put(item.getId(), item.getHours());
        }

        int[] remainingByItem = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            remainingByItem[i] = items.get(i).getHours();
        }
        Set<String> completedItemIds = new HashSet<>();
        String lastScheduledSubject = "";
        int lastSubjectStreakHours = 0;
        int candidateStartIndex = 0;
        int daysWithoutProgress = 0;

        // Главный цикл: идём по дням от startDate до endDate (если задан),
        // пока есть часы в пуле и в расписании остаются доступные слоты.
        while (
            totalHours > 0 &&
            (endDate == null || !currentDate.isAfter(endDate))
        ) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            int maxHours = maxHoursByDay.getOrDefault(dayOfWeek, 0);
            if (maxHours <= 0) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            List<Lesson> existingLessons = lessonRepository.findByDate(
                currentDate,
                currentCalendarId
            );
            existingLessons.removeIf(Lesson::isArchived);
            Set<String> dayExclusiveSubjects = getExclusiveSubjectsForDay(
                dayOfWeek,
                rulesBySubject
            );
            Set<String> subjectsScheduledToday = new HashSet<>();
            Map<String, Integer> lessonsCountBySubjectToday = new HashMap<>();
            Map<Integer, String> subjectBySlotIndex = new HashMap<>();
            Set<Integer> occupiedSlotIndexes = new HashSet<>();
            int occupiedByCustomTime = 0;
            for (Lesson lesson : existingLessons) {
                int duration = Math.max(1, lesson.getDurationHours());
                String normalizedSubject = normalizeSubject(lesson.getTopic());
                if (!normalizedSubject.isEmpty()) {
                    subjectsScheduledToday.add(normalizedSubject);
                    lessonsCountBySubjectToday.merge(
                        normalizedSubject,
                        1,
                        Integer::sum
                    );
                }
                Integer startIndex = SLOT_INDEX_BY_START_TIME.get(lesson.getTime());
                if (startIndex == null) {
                    occupiedByCustomTime += duration;
                    continue;
                }
                for (int offset = 0; offset < duration; offset++) {
                    int slotIndex = startIndex + offset;
                    if (slotIndex >= LESSON_SLOT_START_TIMES.size()) {
                        break;
                    }
                    occupiedSlotIndexes.add(slotIndex);
                    if (!normalizedSubject.isEmpty()) {
                        subjectBySlotIndex.put(slotIndex, normalizedSubject);
                    }
                }
            }

            int availableSlots =
                maxHours - occupiedSlotIndexes.size() - occupiedByCustomTime;
            if (availableSlots <= 0) {
                daysWithoutProgress++;
                currentDate = currentDate.plusDays(1);
                continue;
            }

            int slotOffset = 0;
            int createdToday = 0;
            // Внутренний цикл: пробуем заполнить каждый слот текущего дня.
            while (
                availableSlots > 0 &&
                totalHours > 0 &&
                slotOffset < LESSON_SLOT_START_TIMES.size()
            ) {
                int candidateSlotIndex = slotOffset;
                LocalTime candidateTime = LESSON_SLOT_START_TIMES.get(slotOffset++);

                if (occupiedSlotIndexes.contains(candidateSlotIndex)) {
                    continue;
                }

                AssignmentCandidate candidate = findNextSchedulableItem(
                    items,
                    remainingByItem,
                    currentDate,
                    dayOfWeek,
                    dayExclusiveSubjects,
                    subjectsScheduledToday,
                    lessonsCountBySubjectToday,
                    subjectConsecutiveHours,
                    rulesBySubject,
                    instructors,
                    rooms,
                    dutyDatesByInstructor,
                    occupiedSlotIndexes,
                    subjectBySlotIndex,
                    candidateSlotIndex,
                    availableSlots,
                    lastScheduledSubject,
                    lastSubjectStreakHours,
                    candidateStartIndex
                );
                if (candidate == null) {
                    continue;
                }
                int itemIndex = candidate.itemIndex;
                ScheduleItem currentItem = items.get(itemIndex);
                int blockHours = candidate.blockHours;
                String normalizedSubject = normalizeSubject(currentItem.getTopic());

                if (
                    !canPlaceConsecutiveBlock(
                        occupiedSlotIndexes,
                        candidateSlotIndex,
                        blockHours
                    ) ||
                    availableSlots < blockHours
                ) {
                    continue;
                }

                Lesson lesson = new Lesson(
                    currentItem.getTopic(),
                    currentItem.getLessonName(),
                    currentItem.getClassName(),
                    true,
                    candidateTime,
                    candidate.roomName,
                    candidate.instructorName,
                    currentDate
                );
                lesson.setCalendarId(currentCalendarId);
                lesson.setDurationHours(blockHours);
                lessonRepository.save(lesson);

                createdLessons++;
                totalHours -= blockHours;
                availableSlots -= blockHours;
                markConsecutiveBlockOccupied(
                    occupiedSlotIndexes,
                    candidateSlotIndex,
                    blockHours
                );
                markSubjectBlock(
                    subjectBySlotIndex,
                    candidateSlotIndex,
                    blockHours,
                    normalizedSubject
                );
                if (!normalizedSubject.isEmpty()) {
                    subjectsScheduledToday.add(normalizedSubject);
                    lessonsCountBySubjectToday.merge(
                        normalizedSubject,
                        1,
                        Integer::sum
                    );
                }
                lastDate = currentDate;
                remainingHoursByItemId.merge(
                    currentItem.getId(),
                    -blockHours,
                    Integer::sum
                );
                createdToday++;
                candidateStartIndex = (itemIndex + 1) % items.size();

                if (normalizedSubject.equals(lastScheduledSubject)) {
                    lastSubjectStreakHours += blockHours;
                } else {
                    lastScheduledSubject = normalizedSubject;
                    lastSubjectStreakHours = blockHours;
                }

                remainingByItem[itemIndex] -= blockHours;
                if (remainingByItem[itemIndex] <= 0) {
                    completedItemIds.add(currentItem.getId());
                }
            }

            if (createdToday == 0) {
                daysWithoutProgress++;
                if (daysWithoutProgress > 60) {
                    break;
                }
            } else {
                daysWithoutProgress = 0;
            }
            currentDate = currentDate.plusDays(1);
        }

        if (!completedItemIds.isEmpty()) {
            scheduleItemRepository.deleteAllByIds(new ArrayList<>(completedItemIds));
        }

        List<AutoScheduleResult.RemainingScheduleItem> remainingItems =
            new ArrayList<>();
        for (ScheduleItem item : items) {
            int remaining = remainingHoursByItemId.getOrDefault(item.getId(), 0);
            if (remaining <= 0) {
                continue;
            }
            if (!completedItemIds.contains(item.getId()) && remaining != item.getHours()) {
                item.setHours(remaining);
                scheduleItemRepository.save(item);
            }
            remainingItems.add(
                new AutoScheduleResult.RemainingScheduleItem(
                    item.getTopic(),
                    item.getLessonName(),
                    item.getClassName(),
                    remaining
                )
            );
        }

        return new AutoScheduleResult(
            createdLessons,
            lastDate,
            totalHours,
            remainingItems
        );
    }

    private Map<String, SubjectScheduleRule> toRuleMap(
        List<SubjectScheduleRule> rules
    ) {
        Map<String, SubjectScheduleRule> map = new HashMap<>();
        if (rules == null) {
            return map;
        }
        for (SubjectScheduleRule rule : rules) {
            if (rule == null || rule.getSubject().isBlank()) {
                continue;
            }
            map.put(normalizeSubject(rule.getSubject()), rule);
        }
        return map;
    }

    /**
     * Нормализует название предмета для сопоставления и сравнений.
     */
    private String normalizeSubject(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    /**
     * Подбирает лучший валидный элемент для текущего слота с приоритетом чередования предметов.
     */
    private AssignmentCandidate findNextSchedulableItem(
        List<ScheduleItem> items,
        int[] remainingByItem,
        LocalDate date,
        DayOfWeek day,
        Set<String> dayExclusiveSubjects,
        Set<String> subjectsScheduledToday,
        Map<String, Integer> lessonsCountBySubjectToday,
        Map<String, Integer> subjectConsecutiveHours,
        Map<String, SubjectScheduleRule> rulesBySubject,
        List<InstructorProfile> instructors,
        List<RoomProfile> rooms,
        Map<String, Set<LocalDate>> dutyDatesByInstructor,
        Set<Integer> occupiedSlotIndexes,
        Map<Integer, String> subjectBySlotIndex,
        int candidateSlotIndex,
        int availableSlots,
        String lastScheduledSubject,
        int lastSubjectStreakHours,
        int candidateStartIndex
    ) {
        if (items.isEmpty()) {
            return null;
        }

        int normalizedStartIndex = Math.floorMod(candidateStartIndex, items.size());
        AssignmentCandidate preferDifferentSubject = null;
        AssignmentCandidate preferTwoHourMix = null;
        AssignmentCandidate fallbackCandidate = null;

        // Приоритет выбора:
        // 1) другой предмет (чтобы не ставить одинаковые подряд),
        // 2) тот же предмет, но с ограничением непрерывной серии до 2 часов,
        // 3) fallback на любого валидного кандидата, чтобы не остановить планирование.
        for (int offset = 0; offset < items.size(); offset++) {
            int i = (normalizedStartIndex + offset) % items.size();
            if (remainingByItem[i] <= 0) {
                continue;
            }
            ScheduleItem item = items.get(i);
            String normalizedSubject = normalizeSubject(item.getTopic());
            int subjectBlockHours = subjectConsecutiveHours.getOrDefault(
                normalizedSubject,
                1
            );
            int baseBlockHours = Math.min(subjectBlockHours, remainingByItem[i]);
            if (
                baseBlockHours > availableSlots ||
                !canPlaceConsecutiveBlock(
                    occupiedSlotIndexes,
                    candidateSlotIndex,
                    baseBlockHours
                )
            ) {
                continue;
            }
            if (
                !dayExclusiveSubjects.isEmpty() &&
                !dayExclusiveSubjects.contains(normalizedSubject)
            ) {
                continue;
            }
            SubjectScheduleRule rule = rulesBySubject.get(normalizedSubject);
            if (rule != null && !rule.isAllowedOn(day)) {
                continue;
            }
            if (
                exceedsDailyLessonsLimit(
                    normalizedSubject,
                    rule,
                    lessonsCountBySubjectToday
                )
            ) {
                continue;
            }
            if (
                violatesSameDayCompatibility(
                    normalizedSubject,
                    subjectsScheduledToday,
                    rulesBySubject
                )
            ) {
                continue;
            }
            String roomName = resolveRoomForSubject(rule, rooms);
            if (roomName == null) {
                continue;
            }
            if (
                violatesAdjacentCompatibility(
                    normalizedSubject,
                    candidateSlotIndex,
                    baseBlockHours,
                    subjectBySlotIndex,
                    rulesBySubject
                )
            ) {
                continue;
            }

            AssignmentCandidate fullCandidate = buildCandidate(
                i,
                baseBlockHours,
                date,
                day,
                candidateSlotIndex,
                instructors,
                dutyDatesByInstructor,
                roomName
            );
            if (fullCandidate == null) {
                continue;
            }
            if (fallbackCandidate == null) {
                fallbackCandidate = fullCandidate;
            }

            if (!normalizedSubject.equals(lastScheduledSubject)) {
                if (preferDifferentSubject == null) {
                    preferDifferentSubject = fullCandidate;
                }
                continue;
            }

            int roomForMix = Math.max(0, 2 - lastSubjectStreakHours);
            int mixedBlockHours = Math.min(baseBlockHours, roomForMix);
            if (mixedBlockHours <= 0) {
                continue;
            }
            if (
                violatesAdjacentCompatibility(
                    normalizedSubject,
                    candidateSlotIndex,
                    mixedBlockHours,
                    subjectBySlotIndex,
                    rulesBySubject
                )
            ) {
                continue;
            }
            AssignmentCandidate mixedCandidate = buildCandidate(
                i,
                mixedBlockHours,
                date,
                day,
                candidateSlotIndex,
                instructors,
                dutyDatesByInstructor,
                roomName
            );
            if (mixedCandidate != null && preferTwoHourMix == null) {
                preferTwoHourMix = mixedCandidate;
            }
        }

        if (preferDifferentSubject != null) {
            return preferDifferentSubject;
        }
        if (preferTwoHourMix != null) {
            return preferTwoHourMix;
        }
        return fallbackCandidate;
    }

    /**
     * Формирует кандидата на назначение, включая проверку доступности преподавателя.
     */
    private AssignmentCandidate buildCandidate(
        int itemIndex,
        int blockHours,
        LocalDate date,
        DayOfWeek day,
        int candidateSlotIndex,
        List<InstructorProfile> instructors,
        Map<String, Set<LocalDate>> dutyDatesByInstructor,
        String roomName
    ) {
        if (blockHours <= 0) {
            return null;
        }
        String instructorName = pickAvailableInstructor(
            date,
            day,
            candidateSlotIndex,
            blockHours,
            instructors,
            dutyDatesByInstructor
        );
        if (instructorName == null) {
            return null;
        }
        return new AssignmentCandidate(
            itemIndex,
            blockHours,
            instructorName,
            roomName
        );
    }

    /**
     * Выбирает кабинет по правилу предмета или возвращает кабинет по умолчанию.
     */
    private String resolveRoomForSubject(
        SubjectScheduleRule rule,
        List<RoomProfile> rooms
    ) {
        if (rule != null && rule.getFixedRoom() != null && !rule.getFixedRoom().isBlank()) {
            for (RoomProfile room : rooms) {
                if (rule.getFixedRoom().equalsIgnoreCase(room.getName())) {
                    return room.getName();
                }
            }
            return null;
        }
        if (rooms.isEmpty()) {
            return "Без кабинета";
        }
        return rooms.get(0).getName();
    }

    /**
     * Проверяет конфликты предмета с уже размещёнными предметами текущего дня.
     */
    private boolean violatesSameDayCompatibility(
        String candidateSubject,
        Set<String> subjectsScheduledToday,
        Map<String, SubjectScheduleRule> rulesBySubject
    ) {
        if (
            candidateSubject == null ||
            candidateSubject.isBlank() ||
            subjectsScheduledToday == null ||
            subjectsScheduledToday.isEmpty()
        ) {
            return false;
        }
        for (String placedSubject : subjectsScheduledToday) {
            if (
                hasMutualConflictForSameDay(
                    candidateSubject,
                    placedSubject,
                    rulesBySubject
                )
            ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Проверяет, превышен ли лимит числа занятий предмета за день.
     */
    private boolean exceedsDailyLessonsLimit(
        String candidateSubject,
        SubjectScheduleRule rule,
        Map<String, Integer> lessonsCountBySubjectToday
    ) {
        if (
            rule == null ||
            candidateSubject == null ||
            candidateSubject.isBlank()
        ) {
            return false;
        }
        int maxLessonsPerDay = Math.max(0, rule.getMaxLessonsPerDay());
        if (maxLessonsPerDay <= 0) {
            return false;
        }
        int currentCount = lessonsCountBySubjectToday.getOrDefault(
            candidateSubject,
            0
        );
        return currentCount >= maxLessonsPerDay;
    }

    /**
     * Проверяет конфликт предмета с соседними слотами слева и справа.
     */
    private boolean violatesAdjacentCompatibility(
        String candidateSubject,
        int startSlotIndex,
        int blockHours,
        Map<Integer, String> subjectBySlotIndex,
        Map<String, SubjectScheduleRule> rulesBySubject
    ) {
        // Проверяем конфликт только с соседями блока:
        // слева от начала и справа от конца.
        if (
            candidateSubject == null ||
            candidateSubject.isBlank() ||
            subjectBySlotIndex == null ||
            subjectBySlotIndex.isEmpty()
        ) {
            return false;
        }
        String leftNeighbor = subjectBySlotIndex.get(startSlotIndex - 1);
        if (
            leftNeighbor != null &&
            hasMutualConflictForConsecutive(
                candidateSubject,
                leftNeighbor,
                rulesBySubject
            )
        ) {
            return true;
        }
        String rightNeighbor = subjectBySlotIndex.get(startSlotIndex + blockHours);
        return rightNeighbor != null &&
        hasMutualConflictForConsecutive(
            candidateSubject,
            rightNeighbor,
            rulesBySubject
        );
    }

    /**
     * Проверяет взаимный запрет предметов на размещение подряд.
     */
    private boolean hasMutualConflictForConsecutive(
        String subjectA,
        String subjectB,
        Map<String, SubjectScheduleRule> rulesBySubject
    ) {
        if (subjectA == null || subjectB == null) {
            return false;
        }
        SubjectScheduleRule ruleA = rulesBySubject.get(subjectA);
        SubjectScheduleRule ruleB = rulesBySubject.get(subjectB);
        return (ruleA != null && ruleA.getNoConsecutiveWithSubjects().contains(subjectB)) ||
        (ruleB != null && ruleB.getNoConsecutiveWithSubjects().contains(subjectA));
    }

    /**
     * Проверяет взаимный запрет предметов на размещение в один день.
     */
    private boolean hasMutualConflictForSameDay(
        String subjectA,
        String subjectB,
        Map<String, SubjectScheduleRule> rulesBySubject
    ) {
        if (subjectA == null || subjectB == null) {
            return false;
        }
        SubjectScheduleRule ruleA = rulesBySubject.get(subjectA);
        SubjectScheduleRule ruleB = rulesBySubject.get(subjectB);
        return (ruleA != null && ruleA.getNoSameDayWithSubjects().contains(subjectB)) ||
        (ruleB != null && ruleB.getNoSameDayWithSubjects().contains(subjectA));
    }

    /**
     * Помечает занятые слоты соответствующим предметом для проверок совместимости.
     */
    private void markSubjectBlock(
        Map<Integer, String> subjectBySlotIndex,
        int startSlotIndex,
        int durationHours,
        String normalizedSubject
    ) {
        if (
            normalizedSubject == null ||
            normalizedSubject.isBlank() ||
            durationHours <= 0
        ) {
            return;
        }
        for (int i = 0; i < durationHours; i++) {
            subjectBySlotIndex.put(startSlotIndex + i, normalizedSubject);
        }
    }

    /**
     * Выбирает первого подходящего преподавателя с учётом ограничений дня и дежурств.
     */
    private String pickAvailableInstructor(
        LocalDate date,
        DayOfWeek day,
        int slotIndex,
        int blockHours,
        List<InstructorProfile> instructors,
        Map<String, Set<LocalDate>> dutyDatesByInstructor
    ) {
        if (instructors.isEmpty()) {
            return "Без преподавателя";
        }
        for (InstructorProfile instructor : instructors) {
            if (instructor == null || instructor.getName().isBlank()) {
                continue;
            }
            if (!instructor.isAllowedOn(day)) {
                continue;
            }
            Set<LocalDate> dutyDates = dutyDatesByInstructor.getOrDefault(
                instructor.getId(),
                Set.of()
            );
            if (dutyDates.contains(date.plusDays(-1))) {
                continue;
            }
            if (dutyDates.contains(date) && slotIndex + blockHours > 4) {
                continue;
            }
            return instructor.getName();
        }
        return null;
    }

    private Map<String, Set<LocalDate>> buildDutyDatesByInstructor(
        List<InstructorDuty> duties
    ) {
        Map<String, Set<LocalDate>> result = new HashMap<>();
        if (duties == null) {
            return result;
        }
        for (InstructorDuty duty : duties) {
            if (
                duty == null ||
                duty.getInstructorId() == null ||
                duty.getInstructorId().isBlank() ||
                duty.getDutyDate() == null
            ) {
                continue;
            }
            result
                .computeIfAbsent(duty.getInstructorId(), key -> new HashSet<>())
                .add(duty.getDutyDate());
        }
        return result;
    }

    private Map<String, Integer> buildSubjectConsecutiveHours(
        List<ScheduleItem> items,
        Map<String, SubjectScheduleRule> rulesBySubject
    ) {
        Map<String, Integer> result = new HashMap<>();
        for (ScheduleItem item : items) {
            result.merge(
                normalizeSubject(item.getTopic()),
                Math.max(1, item.getConsecutiveHours()),
                Math::max
            );
        }
        if (rulesBySubject == null || rulesBySubject.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, SubjectScheduleRule> entry : rulesBySubject.entrySet()) {
            SubjectScheduleRule rule = entry.getValue();
            if (rule == null) {
                continue;
            }
            result.put(entry.getKey(), Math.max(1, rule.getConsecutiveHours()));
        }
        return result;
    }

    /**
     * Проверяет, что блок часов помещается в свободные слоты без пересечений.
     */
    private boolean canPlaceConsecutiveBlock(
        Set<Integer> occupiedSlotIndexes,
        int startSlotIndex,
        int durationHours
    ) {
        if (durationHours <= 0) {
            return false;
        }
        int endExclusive = startSlotIndex + durationHours;
        if (endExclusive > LESSON_SLOT_START_TIMES.size()) {
            return false;
        }
        for (int i = startSlotIndex; i < endExclusive; i++) {
            if (occupiedSlotIndexes.contains(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Помечает слоты занятыми после размещения блока занятия.
     */
    private void markConsecutiveBlockOccupied(
        Set<Integer> occupiedSlotIndexes,
        int startSlotIndex,
        int durationHours
    ) {
        for (int i = 0; i < durationHours; i++) {
            occupiedSlotIndexes.add(startSlotIndex + i);
        }
    }

    /**
     * Возвращает предметы, эксклюзивно разрешённые для выбранного дня.
     */
    private Set<String> getExclusiveSubjectsForDay(
        DayOfWeek day,
        Map<String, SubjectScheduleRule> rulesBySubject
    ) {
        Set<String> subjects = new HashSet<>();
        for (Map.Entry<String, SubjectScheduleRule> entry : rulesBySubject.entrySet()) {
            if (entry.getValue().isExclusiveOn(day)) {
                subjects.add(entry.getKey());
            }
        }
        return subjects;
    }

    /**
     * Проверяет корректность входного диапазона дат.
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException(
                "Дата начала не может быть пустой"
            );
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                "Дата окончания не может быть раньше даты начала"
            );
        }
    }

    /**
     * Возвращает занятия в диапазоне или от даты начала при открытом конце.
     */
    private List<Lesson> findLessonsInRange(LocalDate startDate, LocalDate endDate) {
        if (endDate != null) {
            return lessonRepository.findByDateRange(
                startDate,
                endDate,
                currentCalendarId
            );
        }
        List<Lesson> lessons = lessonRepository.findAll(currentCalendarId);
        List<Lesson> filtered = new ArrayList<>();
        for (Lesson lesson : lessons) {
            if (!lesson.getDate().isBefore(startDate)) {
                filtered.add(lesson);
            }
        }
        return filtered;
    }

    /**
     * Возвращает авто-созданные и неархивные занятия в диапазоне.
     */
    private List<Lesson> findAutoScheduledLessonsInRange(
        LocalDate startDate,
        LocalDate endDate
    ) {
        List<Lesson> lessons = findLessonsInRange(startDate, endDate);
        List<Lesson> autoLessons = new ArrayList<>();
        for (Lesson lesson : lessons) {
            if (lesson.isAutoScheduled() && !lesson.isArchived()) {
                autoLessons.add(lesson);
            }
        }
        return autoLessons;
    }

    /**
     * Возвращает авто-занятия, которые должны участвовать в перераспределении.
     */
    private List<Lesson> findAutoScheduledLessonsForReschedule(
        LocalDate startDate,
        LocalDate endDate
    ) {
        List<Lesson> autoLessons = findAutoScheduledLessonsInRange(
            startDate,
            endDate
        );
        if (endDate == null) {
            return autoLessons;
        }

        List<Lesson> allLessons = lessonRepository.findAll(currentCalendarId);
        for (Lesson lesson : allLessons) {
            if (
                lesson.isAutoScheduled() &&
                !lesson.isArchived() &&
                lesson.getDate().isAfter(endDate)
            ) {
                autoLessons.add(lesson);
            }
        }
        return autoLessons;
    }

    /**
     * Проверяет корректность элемента пула распределения перед сохранением.
     */
    private void validateItem(ScheduleItem item) {
        if (item == null) {
            throw new IllegalArgumentException(
                "Элемент списка не может быть null"
            );
        }
        if (item.getTopic() == null || item.getTopic().trim().isEmpty()) {
            throw new IllegalArgumentException("Предмет не может быть пустым");
        }
        if (
            item.getLessonName() == null ||
            item.getLessonName().trim().isEmpty()
        ) {
            throw new IllegalArgumentException("Тема не может быть пустой");
        }
        if (
            item.getClassName() == null ||
            item.getClassName().trim().isEmpty()
        ) {
            throw new IllegalArgumentException("Занятие не может быть пустым");
        }
        if (item.getHours() <= 0) {
            throw new IllegalArgumentException("Часы должны быть больше 0");
        }
        if (item.getConsecutiveHours() <= 0) {
            throw new IllegalArgumentException(
                "Часы подряд должны быть больше 0"
            );
        }
    }

    private static final class ScheduleKey {
        private final String topic;
        private final String lessonName;
        private final String className;
        private final String location;
        private final String instructor;

        /**
     * Создаёт ключ агрегации для объединения однотипных занятий и элементов пула.
     */
        private ScheduleKey(
            String topic,
            String lessonName,
            String className,
            String location,
            String instructor
        ) {
            this.topic = topic;
            this.lessonName = lessonName;
            this.className = className;
            this.location = location;
            this.instructor = instructor;
        }

        /**
     * Сравнивает ключи агрегации по всем полям.
     */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ScheduleKey)) {
                return false;
            }
            ScheduleKey other = (ScheduleKey) obj;
            return topic.equals(other.topic) &&
            lessonName.equals(other.lessonName) &&
            className.equals(other.className) &&
            location.equals(other.location) &&
            instructor.equals(other.instructor);
        }

        /**
     * Возвращает хэш ключа агрегации для map/set структур.
     */
        @Override
        public int hashCode() {
            int result = topic.hashCode();
            result = 31 * result + lessonName.hashCode();
            result = 31 * result + className.hashCode();
            result = 31 * result + location.hashCode();
            result = 31 * result + instructor.hashCode();
            return result;
        }
    }

    private static final class AssignmentCandidate {
        private final int itemIndex;
        private final int blockHours;
        private final String instructorName;
        private final String roomName;

        /**
     * Создаёт структуру кандидата на размещение в слот.
     */
        private AssignmentCandidate(
            int itemIndex,
            int blockHours,
            String instructorName,
            String roomName
        ) {
            this.itemIndex = itemIndex;
            this.blockHours = blockHours;
            this.instructorName = instructorName;
            this.roomName = roomName;
        }
    }

    public static final class HistoryEntry {
        private final String id;
        private final LocalDateTime createdAt;
        private final String label;

        /**
     * Создаёт DTO записи истории для отображения снимков в интерфейсе.
     */
        public HistoryEntry(String id, LocalDateTime createdAt, String label) {
            this.id = id;
            this.createdAt = createdAt;
            this.label = label;
        }

        /**
     * Возвращает идентификатор записи истории.
     */
        public String getId() {
            return id;
        }

        /**
     * Возвращает время создания записи истории.
     */
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        /**
     * Возвращает подпись записи истории.
     */
        public String getLabel() {
            return label;
        }

        /**
     * Возвращает человекочитаемое представление записи истории.
     */
        @Override
        public String toString() {
            return createdAt + " — " + label;
        }
    }

    /**
     * Сериализует список занятий в табличный текст для сохранения в истории.
     */
    private String serializeLessons(List<Lesson> lessons) {
        StringBuilder sb = new StringBuilder();
        for (Lesson lesson : lessons) {
            sb
                .append(encode(lesson.getId()))
                .append('\t')
                .append(encode(lesson.getTopic()))
                .append('\t')
                .append(encode(lesson.getLessonName()))
                .append('\t')
                .append(encode(lesson.getClassName()))
                .append('\t')
                .append(lesson.isAutoScheduled() ? "1" : "0")
                .append('\t')
                .append(lesson.isArchived() ? "1" : "0")
                .append('\t')
                .append(Math.max(1, lesson.getDurationHours()))
                .append('\t')
                .append(lesson.getTime())
                .append('\t')
                .append(encode(lesson.getLocation()))
                .append('\t')
                .append(encode(lesson.getInstructor()))
                .append('\t')
                .append(lesson.getDate())
                .append('\n');
        }
        return sb.toString();
    }

    /**
     * Сериализует пул элементов расписания в текстовый формат истории.
     */
    private String serializeScheduleItems(List<ScheduleItem> items) {
        StringBuilder sb = new StringBuilder();
        for (ScheduleItem item : items) {
            sb
                .append(encode(item.getId()))
                .append('\t')
                .append(encode(item.getTopic()))
                .append('\t')
                .append(encode(item.getLessonName()))
                .append('\t')
                .append(encode(item.getClassName()))
                .append('\t')
                .append(encode(item.getLocation()))
                .append('\t')
                .append(encode(item.getInstructor()))
                .append('\t')
                .append(item.getHours())
                .append('\t')
                .append(Math.max(1, item.getConsecutiveHours()))
                .append('\t')
                .append(item.getCreatedAt())
                .append('\n');
        }
        return sb.toString();
    }

    /**
     * Восстанавливает занятия из сериализованного текстового представления.
     */
    private List<Lesson> deserializeLessons(String blob) {
        List<Lesson> lessons = new ArrayList<>();
        if (blob == null || blob.isBlank()) {
            return lessons;
        }
        String[] lines = blob.split("\\R");
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            if (parts.length < 10) {
                continue;
            }
            Lesson lesson = new Lesson();
            lesson.setId(decode(parts[0]));
            lesson.setTopic(decode(parts[1]));
            lesson.setLessonName(decode(parts[2]));
            lesson.setClassName(decode(parts[3]));
            lesson.setAutoScheduled("1".equals(parts[4]));
            lesson.setArchived("1".equals(parts[5]));
            if (parts.length >= 11) {
                lesson.setDurationHours(Math.max(1, Integer.parseInt(parts[6])));
                lesson.setTime(LocalTime.parse(parts[7]));
                lesson.setLocation(decode(parts[8]));
                lesson.setInstructor(decode(parts[9]));
                lesson.setDate(LocalDate.parse(parts[10]));
            } else {
                lesson.setDurationHours(1);
                lesson.setTime(LocalTime.parse(parts[6]));
                lesson.setLocation(decode(parts[7]));
                lesson.setInstructor(decode(parts[8]));
                lesson.setDate(LocalDate.parse(parts[9]));
            }
            lessons.add(lesson);
        }
        return lessons;
    }

    /**
     * Восстанавливает элементы пула из сериализованного текста.
     */
    private List<ScheduleItem> deserializeScheduleItems(String blob) {
        List<ScheduleItem> items = new ArrayList<>();
        if (blob == null || blob.isBlank()) {
            return items;
        }
        String[] lines = blob.split("\\R");
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            if (parts.length < 8) {
                continue;
            }
            ScheduleItem item = new ScheduleItem();
            item.setId(decode(parts[0]));
            item.setTopic(decode(parts[1]));
            item.setLessonName(decode(parts[2]));
            item.setClassName(decode(parts[3]));
            item.setLocation(decode(parts[4]));
            item.setInstructor(decode(parts[5]));
            item.setHours(Integer.parseInt(parts[6]));
            if (parts.length >= 9) {
                item.setConsecutiveHours(
                    Math.max(1, Integer.parseInt(parts[7]))
                );
                item.setCreatedAt(LocalDateTime.parse(parts[8]));
            } else {
                item.setConsecutiveHours(1);
                item.setCreatedAt(LocalDateTime.parse(parts[7]));
            }
            items.add(item);
        }
        return items;
    }

    /**
     * Кодирует значение в Base64 для безопасного хранения в snapshot-строках.
     */
    private String encode(String value) {
        String safe = value == null ? "" : value;
        return Base64
            .getEncoder()
            .encodeToString(safe.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Декодирует Base64-значение, поддерживая совместимость со старыми форматами.
     */
    private String decode(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return new String(
            Base64.getDecoder().decode(value),
            StandardCharsets.UTF_8
        );
    }

    /**
     * Формирует ключ агрегации из занятия.
     */
    private ScheduleKey scheduleKeyOfLesson(Lesson lesson) {
        return new ScheduleKey(
            safeText(lesson.getTopic()),
            safeText(lesson.getLessonName()),
            safeText(lesson.getClassName()),
            safeText(lesson.getLocation()),
            safeText(lesson.getInstructor())
        );
    }

    /**
     * Формирует ключ агрегации из элемента пула.
     */
    private ScheduleKey scheduleKeyOfItem(ScheduleItem item) {
        return new ScheduleKey(
            safeText(item.getTopic()),
            safeText(item.getLessonName()),
            safeText(item.getClassName()),
            safeText(item.getLocation()),
            safeText(item.getInstructor())
        );
    }

    /**
     * Нормализует null-строку к пустому тексту.
     */
    private String safeText(String value) {
        return value == null ? "" : value;
    }

    /**
     * Находит целевое значение подрядных часов для предмета из правил и пула.
     */
    private int resolveSubjectConsecutiveHours(String topic) {
        String subjectKey = normalizeSubject(topic);
        int result = 1;
        for (ScheduleItem item : scheduleItemRepository.findAll(currentCalendarId)) {
            if (!subjectKey.equals(normalizeSubject(item.getTopic()))) {
                continue;
            }
            result = Math.max(result, Math.max(1, item.getConsecutiveHours()));
        }
        return result;
    }

    /**
     * Синхронизирует подрядные часы у всех элементов одного предмета.
     */
    private void syncSubjectConsecutiveHours(
        String topic,
        int consecutiveHours,
        String sourceItemId
    ) {
        String subjectKey = normalizeSubject(topic);
        if (subjectKey.isBlank()) {
            return;
        }
        int normalizedConsecutive = Math.max(1, consecutiveHours);
        for (ScheduleItem existing : scheduleItemRepository.findAll(currentCalendarId)) {
            if (!subjectKey.equals(normalizeSubject(existing.getTopic()))) {
                continue;
            }
            if (sourceItemId != null && sourceItemId.equals(existing.getId())) {
                continue;
            }
            if (existing.getConsecutiveHours() == normalizedConsecutive) {
                continue;
            }
            existing.setConsecutiveHours(normalizedConsecutive);
            scheduleItemRepository.save(existing);
        }
    }
}
