package com.ororura.slseleven.infrastructure.persistence.sqlite;

import com.ororura.slseleven.domain.model.AppCalendar;
import com.ororura.slseleven.domain.repository.CalendarRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CalendarRepositorySQLite implements CalendarRepository {

    private final SQLiteConnectionProvider provider;

    public CalendarRepositorySQLite(SQLiteConnectionProvider provider) {
        this.provider = provider;
    }

    @Override
    public List<AppCalendar> findAllCalendars() {
        String sql = "SELECT id, name, directory_path FROM calendars ORDER BY directory_path, name";
        List<AppCalendar> calendars = new ArrayList<>();
        try (
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                calendars.add(
                    new AppCalendar(
                        resultSet.getString("id"),
                        resultSet.getString("name"),
                        resultSet.getString("directory_path")
                    )
                );
            }
            return calendars;
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка при чтении календарей", exception);
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

        String calendarId = UUID.randomUUID().toString();
        String normalizedName = name.trim();
        String normalizedDirectoryPath = normalizeDirectoryPath(directoryPath);

        try (Connection connection = provider.getConnection()) {
            connection.setAutoCommit(false);
            try (
                PreparedStatement insertCalendar = connection.prepareStatement(
                    "INSERT INTO calendars (id, name, directory_path) VALUES (?, ?, ?)"
                );
                PreparedStatement insertSettings = connection.prepareStatement(
                    "INSERT INTO schedule_settings (calendar_id, day_of_week, max_hours) VALUES (?, ?, ?)"
                )
            ) {
                insertCalendar.setString(1, calendarId);
                insertCalendar.setString(2, normalizedName);
                insertCalendar.setString(3, normalizedDirectoryPath);
                insertCalendar.executeUpdate();

                for (DayOfWeek day : DayOfWeek.values()) {
                    insertSettings.setString(1, calendarId);
                    insertSettings.setInt(2, day.getValue());
                    insertSettings.setInt(
                        3,
                        day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
                            ? 0
                            : 2
                    );
                    insertSettings.addBatch();
                }
                insertSettings.executeBatch();
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            if (isCalendarNameConflict(exception)) {
                throw new IllegalArgumentException(
                    "Календарь с таким именем уже есть в этой директории"
                );
            }
            throw new RuntimeException("Ошибка при создании календаря", exception);
        }

        return new AppCalendar(calendarId, normalizedName, normalizedDirectoryPath);
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
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, newName.trim());
            statement.setString(2, calendarId);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Календарь не найден");
            }
        } catch (Exception exception) {
            if (isCalendarNameConflict(exception)) {
                throw new IllegalArgumentException(
                    "Календарь с таким именем уже есть в этой директории"
                );
            }
            throw new RuntimeException(
                "Ошибка при переименовании календаря",
                exception
            );
        }
    }

    @Override
    public void moveCalendarToDirectory(String calendarId, String directoryPath) {
        if (calendarId == null || calendarId.isBlank()) {
            throw new IllegalArgumentException("ID календаря не может быть пустым");
        }
        String sql = "UPDATE calendars SET directory_path = ? WHERE id = ?";
        try (
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, normalizeDirectoryPath(directoryPath));
            statement.setString(2, calendarId);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Календарь не найден");
            }
        } catch (Exception exception) {
            if (isCalendarNameConflict(exception)) {
                throw new IllegalArgumentException(
                    "В этой директории уже есть календарь с таким именем"
                );
            }
            throw new RuntimeException("Ошибка при переносе календаря", exception);
        }
    }

    @Override
    public List<String> getCalendarDirectories() {
        String sql =
            "SELECT DISTINCT directory_path FROM calendars WHERE directory_path IS NOT NULL AND TRIM(directory_path) <> '' ORDER BY directory_path";
        List<String> directories = new ArrayList<>();
        try (
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                directories.add(resultSet.getString("directory_path"));
            }
            return directories;
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка при чтении директорий", exception);
        }
    }

    @Override
    public void deleteCalendar(String calendarId) {
        if (calendarId == null || calendarId.isBlank()) {
            throw new IllegalArgumentException("ID календаря не может быть пустым");
        }

        try (Connection connection = provider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                deleteByCalendar(connection, "DELETE FROM lessons WHERE calendar_id = ?", calendarId);
                deleteByCalendar(connection, "DELETE FROM schedule_items WHERE calendar_id = ?", calendarId);
                deleteByCalendar(connection, "DELETE FROM schedule_settings WHERE calendar_id = ?", calendarId);
                deleteByCalendar(connection, "DELETE FROM schedule_subject_rules WHERE calendar_id = ?", calendarId);
                deleteByCalendar(connection, "DELETE FROM schedule_slots WHERE calendar_id = ?", calendarId);
                deleteByCalendar(connection, "DELETE FROM instructor_duties WHERE calendar_id = ?", calendarId);
                deleteByCalendar(connection, "DELETE FROM instructors WHERE calendar_id = ?", calendarId);
                deleteByCalendar(connection, "DELETE FROM rooms WHERE calendar_id = ?", calendarId);
                deleteByCalendar(connection, "DELETE FROM schedule_history WHERE calendar_id = ?", calendarId);

                try (
                    PreparedStatement deleteCalendar = connection.prepareStatement(
                        "DELETE FROM calendars WHERE id = ?"
                    )
                ) {
                    deleteCalendar.setString(1, calendarId);
                    if (deleteCalendar.executeUpdate() == 0) {
                        throw new IllegalArgumentException("Календарь не найден");
                    }
                }

                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка при удалении календаря", exception);
        }
    }

    private void deleteByCalendar(Connection connection, String sql, String calendarId)
        throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, calendarId);
            statement.executeUpdate();
        }
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
        String message = exception == null
            ? ""
            : String.valueOf(exception.getMessage());
        return message.contains("UNIQUE constraint failed")
            && message.contains("calendars");
    }
}
