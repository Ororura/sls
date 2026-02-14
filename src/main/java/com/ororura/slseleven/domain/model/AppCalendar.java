package com.ororura.slseleven.domain.model;

import java.util.Objects;

public class AppCalendar {
    private String id;
    private String name;
    private String directoryPath;

    public AppCalendar(String id, String name) {
        this(id, name, "");
    }

    public AppCalendar(String id, String name, String directoryPath) {
        this.id = id;
        this.name = name;
        this.directoryPath = directoryPath == null ? "" : directoryPath.trim();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath == null
            ? ""
            : directoryPath.trim();
    }

    public String getDisplayName() {
        if (directoryPath == null || directoryPath.isBlank()) {
            return name;
        }
        return directoryPath.replace("/", " / ") + " / " + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppCalendar that = (AppCalendar) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
