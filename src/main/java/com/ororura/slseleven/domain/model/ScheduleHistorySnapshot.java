package com.ororura.slseleven.domain.model;

import java.time.LocalDateTime;

public class ScheduleHistorySnapshot {
    private String id;
    private LocalDateTime createdAt;
    private String label;
    private String lessonsBlob;
    private String scheduleItemsBlob;

    public ScheduleHistorySnapshot() {}

    public ScheduleHistorySnapshot(
        String id,
        LocalDateTime createdAt,
        String label,
        String lessonsBlob,
        String scheduleItemsBlob
    ) {
        this.id = id;
        this.createdAt = createdAt;
        this.label = label;
        this.lessonsBlob = lessonsBlob;
        this.scheduleItemsBlob = scheduleItemsBlob;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLessonsBlob() {
        return lessonsBlob;
    }

    public void setLessonsBlob(String lessonsBlob) {
        this.lessonsBlob = lessonsBlob;
    }

    public String getScheduleItemsBlob() {
        return scheduleItemsBlob;
    }

    public void setScheduleItemsBlob(String scheduleItemsBlob) {
        this.scheduleItemsBlob = scheduleItemsBlob;
    }
}
