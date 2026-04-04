package com.ororura.slseleven.application.support;

import com.ororura.slseleven.application.port.CalendarContext;

public abstract class CalendarScopedUseCase {

    private final CalendarContext calendarContext;

    protected CalendarScopedUseCase(CalendarContext calendarContext) {
        if (calendarContext == null) {
            throw new IllegalArgumentException("Контекст календаря не может быть null");
        }
        this.calendarContext = calendarContext;
    }

    public final void setCurrentCalendarId(String calendarId) {
        if (calendarId == null || calendarId.isBlank()) {
            throw new IllegalArgumentException("ID календаря не может быть пустым");
        }
        calendarContext.setCurrentCalendarId(calendarId);
    }

    public final String getCurrentCalendarId() {
        return calendarContext.getCurrentCalendarId();
    }

    protected final String currentCalendarId() {
        return calendarContext.getCurrentCalendarId();
    }
}
