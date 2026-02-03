package com.ororura.slseleven.domain.model;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

public class SubjectScheduleRule {
    private final String subject;
    private final Set<DayOfWeek> allowedDays;
    private final Set<DayOfWeek> exclusiveDays;

    public SubjectScheduleRule(
        String subject,
        Set<DayOfWeek> allowedDays,
        Set<DayOfWeek> exclusiveDays
    ) {
        this.subject = subject == null ? "" : subject.trim();
        this.allowedDays = allowedDays == null
            ? EnumSet.noneOf(DayOfWeek.class)
            : EnumSet.copyOf(allowedDays);
        this.exclusiveDays = exclusiveDays == null
            ? EnumSet.noneOf(DayOfWeek.class)
            : EnumSet.copyOf(exclusiveDays);
    }

    public String getSubject() {
        return subject;
    }

    public Set<DayOfWeek> getAllowedDays() {
        return EnumSet.copyOf(allowedDays);
    }

    public Set<DayOfWeek> getExclusiveDays() {
        return EnumSet.copyOf(exclusiveDays);
    }

    public boolean isAllowedOn(DayOfWeek day) {
        return allowedDays.contains(day);
    }

    public boolean isExclusiveOn(DayOfWeek day) {
        return exclusiveDays.contains(day);
    }
}
