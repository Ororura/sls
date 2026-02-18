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
     * Получить все занятия в календаре
     */
    List<Lesson> findAll(String calendarId);

    /**
     * Метод findAllAcrossCalendars.
     */
    List<Lesson> findAllAcrossCalendars();

    /**
     * Получить занятия на конкретную дату в календаре
     */
    List<Lesson> findByDate(LocalDate date, String calendarId);

    /**
     * Метод findByDateAcrossCalendars.
     */
    List<Lesson> findByDateAcrossCalendars(LocalDate date);

    /**
     * Получить занятия в диапазоне дат в календаре
     */
    List<Lesson> findByDateRange(
        LocalDate startDate,
        LocalDate endDate,
        String calendarId
    );

    /**
     * Метод findByDateRangeAcrossCalendars.
     */
    List<Lesson> findByDateRangeAcrossCalendars(
        LocalDate startDate,
        LocalDate endDate
    );

    /**
     * Удалить занятие по ID
     */
    void deleteById(String id);

    /**
     * Удалить все занятия календаря
     */
    void deleteAll(String calendarId);

    /**
     * Проверить существование занятия по ID
     */
    boolean existsById(String id);
}
