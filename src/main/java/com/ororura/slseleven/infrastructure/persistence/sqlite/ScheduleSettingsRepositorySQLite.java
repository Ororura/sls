package com.ororura.slseleven.infrastructure.persistence.sqlite;

import com.ororura.slseleven.domain.model.AppCalendar;
import com.ororura.slseleven.domain.model.CalendarDefaults;
import com.ororura.slseleven.domain.model.InstructorDuty;
import com.ororura.slseleven.domain.model.InstructorProfile;
import com.ororura.slseleven.domain.model.RoomProfile;
import com.ororura.slseleven.domain.model.SubjectScheduleRule;
import com.ororura.slseleven.domain.repository.ScheduleSettingsRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScheduleSettingsRepositorySQLite
    implements ScheduleSettingsRepository {

    private final SQLiteConnectionProvider provider;

    public ScheduleSettingsRepositorySQLite(SQLiteConnectionProvider provider) {
        this.provider = provider;
    }

    @Override
    public Map<DayOfWeek, Integer> getMaxHoursByDay() {
        String sql =
            "SELECT day_of_week, max_hours FROM schedule_settings WHERE calendar_id = ?";
        Map<DayOfWeek, Integer> result = new EnumMap<>(DayOfWeek.class);

        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, getActiveCalendarId());
            ResultSet rs = ps.executeQuery();
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
        String sql =
            "INSERT OR REPLACE INTO schedule_settings (calendar_id, day_of_week, max_hours) VALUES (?, ?, ?)";

        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, getActiveCalendarId());
            ps.setInt(2, dayOfWeek.getValue());
            ps.setInt(3, maxHours);
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

        String sql =
            "INSERT OR REPLACE INTO schedule_settings (calendar_id, day_of_week, max_hours) VALUES (?, ?, ?)";
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            String calendarId = getActiveCalendarId();
            for (Map.Entry<DayOfWeek, Integer> entry : maxHoursByDay.entrySet()) {
                ps.setString(1, calendarId);
                ps.setInt(2, entry.getKey().getValue());
                ps.setInt(3, entry.getValue());
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
            "SELECT subject, allowed_days, exclusive_days, consecutive_hours, fixed_room FROM schedule_subject_rules WHERE calendar_id = ? ORDER BY subject";
        List<SubjectScheduleRule> rules = new ArrayList<>();
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, getActiveCalendarId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String subject = rs.getString("subject");
                int allowedMask = rs.getInt("allowed_days");
                int exclusiveMask = rs.getInt("exclusive_days");
                int consecutiveHours = Math.max(
                    1,
                    rs.getInt("consecutive_hours")
                );
                String fixedRoom = rs.getString("fixed_room");
                rules.add(
                    new SubjectScheduleRule(
                        subject,
                        fromMask(allowedMask),
                        fromMask(exclusiveMask),
                        consecutiveHours,
                        fixedRoom
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
        String deleteSql =
            "DELETE FROM schedule_subject_rules WHERE calendar_id = ?";
        String insertSql =
            "INSERT INTO schedule_subject_rules (calendar_id, subject, allowed_days, exclusive_days, consecutive_hours, fixed_room) VALUES (?, ?, ?, ?, ?, ?)";
        try (
            Connection conn = provider.getConnection();
            PreparedStatement delete = conn.prepareStatement(deleteSql);
            PreparedStatement insert = conn.prepareStatement(insertSql)
        ) {
            String calendarId = getActiveCalendarId();
            delete.setString(1, calendarId);
            delete.executeUpdate();
            if (rules == null || rules.isEmpty()) {
                return;
            }
            for (SubjectScheduleRule rule : rules) {
                if (rule == null || rule.getSubject().isBlank()) {
                    continue;
                }
                insert.setString(1, calendarId);
                insert.setString(2, rule.getSubject().trim());
                insert.setInt(3, toMask(rule.getAllowedDays()));
                insert.setInt(4, toMask(rule.getExclusiveDays()));
                insert.setInt(5, Math.max(1, rule.getConsecutiveHours()));
                insert.setString(6, rule.getFixedRoom());
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

    @Override
    public List<InstructorProfile> getInstructors() {
        String sql =
            "SELECT id, name, allowed_days FROM instructors WHERE calendar_id = ? ORDER BY name";
        List<InstructorProfile> instructors = new ArrayList<>();
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, getActiveCalendarId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                instructors.add(
                    new InstructorProfile(
                        rs.getString("id"),
                        rs.getString("name"),
                        fromMask(rs.getInt("allowed_days"))
                    )
                );
            }
            return instructors;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при чтении преподавателей", e);
        }
    }

    @Override
    public InstructorProfile createInstructor(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Имя преподавателя не может быть пустым"
            );
        }
        String id = UUID.randomUUID().toString();
        String sql =
            "INSERT INTO instructors (id, calendar_id, name, allowed_days) VALUES (?, ?, ?, ?)";
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, id);
            ps.setString(2, getActiveCalendarId());
            ps.setString(3, name.trim());
            ps.setInt(4, toMask(EnumSet.allOf(DayOfWeek.class)));
            ps.executeUpdate();
            return new InstructorProfile(
                id,
                name.trim(),
                EnumSet.allOf(DayOfWeek.class)
            );
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при создании преподавателя", e);
        }
    }

    @Override
    public void updateInstructor(InstructorProfile instructor) {
        if (
            instructor == null ||
            instructor.getId() == null ||
            instructor.getId().isBlank()
        ) {
            throw new IllegalArgumentException("Некорректный преподаватель");
        }
        String sql =
            "UPDATE instructors SET name = ?, allowed_days = ? WHERE id = ? AND calendar_id = ?";
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, instructor.getName().trim());
            ps.setInt(2, toMask(instructor.getAllowedDays()));
            ps.setString(3, instructor.getId());
            ps.setString(4, getActiveCalendarId());
            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("Преподаватель не найден");
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при сохранении преподавателя", e);
        }
    }

    @Override
    public void deleteInstructor(String instructorId) {
        if (instructorId == null || instructorId.isBlank()) {
            throw new IllegalArgumentException("ID преподавателя не может быть пустым");
        }
        try (Connection conn = provider.getConnection()) {
            conn.setAutoCommit(false);
            try (
                PreparedStatement deleteDuties = conn.prepareStatement(
                    "DELETE FROM instructor_duties WHERE instructor_id = ? AND calendar_id = ?"
                );
                PreparedStatement deleteInstructor = conn.prepareStatement(
                    "DELETE FROM instructors WHERE id = ? AND calendar_id = ?"
                )
            ) {
                String calendarId = getActiveCalendarId();
                deleteDuties.setString(1, instructorId);
                deleteDuties.setString(2, calendarId);
                deleteDuties.executeUpdate();

                deleteInstructor.setString(1, instructorId);
                deleteInstructor.setString(2, calendarId);
                deleteInstructor.executeUpdate();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при удалении преподавателя", e);
        }
    }

    @Override
    public List<RoomProfile> getRooms() {
        String sql = "SELECT id, name FROM rooms WHERE calendar_id = ? ORDER BY name";
        List<RoomProfile> rooms = new ArrayList<>();
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, getActiveCalendarId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rooms.add(new RoomProfile(rs.getString("id"), rs.getString("name")));
            }
            return rooms;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при чтении кабинетов", e);
        }
    }

    @Override
    public RoomProfile createRoom(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название кабинета не может быть пустым");
        }
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO rooms (id, calendar_id, name) VALUES (?, ?, ?)";
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, id);
            ps.setString(2, getActiveCalendarId());
            ps.setString(3, name.trim());
            ps.executeUpdate();
            return new RoomProfile(id, name.trim());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при создании кабинета", e);
        }
    }

    @Override
    public void renameRoom(String roomId, String newName) {
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("ID кабинета не может быть пустым");
        }
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Название кабинета не может быть пустым"
            );
        }
        String sql = "UPDATE rooms SET name = ? WHERE id = ? AND calendar_id = ?";
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, newName.trim());
            ps.setString(2, roomId);
            ps.setString(3, getActiveCalendarId());
            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("Кабинет не найден");
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при переименовании кабинета", e);
        }
    }

    @Override
    public void deleteRoom(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("ID кабинета не может быть пустым");
        }
        String sql = "DELETE FROM rooms WHERE id = ? AND calendar_id = ?";
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, roomId);
            ps.setString(2, getActiveCalendarId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при удалении кабинета", e);
        }
    }

    @Override
    public List<InstructorDuty> getInstructorDuties() {
        String sql =
            "SELECT instructor_id, duty_date FROM instructor_duties WHERE calendar_id = ?";
        List<InstructorDuty> duties = new ArrayList<>();
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, getActiveCalendarId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                duties.add(
                    new InstructorDuty(
                        rs.getString("instructor_id"),
                        LocalDate.parse(rs.getString("duty_date"))
                    )
                );
            }
            return duties;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при чтении нарядов", e);
        }
    }

    @Override
    public void addInstructorDuty(String instructorId, LocalDate dutyDate) {
        if (
            instructorId == null ||
            instructorId.isBlank() ||
            dutyDate == null
        ) {
            throw new IllegalArgumentException("Некорректный наряд");
        }
        String sql =
            "INSERT OR REPLACE INTO instructor_duties (calendar_id, instructor_id, duty_date) VALUES (?, ?, ?)";
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, getActiveCalendarId());
            ps.setString(2, instructorId);
            ps.setString(3, dutyDate.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при добавлении наряда", e);
        }
    }

    @Override
    public void removeInstructorDuty(String instructorId, LocalDate dutyDate) {
        if (
            instructorId == null ||
            instructorId.isBlank() ||
            dutyDate == null
        ) {
            throw new IllegalArgumentException("Некорректный наряд");
        }
        String sql =
            "DELETE FROM instructor_duties WHERE calendar_id = ? AND instructor_id = ? AND duty_date = ?";
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, getActiveCalendarId());
            ps.setString(2, instructorId);
            ps.setString(3, dutyDate.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при удалении наряда", e);
        }
    }

    @Override
    public List<AppCalendar> findAllCalendars() {
        String sql = "SELECT id, name, directory_path FROM calendars ORDER BY directory_path, name";
        List<AppCalendar> calendars = new ArrayList<>();
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                calendars.add(
                    new AppCalendar(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("directory_path")
                    )
                );
            }
            return calendars;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при чтении календарей", e);
        }
    }

    @Override
    public AppCalendar createCalendar(String name) {
        return createCalendar(name, "");
    }

    @Override
    public AppCalendar createCalendar(String name, String directoryPath) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название календаря не может быть пустым");
        }

        String id = UUID.randomUUID().toString();
        String normalizedName = name.trim();
        String normalizedDirectoryPath = normalizeDirectoryPath(directoryPath);
        try (Connection conn = provider.getConnection()) {
            conn.setAutoCommit(false);
            try (
                PreparedStatement insertCalendar = conn.prepareStatement(
                    "INSERT INTO calendars (id, name, directory_path) VALUES (?, ?, ?)"
                );
                PreparedStatement insertSettings = conn.prepareStatement(
                    "INSERT INTO schedule_settings (calendar_id, day_of_week, max_hours) VALUES (?, ?, ?)"
                )
            ) {
                insertCalendar.setString(1, id);
                insertCalendar.setString(2, normalizedName);
                insertCalendar.setString(3, normalizedDirectoryPath);
                insertCalendar.executeUpdate();

                for (DayOfWeek day : DayOfWeek.values()) {
                    insertSettings.setString(1, id);
                    insertSettings.setInt(2, day.getValue());
                    insertSettings.setInt(
                        3,
                        day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY ? 0 : 2
                    );
                    insertSettings.addBatch();
                }
                insertSettings.executeBatch();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            if (isCalendarNameConflict(e)) {
                throw new IllegalArgumentException(
                    "Календарь с таким именем уже есть в этой директории"
                );
            }
            throw new RuntimeException("Ошибка при создании календаря", e);
        }

        return new AppCalendar(id, normalizedName, normalizedDirectoryPath);
    }

    @Override
    public void renameCalendar(String calendarId, String newName) {
        if (calendarId == null || calendarId.isBlank()) {
            throw new IllegalArgumentException("ID календаря не может быть пустым");
        }
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Название календаря не может быть пустым");
        }

        String sql = "UPDATE calendars SET name = ? WHERE id = ?";
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, newName.trim());
            ps.setString(2, calendarId);
            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("Календарь не найден");
            }
        } catch (Exception e) {
            if (isCalendarNameConflict(e)) {
                throw new IllegalArgumentException(
                    "Календарь с таким именем уже есть в этой директории"
                );
            }
            throw new RuntimeException("Ошибка при переименовании календаря", e);
        }
    }

    @Override
    public void moveCalendarToDirectory(String calendarId, String directoryPath) {
        if (calendarId == null || calendarId.isBlank()) {
            throw new IllegalArgumentException("ID календаря не может быть пустым");
        }
        String sql = "UPDATE calendars SET directory_path = ? WHERE id = ?";
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, normalizeDirectoryPath(directoryPath));
            ps.setString(2, calendarId);
            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("Календарь не найден");
            }
        } catch (Exception e) {
            if (isCalendarNameConflict(e)) {
                throw new IllegalArgumentException(
                    "В этой директории уже есть календарь с таким именем"
                );
            }
            throw new RuntimeException("Ошибка при переносе календаря", e);
        }
    }

    @Override
    public List<String> getCalendarDirectories() {
        String sql =
            "SELECT DISTINCT directory_path FROM calendars WHERE directory_path IS NOT NULL AND TRIM(directory_path) <> '' ORDER BY directory_path";
        List<String> directories = new ArrayList<>();
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                directories.add(rs.getString("directory_path"));
            }
            return directories;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при чтении директорий", e);
        }
    }

    @Override
    public void deleteCalendar(String calendarId) {
        if (calendarId == null || calendarId.isBlank()) {
            throw new IllegalArgumentException("ID календаря не может быть пустым");
        }

        List<AppCalendar> calendars = findAllCalendars();
        if (calendars.size() <= 1) {
            throw new IllegalStateException("Нельзя удалить единственный календарь");
        }

        String activeId = getActiveCalendarId();
        String fallbackId = calendars
            .stream()
            .map(AppCalendar::getId)
            .filter(id -> !id.equals(calendarId))
            .findFirst()
            .orElse(CalendarDefaults.DEFAULT_ID);

        try (Connection conn = provider.getConnection()) {
            conn.setAutoCommit(false);
            try {
                deleteByCalendar(conn, "DELETE FROM lessons WHERE calendar_id = ?", calendarId);
                deleteByCalendar(conn, "DELETE FROM schedule_items WHERE calendar_id = ?", calendarId);
                deleteByCalendar(conn, "DELETE FROM schedule_settings WHERE calendar_id = ?", calendarId);
                deleteByCalendar(conn, "DELETE FROM schedule_subject_rules WHERE calendar_id = ?", calendarId);
                deleteByCalendar(conn, "DELETE FROM instructor_duties WHERE calendar_id = ?", calendarId);
                deleteByCalendar(conn, "DELETE FROM instructors WHERE calendar_id = ?", calendarId);
                deleteByCalendar(conn, "DELETE FROM rooms WHERE calendar_id = ?", calendarId);
                deleteByCalendar(conn, "DELETE FROM schedule_history WHERE calendar_id = ?", calendarId);
                deleteByCalendar(conn, "DELETE FROM calendars WHERE id = ?", calendarId);

                if (calendarId.equals(activeId)) {
                    setActiveCalendarIdInConnection(conn, fallbackId);
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при удалении календаря", e);
        }
    }

    @Override
    public String getActiveCalendarId() {
        String sql = "SELECT value FROM app_settings WHERE key = 'active_calendar_id'";
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {
            if (!rs.next()) {
                setActiveCalendarId(CalendarDefaults.DEFAULT_ID);
                return CalendarDefaults.DEFAULT_ID;
            }
            String value = rs.getString("value");
            if (value == null || value.isBlank()) {
                setActiveCalendarId(CalendarDefaults.DEFAULT_ID);
                return CalendarDefaults.DEFAULT_ID;
            }
            return value;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при чтении активного календаря", e);
        }
    }

    @Override
    public void setActiveCalendarId(String calendarId) {
        if (calendarId == null || calendarId.isBlank()) {
            throw new IllegalArgumentException("ID календаря не может быть пустым");
        }

        String ensureSql = "SELECT 1 FROM calendars WHERE id = ? LIMIT 1";
        try (
            Connection conn = provider.getConnection();
            PreparedStatement ensure = conn.prepareStatement(ensureSql)
        ) {
            ensure.setString(1, calendarId);
            ResultSet rs = ensure.executeQuery();
            if (!rs.next()) {
                throw new IllegalArgumentException("Календарь не найден");
            }
            setActiveCalendarIdInConnection(conn, calendarId);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при сохранении активного календаря", e);
        }
    }

    private void deleteByCalendar(Connection conn, String sql, String calendarId)
        throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, calendarId);
            ps.executeUpdate();
        }
    }

    private void setActiveCalendarIdInConnection(Connection conn, String calendarId)
        throws Exception {
        try (
            PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO app_settings (key, value) VALUES ('active_calendar_id', ?)"
            )
        ) {
            ps.setString(1, calendarId);
            ps.executeUpdate();
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

    private String normalizeDirectoryPath(String directoryPath) {
        if (directoryPath == null || directoryPath.isBlank()) {
            return "";
        }
        String normalized = directoryPath
            .trim()
            .replace("\\", "/")
            .replaceAll("/+", "/");
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isCalendarNameConflict(Exception exception) {
        String message = exception == null ? "" : String.valueOf(exception.getMessage());
        return message.contains("UNIQUE constraint failed") &&
        message.contains("calendars");
    }
}
