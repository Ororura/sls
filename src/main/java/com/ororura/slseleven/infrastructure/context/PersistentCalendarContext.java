package com.ororura.slseleven.infrastructure.context;

import com.ororura.slseleven.application.port.CalendarContext;
import com.ororura.slseleven.domain.repository.ActiveCalendarRepository;

public class PersistentCalendarContext implements CalendarContext {

    private final ActiveCalendarRepository activeCalendarRepository;

    public PersistentCalendarContext(
        ActiveCalendarRepository activeCalendarRepository
    ) {
        if (activeCalendarRepository == null) {
            throw new IllegalArgumentException("Репозиторий активного календаря не может быть null");
        }
        this.activeCalendarRepository = activeCalendarRepository;
    }

    @Override
    public String getCurrentCalendarId() {
        return activeCalendarRepository.getActiveCalendarId();
    }

    @Override
    public void setCurrentCalendarId(String calendarId) {
        if (calendarId == null || calendarId.isBlank()) {
            throw new IllegalArgumentException("ID календаря не может быть пустым");
        }
        activeCalendarRepository.setActiveCalendarId(calendarId);
    }
}
