package com.ororura.slseleven.domain.model;

import java.time.LocalDate;

public class InstructorDuty {
    private final String instructorId;
    private final LocalDate dutyDate;

    public InstructorDuty(String instructorId, LocalDate dutyDate) {
        this.instructorId = instructorId;
        this.dutyDate = dutyDate;
    }

    public String getInstructorId() {
        return instructorId;
    }

    public LocalDate getDutyDate() {
        return dutyDate;
    }
}
