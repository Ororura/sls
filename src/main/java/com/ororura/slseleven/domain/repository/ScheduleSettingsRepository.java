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

    /**
     * Метод saveMaxHours.
     */
    void saveMaxHours(DayOfWeek dayOfWeek, int maxHours);

    /**
     * Метод saveAll.
     */
    void saveAll(Map<DayOfWeek, Integer> maxHoursByDay);

    /**
     * Метод getSubjectRules.
     */
    List<SubjectScheduleRule> getSubjectRules();

    /**
     * Метод saveSubjectRules.
     */
    void saveSubjectRules(List<SubjectScheduleRule> rules);

    /**
     * Метод getInstructors.
     */
    List<InstructorProfile> getInstructors();

    /**
     * Метод createInstructor.
     */
    InstructorProfile createInstructor(String name);

    /**
     * Метод updateInstructor.
     */
    void updateInstructor(InstructorProfile instructor);

    /**
     * Метод deleteInstructor.
     */
    void deleteInstructor(String instructorId);

    /**
     * Метод getRooms.
     */
    List<RoomProfile> getRooms();

    /**
     * Метод createRoom.
     */
    RoomProfile createRoom(String name);

    /**
     * Метод renameRoom.
     */
    void renameRoom(String roomId, String newName);

    /**
     * Метод deleteRoom.
     */
    void deleteRoom(String roomId);

    /**
     * Метод getInstructorDuties.
     */
    List<InstructorDuty> getInstructorDuties();

    /**
     * Метод addInstructorDuty.
     */
    void addInstructorDuty(String instructorId, LocalDate dutyDate);

    /**
     * Метод removeInstructorDuty.
     */
    void removeInstructorDuty(String instructorId, LocalDate dutyDate);

    /**
     * Метод findAllCalendars.
     */
    List<AppCalendar> findAllCalendars();

    /**
     * Метод createCalendar.
     */
    AppCalendar createCalendar(String name);

    /**
     * Метод createCalendar.
     */
    AppCalendar createCalendar(String name, String directoryPath);

    /**
     * Метод renameCalendar.
     */
    void renameCalendar(String calendarId, String newName);

    /**
     * Метод moveCalendarToDirectory.
     */
    void moveCalendarToDirectory(String calendarId, String directoryPath);

    /**
     * Метод getCalendarDirectories.
     */
    List<String> getCalendarDirectories();

    /**
     * Метод deleteCalendar.
     */
    void deleteCalendar(String calendarId);

    /**
     * Метод getActiveCalendarId.
     */
    String getActiveCalendarId();

    /**
     * Метод setActiveCalendarId.
     */
    void setActiveCalendarId(String calendarId);
}
