package com.ororura.slseleven.domain.repository;

import java.time.DayOfWeek;
import java.util.Map;

/**
 * Репозиторий для настроек автопланирования.
 */
public interface ScheduleSettingsRepository {
    Map<DayOfWeek, Integer> getMaxHoursByDay();

    void saveMaxHours(DayOfWeek dayOfWeek, int maxHours);

    void saveAll(Map<DayOfWeek, Integer> maxHoursByDay);
}
