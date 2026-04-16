package com.ororura.slseleven.application.service;

import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.domain.model.ScheduleItem;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ScheduleSnapshotCodec {

    public String serializeLessons(List<Lesson> lessons) {
        StringBuilder sb = new StringBuilder();
        for (Lesson lesson : lessons) {
            sb
                .append(encode(lesson.getId()))
                .append('\t')
                .append(encode(lesson.getTopic()))
                .append('\t')
                .append(encode(lesson.getLessonName()))
                .append('\t')
                .append(encode(lesson.getClassName()))
                .append('\t')
                .append(lesson.isAutoScheduled() ? "1" : "0")
                .append('\t')
                .append(lesson.isArchived() ? "1" : "0")
                .append('\t')
                .append(Math.max(1, lesson.getDurationHours()))
                .append('\t')
                .append(lesson.getTime())
                .append('\t')
                .append(encode(lesson.getLocation()))
                .append('\t')
                .append(encode(lesson.getInstructor()))
                .append('\t')
                .append(lesson.getDate())
                .append('\n');
        }
        return sb.toString();
    }

    public String serializeScheduleItems(List<ScheduleItem> items) {
        StringBuilder sb = new StringBuilder();
        for (ScheduleItem item : items) {
            sb
                .append(encode(item.getId()))
                .append('\t')
                .append(encode(item.getTopic()))
                .append('\t')
                .append(encode(item.getLessonName()))
                .append('\t')
                .append(encode(item.getClassName()))
                .append('\t')
                .append(encode(item.getLocation()))
                .append('\t')
                .append(encode(item.getInstructor()))
                .append('\t')
                .append(item.getHours())
                .append('\t')
                .append(Math.max(1, item.getConsecutiveHours()))
                .append('\t')
                .append(item.getCreatedAt())
                .append('\n');
        }
        return sb.toString();
    }

    public List<Lesson> deserializeLessons(String blob) {
        List<Lesson> lessons = new ArrayList<>();
        if (blob == null || blob.isBlank()) {
            return lessons;
        }
        String[] lines = blob.split("\\R");
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            if (parts.length < 10) {
                continue;
            }
            Lesson lesson = new Lesson();
            lesson.setId(decode(parts[0]));
            lesson.setTopic(decode(parts[1]));
            lesson.setLessonName(decode(parts[2]));
            lesson.setClassName(decode(parts[3]));
            lesson.setAutoScheduled("1".equals(parts[4]));
            lesson.setArchived("1".equals(parts[5]));
            if (parts.length >= 11) {
                lesson.setDurationHours(Math.max(1, Integer.parseInt(parts[6])));
                lesson.setTime(LocalTime.parse(parts[7]));
                lesson.setLocation(decode(parts[8]));
                lesson.setInstructor(decode(parts[9]));
                lesson.setDate(LocalDate.parse(parts[10]));
            } else {
                lesson.setDurationHours(1);
                lesson.setTime(LocalTime.parse(parts[6]));
                lesson.setLocation(decode(parts[7]));
                lesson.setInstructor(decode(parts[8]));
                lesson.setDate(LocalDate.parse(parts[9]));
            }
            lessons.add(lesson);
        }
        return lessons;
    }

    public List<ScheduleItem> deserializeScheduleItems(String blob) {
        List<ScheduleItem> items = new ArrayList<>();
        if (blob == null || blob.isBlank()) {
            return items;
        }
        String[] lines = blob.split("\\R");
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            if (parts.length < 8) {
                continue;
            }
            ScheduleItem item = new ScheduleItem();
            item.setId(decode(parts[0]));
            item.setTopic(decode(parts[1]));
            item.setLessonName(decode(parts[2]));
            item.setClassName(decode(parts[3]));
            item.setLocation(decode(parts[4]));
            item.setInstructor(decode(parts[5]));
            item.setHours(Integer.parseInt(parts[6]));
            if (parts.length >= 9) {
                item.setConsecutiveHours(
                    Math.max(1, Integer.parseInt(parts[7]))
                );
                item.setCreatedAt(LocalDateTime.parse(parts[8]));
            } else {
                item.setConsecutiveHours(1);
                item.setCreatedAt(LocalDateTime.parse(parts[7]));
            }
            items.add(item);
        }
        return items;
    }

    private String encode(String value) {
        String safe = value == null ? "" : value;
        return Base64
            .getEncoder()
            .encodeToString(safe.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return new String(
            Base64.getDecoder().decode(value),
            StandardCharsets.UTF_8
        );
    }
}
