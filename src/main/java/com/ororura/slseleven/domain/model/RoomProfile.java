package com.ororura.slseleven.domain.model;

public class RoomProfile {
    private String id;
    private String name;

    public RoomProfile(String id, String name) {
        this.id = id;
        this.name = name == null ? "" : name.trim();
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
        this.name = name == null ? "" : name.trim();
    }
}
