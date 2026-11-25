package com.braille.braille_learning.repository;

import com.braille.braille_learning.entity.Lesson;
import com.braille.braille_learning.entity.LessonProgress;
import com.braille.braille_learning.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    Optional<LessonProgress> findByUserAndLesson(User user, Lesson lesson);

    List<LessonProgress> findByUserId(Long userId);

    List<LessonProgress> findByUserIdAndCompletedTrueOrderByCompletedAtDesc(Long userId);

    List<LessonProgress> findByUserIdAndCompletedTrue(Long userId);

    List<LessonProgress> findByUserAndCompletedTrue(User user);

    boolean existsByUserIdAndLessonIdAndCompletedTrue(Long userId, Long lessonId);

    long countByUserIdAndCompletedTrue(Long userId);
}
