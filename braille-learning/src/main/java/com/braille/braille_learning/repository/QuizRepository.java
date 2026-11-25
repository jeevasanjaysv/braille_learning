package com.braille.braille_learning.repository;

import com.braille.braille_learning.entity.Lesson;
import com.braille.braille_learning.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByLessonId(Long lessonId);

    List<Quiz> findByLesson(Lesson lesson);

    Optional<Quiz> findByTitle(String title);

    boolean existsByTitle(String title);
}
