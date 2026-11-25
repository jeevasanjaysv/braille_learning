package com.braille.braille_learning.repository;

import com.braille.braille_learning.entity.BrailleCharacter;
import com.braille.braille_learning.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrailleCharacterRepository extends JpaRepository<BrailleCharacter, Long> {

    List<BrailleCharacter> findByLessonId(Long lessonId);

    List<BrailleCharacter> findByLesson(Lesson lesson);

    List<BrailleCharacter> findByLessonIdOrderByCharacterAsc(Long lessonId);

    Optional<BrailleCharacter> findByLessonIdAndCharacter(Long lessonId, String character);

    boolean existsByLessonIdAndCharacter(Long lessonId, String character);
}
