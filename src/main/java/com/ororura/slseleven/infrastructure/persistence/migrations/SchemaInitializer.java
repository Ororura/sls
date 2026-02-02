package com.ororura.slseleven.infrastructure.persistence.migrations;

import com.ororura.slseleven.infrastructure.persistence.sqlite.SQLiteConnectionProvider;
import java.sql.Connection;
import java.sql.Statement;

public class SchemaInitializer {

    public static void init(SQLiteConnectionProvider provider) {
        String lessonsSql =
            "CREATE TABLE IF NOT EXISTS lessons (\n" +
            "    id TEXT PRIMARY KEY,\n" +
            "    topic TEXT NOT NULL,\n" +
            "    lesson_name TEXT NOT NULL,\n" +
            "    class_name TEXT NOT NULL DEFAULT '',\n" +
            "    auto_scheduled INTEGER NOT NULL DEFAULT 0,\n" +
            "    time TEXT NOT NULL,\n" +
            "    location TEXT NOT NULL,\n" +
            "    instructor TEXT NOT NULL,\n" +
            "    date TEXT NOT NULL\n" +
            ");";

        String scheduleItemsSql =
            "CREATE TABLE IF NOT EXISTS schedule_items (\n" +
            "    id TEXT PRIMARY KEY,\n" +
            "    topic TEXT NOT NULL,\n" +
            "    lesson_name TEXT NOT NULL,\n" +
            "    class_name TEXT NOT NULL DEFAULT '',\n" +
            "    location TEXT NOT NULL,\n" +
            "    instructor TEXT NOT NULL,\n" +
            "    hours INTEGER NOT NULL,\n" +
            "    created_at TEXT NOT NULL\n" +
            ");";

        String scheduleSettingsSql =
            "CREATE TABLE IF NOT EXISTS schedule_settings (\n" +
            "    day_of_week INTEGER PRIMARY KEY,\n" +
            "    max_hours INTEGER NOT NULL\n" +
            ");";

        try (
            Connection c = provider.getConnection();
            Statement s = c.createStatement()
        ) {
            s.execute(lessonsSql);
            s.execute(scheduleItemsSql);
            s.execute(scheduleSettingsSql);
            ensureColumnExists(
                s,
                "ALTER TABLE lessons ADD COLUMN class_name TEXT NOT NULL DEFAULT ''"
            );
            ensureColumnExists(
                s,
                "ALTER TABLE lessons ADD COLUMN auto_scheduled INTEGER NOT NULL DEFAULT 0"
            );
            ensureColumnExists(
                s,
                "ALTER TABLE schedule_items ADD COLUMN class_name TEXT NOT NULL DEFAULT ''"
            );
            s.execute(
                "INSERT OR IGNORE INTO schedule_settings (day_of_week, max_hours) VALUES (1, 2)"
            );
            s.execute(
                "INSERT OR IGNORE INTO schedule_settings (day_of_week, max_hours) VALUES (2, 2)"
            );
            s.execute(
                "INSERT OR IGNORE INTO schedule_settings (day_of_week, max_hours) VALUES (3, 2)"
            );
            s.execute(
                "INSERT OR IGNORE INTO schedule_settings (day_of_week, max_hours) VALUES (4, 2)"
            );
            s.execute(
                "INSERT OR IGNORE INTO schedule_settings (day_of_week, max_hours) VALUES (5, 2)"
            );
            s.execute(
                "INSERT OR IGNORE INTO schedule_settings (day_of_week, max_hours) VALUES (6, 0)"
            );
            s.execute(
                "INSERT OR IGNORE INTO schedule_settings (day_of_week, max_hours) VALUES (7, 0)"
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void ensureColumnExists(Statement statement, String sql)
        throws Exception {
        try {
            statement.execute(sql);
        } catch (Exception ignored) {}
    }
}
