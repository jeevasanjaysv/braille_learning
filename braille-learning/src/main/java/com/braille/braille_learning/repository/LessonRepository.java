package com.braille.braille_learning.repository;

import com.braille.braille_learning.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCategory(String category);

    List<Lesson> findByCategoryOrderByLessonOrderAsc(String category);

    List<Lesson> findAllByOrderByLessonOrderAsc();

    Optional<Lesson> findByLessonOrder(Integer lessonOrder);

    Optional<Lesson> findByTitle(String title);

    boolean existsByTitle(String title);

    boolean existsByLessonOrder(Integer lessonOrder);
}
