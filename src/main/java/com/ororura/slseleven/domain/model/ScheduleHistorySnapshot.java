package com.ororura.slseleven.domain.model;

import java.time.LocalDateTime;

public class ScheduleHistorySnapshot {
    private String id;
    private String calendarId;
    private LocalDateTime createdAt;
    private String label;
    private String lessonsBlob;
    private String scheduleItemsBlob;

    /**
     * Метод ScheduleHistorySnapshot.
     */
    public ScheduleHistorySnapshot() {}

    /**
     * Метод ScheduleHistorySnapshot.
     */
    public ScheduleHistorySnapshot(
        String id,
        String calendarId,
        LocalDateTime createdAt,
        String label,
        String lessonsBlob,
        String scheduleItemsBlob
    ) {
        this.id = id;
        this.calendarId = calendarId;
        this.createdAt = createdAt;
        this.label = label;
        this.lessonsBlob = lessonsBlob;
        this.scheduleItemsBlob = scheduleItemsBlob;
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
     * Метод getCalendarId.
     */
    public String getCalendarId() {
        return calendarId;
    }

    /**
     * Метод setCalendarId.
     */
    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }

    /**
     * Метод getCreatedAt.
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Метод setCreatedAt.
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Метод getLabel.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Метод setLabel.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Метод getLessonsBlob.
     */
    public String getLessonsBlob() {
        return lessonsBlob;
    }

    /**
     * Метод setLessonsBlob.
     */
    public void setLessonsBlob(String lessonsBlob) {
        this.lessonsBlob = lessonsBlob;
    }

    /**
     * Метод getScheduleItemsBlob.
     */
    public String getScheduleItemsBlob() {
        return scheduleItemsBlob;
    }

    /**
     * Метод setScheduleItemsBlob.
     */
    public void setScheduleItemsBlob(String scheduleItemsBlob) {
        this.scheduleItemsBlob = scheduleItemsBlob;
    }
}
