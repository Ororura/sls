package com.ororura.slseleven.application.service;

import com.ororura.slseleven.application.port.CalendarContext;
import com.ororura.slseleven.domain.model.AppCalendar;
import com.ororura.slseleven.domain.repository.CalendarRepository;
import java.util.List;

public class CalendarManagementService {

    private final CalendarRepository calendarRepository;
    private final CalendarContext calendarContext;

    public CalendarManagementService(
        CalendarRepository calendarRepository,
        CalendarContext calendarContext
    ) {
        this.calendarRepository = calendarRepository;
        this.calendarContext = calendarContext;
    }

    public List<AppCalendar> getCalendars() {
        return calendarRepository.findAllCalendars();
    }

    public AppCalendar createCalendar(String name) {
        return calendarRepository.createCalendar(name);
    }

    public AppCalendar createCalendar(String name, String directoryPath) {
        return calendarRepository.createCalendar(name, directoryPath);
    }

    public void renameCalendar(String calendarId, String newName) {
        calendarRepository.renameCalendar(calendarId, newName);
    }

    public void moveCalendarToDirectory(String calendarId, String directoryPath) {
        calendarRepository.moveCalendarToDirectory(calendarId, directoryPath);
    }

    public List<String> getCalendarDirectories() {
        return calendarRepository.getCalendarDirectories();
    }

    public void deleteCalendar(String calendarId) {
        List<AppCalendar> calendars = calendarRepository.findAllCalendars();
        if (calendars.size() <= 1) {
            throw new IllegalStateException("Нельзя удалить единственный календарь");
        }

        boolean deletingActiveCalendar = currentCalendarId().equals(calendarId);
        String fallbackCalendarId = calendars
            .stream()
            .map(AppCalendar::getId)
            .filter(id -> !id.equals(calendarId))
            .findFirst()
            .orElseThrow(() ->
                new IllegalStateException("Не удалось определить резервный календарь")
            );

        calendarRepository.deleteCalendar(calendarId);
        if (deletingActiveCalendar) {
            calendarContext.setCurrentCalendarId(fallbackCalendarId);
        }
    }

    private String currentCalendarId() {
        return calendarContext.getCurrentCalendarId();
    }
}
