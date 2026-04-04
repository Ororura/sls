package com.ororura.slseleven.domain.repository;

import com.ororura.slseleven.domain.model.AppCalendar;
import java.util.List;

public interface CalendarRepository {
    List<AppCalendar> findAllCalendars();

    AppCalendar createCalendar(String name);

    AppCalendar createCalendar(String name, String directoryPath);

    void renameCalendar(String calendarId, String newName);

    void moveCalendarToDirectory(String calendarId, String directoryPath);

    List<String> getCalendarDirectories();

    void deleteCalendar(String calendarId);
}
