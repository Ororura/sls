package com.ororura.slseleven.domain.model;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

public class InstructorProfile {
    private String id;
    private String name;
    private Set<DayOfWeek> allowedDays;

    public InstructorProfile(String id, String name, Set<DayOfWeek> allowedDays) {
        this.id = id;
        this.name = name == null ? "" : name.trim();
        this.allowedDays = allowedDays == null || allowedDays.isEmpty()
            ? EnumSet.allOf(DayOfWeek.class)
            : EnumSet.copyOf(allowedDays);
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

    public Set<DayOfWeek> getAllowedDays() {
        return EnumSet.copyOf(allowedDays);
    }

    public void setAllowedDays(Set<DayOfWeek> allowedDays) {
        this.allowedDays = allowedDays == null || allowedDays.isEmpty()
            ? EnumSet.allOf(DayOfWeek.class)
            : EnumSet.copyOf(allowedDays);
    }

    public boolean isAllowedOn(DayOfWeek day) {
        return allowedDays.contains(day);
    }
}
