package com.ororura.slseleven.usecase;

import java.time.LocalDate;

public class AutoScheduleResult {
    private final int createdLessons;
    private final LocalDate lastScheduledDate;
    private final int remainingHours;

    public AutoScheduleResult(int createdLessons, LocalDate lastScheduledDate, int remainingHours) {
        this.createdLessons = createdLessons;
        this.lastScheduledDate = lastScheduledDate;
        this.remainingHours = remainingHours;
    }

    public int getCreatedLessons() {
        return createdLessons;
    }

    public LocalDate getLastScheduledDate() {
        return lastScheduledDate;
    }

    public int getRemainingHours() {
        return remainingHours;
    }
}
