package com.ororura.slseleven.infrastructure.persistence.migrations;

import com.ororura.slseleven.infrastructure.persistence.sqlite.SQLiteConnectionProvider;
import java.sql.Connection;
import java.sql.Statement;

public class SchemaInitializer {

    public static void init(SQLiteConnectionProvider provider) {
        String calendarsSql =
            "CREATE TABLE IF NOT EXISTS calendars (\n" +
            "    id TEXT PRIMARY KEY,\n" +
            "    name TEXT NOT NULL,\n" +
            "    directory_path TEXT NOT NULL DEFAULT '',\n" +
            "    UNIQUE(directory_path, name)\n" +
            ");";

        String appSettingsSql =
            "CREATE TABLE IF NOT EXISTS app_settings (\n" +
            "    key TEXT PRIMARY KEY,\n" +
            "    value TEXT NOT NULL\n" +
            ");";

        String lessonsSql =
            "CREATE TABLE IF NOT EXISTS lessons (\n" +
            "    id TEXT PRIMARY KEY,\n" +
            "    calendar_id TEXT NOT NULL DEFAULT 'default',\n" +
            "    topic TEXT NOT NULL,\n" +
            "    lesson_name TEXT NOT NULL,\n" +
            "    class_name TEXT NOT NULL DEFAULT '',\n" +
            "    auto_scheduled INTEGER NOT NULL DEFAULT 0,\n" +
            "    archived INTEGER NOT NULL DEFAULT 0,\n" +
            "    duration_hours INTEGER NOT NULL DEFAULT 1,\n" +
            "    time TEXT NOT NULL,\n" +
            "    location TEXT NOT NULL,\n" +
            "    instructor TEXT NOT NULL,\n" +
            "    date TEXT NOT NULL\n" +
            ");";

        String scheduleItemsSql =
            "CREATE TABLE IF NOT EXISTS schedule_items (\n" +
            "    id TEXT PRIMARY KEY,\n" +
            "    calendar_id TEXT NOT NULL DEFAULT 'default',\n" +
            "    topic TEXT NOT NULL,\n" +
            "    lesson_name TEXT NOT NULL,\n" +
            "    class_name TEXT NOT NULL DEFAULT '',\n" +
            "    location TEXT NOT NULL,\n" +
            "    instructor TEXT NOT NULL,\n" +
            "    hours INTEGER NOT NULL,\n" +
            "    consecutive_hours INTEGER NOT NULL DEFAULT 1,\n" +
            "    created_at TEXT NOT NULL\n" +
            ");";

        String scheduleSettingsSql =
            "CREATE TABLE IF NOT EXISTS schedule_settings (\n" +
            "    calendar_id TEXT NOT NULL DEFAULT 'default',\n" +
            "    day_of_week INTEGER NOT NULL,\n" +
            "    max_hours INTEGER NOT NULL,\n" +
            "    PRIMARY KEY (calendar_id, day_of_week)\n" +
            ");";

        String scheduleSubjectRulesSql =
            "CREATE TABLE IF NOT EXISTS schedule_subject_rules (\n" +
            "    calendar_id TEXT NOT NULL DEFAULT 'default',\n" +
            "    subject TEXT NOT NULL,\n" +
            "    allowed_days INTEGER NOT NULL,\n" +
            "    exclusive_days INTEGER NOT NULL,\n" +
            "    consecutive_hours INTEGER NOT NULL DEFAULT 1,\n" +
            "    fixed_room TEXT NOT NULL DEFAULT '',\n" +
            "    PRIMARY KEY (calendar_id, subject)\n" +
            ");";

        String instructorsSql =
            "CREATE TABLE IF NOT EXISTS instructors (\n" +
            "    id TEXT PRIMARY KEY,\n" +
            "    calendar_id TEXT NOT NULL DEFAULT 'default',\n" +
            "    name TEXT NOT NULL,\n" +
            "    allowed_days INTEGER NOT NULL DEFAULT 127\n" +
            ");";

        String roomsSql =
            "CREATE TABLE IF NOT EXISTS rooms (\n" +
            "    id TEXT PRIMARY KEY,\n" +
            "    calendar_id TEXT NOT NULL DEFAULT 'default',\n" +
            "    name TEXT NOT NULL\n" +
            ");";

        String instructorDutiesSql =
            "CREATE TABLE IF NOT EXISTS instructor_duties (\n" +
            "    calendar_id TEXT NOT NULL DEFAULT 'default',\n" +
            "    instructor_id TEXT NOT NULL,\n" +
            "    duty_date TEXT NOT NULL,\n" +
            "    PRIMARY KEY (calendar_id, instructor_id, duty_date)\n" +
            ");";

        String scheduleHistorySql =
            "CREATE TABLE IF NOT EXISTS schedule_history (\n" +
            "    id TEXT PRIMARY KEY,\n" +
            "    calendar_id TEXT NOT NULL DEFAULT 'default',\n" +
            "    created_at TEXT NOT NULL,\n" +
            "    label TEXT NOT NULL,\n" +
            "    lessons_blob TEXT NOT NULL,\n" +
            "    schedule_items_blob TEXT NOT NULL\n" +
            ");";

        try (
            Connection c = provider.getConnection();
            Statement s = c.createStatement()
        ) {
            s.execute(calendarsSql);
            s.execute(appSettingsSql);
            s.execute(lessonsSql);
            s.execute(scheduleItemsSql);
            s.execute(scheduleSettingsSql);
            s.execute(scheduleSubjectRulesSql);
            s.execute(instructorsSql);
            s.execute(roomsSql);
            s.execute(instructorDutiesSql);
            s.execute(scheduleHistorySql);
            ensureColumnExists(
                s,
                "ALTER TABLE calendars ADD COLUMN directory_path TEXT NOT NULL DEFAULT ''"
            );
            ensureColumnExists(
                s,
                "ALTER TABLE lessons ADD COLUMN calendar_id TEXT NOT NULL DEFAULT 'default'"
            );
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
                "ALTER TABLE lessons ADD COLUMN archived INTEGER NOT NULL DEFAULT 0"
            );
            ensureColumnExists(
                s,
                "ALTER TABLE lessons ADD COLUMN duration_hours INTEGER NOT NULL DEFAULT 1"
            );
            ensureColumnExists(
                s,
                "ALTER TABLE schedule_items ADD COLUMN calendar_id TEXT NOT NULL DEFAULT 'default'"
            );
            ensureColumnExists(
                s,
                "ALTER TABLE schedule_items ADD COLUMN class_name TEXT NOT NULL DEFAULT ''"
            );
            ensureColumnExists(
                s,
                "ALTER TABLE schedule_items ADD COLUMN consecutive_hours INTEGER NOT NULL DEFAULT 1"
            );
            ensureColumnExists(
                s,
                "ALTER TABLE schedule_settings ADD COLUMN calendar_id TEXT NOT NULL DEFAULT 'default'"
            );
            ensureColumnExists(
                s,
                "ALTER TABLE schedule_subject_rules ADD COLUMN calendar_id TEXT NOT NULL DEFAULT 'default'"
            );
            ensureColumnExists(
                s,
                "ALTER TABLE schedule_subject_rules ADD COLUMN consecutive_hours INTEGER NOT NULL DEFAULT 1"
            );
            ensureColumnExists(
                s,
                "ALTER TABLE schedule_subject_rules ADD COLUMN fixed_room TEXT NOT NULL DEFAULT ''"
            );
            ensureColumnExists(
                s,
                "ALTER TABLE schedule_history ADD COLUMN calendar_id TEXT NOT NULL DEFAULT 'default'"
            );
            normalizeScheduleSettingsTable(s);
            normalizeScheduleSubjectRulesTable(s);
            normalizeCalendarsTable(s);
            s.execute(
                "INSERT OR IGNORE INTO calendars (id, name) VALUES ('default', 'Основной')"
            );
            s.execute(
                "INSERT OR IGNORE INTO app_settings (key, value) VALUES ('active_calendar_id', 'default')"
            );
            s.execute(
                "INSERT OR IGNORE INTO schedule_settings (calendar_id, day_of_week, max_hours) VALUES ('default', 1, 2)"
            );
            s.execute(
                "INSERT OR IGNORE INTO schedule_settings (calendar_id, day_of_week, max_hours) VALUES ('default', 2, 2)"
            );
            s.execute(
                "INSERT OR IGNORE INTO schedule_settings (calendar_id, day_of_week, max_hours) VALUES ('default', 3, 2)"
            );
            s.execute(
                "INSERT OR IGNORE INTO schedule_settings (calendar_id, day_of_week, max_hours) VALUES ('default', 4, 2)"
            );
            s.execute(
                "INSERT OR IGNORE INTO schedule_settings (calendar_id, day_of_week, max_hours) VALUES ('default', 5, 2)"
            );
            s.execute(
                "INSERT OR IGNORE INTO schedule_settings (calendar_id, day_of_week, max_hours) VALUES ('default', 6, 0)"
            );
            s.execute(
                "INSERT OR IGNORE INTO schedule_settings (calendar_id, day_of_week, max_hours) VALUES ('default', 7, 0)"
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

    private static void normalizeScheduleSettingsTable(Statement s)
        throws Exception {
        s.execute("DROP TABLE IF EXISTS schedule_settings_new");
        s.execute(
            "CREATE TABLE schedule_settings_new (\n" +
            "    calendar_id TEXT NOT NULL DEFAULT 'default',\n" +
            "    day_of_week INTEGER NOT NULL,\n" +
            "    max_hours INTEGER NOT NULL,\n" +
            "    PRIMARY KEY (calendar_id, day_of_week)\n" +
            ");"
        );
        s.execute(
            "INSERT OR IGNORE INTO schedule_settings_new (calendar_id, day_of_week, max_hours) " +
            "SELECT COALESCE(calendar_id, 'default'), day_of_week, max_hours FROM schedule_settings"
        );
        s.execute("DROP TABLE schedule_settings");
        s.execute("ALTER TABLE schedule_settings_new RENAME TO schedule_settings");
    }

    private static void normalizeScheduleSubjectRulesTable(Statement s)
        throws Exception {
        s.execute("DROP TABLE IF EXISTS schedule_subject_rules_new");
        s.execute(
            "CREATE TABLE schedule_subject_rules_new (\n" +
            "    calendar_id TEXT NOT NULL DEFAULT 'default',\n" +
            "    subject TEXT NOT NULL,\n" +
            "    allowed_days INTEGER NOT NULL,\n" +
            "    exclusive_days INTEGER NOT NULL,\n" +
            "    consecutive_hours INTEGER NOT NULL DEFAULT 1,\n" +
            "    fixed_room TEXT NOT NULL DEFAULT '',\n" +
            "    PRIMARY KEY (calendar_id, subject)\n" +
            ");"
        );
        s.execute(
            "INSERT OR IGNORE INTO schedule_subject_rules_new (calendar_id, subject, allowed_days, exclusive_days, consecutive_hours, fixed_room) " +
            "SELECT COALESCE(calendar_id, 'default'), subject, allowed_days, exclusive_days, COALESCE(consecutive_hours, 1), COALESCE(fixed_room, '') FROM schedule_subject_rules"
        );
        s.execute("DROP TABLE schedule_subject_rules");
        s.execute(
            "ALTER TABLE schedule_subject_rules_new RENAME TO schedule_subject_rules"
        );
    }

    private static void normalizeCalendarsTable(Statement s) throws Exception {
        s.execute("DROP TABLE IF EXISTS calendars_new");
        s.execute(
            "CREATE TABLE calendars_new (\n" +
            "    id TEXT PRIMARY KEY,\n" +
            "    name TEXT NOT NULL,\n" +
            "    directory_path TEXT NOT NULL DEFAULT '',\n" +
            "    UNIQUE(directory_path, name)\n" +
            ");"
        );
        s.execute(
            "INSERT OR IGNORE INTO calendars_new (id, name, directory_path) " +
            "SELECT id, name, COALESCE(directory_path, '') FROM calendars"
        );
        s.execute("DROP TABLE calendars");
        s.execute("ALTER TABLE calendars_new RENAME TO calendars");
    }
}
