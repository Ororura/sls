package com.ororura.slseleven.application.usecase;

import java.time.LocalDate;
import java.util.List;

public class AutoScheduleResult {
    private final int createdLessons;
    private final LocalDate lastScheduledDate;
    private final int remainingHours;
    private final List<RemainingScheduleItem> remainingItems;

    /**
     * Метод AutoScheduleResult.
     */
    public AutoScheduleResult(
        int createdLessons,
        LocalDate lastScheduledDate,
        int remainingHours,
        List<RemainingScheduleItem> remainingItems
    ) {
        this.createdLessons = createdLessons;
        this.lastScheduledDate = lastScheduledDate;
        this.remainingHours = remainingHours;
        this.remainingItems = remainingItems == null
            ? List.of()
            : List.copyOf(remainingItems);
    }

    /**
     * Метод getCreatedLessons.
     */
    public int getCreatedLessons() {
        return createdLessons;
    }

    /**
     * Метод getLastScheduledDate.
     */
    public LocalDate getLastScheduledDate() {
        return lastScheduledDate;
    }

    /**
     * Метод getRemainingHours.
     */
    public int getRemainingHours() {
        return remainingHours;
    }

    /**
     * Метод getRemainingItems.
     */
    public List<RemainingScheduleItem> getRemainingItems() {
        return remainingItems;
    }

    public static final class RemainingScheduleItem {
        private final String topic;
        private final String lessonName;
        private final String className;
        private final int hours;

        /**
         * Метод RemainingScheduleItem.
         */
        public RemainingScheduleItem(
            String topic,
            String lessonName,
            String className,
            int hours
        ) {
            this.topic = topic;
            this.lessonName = lessonName;
            this.className = className;
            this.hours = hours;
        }

        /**
         * Метод getTopic.
         */
        public String getTopic() {
            return topic;
        }

        /**
         * Метод getLessonName.
         */
        public String getLessonName() {
            return lessonName;
        }

        /**
         * Метод getClassName.
         */
        public String getClassName() {
            return className;
        }

        /**
         * Метод getHours.
         */
        public int getHours() {
            return hours;
        }
    }
}
