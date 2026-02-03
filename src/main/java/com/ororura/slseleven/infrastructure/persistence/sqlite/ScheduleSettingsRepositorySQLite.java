package com.ororura.slseleven.infrastructure.persistence.sqlite;

import com.ororura.slseleven.domain.model.SubjectScheduleRule;
import com.ororura.slseleven.domain.repository.ScheduleSettingsRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class ScheduleSettingsRepositorySQLite implements ScheduleSettingsRepository {
    private final SQLiteConnectionProvider provider;

    public ScheduleSettingsRepositorySQLite(SQLiteConnectionProvider provider) {
        this.provider = provider;
    }

    @Override
    public Map<DayOfWeek, Integer> getMaxHoursByDay() {
        String sql = "SELECT day_of_week, max_hours FROM schedule_settings";
        Map<DayOfWeek, Integer> result = new EnumMap<>(DayOfWeek.class);

        try (Connection conn = provider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int day = rs.getInt("day_of_week");
                int maxHours = rs.getInt("max_hours");
                result.put(DayOfWeek.of(day), maxHours);
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении настроек", e);
        }

        for (DayOfWeek day : DayOfWeek.values()) {
            result.putIfAbsent(day, 0);
        }
        return result;
    }

    @Override
    public void saveMaxHours(DayOfWeek dayOfWeek, int maxHours) {
        String sql = "INSERT OR REPLACE INTO schedule_settings (day_of_week, max_hours) VALUES (?, ?)";

        try (Connection conn = provider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dayOfWeek.getValue());
            ps.setInt(2, maxHours);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при сохранении настроек", e);
        }
    }

    @Override
    public void saveAll(Map<DayOfWeek, Integer> maxHoursByDay) {
        if (maxHoursByDay == null || maxHoursByDay.isEmpty()) {
            return;
        }

        String sql = "INSERT OR REPLACE INTO schedule_settings (day_of_week, max_hours) VALUES (?, ?)";
        try (Connection conn = provider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<DayOfWeek, Integer> entry : maxHoursByDay.entrySet()) {
                ps.setInt(1, entry.getKey().getValue());
                ps.setInt(2, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при сохранении настроек", e);
        }
    }

    @Override
    public List<SubjectScheduleRule> getSubjectRules() {
        String sql =
            "SELECT subject, allowed_days, exclusive_days FROM schedule_subject_rules ORDER BY subject";
        List<SubjectScheduleRule> rules = new ArrayList<>();
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                String subject = rs.getString("subject");
                int allowedMask = rs.getInt("allowed_days");
                int exclusiveMask = rs.getInt("exclusive_days");
                rules.add(
                    new SubjectScheduleRule(
                        subject,
                        fromMask(allowedMask),
                        fromMask(exclusiveMask)
                    )
                );
            }
            return rules;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при чтении правил предметов", e);
        }
    }

    @Override
    public void saveSubjectRules(List<SubjectScheduleRule> rules) {
        String deleteSql = "DELETE FROM schedule_subject_rules";
        String insertSql =
            "INSERT INTO schedule_subject_rules (subject, allowed_days, exclusive_days) VALUES (?, ?, ?)";
        try (
            Connection conn = provider.getConnection();
            PreparedStatement delete = conn.prepareStatement(deleteSql);
            PreparedStatement insert = conn.prepareStatement(insertSql)
        ) {
            delete.executeUpdate();
            if (rules == null || rules.isEmpty()) {
                return;
            }
            for (SubjectScheduleRule rule : rules) {
                if (rule == null || rule.getSubject().isBlank()) {
                    continue;
                }
                insert.setString(1, rule.getSubject().trim());
                insert.setInt(2, toMask(rule.getAllowedDays()));
                insert.setInt(3, toMask(rule.getExclusiveDays()));
                insert.addBatch();
            }
            insert.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException(
                "Ошибка при сохранении правил предметов",
                e
            );
        }
    }

    private int toMask(Iterable<DayOfWeek> days) {
        int mask = 0;
        for (DayOfWeek day : days) {
            mask |= 1 << (day.getValue() - 1);
        }
        return mask;
    }

    private EnumSet<DayOfWeek> fromMask(int mask) {
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            int bit = 1 << (day.getValue() - 1);
            if ((mask & bit) != 0) {
                days.add(day);
            }
        }
        return days;
    }
}
