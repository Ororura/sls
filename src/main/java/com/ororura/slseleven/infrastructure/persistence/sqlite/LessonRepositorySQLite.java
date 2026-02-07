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
            "INSERT OR REPLACE INTO lessons (id, calendar_id, topic, lesson_name, class_name, auto_scheduled, archived, time, location, instructor, date) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, lesson.getId());
            ps.setString(
                2,
                lesson.getCalendarId() == null || lesson.getCalendarId().isBlank()
                    ? Lesson.DEFAULT_CALENDAR_ID
                    : lesson.getCalendarId()
            );
            ps.setString(3, lesson.getTopic());
            ps.setString(4, lesson.getLessonName());
            ps.setString(5, lesson.getClassName());
            ps.setInt(6, lesson.isAutoScheduled() ? 1 : 0);
            ps.setInt(7, lesson.isArchived() ? 1 : 0);
            ps.setString(8, lesson.getTime().toString());
            ps.setString(9, lesson.getLocation());
            ps.setString(10, lesson.getInstructor());
            ps.setString(11, lesson.getDate().toString());

            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при сохранении занятия", e);
        }
    }

    @Override
    public Optional<Lesson> findById(String id) {
        String sql =
            "SELECT id, calendar_id, topic, lesson_name, class_name, auto_scheduled, archived, time, location, instructor, date " +
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
    public List<Lesson> findAll(String calendarId) {
        String sql =
            "SELECT id, calendar_id, topic, lesson_name, class_name, auto_scheduled, archived, time, location, instructor, date " +
            "FROM lessons WHERE calendar_id = ? ORDER BY date, time";

        List<Lesson> lessons = new ArrayList<>();
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, calendarId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lessons.add(mapResultSetToLesson(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении всех занятий", e);
        }
        return lessons;
    }

    @Override
    public List<Lesson> findAllAcrossCalendars() {
        String sql =
            "SELECT id, calendar_id, topic, lesson_name, class_name, auto_scheduled, archived, time, location, instructor, date " +
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
    public List<Lesson> findByDate(LocalDate date, String calendarId) {
        String sql =
            "SELECT id, calendar_id, topic, lesson_name, class_name, auto_scheduled, archived, time, location, instructor, date " +
            "FROM lessons WHERE date = ? AND calendar_id = ? ORDER BY time";

        List<Lesson> lessons = new ArrayList<>();
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, date.toString());
            ps.setString(2, calendarId);
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
    public List<Lesson> findByDateAcrossCalendars(LocalDate date) {
        String sql =
            "SELECT id, calendar_id, topic, lesson_name, class_name, auto_scheduled, archived, time, location, instructor, date " +
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
        LocalDate endDate,
        String calendarId
    ) {
        String sql =
            "SELECT id, calendar_id, topic, lesson_name, class_name, auto_scheduled, archived, time, location, instructor, date " +
            "FROM lessons WHERE date >= ? AND date <= ? AND calendar_id = ? ORDER BY date, time";

        List<Lesson> lessons = new ArrayList<>();
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, startDate.toString());
            ps.setString(2, endDate.toString());
            ps.setString(3, calendarId);
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
    public List<Lesson> findByDateRangeAcrossCalendars(
        LocalDate startDate,
        LocalDate endDate
    ) {
        String sql =
            "SELECT id, calendar_id, topic, lesson_name, class_name, auto_scheduled, archived, time, location, instructor, date " +
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
    public void deleteAll(String calendarId) {
        String sql = "DELETE FROM lessons WHERE calendar_id = ?";

        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, calendarId);
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
        lesson.setCalendarId(rs.getString("calendar_id"));
        lesson.setTopic(rs.getString("topic"));
        lesson.setLessonName(rs.getString("lesson_name"));
        lesson.setClassName(rs.getString("class_name"));
        lesson.setAutoScheduled(rs.getInt("auto_scheduled") == 1);
        lesson.setArchived(rs.getInt("archived") == 1);
        lesson.setTime(LocalTime.parse(rs.getString("time")));
        lesson.setLocation(rs.getString("location"));
        lesson.setInstructor(rs.getString("instructor"));
        lesson.setDate(LocalDate.parse(rs.getString("date")));
        return lesson;
    }
}
