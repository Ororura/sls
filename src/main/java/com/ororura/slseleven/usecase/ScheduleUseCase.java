package com.ororura.slseleven.usecase;

import com.ororura.slseleven.domain.model.Lesson;
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
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private final LessonRepository lessonRepository;
    private final ScheduleItemRepository scheduleItemRepository;
    private final ScheduleSettingsRepository scheduleSettingsRepository;
    private final ScheduleHistoryRepository scheduleHistoryRepository;

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
    }

    public List<ScheduleItem> getAllItems() {
        return scheduleItemRepository.findAll();
    }

    public void createItem(ScheduleItem item) {
        validateItem(item);
        scheduleItemRepository.save(item);
    }

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
        scheduleItemRepository.save(item);
    }

    public void deleteItem(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "ID элемента списка не может быть пустым"
            );
        }
        scheduleItemRepository.deleteById(id);
    }

    public void deleteAllItems() {
        scheduleItemRepository.deleteAll();
    }

    public Map<DayOfWeek, Integer> getMaxHoursByDay() {
        return scheduleSettingsRepository.getMaxHoursByDay();
    }

    public List<SubjectScheduleRule> getSubjectRules() {
        return scheduleSettingsRepository.getSubjectRules();
    }

    public void saveSubjectRules(List<SubjectScheduleRule> rules) {
        scheduleSettingsRepository.saveSubjectRules(rules);
    }

    public List<HistoryEntry> getHistoryEntries() {
        List<ScheduleHistorySnapshot> snapshots = scheduleHistoryRepository.findAll();
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

    public boolean createHistorySnapshot(String label) {
        List<Lesson> lessons = lessonRepository.findAll();
        List<ScheduleItem> items = scheduleItemRepository.findAll();
        if (lessons.isEmpty() && items.isEmpty()) {
            return false;
        }

        String snapshotId = java.util.UUID.randomUUID().toString();
        ScheduleHistorySnapshot snapshot = new ScheduleHistorySnapshot(
            snapshotId,
            LocalDateTime.now(),
            label == null || label.isBlank() ? "Ручной снимок" : label,
            serializeLessons(lessons),
            serializeScheduleItems(items)
        );
        scheduleHistoryRepository.save(snapshot);
        return true;
    }

    public boolean restoreFromHistory(String historyId) {
        if (historyId == null || historyId.isBlank()) {
            throw new IllegalArgumentException("ID снимка не может быть пустым");
        }
        ScheduleHistorySnapshot snapshot = scheduleHistoryRepository
            .findById(historyId)
            .orElse(null);
        if (snapshot == null) {
            return false;
        }

        List<Lesson> lessons = deserializeLessons(snapshot.getLessonsBlob());
        List<ScheduleItem> items = deserializeScheduleItems(
            snapshot.getScheduleItemsBlob()
        );

        lessonRepository.deleteAll();
        scheduleItemRepository.deleteAll();

        for (Lesson lesson : lessons) {
            lessonRepository.save(lesson);
        }
        for (ScheduleItem item : items) {
            scheduleItemRepository.save(item);
        }
        return true;
    }

    public void saveMaxHoursByDay(Map<DayOfWeek, Integer> maxHoursByDay) {
        scheduleSettingsRepository.saveAll(maxHoursByDay);
    }

    public int countLessonsInRange(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return findLessonsInRange(startDate, endDate).size();
    }

    public int moveArchivedLessonsToPool() {
        List<Lesson> allLessons = lessonRepository.findAll();
        Map<ScheduleKey, Integer> aggregatedHours = new LinkedHashMap<>();
        List<String> archivedLessonIds = new ArrayList<>();

        for (Lesson lesson : allLessons) {
            if (!lesson.isArchived()) {
                continue;
            }
            ScheduleKey key = new ScheduleKey(
                lesson.getTopic(),
                lesson.getLessonName(),
                lesson.getClassName(),
                lesson.getLocation(),
                lesson.getInstructor()
            );
            aggregatedHours.merge(key, 1, Integer::sum);
            archivedLessonIds.add(lesson.getId());
        }

        if (aggregatedHours.isEmpty()) {
            return 0;
        }

        Map<ScheduleKey, ScheduleItem> existingItemsByKey = new HashMap<>();
        for (ScheduleItem item : scheduleItemRepository.findAll()) {
            existingItemsByKey.put(
                new ScheduleKey(
                    item.getTopic(),
                    item.getLessonName(),
                    item.getClassName(),
                    item.getLocation(),
                    item.getInstructor()
                ),
                item
            );
        }

        for (Map.Entry<ScheduleKey, Integer> entry : aggregatedHours.entrySet()) {
            ScheduleKey key = entry.getKey();
            int hoursToAdd = entry.getValue();
            ScheduleItem existing = existingItemsByKey.get(key);
            if (existing != null) {
                existing.setHours(existing.getHours() + hoursToAdd);
                scheduleItemRepository.save(existing);
                continue;
            }
            scheduleItemRepository.save(
                new ScheduleItem(
                    key.topic,
                    key.lessonName,
                    key.className,
                    key.location,
                    key.instructor,
                    hoursToAdd
                )
            );
        }

        for (String lessonId : archivedLessonIds) {
            lessonRepository.deleteById(lessonId);
        }
        return archivedLessonIds.size();
    }

    public int countAutoScheduledLessonsInRange(
        LocalDate startDate,
        LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);
        return findAutoScheduledLessonsInRange(startDate, endDate).size();
    }

    public int countAutoScheduledLessonsForReschedule(
        LocalDate startDate,
        LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);
        return findAutoScheduledLessonsForReschedule(startDate, endDate).size();
    }

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

        List<ScheduleItem> queuedItems = scheduleItemRepository.findAll();
        Map<ScheduleKey, Integer> aggregatedHours = new LinkedHashMap<>();

        for (ScheduleItem item : queuedItems) {
            ScheduleKey key = new ScheduleKey(
                item.getTopic(),
                item.getLessonName(),
                item.getClassName(),
                item.getLocation(),
                item.getInstructor()
            );
            aggregatedHours.merge(key, item.getHours(), Integer::sum);
        }

        for (Lesson lesson : lessonsToReschedule) {
            ScheduleKey key = new ScheduleKey(
                lesson.getTopic(),
                lesson.getLessonName(),
                lesson.getClassName(),
                lesson.getLocation(),
                lesson.getInstructor()
            );
            aggregatedHours.merge(key, 1, Integer::sum);
        }

        scheduleItemRepository.deleteAll();
        for (Map.Entry<ScheduleKey, Integer> entry : aggregatedHours.entrySet()) {
            ScheduleKey key = entry.getKey();
            scheduleItemRepository.save(
                new ScheduleItem(
                    key.topic,
                    key.lessonName,
                    key.className,
                    key.location,
                    key.instructor,
                    entry.getValue()
                )
            );
        }

        for (Lesson lesson : lessonsToReschedule) {
            lessonRepository.deleteById(lesson.getId());
        }

        return autoSchedule(startDate, endDate);
    }

    public AutoScheduleResult autoSchedule(LocalDate startDate) {
        return autoSchedule(startDate, null);
    }

    public AutoScheduleResult autoSchedule(
        LocalDate startDate,
        LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);

        List<ScheduleItem> items = scheduleItemRepository.findAll();
        if (items.isEmpty()) {
            return new AutoScheduleResult(0, null, 0, List.of());
        }

        Map<DayOfWeek, Integer> maxHoursByDay = new EnumMap<>(
            scheduleSettingsRepository.getMaxHoursByDay()
        );
        Map<String, SubjectScheduleRule> rulesBySubject = toRuleMap(
            scheduleSettingsRepository.getSubjectRules()
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
        int daysWithoutProgress = 0;

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
                currentDate
            );
            existingLessons.removeIf(Lesson::isArchived);
            Set<String> dayExclusiveSubjects = getExclusiveSubjectsForDay(
                dayOfWeek,
                rulesBySubject
            );
            int existingCount = existingLessons.size();
            int availableSlots = maxHours - existingCount;
            if (availableSlots <= 0) {
                daysWithoutProgress++;
                currentDate = currentDate.plusDays(1);
                continue;
            }

            Set<LocalTime> occupiedTimes = new HashSet<>();
            for (Lesson lesson : existingLessons) {
                occupiedTimes.add(lesson.getTime());
            }

            int slotOffset = 0;
            int createdToday = 0;
            while (
                availableSlots > 0 &&
                totalHours > 0 &&
                slotOffset < LESSON_SLOT_START_TIMES.size()
            ) {
                LocalTime candidateTime = LESSON_SLOT_START_TIMES.get(
                    slotOffset++
                );

                if (occupiedTimes.contains(candidateTime)) {
                    continue;
                }

                int itemIndex = findNextSchedulableItemIndex(
                    items,
                    remainingByItem,
                    dayOfWeek,
                    dayExclusiveSubjects,
                    rulesBySubject
                );
                if (itemIndex < 0) {
                    break;
                }
                ScheduleItem currentItem = items.get(itemIndex);
                Lesson lesson = new Lesson(
                    currentItem.getTopic(),
                    currentItem.getLessonName(),
                    currentItem.getClassName(),
                    true,
                    candidateTime,
                    currentItem.getLocation(),
                    currentItem.getInstructor(),
                    currentDate
                );
                lessonRepository.save(lesson);

                createdLessons++;
                totalHours--;
                availableSlots--;
                occupiedTimes.add(candidateTime);
                lastDate = currentDate;
                remainingHoursByItemId.merge(currentItem.getId(), -1, Integer::sum);
                createdToday++;

                remainingByItem[itemIndex]--;
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

    private String normalizeSubject(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private int findNextSchedulableItemIndex(
        List<ScheduleItem> items,
        int[] remainingByItem,
        DayOfWeek day,
        Set<String> dayExclusiveSubjects,
        Map<String, SubjectScheduleRule> rulesBySubject
    ) {
        for (int i = 0; i < items.size(); i++) {
            if (remainingByItem[i] <= 0) {
                continue;
            }
            ScheduleItem item = items.get(i);
            String normalizedSubject = normalizeSubject(item.getTopic());
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
            return i;
        }
        return -1;
    }

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

    private List<Lesson> findLessonsInRange(LocalDate startDate, LocalDate endDate) {
        if (endDate != null) {
            return lessonRepository.findByDateRange(startDate, endDate);
        }
        List<Lesson> lessons = lessonRepository.findAll();
        List<Lesson> filtered = new ArrayList<>();
        for (Lesson lesson : lessons) {
            if (!lesson.getDate().isBefore(startDate)) {
                filtered.add(lesson);
            }
        }
        return filtered;
    }

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

        List<Lesson> allLessons = lessonRepository.findAll();
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
        if (item.getLocation() == null || item.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Место не может быть пустым");
        }
        if (
            item.getInstructor() == null ||
            item.getInstructor().trim().isEmpty()
        ) {
            throw new IllegalArgumentException(
                "Преподаватель не может быть пустым"
            );
        }
        if (item.getHours() <= 0) {
            throw new IllegalArgumentException("Часы должны быть больше 0");
        }
    }

    private static final class ScheduleKey {
        private final String topic;
        private final String lessonName;
        private final String className;
        private final String location;
        private final String instructor;

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

    public static final class HistoryEntry {
        private final String id;
        private final LocalDateTime createdAt;
        private final String label;

        public HistoryEntry(String id, LocalDateTime createdAt, String label) {
            this.id = id;
            this.createdAt = createdAt;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return createdAt + " — " + label;
        }
    }

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
                .append(item.getCreatedAt())
                .append('\n');
        }
        return sb.toString();
    }

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
            lesson.setTime(LocalTime.parse(parts[6]));
            lesson.setLocation(decode(parts[7]));
            lesson.setInstructor(decode(parts[8]));
            lesson.setDate(LocalDate.parse(parts[9]));
            lessons.add(lesson);
        }
        return lessons;
    }

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
            item.setCreatedAt(LocalDateTime.parse(parts[7]));
            items.add(item);
        }
        return items;
    }

    private String encode(String value) {
        String safe = value == null ? "" : value;
        return Base64
            .getEncoder()
            .encodeToString(safe.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return new String(
            Base64.getDecoder().decode(value),
            StandardCharsets.UTF_8
        );
    }
}
