package com.ororura.slseleven.domain.repository;

import com.ororura.slseleven.domain.model.Lesson;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Интерфейс репозитория для работы с занятиями
 */
public interface LessonRepository {
    /**
     * Сохранить или обновить занятие
     */
    void save(Lesson lesson);

    /**
     * Найти занятие по ID
     */
    Optional<Lesson> findById(String id);

    /**
     * Получить все занятия
     */
    List<Lesson> findAll();

    /**
     * Получить занятия на конкретную дату
     */
    List<Lesson> findByDate(LocalDate date);

    /**
     * Получить занятия в диапазоне дат
     */
    List<Lesson> findByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Удалить занятие по ID
     */
    void deleteById(String id);

    /**
     * Удалить все занятия
     */
    void deleteAll();

    /**
     * Проверить существование занятия по ID
     */
    boolean existsById(String id);
}
