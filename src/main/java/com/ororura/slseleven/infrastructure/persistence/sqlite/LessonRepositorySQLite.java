package com.ororura.slseleven.infrastructure.persistence.sqlite;

import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.domain.repository.LessonRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Реализация репозитория для SQLite
 */
public class LessonRepositorySQLite implements LessonRepository {

    private final SQLiteConnectionProvider provider;

    public LessonRepositorySQLite(SQLiteConnectionProvider provider) {
        this.provider = provider;
    }

    @Override
    public void save(Lesson lesson) {
        String sql =
            "INSERT OR REPLACE INTO lessons (id, topic, lesson_name, time, location, instructor, date) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, lesson.getId());
            ps.setString(2, lesson.getTopic());
            ps.setString(3, lesson.getLessonName());
            ps.setString(4, lesson.getTime().toString());
            ps.setString(5, lesson.getLocation());
            ps.setString(6, lesson.getInstructor());
            ps.setString(7, lesson.getDate().toString());

            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при сохранении занятия", e);
        }
    }

    @Override
    public Optional<Lesson> findById(String id) {
        String sql =
            "SELECT id, topic, lesson_name, time, location, instructor, date " +
            "FROM lessons WHERE id = ?";

        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToLesson(rs));
            }

            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при поиске занятия", e);
        }
    }

    @Override
    public List<Lesson> findAll() {
        String sql =
            "SELECT id, topic, lesson_name, time, location, instructor, date " +
            "FROM lessons ORDER BY date, time";

        List<Lesson> lessons = new ArrayList<>();
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                lessons.add(mapResultSetToLesson(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении всех занятий", e);
        }
        return lessons;
    }

    @Override
    public List<Lesson> findByDate(LocalDate date) {
        String sql =
            "SELECT id, topic, lesson_name, time, location, instructor, date " +
            "FROM lessons WHERE date = ? ORDER BY time";

        List<Lesson> lessons = new ArrayList<>();
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, date.toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                lessons.add(mapResultSetToLesson(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при поиске занятий по дате", e);
        }
        return lessons;
    }

    @Override
    public List<Lesson> findByDateRange(
        LocalDate startDate,
        LocalDate endDate
    ) {
        String sql =
            "SELECT id, topic, lesson_name, time, location, instructor, date " +
            "FROM lessons WHERE date >= ? AND date <= ? ORDER BY date, time";

        List<Lesson> lessons = new ArrayList<>();
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, startDate.toString());
            ps.setString(2, endDate.toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                lessons.add(mapResultSetToLesson(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException(
                "Ошибка при поиске занятий в диапазоне дат",
                e
            );
        }
        return lessons;
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM lessons WHERE id = ?";

        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при удалении занятия", e);
        }
    }

    @Override
    public void deleteAll() {
        String sql = "DELETE FROM lessons";

        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при удалении всех занятий", e);
        }
    }

    @Override
    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM lessons WHERE id = ? LIMIT 1";

        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            throw new RuntimeException(
                "Ошибка при проверке существования занятия",
                e
            );
        }
    }

    private Lesson mapResultSetToLesson(ResultSet rs) throws Exception {
        Lesson lesson = new Lesson();
        lesson.setId(rs.getString("id"));
        lesson.setTopic(rs.getString("topic"));
        lesson.setLessonName(rs.getString("lesson_name"));
        lesson.setTime(LocalTime.parse(rs.getString("time")));
        lesson.setLocation(rs.getString("location"));
        lesson.setInstructor(rs.getString("instructor"));
        lesson.setDate(LocalDate.parse(rs.getString("date")));
        return lesson;
    }
}
