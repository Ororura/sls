package com.ororura.slseleven.infrastructure.persistence.sqlite;

import com.ororura.slseleven.domain.repository.ScheduleSettingsRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.util.EnumMap;
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
}
