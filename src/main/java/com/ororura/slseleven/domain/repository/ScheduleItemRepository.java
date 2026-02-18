package com.ororura.slseleven.domain.repository;

import com.ororura.slseleven.domain.model.ScheduleItem;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для элементов списка расписаний.
 */
public interface ScheduleItemRepository {
    /**
     * Метод save.
     */
    void save(ScheduleItem item);

    /**
     * Метод findById.
     */
    Optional<ScheduleItem> findById(String id);

    /**
     * Метод findAll.
     */
    List<ScheduleItem> findAll(String calendarId);

    /**
     * Метод deleteById.
     */
    void deleteById(String id);

    /**
     * Метод deleteAllByIds.
     */
    void deleteAllByIds(List<String> ids);

    /**
     * Метод deleteAll.
     */
    void deleteAll(String calendarId);

    /**
     * Метод existsById.
     */
    boolean existsById(String id);
}
