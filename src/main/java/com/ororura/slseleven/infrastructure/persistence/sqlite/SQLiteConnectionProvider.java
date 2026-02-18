package com.ororura.slseleven.infrastructure.persistence.sqlite;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

public class SQLiteConnectionProvider {
    private final String url;

    /**
     * Метод SQLiteConnectionProvider.
     */
    public SQLiteConnectionProvider(Path path) {
        this.url = "jdbc:sqlite:" + path.toAbsolutePath();
    }

    /**
     * Метод getConnection.
     */
    public Connection getConnection() throws Exception {
        return DriverManager.getConnection(url);
    }
}
