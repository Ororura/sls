package com.ororura.slseleven.application.usecase;

import java.time.LocalDate;
import java.util.List;

public class AutoScheduleResult {
    private final int createdLessons;
    private final LocalDate lastScheduledDate;
    private final int remainingHours;
    private final List<RemainingScheduleItem> remainingItems;
    private final boolean dryRun;

    /**
     * Метод AutoScheduleResult.
     */
    public AutoScheduleResult(
        int createdLessons,
        LocalDate lastScheduledDate,
        int remainingHours,
        List<RemainingScheduleItem> remainingItems
    ) {
        this(createdLessons, lastScheduledDate, remainingHours, remainingItems, false);
    }

    public AutoScheduleResult(
        int createdLessons,
        LocalDate lastScheduledDate,
        int remainingHours,
        List<RemainingScheduleItem> remainingItems,
        boolean dryRun
    ) {
        this.createdLessons = createdLessons;
        this.lastScheduledDate = lastScheduledDate;
        this.remainingHours = remainingHours;
        this.remainingItems = remainingItems == null
            ? List.of()
            : List.copyOf(remainingItems);
        this.dryRun = dryRun;
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

    public boolean isDryRun() {
        return dryRun;
    }

    public static final class RemainingScheduleItem {
        private final String topic;
        private final String lessonName;
        private final String className;
        private final int hours;
        private final List<String> reasons;

        /**
         * Метод RemainingScheduleItem.
         */
        public RemainingScheduleItem(
            String topic,
            String lessonName,
            String className,
            int hours
        ) {
            this(topic, lessonName, className, hours, List.of());
        }

        public RemainingScheduleItem(
            String topic,
            String lessonName,
            String className,
            int hours,
            List<String> reasons
        ) {
            this.topic = topic;
            this.lessonName = lessonName;
            this.className = className;
            this.hours = hours;
            this.reasons = reasons == null ? List.of() : List.copyOf(reasons);
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

        public List<String> getReasons() {
            return reasons;
        }
    }
}
