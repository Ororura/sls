package com.ororura.slseleven.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Элемент списка расписаний (кол-во часов для распределения).
 */
public class ScheduleItem {
    public static final String DEFAULT_CALENDAR_ID = CalendarDefaults.DEFAULT_ID;

    private String id;
    private String calendarId;
    private String topic;
    private String lessonName;
    private String className;
    private String location;
    private String instructor;
    private int hours;
    private int consecutiveHours;
    private LocalDateTime createdAt;

    public ScheduleItem() {
        this.id = java.util.UUID.randomUUID().toString();
        this.calendarId = DEFAULT_CALENDAR_ID;
        this.consecutiveHours = 1;
        this.createdAt = LocalDateTime.now();
    }

    public ScheduleItem(
        String topic,
        String lessonName,
        String className,
        String location,
        String instructor,
        int hours
    ) {
        this();
        this.topic = topic;
        this.lessonName = lessonName;
        this.className = className;
        this.location = location;
        this.instructor = instructor;
        this.hours = hours;
    }

    public ScheduleItem(
        String topic,
        String lessonName,
        String location,
        String instructor,
        int hours
    ) {
        this(topic, lessonName, lessonName, location, instructor, hours);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getLessonName() {
        return lessonName;
    }

    public void setLessonName(String lessonName) {
        this.lessonName = lessonName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public int getConsecutiveHours() {
        return consecutiveHours;
    }

    public void setConsecutiveHours(int consecutiveHours) {
        this.consecutiveHours = consecutiveHours;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduleItem that = (ScheduleItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
