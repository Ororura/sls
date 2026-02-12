package com.ororura.slseleven.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Доменная модель занятия
 */
public class Lesson {
    public static final String DEFAULT_CALENDAR_ID = "default";

    private String id;
    private String calendarId;
    private String topic; // Предмет
    private String lessonName; // Тема
    private String className; // Занятие
    private boolean autoScheduled; // Создано автораспределением
    private boolean archived; // Архивное занятие
    private LocalTime time; // Время
    private String location; // Расположение проведения занятия
    private String instructor; // Кто проводит
    private LocalDate date; // Дата занятия
    private int durationHours; // Длительность занятия в академических часах подряд

    public Lesson() {
        this.id = java.util.UUID.randomUUID().toString();
        this.calendarId = DEFAULT_CALENDAR_ID;
        this.durationHours = 1;
    }

    public Lesson(
        String topic,
        String lessonName,
        String className,
        LocalTime time,
        String location,
        String instructor,
        LocalDate date
    ) {
        this();
        this.topic = topic;
        this.lessonName = lessonName;
        this.className = className;
        this.autoScheduled = false;
        this.archived = false;
        this.durationHours = 1;
        this.time = time;
        this.location = location;
        this.instructor = instructor;
        this.date = date;
    }

    public Lesson(
        String topic,
        String lessonName,
        String className,
        boolean autoScheduled,
        LocalTime time,
        String location,
        String instructor,
        LocalDate date
    ) {
        this(topic, lessonName, className, time, location, instructor, date);
        this.autoScheduled = autoScheduled;
    }

    public Lesson(
        String topic,
        String lessonName,
        LocalTime time,
        String location,
        String instructor,
        LocalDate date
    ) {
        this(topic, lessonName, lessonName, time, location, instructor, date);
    }

    // Getters and Setters
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

    public boolean isAutoScheduled() {
        return autoScheduled;
    }

    public void setAutoScheduled(boolean autoScheduled) {
        this.autoScheduled = autoScheduled;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(int durationHours) {
        this.durationHours = durationHours;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lesson lesson = (Lesson) o;
        return Objects.equals(id, lesson.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%s - %s (%s)", time, lessonName, instructor);
    }
}
