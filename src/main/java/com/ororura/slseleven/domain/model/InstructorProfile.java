package com.ororura.slseleven.domain.model;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

public class InstructorProfile {
    private String id;
    private String name;
    private Set<DayOfWeek> allowedDays;

    /**
     * Метод InstructorProfile.
     */
    public InstructorProfile(String id, String name, Set<DayOfWeek> allowedDays) {
        this.id = id;
        this.name = name == null ? "" : name.trim();
        this.allowedDays = allowedDays == null || allowedDays.isEmpty()
            ? EnumSet.allOf(DayOfWeek.class)
            : EnumSet.copyOf(allowedDays);
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

    /**
     * Метод getAllowedDays.
     */
    public Set<DayOfWeek> getAllowedDays() {
        return EnumSet.copyOf(allowedDays);
    }

    /**
     * Метод setAllowedDays.
     */
    public void setAllowedDays(Set<DayOfWeek> allowedDays) {
        this.allowedDays = allowedDays == null || allowedDays.isEmpty()
            ? EnumSet.allOf(DayOfWeek.class)
            : EnumSet.copyOf(allowedDays);
    }

    /**
     * Метод isAllowedOn.
     */
    public boolean isAllowedOn(DayOfWeek day) {
        return allowedDays.contains(day);
    }
}
