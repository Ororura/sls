package com.ororura.slseleven.domain.repository;

import com.ororura.slseleven.domain.model.SubjectScheduleRule;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

/**
 * Репозиторий для настроек автопланирования.
 */
public interface ScheduleSettingsRepository {
    Map<DayOfWeek, Integer> getMaxHoursByDay();

    void saveMaxHours(DayOfWeek dayOfWeek, int maxHours);

    void saveAll(Map<DayOfWeek, Integer> maxHoursByDay);

    List<SubjectScheduleRule> getSubjectRules();

    void saveSubjectRules(List<SubjectScheduleRule> rules);
}
