package com.ororura.slseleven.domain.model;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class SubjectScheduleRule {
    private final String subject;
    private final Set<DayOfWeek> allowedDays;
    private final Set<DayOfWeek> exclusiveDays;
    private final int consecutiveHours;
    private final int maxLessonsPerDay;
    private final String fixedRoom;
    private final Set<String> noConsecutiveWithSubjects;
    private final Set<String> noSameDayWithSubjects;

    public SubjectScheduleRule(
        String subject,
        Set<DayOfWeek> allowedDays,
        Set<DayOfWeek> exclusiveDays
    ) {
        this(subject, allowedDays, exclusiveDays, 1, "");
    }

    public SubjectScheduleRule(
        String subject,
        Set<DayOfWeek> allowedDays,
        Set<DayOfWeek> exclusiveDays,
        int consecutiveHours
    ) {
        this(subject, allowedDays, exclusiveDays, consecutiveHours, "");
    }

    public SubjectScheduleRule(
        String subject,
        Set<DayOfWeek> allowedDays,
        Set<DayOfWeek> exclusiveDays,
        int consecutiveHours,
        String fixedRoom
    ) {
        this(
            subject,
            allowedDays,
            exclusiveDays,
            consecutiveHours,
            0,
            fixedRoom,
            Set.of(),
            Set.of()
        );
    }

    public SubjectScheduleRule(
        String subject,
        Set<DayOfWeek> allowedDays,
        Set<DayOfWeek> exclusiveDays,
        int consecutiveHours,
        int maxLessonsPerDay,
        String fixedRoom
    ) {
        this(
            subject,
            allowedDays,
            exclusiveDays,
            consecutiveHours,
            maxLessonsPerDay,
            fixedRoom,
            Set.of(),
            Set.of()
        );
    }

    public SubjectScheduleRule(
        String subject,
        Set<DayOfWeek> allowedDays,
        Set<DayOfWeek> exclusiveDays,
        int consecutiveHours,
        String fixedRoom,
        Set<String> noConsecutiveWithSubjects,
        Set<String> noSameDayWithSubjects
    ) {
        this(
            subject,
            allowedDays,
            exclusiveDays,
            consecutiveHours,
            0,
            fixedRoom,
            noConsecutiveWithSubjects,
            noSameDayWithSubjects
        );
    }

    public SubjectScheduleRule(
        String subject,
        Set<DayOfWeek> allowedDays,
        Set<DayOfWeek> exclusiveDays,
        int consecutiveHours,
        int maxLessonsPerDay,
        String fixedRoom,
        Set<String> noConsecutiveWithSubjects,
        Set<String> noSameDayWithSubjects
    ) {
        this.subject = subject == null ? "" : subject.trim();
        this.allowedDays = allowedDays == null
            ? EnumSet.noneOf(DayOfWeek.class)
            : EnumSet.copyOf(allowedDays);
        this.exclusiveDays = exclusiveDays == null
            ? EnumSet.noneOf(DayOfWeek.class)
            : EnumSet.copyOf(exclusiveDays);
        this.consecutiveHours = Math.max(1, consecutiveHours);
        this.maxLessonsPerDay = Math.max(0, maxLessonsPerDay);
        this.fixedRoom = fixedRoom == null ? "" : fixedRoom.trim();
        this.noConsecutiveWithSubjects = normalizeSubjectSet(
            noConsecutiveWithSubjects
        );
        this.noSameDayWithSubjects = normalizeSubjectSet(noSameDayWithSubjects);
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

    public int getConsecutiveHours() {
        return consecutiveHours;
    }

    public int getMaxLessonsPerDay() {
        return maxLessonsPerDay;
    }

    public String getFixedRoom() {
        return fixedRoom;
    }

    public Set<String> getNoConsecutiveWithSubjects() {
        return Collections.unmodifiableSet(noConsecutiveWithSubjects);
    }

    public Set<String> getNoSameDayWithSubjects() {
        return Collections.unmodifiableSet(noSameDayWithSubjects);
    }

    private Set<String> normalizeSubjectSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String normalized = value.trim().toLowerCase();
            if (!normalized.isEmpty()) {
                result.add(normalized);
            }
        }
        return result.isEmpty() ? Set.of() : result;
    }
}
