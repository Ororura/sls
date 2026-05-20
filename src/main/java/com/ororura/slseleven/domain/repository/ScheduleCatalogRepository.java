package com.ororura.slseleven.domain.repository;

import com.ororura.slseleven.domain.model.InstructorDuty;
import com.ororura.slseleven.domain.model.InstructorProfile;
import com.ororura.slseleven.domain.model.RoomProfile;
import com.ororura.slseleven.domain.model.SubjectScheduleRule;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface ScheduleCatalogRepository {
    Map<DayOfWeek, Integer> getMaxHoursByDay(String calendarId);

    void saveMaxHoursByDay(String calendarId, Map<DayOfWeek, Integer> maxHoursByDay);

    List<LocalTime> getScheduleSlots(String calendarId);

    void saveScheduleSlots(String calendarId, List<LocalTime> slots);

    List<SubjectScheduleRule> getSubjectRules(String calendarId);

    void saveSubjectRules(String calendarId, List<SubjectScheduleRule> rules);

    List<InstructorProfile> getInstructors(String calendarId);

    InstructorProfile createInstructor(String calendarId, String name);

    void updateInstructor(String calendarId, InstructorProfile instructor);

    void deleteInstructor(String calendarId, String instructorId);

    List<RoomProfile> getRooms(String calendarId);

    RoomProfile createRoom(String calendarId, String name);

    void renameRoom(String calendarId, String roomId, String newName);

    void deleteRoom(String calendarId, String roomId);

    List<InstructorDuty> getInstructorDuties(String calendarId);

    void addInstructorDuty(String calendarId, String instructorId, LocalDate dutyDate);

    void removeInstructorDuty(String calendarId, String instructorId, LocalDate dutyDate);
}
