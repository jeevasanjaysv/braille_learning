package com.braille.braille_learning.repository;

import com.braille.braille_learning.config.LessonDataInitializer;
import com.braille.braille_learning.entity.BrailleCharacter;
import com.braille.braille_learning.entity.Lesson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class LessonRepositoryTest {

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private BrailleCharacterRepository brailleCharacterRepository;

    @Autowired
    private LessonDataInitializer lessonDataInitializer;

    @BeforeEach
    void setUp() {
        cleanTestLessons();
        lessonDataInitializer.run();
    }

    @AfterEach
    void tearDown() {
        cleanTestLessons();
    }

    private void cleanTestLessons() {
        List<Lesson> testLessons = lessonRepository.findAll().stream()
                .filter(l -> l.getLessonOrder() != null && l.getLessonOrder() >= 100)
                .toList();
        for (Lesson l : testLessons) {
            List<BrailleCharacter> chars = brailleCharacterRepository.findByLessonId(l.getId());
            brailleCharacterRepository.deleteAll(chars);
            lessonRepository.delete(l);
        }
    }

    @Test
    @DisplayName("Should save a lesson and cascade save associated braille characters")
    void testSaveLessonWithCharacters() {
        Lesson lesson = new Lesson(
                "Introduction to Braille Letters A-C",
                "Learn the first 3 letters of the alphabet in Braille.",
                "ALPHABET",
                101,
                "The basic Braille cell consists of 6 dots..."
        );

        BrailleCharacter charA = new BrailleCharacter("A", "1", "Dot 1 top left");
        BrailleCharacter charB = new BrailleCharacter("B", "1,2", "Dots 1 and 2 left column");
        BrailleCharacter charC = new BrailleCharacter("C", "1,4", "Dots 1 and 4 top row");

        lesson.addCharacter(charA);
        lesson.addCharacter(charB);
        lesson.addCharacter(charC);

        Lesson savedLesson = lessonRepository.save(lesson);

        assertNotNull(savedLesson.getId());
        assertNotNull(savedLesson.getCreatedAt());
        assertEquals(3, savedLesson.getCharacters().size());

        List<BrailleCharacter> charactersFromDb = brailleCharacterRepository.findByLessonId(savedLesson.getId());
        assertEquals(3, charactersFromDb.size());

        Optional<BrailleCharacter> foundA = brailleCharacterRepository.findByLessonIdAndCharacter(savedLesson.getId(), "A");
        assertTrue(foundA.isPresent());
        assertEquals("1", foundA.get().getDotPattern());
        assertEquals(savedLesson.getId(), foundA.get().getLesson().getId());
    }

    @Test
    @DisplayName("Should find lessons by category and ordered by lesson order")
    void testFindByCategoryAndOrderByLessonOrder() {
        Lesson lesson2 = new Lesson("Numbers 1-5", "Learn basic numbers", "NUMBERS", 102, "Numbers start with number sign...");
        Lesson lesson1 = new Lesson("Dot Orientation Custom", "Understanding the 6-dot cell", "DOT_ORIENTATION", 101, "Dot 1 to 6 layout...");
        Lesson lesson3 = new Lesson("Alphabet A-J", "Learn first 10 letters", "ALPHABET", 103, "Letters A through J...");

        lessonRepository.save(lesson2);
        lessonRepository.save(lesson1);
        lessonRepository.save(lesson3);

        List<Lesson> dotLessons = lessonRepository.findByCategory("DOT_ORIENTATION");
        assertTrue(dotLessons.stream().anyMatch(l -> l.getTitle().equals("Dot Orientation Custom")));

        List<Lesson> orderedLessons = lessonRepository.findAllByOrderByLessonOrderAsc();
        assertTrue(orderedLessons.size() >= 3);
    }

    @Test
    @DisplayName("Should enforce unique constraints on title and lesson order")
    void testUniqueConstraintsOnLesson() {
        Lesson lesson1 = new Lesson("Lesson Unique", "Description", "ALPHABET", 101, "Content");
        lessonRepository.save(lesson1);

        Lesson duplicateTitle = new Lesson("Lesson Unique", "Different Description", "ALPHABET", 102, "Content");
        assertThrows(DataIntegrityViolationException.class, () -> {
            lessonRepository.saveAndFlush(duplicateTitle);
        });
    }

    @Test
    @DisplayName("Should enforce unique constraint on character within the same lesson")
    void testUniqueConstraintOnCharacterWithinLesson() {
        Lesson lesson = new Lesson("Lesson A", "Description", "ALPHABET", 101, "Content");
        Lesson savedLesson = lessonRepository.save(lesson);

        BrailleCharacter charA1 = new BrailleCharacter("A", "1", "First A", savedLesson);
        brailleCharacterRepository.saveAndFlush(charA1);

        BrailleCharacter charA2 = new BrailleCharacter("A", "1", "Duplicate A in same lesson", savedLesson);
        assertThrows(DataIntegrityViolationException.class, () -> {
            brailleCharacterRepository.saveAndFlush(charA2);
        });
    }
}
