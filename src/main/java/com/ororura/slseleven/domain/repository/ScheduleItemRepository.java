package com.ororura.slseleven.domain.repository;

import com.ororura.slseleven.domain.model.ScheduleItem;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для элементов списка расписаний.
 */
public interface ScheduleItemRepository {
    void save(ScheduleItem item);

    Optional<ScheduleItem> findById(String id);

    List<ScheduleItem> findAll();

    void deleteById(String id);

    void deleteAllByIds(List<String> ids);

    void deleteAll();

    boolean existsById(String id);
}
