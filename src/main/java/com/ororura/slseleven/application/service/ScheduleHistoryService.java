package com.ororura.slseleven.application.service;

import com.ororura.slseleven.application.port.CalendarContext;
import com.ororura.slseleven.application.usecase.ScheduleHistoryEntry;
import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.domain.model.ScheduleHistorySnapshot;
import com.ororura.slseleven.domain.model.ScheduleItem;
import com.ororura.slseleven.domain.repository.LessonRepository;
import com.ororura.slseleven.domain.repository.ScheduleHistoryRepository;
import com.ororura.slseleven.domain.repository.ScheduleItemRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScheduleHistoryService {

    private final LessonRepository lessonRepository;
    private final ScheduleItemRepository scheduleItemRepository;
    private final ScheduleHistoryRepository scheduleHistoryRepository;
    private final CalendarContext calendarContext;
    private final ScheduleSnapshotCodec snapshotCodec;

    public ScheduleHistoryService(
        LessonRepository lessonRepository,
        ScheduleItemRepository scheduleItemRepository,
        ScheduleHistoryRepository scheduleHistoryRepository,
        CalendarContext calendarContext,
        ScheduleSnapshotCodec snapshotCodec
    ) {
        this.lessonRepository = lessonRepository;
        this.scheduleItemRepository = scheduleItemRepository;
        this.scheduleHistoryRepository = scheduleHistoryRepository;
        this.calendarContext = calendarContext;
        this.snapshotCodec = snapshotCodec;
    }

    public List<ScheduleHistoryEntry> getHistoryEntries() {
        List<ScheduleHistorySnapshot> snapshots = scheduleHistoryRepository.findAll(
            currentCalendarId()
        );
        List<ScheduleHistoryEntry> entries = new ArrayList<>();
        for (ScheduleHistorySnapshot snapshot : snapshots) {
            entries.add(
                new ScheduleHistoryEntry(
                    snapshot.getId(),
                    snapshot.getCreatedAt(),
                    snapshot.getLabel()
                )
            );
        }
        return entries;
    }

    public boolean createHistorySnapshot(String label) {
        List<Lesson> lessons = lessonRepository.findAll(currentCalendarId());
        List<ScheduleItem> items = scheduleItemRepository.findAll(currentCalendarId());
        if (lessons.isEmpty() && items.isEmpty()) {
            return false;
        }

        String snapshotId = java.util.UUID.randomUUID().toString();
        ScheduleHistorySnapshot snapshot = new ScheduleHistorySnapshot(
            snapshotId,
            currentCalendarId(),
            LocalDateTime.now(),
            label == null || label.isBlank() ? "Ручной снимок" : label,
            snapshotCodec.serializeLessons(lessons),
            snapshotCodec.serializeScheduleItems(items)
        );
        scheduleHistoryRepository.save(snapshot);
        return true;
    }

    public boolean restoreFromHistory(String historyId) {
        if (historyId == null || historyId.isBlank()) {
            throw new IllegalArgumentException("ID снимка не может быть пустым");
        }
        ScheduleHistorySnapshot snapshot = scheduleHistoryRepository
            .findById(historyId, currentCalendarId())
            .orElse(null);
        if (snapshot == null) {
            return false;
        }

        List<Lesson> lessons = snapshotCodec.deserializeLessons(
            snapshot.getLessonsBlob()
        );
        List<ScheduleItem> items = snapshotCodec.deserializeScheduleItems(
            snapshot.getScheduleItemsBlob()
        );

        lessonRepository.deleteAll(currentCalendarId());
        scheduleItemRepository.deleteAll(currentCalendarId());

        for (Lesson lesson : lessons) {
            lesson.setCalendarId(currentCalendarId());
            lessonRepository.save(lesson);
        }
        for (ScheduleItem item : items) {
            item.setCalendarId(currentCalendarId());
            scheduleItemRepository.save(item);
        }
        return true;
    }

    private String currentCalendarId() {
        return calendarContext.getCurrentCalendarId();
    }
}
