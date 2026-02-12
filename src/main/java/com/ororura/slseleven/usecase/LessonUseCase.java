package com.ororura.slseleven.usecase;

import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.domain.repository.LessonRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Use case для работы с занятиями
 * Содержит бизнес-логику приложения
 */
public class LessonUseCase {

    private final LessonRepository lessonRepository;
    private String currentCalendarId = Lesson.DEFAULT_CALENDAR_ID;

    public LessonUseCase(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    public void setCurrentCalendarId(String calendarId) {
        if (calendarId == null || calendarId.isBlank()) {
            throw new IllegalArgumentException("ID календаря не может быть пустым");
        }
        this.currentCalendarId = calendarId;
    }

    /**
     * Создать новое занятие
     */
    public void createLesson(Lesson lesson) {
        if (lesson == null) {
            throw new IllegalArgumentException("Занятие не может быть null");
        }
        normalizeDuration(lesson);
        lesson.setCalendarId(currentCalendarId);
        lessonRepository.save(lesson);
    }

    /**
     * Обновить существующее занятие
     */
    public void updateLesson(Lesson lesson) {
        if (lesson == null || lesson.getId() == null) {
            throw new IllegalArgumentException(
                "Занятие или ID не может быть null"
            );
        }
        if (!lessonRepository.existsById(lesson.getId())) {
            throw new IllegalArgumentException(
                "Занятие с ID " + lesson.getId() + " не найдено"
            );
        }
        Lesson existing = lessonRepository.findById(lesson.getId()).orElse(null);
        if (existing == null || !currentCalendarId.equals(existing.getCalendarId())) {
            throw new IllegalArgumentException(
                "Занятие с ID " + lesson.getId() + " не найдено"
            );
        }
        normalizeDuration(lesson);
        lesson.setCalendarId(currentCalendarId);
        lessonRepository.save(lesson);
    }

    /**
     * Удалить занятие
     */
    public void deleteLesson(String lessonId) {
        if (lessonId == null || lessonId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "ID занятия не может быть пустым"
            );
        }
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null || !currentCalendarId.equals(lesson.getCalendarId())) {
            throw new IllegalArgumentException(
                "Занятие с ID " + lessonId + " не найдено"
            );
        }
        lessonRepository.deleteById(lessonId);
    }

    /**
     * Удалить все занятия
     */
    public void deleteAllLessons() {
        lessonRepository.deleteAll(currentCalendarId);
    }

    public void deleteAutoScheduledLesson(String lessonId) {
        if (lessonId == null || lessonId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "ID занятия не может быть пустым"
            );
        }
        Lesson lesson = lessonRepository
            .findById(lessonId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Занятие с ID " + lessonId + " не найдено"
                )
            );
        if (!currentCalendarId.equals(lesson.getCalendarId())) {
            throw new IllegalArgumentException(
                "Занятие с ID " + lessonId + " не найдено"
            );
        }
        if (!lesson.isAutoScheduled()) {
            throw new IllegalArgumentException(
                "Выбранное занятие не создано автораспределением"
            );
        }
        lessonRepository.deleteById(lessonId);
    }

    public int deleteAllAutoScheduledLessons() {
        List<Lesson> allLessons = lessonRepository.findAll(currentCalendarId);
        int deleted = 0;
        for (Lesson lesson : allLessons) {
            if (lesson.isAutoScheduled()) {
                lessonRepository.deleteById(lesson.getId());
                deleted++;
            }
        }
        return deleted;
    }

    public int archivePastLessons(LocalDate today) {
        if (today == null) {
            throw new IllegalArgumentException("Дата не может быть null");
        }
        List<Lesson> allLessons = lessonRepository.findAll(currentCalendarId);
        int archived = 0;
        for (Lesson lesson : allLessons) {
            if (lesson.isArchived()) {
                continue;
            }
            if (lesson.getDate() != null && lesson.getDate().isBefore(today)) {
                lesson.setArchived(true);
                lessonRepository.save(lesson);
                archived++;
            }
        }
        return archived;
    }

    /**
     * Получить занятие по ID
     */
    public Optional<Lesson> getLessonById(String lessonId) {
        if (lessonId == null || lessonId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "ID занятия не может быть пустым"
            );
        }
        return lessonRepository
            .findById(lessonId)
            .filter(lesson -> currentCalendarId.equals(lesson.getCalendarId()));
    }

    /**
     * Получить все занятия
     */
    public List<Lesson> getAllLessons() {
        List<Lesson> allLessons = lessonRepository.findAll(currentCalendarId);
        List<Lesson> activeLessons = new java.util.ArrayList<>();
        for (Lesson lesson : allLessons) {
            if (!lesson.isArchived()) {
                activeLessons.add(lesson);
            }
        }
        return activeLessons;
    }

    public List<Lesson> getArchivedLessons() {
        List<Lesson> allLessons = lessonRepository.findAll(currentCalendarId);
        List<Lesson> archivedLessons = new java.util.ArrayList<>();
        for (Lesson lesson : allLessons) {
            if (lesson.isArchived()) {
                archivedLessons.add(lesson);
            }
        }
        return archivedLessons;
    }

    /**
     * Получить занятия на конкретную дату
     */
    public List<Lesson> getLessonsByDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Дата не может быть null");
        }
        List<Lesson> lessons = lessonRepository.findByDate(date, currentCalendarId);
        List<Lesson> activeLessons = new java.util.ArrayList<>();
        for (Lesson lesson : lessons) {
            if (!lesson.isArchived()) {
                activeLessons.add(lesson);
            }
        }
        return activeLessons;
    }

    public List<Lesson> getLessonsByDateAllCalendars(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Дата не может быть null");
        }
        List<Lesson> lessons = lessonRepository.findByDateAcrossCalendars(date);
        List<Lesson> activeLessons = new java.util.ArrayList<>();
        for (Lesson lesson : lessons) {
            if (!lesson.isArchived()) {
                activeLessons.add(lesson);
            }
        }
        return activeLessons;
    }

    /**
     * Получить занятия в диапазоне дат
     */
    public List<Lesson> getLessonsByDateRange(
        LocalDate startDate,
        LocalDate endDate
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Даты не могут быть null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                "Начальная дата не может быть позже конечной"
            );
        }
        List<Lesson> lessons = lessonRepository.findByDateRange(startDate, endDate, currentCalendarId);
        List<Lesson> activeLessons = new java.util.ArrayList<>();
        for (Lesson lesson : lessons) {
            if (!lesson.isArchived()) {
                activeLessons.add(lesson);
            }
        }
        return activeLessons;
    }

    public List<Lesson> getLessonsByDateRangeAllCalendars(
        LocalDate startDate,
        LocalDate endDate
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Даты не могут быть null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                "Начальная дата не может быть позже конечной"
            );
        }
        List<Lesson> lessons = lessonRepository.findByDateRangeAcrossCalendars(
            startDate,
            endDate
        );
        List<Lesson> activeLessons = new java.util.ArrayList<>();
        for (Lesson lesson : lessons) {
            if (!lesson.isArchived()) {
                activeLessons.add(lesson);
            }
        }
        return activeLessons;
    }

    private void normalizeDuration(Lesson lesson) {
        if (lesson.getDurationHours() <= 0) {
            lesson.setDurationHours(1);
        }
    }
}
