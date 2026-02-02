package com.ororura.slseleven.usecase;

import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.domain.model.ScheduleItem;
import com.ororura.slseleven.domain.repository.LessonRepository;
import com.ororura.slseleven.domain.repository.ScheduleItemRepository;
import com.ororura.slseleven.domain.repository.ScheduleSettingsRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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

    public ScheduleUseCase(
        LessonRepository lessonRepository,
        ScheduleItemRepository scheduleItemRepository,
        ScheduleSettingsRepository scheduleSettingsRepository
    ) {
        this.lessonRepository = lessonRepository;
        this.scheduleItemRepository = scheduleItemRepository;
        this.scheduleSettingsRepository = scheduleSettingsRepository;
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

        int itemIndex = 0;
        int remainingInItem = items.get(0).getHours();
        Set<String> completedItemIds = new HashSet<>();

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
            int existingCount = existingLessons.size();
            int availableSlots = maxHours - existingCount;
            if (availableSlots <= 0) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            Set<LocalTime> occupiedTimes = new HashSet<>();
            for (Lesson lesson : existingLessons) {
                occupiedTimes.add(lesson.getTime());
            }

            int slotOffset = 0;
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

                remainingInItem--;
                if (remainingInItem <= 0) {
                    completedItemIds.add(currentItem.getId());
                    itemIndex++;
                    if (itemIndex >= items.size()) {
                        break;
                    }
                    remainingInItem = items.get(itemIndex).getHours();
                }
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
}
