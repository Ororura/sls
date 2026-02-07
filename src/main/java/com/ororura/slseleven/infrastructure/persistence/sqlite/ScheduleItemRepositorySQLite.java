package com.ororura.slseleven.infrastructure.persistence.sqlite;

import com.ororura.slseleven.domain.model.ScheduleItem;
import com.ororura.slseleven.domain.repository.ScheduleItemRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ScheduleItemRepositorySQLite implements ScheduleItemRepository {

    private final SQLiteConnectionProvider provider;

    public ScheduleItemRepositorySQLite(SQLiteConnectionProvider provider) {
        this.provider = provider;
    }

    @Override
    public void save(ScheduleItem item) {
        String sql =
            "INSERT OR REPLACE INTO schedule_items " +
            "(id, calendar_id, topic, lesson_name, class_name, location, instructor, hours, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, item.getId());
            ps.setString(
                2,
                item.getCalendarId() == null || item.getCalendarId().isBlank()
                    ? ScheduleItem.DEFAULT_CALENDAR_ID
                    : item.getCalendarId()
            );
            ps.setString(3, item.getTopic());
            ps.setString(4, item.getLessonName());
            ps.setString(5, item.getClassName());
            ps.setString(6, item.getLocation());
            ps.setString(7, item.getInstructor());
            ps.setInt(8, item.getHours());
            ps.setString(9, item.getCreatedAt().toString());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(
                "Ошибка при сохранении элемента списка",
                e
            );
        }
    }

    @Override
    public Optional<ScheduleItem> findById(String id) {
        String sql =
            "SELECT id, calendar_id, topic, lesson_name, class_name, location, instructor, hours, created_at " +
            "FROM schedule_items WHERE id = ?";

        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSet(rs));
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при поиске элемента списка", e);
        }
    }

    @Override
    public List<ScheduleItem> findAll(String calendarId) {
        String sql =
            "SELECT id, calendar_id, topic, lesson_name, class_name, location, instructor, hours, created_at " +
            "FROM schedule_items WHERE calendar_id = ? ORDER BY created_at";

        List<ScheduleItem> items = new ArrayList<>();
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, calendarId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(mapResultSet(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении списка", e);
        }
        return items;
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM schedule_items WHERE id = ?";

        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(
                "Ошибка при удалении элемента списка",
                e
            );
        }
    }

    @Override
    public void deleteAllByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder(
            "DELETE FROM schedule_items WHERE id IN ("
        );
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("?");
        }
        sb.append(")");

        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sb.toString())
        ) {
            for (int i = 0; i < ids.size(); i++) {
                ps.setString(i + 1, ids.get(i));
            }
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(
                "Ошибка при удалении элементов списка",
                e
            );
        }
    }

    @Override
    public void deleteAll(String calendarId) {
        String sql = "DELETE FROM schedule_items WHERE calendar_id = ?";

        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, calendarId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(
                "Ошибка при удалении всех элементов списка",
                e
            );
        }
    }

    @Override
    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM schedule_items WHERE id = ? LIMIT 1";

        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            throw new RuntimeException(
                "Ошибка при проверке существования элемента списка",
                e
            );
        }
    }

    private ScheduleItem mapResultSet(ResultSet rs) throws Exception {
        ScheduleItem item = new ScheduleItem();
        item.setId(rs.getString("id"));
        item.setCalendarId(rs.getString("calendar_id"));
        item.setTopic(rs.getString("topic"));
        item.setLessonName(rs.getString("lesson_name"));
        item.setClassName(rs.getString("class_name"));
        item.setLocation(rs.getString("location"));
        item.setInstructor(rs.getString("instructor"));
        item.setHours(rs.getInt("hours"));
        item.setCreatedAt(LocalDateTime.parse(rs.getString("created_at")));
        return item;
    }
}
