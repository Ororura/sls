package com.ororura.slseleven.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Доменная модель занятия
 */
public class Lesson {
    public static final String DEFAULT_CALENDAR_ID = CalendarDefaults.DEFAULT_ID;

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

    /**
     * Метод Lesson.
     */
    public Lesson() {
        this.id = java.util.UUID.randomUUID().toString();
        this.calendarId = DEFAULT_CALENDAR_ID;
        this.durationHours = 1;
    }

    /**
     * Метод Lesson.
     */
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

    /**
     * Метод Lesson.
     */
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

    /**
     * Метод Lesson.
     */
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
     * Метод isAutoScheduled.
     */
    public boolean isAutoScheduled() {
        return autoScheduled;
    }

    /**
     * Метод setAutoScheduled.
     */
    public void setAutoScheduled(boolean autoScheduled) {
        this.autoScheduled = autoScheduled;
    }

    /**
     * Метод isArchived.
     */
    public boolean isArchived() {
        return archived;
    }

    /**
     * Метод setArchived.
     */
    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    /**
     * Метод getTime.
     */
    public LocalTime getTime() {
        return time;
    }

    /**
     * Метод setTime.
     */
    public void setTime(LocalTime time) {
        this.time = time;
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
     * Метод getDate.
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Метод setDate.
     */
    public void setDate(LocalDate date) {
        this.date = date;
    }

    /**
     * Метод getDurationHours.
     */
    public int getDurationHours() {
        return durationHours;
    }

    /**
     * Метод setDurationHours.
     */
    public void setDurationHours(int durationHours) {
        this.durationHours = durationHours;
    }

    /**
     * Метод equals.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lesson lesson = (Lesson) o;
        return Objects.equals(id, lesson.id);
    }

    /**
     * Метод hashCode.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Метод toString.
     */
    @Override
    public String toString() {
        return String.format("%s - %s (%s)", time, lessonName, instructor);
    }
}
