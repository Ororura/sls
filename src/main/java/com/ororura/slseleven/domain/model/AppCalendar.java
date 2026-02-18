package com.ororura.slseleven.domain.model;

import java.util.Objects;

public class AppCalendar {
    private String id;
    private String name;
    private String directoryPath;

    /**
     * Метод AppCalendar.
     */
    public AppCalendar(String id, String name) {
        this(id, name, "");
    }

    /**
     * Метод AppCalendar.
     */
    public AppCalendar(String id, String name, String directoryPath) {
        this.id = id;
        this.name = name;
        this.directoryPath = directoryPath == null ? "" : directoryPath.trim();
    }

    /**
     * Метод getId.
     */
    public String getId() {
        return id;
    }

    /**
     * Метод setId.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Метод getName.
     */
    public String getName() {
        return name;
    }

    /**
     * Метод setName.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Метод getDirectoryPath.
     */
    public String getDirectoryPath() {
        return directoryPath;
    }

    /**
     * Метод setDirectoryPath.
     */
    public void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath == null
            ? ""
            : directoryPath.trim();
    }

    /**
     * Метод getDisplayName.
     */
    public String getDisplayName() {
        if (directoryPath == null || directoryPath.isBlank()) {
            return name;
        }
        return directoryPath.replace("/", " / ") + " / " + name;
    }

    /**
     * Метод equals.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppCalendar that = (AppCalendar) o;
        return Objects.equals(id, that.id);
    }

    /**
     * Метод hashCode.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Метод toString.
     */
    @Override
    public String toString() {
        return getDisplayName();
    }
}
