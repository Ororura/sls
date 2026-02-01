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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScheduleUseCase {
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);
    private static final int MAX_DAILY_SLOTS = 24;

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
            throw new IllegalArgumentException("Элемент списка или ID не может быть null");
        }
        validateItem(item);
        if (!scheduleItemRepository.existsById(item.getId())) {
            throw new IllegalArgumentException("Элемент списка с ID " + item.getId() + " не найден");
        }
        scheduleItemRepository.save(item);
    }

    public void deleteItem(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID элемента списка не может быть пустым");
        }
        scheduleItemRepository.deleteById(id);
    }

    public Map<DayOfWeek, Integer> getMaxHoursByDay() {
        return scheduleSettingsRepository.getMaxHoursByDay();
    }

    public void saveMaxHoursByDay(Map<DayOfWeek, Integer> maxHoursByDay) {
        scheduleSettingsRepository.saveAll(maxHoursByDay);
    }

    public AutoScheduleResult autoSchedule(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Дата начала не может быть пустой");
        }

        List<ScheduleItem> items = scheduleItemRepository.findAll();
        if (items.isEmpty()) {
            return new AutoScheduleResult(0, null, 0);
        }

        Map<DayOfWeek, Integer> maxHoursByDay = new EnumMap<>(scheduleSettingsRepository.getMaxHoursByDay());
        boolean hasCapacity = maxHoursByDay.values().stream().anyMatch(hours -> hours != null && hours > 0);
        if (!hasCapacity) {
            throw new IllegalStateException("Нужно задать часы занятий хотя бы для одного дня недели");
        }

        int totalHours = items.stream().mapToInt(ScheduleItem::getHours).sum();
        int createdLessons = 0;
        LocalDate currentDate = startDate;
        LocalDate lastDate = null;

        int itemIndex = 0;
        int remainingInItem = items.get(0).getHours();
        List<String> completedItemIds = new ArrayList<>();

        while (totalHours > 0) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            int maxHours = maxHoursByDay.getOrDefault(dayOfWeek, 0);
            if (maxHours <= 0) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            List<Lesson> existingLessons = lessonRepository.findByDate(currentDate);
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
            while (availableSlots > 0 && totalHours > 0) {
                if (slotOffset >= MAX_DAILY_SLOTS) {
                    break;
                }

                LocalTime candidateTime = DEFAULT_START_TIME.plusHours(slotOffset);
                slotOffset++;

                if (occupiedTimes.contains(candidateTime)) {
                    continue;
                }

                ScheduleItem currentItem = items.get(itemIndex);
                Lesson lesson = new Lesson(
                    currentItem.getTopic(),
                    currentItem.getLessonName(),
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

        scheduleItemRepository.deleteAllByIds(completedItemIds);
        return new AutoScheduleResult(createdLessons, lastDate, totalHours);
    }

    private void validateItem(ScheduleItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Элемент списка не может быть null");
        }
        if (item.getTopic() == null || item.getTopic().trim().isEmpty()) {
            throw new IllegalArgumentException("Тема не может быть пустой");
        }
        if (item.getLessonName() == null || item.getLessonName().trim().isEmpty()) {
            throw new IllegalArgumentException("Занятие не может быть пустым");
        }
        if (item.getLocation() == null || item.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Место не может быть пустым");
        }
        if (item.getInstructor() == null || item.getInstructor().trim().isEmpty()) {
            throw new IllegalArgumentException("Преподаватель не может быть пустым");
        }
        if (item.getHours() <= 0) {
            throw new IllegalArgumentException("Часы должны быть больше 0");
        }
    }
}
