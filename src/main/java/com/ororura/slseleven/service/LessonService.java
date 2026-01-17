package com.ororura.slseleven.service;

import com.ororura.slseleven.domain.Lesson;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для управления занятиями
 */
public class LessonService {
    private final Map<String, Lesson> lessons = new HashMap<>();
    private final List<LessonChangeListener> listeners = new ArrayList<>();

    /**
     * Добавить занятие
     */
    public void addLesson(Lesson lesson) {
        if (lesson == null) {
            throw new IllegalArgumentException("Занятие не может быть null");
        }
        lessons.put(lesson.getId(), lesson);
        notifyListeners();
    }

    /**
     * Обновить занятие
     */
    public void updateLesson(Lesson lesson) {
        if (lesson == null || !lessons.containsKey(lesson.getId())) {
            throw new IllegalArgumentException("Занятие не найдено");
        }
        lessons.put(lesson.getId(), lesson);
        notifyListeners();
    }

    /**
     * Удалить занятие
     */
    public void deleteLesson(String lessonId) {
        if (lessonId == null || !lessons.containsKey(lessonId)) {
            throw new IllegalArgumentException("Занятие не найдено");
        }
        lessons.remove(lessonId);
        notifyListeners();
    }

    /**
     * Получить занятие по ID
     */
    public Lesson getLesson(String lessonId) {
        return lessons.get(lessonId);
    }

    /**
     * Получить все занятия
     */
    public List<Lesson> getAllLessons() {
        return new ArrayList<>(lessons.values());
    }

    /**
     * Получить занятия на конкретную дату
     */
    public List<Lesson> getLessonsByDate(LocalDate date) {
        return lessons.values().stream()
                .filter(lesson -> lesson.getDate().equals(date))
                .sorted(Comparator.comparing(Lesson::getTime))
                .collect(Collectors.toList());
    }

    /**
     * Получить занятия в диапазоне дат
     */
    public List<Lesson> getLessonsByDateRange(LocalDate startDate, LocalDate endDate) {
        return lessons.values().stream()
                .filter(lesson -> !lesson.getDate().isBefore(startDate) && !lesson.getDate().isAfter(endDate))
                .sorted(Comparator.comparing(Lesson::getDate).thenComparing(Lesson::getTime))
                .collect(Collectors.toList());
    }

    /**
     * Добавить слушателя изменений
     */
    public void addListener(LessonChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Удалить слушателя изменений
     */
    public void removeListener(LessonChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        listeners.forEach(LessonChangeListener::onLessonsChanged);
    }

    /**
     * Интерфейс для слушателей изменений занятий
     */
    @FunctionalInterface
    public interface LessonChangeListener {
        void onLessonsChanged();
    }
}
