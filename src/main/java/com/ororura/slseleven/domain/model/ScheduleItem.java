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

    /**
     * Метод ScheduleItem.
     */
    public ScheduleItem() {
        this.id = java.util.UUID.randomUUID().toString();
        this.calendarId = DEFAULT_CALENDAR_ID;
        this.consecutiveHours = 1;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Метод ScheduleItem.
     */
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

    /**
     * Метод ScheduleItem.
     */
    public ScheduleItem(
        String topic,
        String lessonName,
        String location,
        String instructor,
        int hours
    ) {
        this(topic, lessonName, lessonName, location, instructor, hours);
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
     * Метод getTopic.
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Метод setTopic.
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * Метод getLessonName.
     */
    public String getLessonName() {
        return lessonName;
    }

    /**
     * Метод setLessonName.
     */
    public void setLessonName(String lessonName) {
        this.lessonName = lessonName;
    }

    /**
     * Метод getClassName.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Метод setClassName.
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Метод getLocation.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Метод setLocation.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Метод getInstructor.
     */
    public String getInstructor() {
        return instructor;
    }

    /**
     * Метод setInstructor.
     */
    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    /**
     * Метод getHours.
     */
    public int getHours() {
        return hours;
    }

    /**
     * Метод setHours.
     */
    public void setHours(int hours) {
        this.hours = hours;
    }

    /**
     * Метод getConsecutiveHours.
     */
    public int getConsecutiveHours() {
        return consecutiveHours;
    }

    /**
     * Метод setConsecutiveHours.
     */
    public void setConsecutiveHours(int consecutiveHours) {
        this.consecutiveHours = consecutiveHours;
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
     * Метод equals.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduleItem that = (ScheduleItem) o;
        return Objects.equals(id, that.id);
    }

    /**
     * Метод hashCode.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
