package com.ororura.slseleven.application.usecase;

import java.time.LocalDateTime;

public final class ScheduleHistoryEntry {

    private final String id;
    private final LocalDateTime createdAt;
    private final String label;

    public ScheduleHistoryEntry(
        String id,
        LocalDateTime createdAt,
        String label
    ) {
        this.id = id;
        this.createdAt = createdAt;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return createdAt + " - " + label;
    }
}
