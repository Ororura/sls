package com.ororura.slseleven.domain.repository;

import com.ororura.slseleven.domain.model.AppCalendar;
import com.ororura.slseleven.domain.model.InstructorDuty;
import com.ororura.slseleven.domain.model.InstructorProfile;
import com.ororura.slseleven.domain.model.RoomProfile;
import com.ororura.slseleven.domain.model.SubjectScheduleRule;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Репозиторий для настроек автопланирования.
 */
public interface ScheduleSettingsRepository {
    Map<DayOfWeek, Integer> getMaxHoursByDay();

    void saveMaxHours(DayOfWeek dayOfWeek, int maxHours);

    void saveAll(Map<DayOfWeek, Integer> maxHoursByDay);

    List<SubjectScheduleRule> getSubjectRules();

    void saveSubjectRules(List<SubjectScheduleRule> rules);

    List<InstructorProfile> getInstructors();

    InstructorProfile createInstructor(String name);

    void updateInstructor(InstructorProfile instructor);

    void deleteInstructor(String instructorId);

    List<RoomProfile> getRooms();

    RoomProfile createRoom(String name);

    void renameRoom(String roomId, String newName);

    void deleteRoom(String roomId);

    List<InstructorDuty> getInstructorDuties();

    void addInstructorDuty(String instructorId, LocalDate dutyDate);

    void removeInstructorDuty(String instructorId, LocalDate dutyDate);

    List<AppCalendar> findAllCalendars();

    AppCalendar createCalendar(String name);

    AppCalendar createCalendar(String name, String directoryPath);

    void renameCalendar(String calendarId, String newName);

    void moveCalendarToDirectory(String calendarId, String directoryPath);

    List<String> getCalendarDirectories();

    void deleteCalendar(String calendarId);

    String getActiveCalendarId();

    void setActiveCalendarId(String calendarId);
}
