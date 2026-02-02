package com.ororura.slseleven.infrastructure.persistence.sqlite;

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

    public ScheduleHistoryRepositorySQLite(SQLiteConnectionProvider provider) {
        this.provider = provider;
    }

    @Override
    public void save(ScheduleHistorySnapshot snapshot) {
        String sql =
            "INSERT OR REPLACE INTO schedule_history " +
            "(id, created_at, label, lessons_blob, schedule_items_blob) VALUES (?, ?, ?, ?, ?)";

        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, snapshot.getId());
            ps.setString(2, snapshot.getCreatedAt().toString());
            ps.setString(3, snapshot.getLabel());
            ps.setString(4, snapshot.getLessonsBlob());
            ps.setString(5, snapshot.getScheduleItemsBlob());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при сохранении истории", e);
        }
    }

    @Override
    public List<ScheduleHistorySnapshot> findAll() {
        String sql =
            "SELECT id, created_at, label, lessons_blob, schedule_items_blob " +
            "FROM schedule_history ORDER BY created_at DESC";

        List<ScheduleHistorySnapshot> snapshots = new ArrayList<>();
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                snapshots.add(map(rs));
            }
            return snapshots;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при чтении истории", e);
        }
    }

    @Override
    public Optional<ScheduleHistorySnapshot> findById(String id) {
        String sql =
            "SELECT id, created_at, label, lessons_blob, schedule_items_blob " +
            "FROM schedule_history WHERE id = ?";
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(map(rs));
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при чтении снимка истории", e);
        }
    }

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

    private ScheduleHistorySnapshot map(ResultSet rs) throws Exception {
        return new ScheduleHistorySnapshot(
            rs.getString("id"),
            LocalDateTime.parse(rs.getString("created_at")),
            rs.getString("label"),
            rs.getString("lessons_blob"),
            rs.getString("schedule_items_blob")
        );
    }
}
