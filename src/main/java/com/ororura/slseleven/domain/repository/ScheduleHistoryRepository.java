package com.ororura.slseleven.domain.repository;

import com.ororura.slseleven.domain.model.ScheduleHistorySnapshot;
import java.util.List;
import java.util.Optional;

public interface ScheduleHistoryRepository {
    /**
     * Метод save.
     */
    void save(ScheduleHistorySnapshot snapshot);

    /**
     * Метод findAll.
     */
    List<ScheduleHistorySnapshot> findAll(String calendarId);

    /**
     * Метод findById.
     */
    Optional<ScheduleHistorySnapshot> findById(String id, String calendarId);

    /**
     * Метод deleteById.
     */
    void deleteById(String id);
}
