package com.ororura.slseleven.infrastructure.bootstrap;

import com.ororura.slseleven.application.usecase.LessonUseCase;
import com.ororura.slseleven.application.usecase.ScheduleUseCase;

public class ApplicationServices {

    private final LessonUseCase lessonUseCase;
    private final ScheduleUseCase scheduleUseCase;

    public ApplicationServices(
        LessonUseCase lessonUseCase,
        ScheduleUseCase scheduleUseCase
    ) {
        this.lessonUseCase = lessonUseCase;
        this.scheduleUseCase = scheduleUseCase;
    }

    public LessonUseCase getLessonUseCase() {
        return lessonUseCase;
    }

    public ScheduleUseCase getScheduleUseCase() {
        return scheduleUseCase;
    }
}
