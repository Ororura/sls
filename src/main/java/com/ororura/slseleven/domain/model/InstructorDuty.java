package com.ororura.slseleven.domain.model;

import java.time.LocalDate;

public class InstructorDuty {
    private final String instructorId;
    private final LocalDate dutyDate;

    /**
     * Метод InstructorDuty.
     */
    public InstructorDuty(String instructorId, LocalDate dutyDate) {
        this.instructorId = instructorId;
        this.dutyDate = dutyDate;
    }

    /**
     * Метод getInstructorId.
     */
    public String getInstructorId() {
        return instructorId;
    }

    /**
     * Метод getDutyDate.
     */
    public LocalDate getDutyDate() {
        return dutyDate;
    }
}
