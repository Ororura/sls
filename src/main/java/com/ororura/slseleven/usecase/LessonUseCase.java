package com.ororura.slseleven.usecase;

import com.ororura.slseleven.domain.model.Lesson;
import com.ororura.slseleven.domain.repository.LessonRepository;

import java.time.LocalDate;
import java.time.LocalTime;

public class CreateLessonUseCase {
    private final LessonRepository lessonRepository;

    public CreateLessonUseCase(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    public void execute(String topic, String lessonName, LocalTime time, String location,
                        String instructor, LocalDate date) {
        Lesson lesson = new Lesson(topic, lessonName, time, location, instructor, date);
        lessonRepository.save(lesson);
    }
}
