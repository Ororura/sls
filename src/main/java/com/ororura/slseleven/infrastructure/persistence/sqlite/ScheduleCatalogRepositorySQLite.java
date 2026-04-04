package com.ororura.slseleven.infrastructure.persistence.sqlite;

import com.ororura.slseleven.domain.model.InstructorDuty;
import com.ororura.slseleven.domain.model.InstructorProfile;
import com.ororura.slseleven.domain.model.RoomProfile;
import com.ororura.slseleven.domain.model.SubjectScheduleRule;
import com.ororura.slseleven.domain.repository.ScheduleCatalogRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ScheduleCatalogRepositorySQLite implements ScheduleCatalogRepository {

    private final SQLiteConnectionProvider provider;

    public ScheduleCatalogRepositorySQLite(SQLiteConnectionProvider provider) {
        this.provider = provider;
    }

    @Override
    public Map<DayOfWeek, Integer> getMaxHoursByDay(String calendarId) {
        String sql =
            "SELECT day_of_week, max_hours FROM schedule_settings WHERE calendar_id = ?";
        Map<DayOfWeek, Integer> result = new EnumMap<>(DayOfWeek.class);

        try (
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, calendarId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                result.put(
                    DayOfWeek.of(resultSet.getInt("day_of_week")),
                    resultSet.getInt("max_hours")
                );
            }
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка при получении настроек", exception);
        }

        for (DayOfWeek day : DayOfWeek.values()) {
            result.putIfAbsent(day, 0);
        }
        return result;
    }

    @Override
    public void saveMaxHoursByDay(
        String calendarId,
        Map<DayOfWeek, Integer> maxHoursByDay
    ) {
        if (maxHoursByDay == null || maxHoursByDay.isEmpty()) {
            return;
        }

        String sql =
            "INSERT OR REPLACE INTO schedule_settings (calendar_id, day_of_week, max_hours) VALUES (?, ?, ?)";
        try (
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            for (Map.Entry<DayOfWeek, Integer> entry : maxHoursByDay.entrySet()) {
                statement.setString(1, calendarId);
                statement.setInt(2, entry.getKey().getValue());
                statement.setInt(3, entry.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка при сохранении настроек", exception);
        }
    }

    @Override
    public List<SubjectScheduleRule> getSubjectRules(String calendarId) {
        String sql =
            "SELECT subject, allowed_days, exclusive_days, consecutive_hours, max_lessons_per_day, fixed_room, avoid_consecutive_with, avoid_same_day_with FROM schedule_subject_rules WHERE calendar_id = ? ORDER BY subject";
        List<SubjectScheduleRule> rules = new ArrayList<>();
        try (
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, calendarId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                rules.add(
                    new SubjectScheduleRule(
                        resultSet.getString("subject"),
                        fromMask(resultSet.getInt("allowed_days")),
                        fromMask(resultSet.getInt("exclusive_days")),
                        Math.max(1, resultSet.getInt("consecutive_hours")),
                        Math.max(0, resultSet.getInt("max_lessons_per_day")),
                        resultSet.getString("fixed_room"),
                        decodeSubjectList(resultSet.getString("avoid_consecutive_with")),
                        decodeSubjectList(resultSet.getString("avoid_same_day_with"))
                    )
                );
            }
            return rules;
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка при чтении правил предметов", exception);
        }
    }

    @Override
    public void saveSubjectRules(
        String calendarId,
        List<SubjectScheduleRule> rules
    ) {
        String deleteSql =
            "DELETE FROM schedule_subject_rules WHERE calendar_id = ?";
        String insertSql =
            "INSERT INTO schedule_subject_rules (calendar_id, subject, allowed_days, exclusive_days, consecutive_hours, max_lessons_per_day, fixed_room, avoid_consecutive_with, avoid_same_day_with) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (
            Connection connection = provider.getConnection();
            PreparedStatement deleteStatement =
                connection.prepareStatement(deleteSql);
            PreparedStatement insertStatement =
                connection.prepareStatement(insertSql)
        ) {
            deleteStatement.setString(1, calendarId);
            deleteStatement.executeUpdate();
            if (rules == null || rules.isEmpty()) {
                return;
            }

            for (SubjectScheduleRule rule : rules) {
                if (rule == null || rule.getSubject().isBlank()) {
                    continue;
                }
                insertStatement.setString(1, calendarId);
                insertStatement.setString(2, rule.getSubject().trim());
                insertStatement.setInt(3, toMask(rule.getAllowedDays()));
                insertStatement.setInt(4, toMask(rule.getExclusiveDays()));
                insertStatement.setInt(5, Math.max(1, rule.getConsecutiveHours()));
                insertStatement.setInt(6, Math.max(0, rule.getMaxLessonsPerDay()));
                insertStatement.setString(7, rule.getFixedRoom());
                insertStatement.setString(
                    8,
                    encodeSubjectList(rule.getNoConsecutiveWithSubjects())
                );
                insertStatement.setString(
                    9,
                    encodeSubjectList(rule.getNoSameDayWithSubjects())
                );
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        } catch (Exception exception) {
            throw new RuntimeException(
                "Ошибка при сохранении правил предметов",
                exception
            );
        }
    }

    @Override
    public List<InstructorProfile> getInstructors(String calendarId) {
        String sql =
            "SELECT id, name, allowed_days FROM instructors WHERE calendar_id = ? ORDER BY name";
        List<InstructorProfile> instructors = new ArrayList<>();
        try (
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, calendarId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                instructors.add(
                    new InstructorProfile(
                        resultSet.getString("id"),
                        resultSet.getString("name"),
                        fromMask(resultSet.getInt("allowed_days"))
                    )
                );
            }
            return instructors;
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка при чтении преподавателей", exception);
        }
    }

    @Override
    public InstructorProfile createInstructor(String calendarId, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя преподавателя не может быть пустым");
        }
        String instructorId = UUID.randomUUID().toString();
        String sql =
            "INSERT INTO instructors (id, calendar_id, name, allowed_days) VALUES (?, ?, ?, ?)";
        try (
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, instructorId);
            statement.setString(2, calendarId);
            statement.setString(3, name.trim());
            statement.setInt(4, toMask(EnumSet.allOf(DayOfWeek.class)));
            statement.executeUpdate();
            return new InstructorProfile(
                instructorId,
                name.trim(),
                EnumSet.allOf(DayOfWeek.class)
            );
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка при создании преподавателя", exception);
        }
    }

    @Override
    public void updateInstructor(String calendarId, InstructorProfile instructor) {
        if (
            instructor == null
            || instructor.getId() == null
            || instructor.getId().isBlank()
        ) {
            throw new IllegalArgumentException("Некорректный преподаватель");
        }
        String sql =
            "UPDATE instructors SET name = ?, allowed_days = ? WHERE id = ? AND calendar_id = ?";
        try (
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, instructor.getName().trim());
            statement.setInt(2, toMask(instructor.getAllowedDays()));
            statement.setString(3, instructor.getId());
            statement.setString(4, calendarId);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Преподаватель не найден");
            }
        } catch (Exception exception) {
            throw new RuntimeException(
                "Ошибка при сохранении преподавателя",
                exception
            );
        }
    }

    @Override
    public void deleteInstructor(String calendarId, String instructorId) {
        if (instructorId == null || instructorId.isBlank()) {
            throw new IllegalArgumentException("ID преподавателя не может быть пустым");
        }
        try (Connection connection = provider.getConnection()) {
            connection.setAutoCommit(false);
            try (
                PreparedStatement deleteDuties = connection.prepareStatement(
                    "DELETE FROM instructor_duties WHERE instructor_id = ? AND calendar_id = ?"
                );
                PreparedStatement deleteInstructor = connection.prepareStatement(
                    "DELETE FROM instructors WHERE id = ? AND calendar_id = ?"
                )
            ) {
                deleteDuties.setString(1, instructorId);
                deleteDuties.setString(2, calendarId);
                deleteDuties.executeUpdate();

                deleteInstructor.setString(1, instructorId);
                deleteInstructor.setString(2, calendarId);
                deleteInstructor.executeUpdate();
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка при удалении преподавателя", exception);
        }
    }

    @Override
    public List<RoomProfile> getRooms(String calendarId) {
        String sql = "SELECT id, name FROM rooms WHERE calendar_id = ? ORDER BY name";
        List<RoomProfile> rooms = new ArrayList<>();
        try (
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, calendarId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                rooms.add(
                    new RoomProfile(
                        resultSet.getString("id"),
                        resultSet.getString("name")
                    )
                );
            }
            return rooms;
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка при чтении кабинетов", exception);
        }
    }

    @Override
    public RoomProfile createRoom(String calendarId, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название кабинета не может быть пустым");
        }
        String roomId = UUID.randomUUID().toString();
        String sql = "INSERT INTO rooms (id, calendar_id, name) VALUES (?, ?, ?)";
        try (
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, roomId);
            statement.setString(2, calendarId);
            statement.setString(3, name.trim());
            statement.executeUpdate();
            return new RoomProfile(roomId, name.trim());
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка при создании кабинета", exception);
        }
    }

    @Override
    public void renameRoom(String calendarId, String roomId, String newName) {
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("ID кабинета не может быть пустым");
        }
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Название кабинета не может быть пустым");
        }
        String sql = "UPDATE rooms SET name = ? WHERE id = ? AND calendar_id = ?";
        try (
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, newName.trim());
            statement.setString(2, roomId);
            statement.setString(3, calendarId);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Кабинет не найден");
            }
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка при переименовании кабинета", exception);
        }
    }

    @Override
    public void deleteRoom(String calendarId, String roomId) {
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("ID кабинета не может быть пустым");
        }
        String sql = "DELETE FROM rooms WHERE id = ? AND calendar_id = ?";
        try (
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, roomId);
            statement.setString(2, calendarId);
            statement.executeUpdate();
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка при удалении кабинета", exception);
        }
    }

    @Override
    public List<InstructorDuty> getInstructorDuties(String calendarId) {
        String sql =
            "SELECT instructor_id, duty_date FROM instructor_duties WHERE calendar_id = ?";
        List<InstructorDuty> duties = new ArrayList<>();
        try (
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, calendarId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                duties.add(
                    new InstructorDuty(
                        resultSet.getString("instructor_id"),
                        LocalDate.parse(resultSet.getString("duty_date"))
                    )
                );
            }
            return duties;
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка при чтении нарядов", exception);
        }
    }

    @Override
    public void addInstructorDuty(
        String calendarId,
        String instructorId,
        LocalDate dutyDate
    ) {
        if (instructorId == null || instructorId.isBlank() || dutyDate == null) {
            throw new IllegalArgumentException("Некорректный наряд");
        }
        String sql =
            "INSERT OR REPLACE INTO instructor_duties (calendar_id, instructor_id, duty_date) VALUES (?, ?, ?)";
        try (
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, calendarId);
            statement.setString(2, instructorId);
            statement.setString(3, dutyDate.toString());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка при добавлении наряда", exception);
        }
    }

    @Override
    public void removeInstructorDuty(
        String calendarId,
        String instructorId,
        LocalDate dutyDate
    ) {
        if (instructorId == null || instructorId.isBlank() || dutyDate == null) {
            throw new IllegalArgumentException("Некорректный наряд");
        }
        String sql =
            "DELETE FROM instructor_duties WHERE calendar_id = ? AND instructor_id = ? AND duty_date = ?";
        try (
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, calendarId);
            statement.setString(2, instructorId);
            statement.setString(3, dutyDate.toString());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка при удалении наряда", exception);
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

    private String encodeSubjectList(Set<String> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            return "";
        }
        return String.join("\n", subjects);
    }

    private Set<String> decodeSubjectList(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        String[] parts = value.split("\\n");
        for (String part : parts) {
            String normalized = part == null ? "" : part.trim().toLowerCase();
            if (!normalized.isEmpty()) {
                result.add(normalized);
            }
        }
        return result.isEmpty() ? Set.of() : result;
    }
}
