package com.ororura.slseleven.infrastructure.persistence.sqlite;

import com.ororura.slseleven.domain.model.CalendarDefaults;
import com.ororura.slseleven.domain.model.ScheduleHistorySnapshot;
import com.ororura.slseleven.domain.repository.ScheduleHistoryRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ScheduleHistoryRepositorySQLite implements ScheduleHistoryRepository {

    private final SQLiteConnectionProvider provider;
    private static final String SNAPSHOT_COLUMNS =
        "id, calendar_id, created_at, label, lessons_blob, schedule_items_blob";
    private static final String SNAPSHOT_SELECT =
        "SELECT " + SNAPSHOT_COLUMNS + " FROM schedule_history";

    /**
     * Метод ScheduleHistoryRepositorySQLite.
     */
    public ScheduleHistoryRepositorySQLite(SQLiteConnectionProvider provider) {
        this.provider = provider;
    }

    /**
     * Метод save.
     */
    @Override
    public void save(ScheduleHistorySnapshot snapshot) {
        String sql =
            "INSERT OR REPLACE INTO schedule_history " +
            "(id, calendar_id, created_at, label, lessons_blob, schedule_items_blob) VALUES (?, ?, ?, ?, ?, ?)";

        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, snapshot.getId());
            ps.setString(
                2,
                snapshot.getCalendarId() == null || snapshot.getCalendarId().isBlank()
                    ? CalendarDefaults.DEFAULT_ID
                    : snapshot.getCalendarId()
            );
            ps.setString(3, snapshot.getCreatedAt().toString());
            ps.setString(4, snapshot.getLabel());
            ps.setString(5, snapshot.getLessonsBlob());
            ps.setString(6, snapshot.getScheduleItemsBlob());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при сохранении истории", e);
        }
    }

    /**
     * Метод findAll.
     */
    @Override
    public List<ScheduleHistorySnapshot> findAll(String calendarId) {
        String sql =
            SNAPSHOT_SELECT +
            " WHERE calendar_id = ? ORDER BY created_at DESC";

        List<ScheduleHistorySnapshot> snapshots = new ArrayList<>();
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, calendarId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                snapshots.add(map(rs));
            }
            return snapshots;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при чтении истории", e);
        }
    }

    /**
     * Метод findById.
     */
    @Override
    public Optional<ScheduleHistorySnapshot> findById(
        String id,
        String calendarId
    ) {
        String sql = SNAPSHOT_SELECT + " WHERE id = ? AND calendar_id = ?";
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, id);
            ps.setString(2, calendarId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(map(rs));
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при чтении снимка истории", e);
        }
    }

    /**
     * Метод deleteById.
     */
    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM schedule_history WHERE id = ?";
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при удалении снимка истории", e);
        }
    }

    /**
     * Метод map.
     */
    private ScheduleHistorySnapshot map(ResultSet rs) throws Exception {
        return new ScheduleHistorySnapshot(
            rs.getString("id"),
            rs.getString("calendar_id"),
            LocalDateTime.parse(rs.getString("created_at")),
            rs.getString("label"),
            rs.getString("lessons_blob"),
            rs.getString("schedule_items_blob")
        );
    }
}
