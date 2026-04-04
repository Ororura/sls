package com.ororura.slseleven.infrastructure.persistence.sqlite;

import com.ororura.slseleven.domain.model.CalendarDefaults;
import com.ororura.slseleven.domain.repository.ActiveCalendarRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ActiveCalendarRepositorySQLite implements ActiveCalendarRepository {

    private final SQLiteConnectionProvider provider;

    public ActiveCalendarRepositorySQLite(SQLiteConnectionProvider provider) {
        this.provider = provider;
    }

    @Override
    public String getActiveCalendarId() {
        String sql = "SELECT value FROM app_settings WHERE key = 'active_calendar_id'";
        try (
            Connection connection = provider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()
        ) {
            if (!resultSet.next()) {
                setActiveCalendarId(CalendarDefaults.DEFAULT_ID);
                return CalendarDefaults.DEFAULT_ID;
            }
            String value = resultSet.getString("value");
            if (value == null || value.isBlank()) {
                setActiveCalendarId(CalendarDefaults.DEFAULT_ID);
                return CalendarDefaults.DEFAULT_ID;
            }
            return value;
        } catch (Exception exception) {
            throw new RuntimeException(
                "Ошибка при чтении активного календаря",
                exception
            );
        }
    }

    @Override
    public void setActiveCalendarId(String calendarId) {
        if (calendarId == null || calendarId.isBlank()) {
            throw new IllegalArgumentException("ID календаря не может быть пустым");
        }

        String ensureSql = "SELECT 1 FROM calendars WHERE id = ? LIMIT 1";
        try (
            Connection connection = provider.getConnection();
            PreparedStatement ensureStatement =
                connection.prepareStatement(ensureSql)
        ) {
            ensureStatement.setString(1, calendarId);
            ResultSet resultSet = ensureStatement.executeQuery();
            if (!resultSet.next()) {
                throw new IllegalArgumentException("Календарь не найден");
            }
            saveActiveCalendarId(connection, calendarId);
        } catch (Exception exception) {
            throw new RuntimeException(
                "Ошибка при сохранении активного календаря",
                exception
            );
        }
    }

    private void saveActiveCalendarId(Connection connection, String calendarId)
        throws Exception {
        try (
            PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO app_settings (key, value) VALUES ('active_calendar_id', ?)"
            )
        ) {
            statement.setString(1, calendarId);
            statement.executeUpdate();
        }
    }
}
