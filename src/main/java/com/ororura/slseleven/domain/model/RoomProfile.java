package com.ororura.slseleven.domain.model;

public class RoomProfile {
    private String id;
    private String name;

    /**
     * Метод RoomProfile.
     */
    public RoomProfile(String id, String name) {
        this.id = id;
        this.name = name == null ? "" : name.trim();
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
        this.name = name == null ? "" : name.trim();
    }
}
