package com.ororura.slseleven.infrastructure.persistence.sqlite;

import com.ororura.slseleven.domain.model.Lesson;

import java.util.Optional;

public interface LessonRepository {
    void save(Lesson lesson);
    Optional<Lesson> findById(long id);
}
