package com.ororura.slseleven.infrastructure.persistence.migrations;

import com.ororura.slseleven.infrastructure.persistence.sqlite.SQLiteConnectionProvider;

import java.sql.Connection;
import java.sql.Statement;

public class SchemaInitializer {
    public static void init(SQLiteConnectionProvider provider) {
        String sql = "CREATE TABLE IF NOT EXISTS lessons (\n" + "    id TEXT PRIMARY KEY,\n" + "    topic TEXT NOT NULL,\n" + "    lesson_name TEXT NOT NULL,\n" + "    time TEXT NOT NULL,\n" + "    location TEXT NOT NULL,\n" + "    instructor TEXT NOT NULL,\n" + "    date TEXT NOT NULL\n" + ");";


        try (Connection c = provider.getConnection(); Statement s = c.createStatement()) {
            s.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
