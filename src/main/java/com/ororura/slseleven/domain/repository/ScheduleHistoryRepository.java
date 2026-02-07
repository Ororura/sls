package com.ororura.slseleven.domain.repository;

import com.ororura.slseleven.domain.model.ScheduleHistorySnapshot;
import java.util.List;
import java.util.Optional;

public interface ScheduleHistoryRepository {
    void save(ScheduleHistorySnapshot snapshot);

    List<ScheduleHistorySnapshot> findAll(String calendarId);

    Optional<ScheduleHistorySnapshot> findById(String id, String calendarId);

    void deleteById(String id);
}
