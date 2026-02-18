package com.ororura.slseleven.ui;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class UiFormatters {
    public static final Locale RU_LOCALE = new Locale("ru");
    public static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter LONG_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy", RU_LOCALE);
    public static final DateTimeFormatter MONTH_YEAR_FORMATTER =
            DateTimeFormatter.ofPattern("LLLL yyyy", RU_LOCALE);

    /**
     * Метод UiFormatters.
     */
    private UiFormatters() {
    }
}
