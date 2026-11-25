package com.braille.braille_learning.config;

import com.braille.braille_learning.entity.BrailleCharacter;
import com.braille.braille_learning.entity.Lesson;
import com.braille.braille_learning.repository.BrailleCharacterRepository;
import com.braille.braille_learning.repository.LessonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class LessonDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LessonDataInitializer.class);

    private final LessonRepository lessonRepository;
    private final BrailleCharacterRepository brailleCharacterRepository;

    public LessonDataInitializer(LessonRepository lessonRepository, BrailleCharacterRepository brailleCharacterRepository) {
        this.lessonRepository = lessonRepository;
        this.brailleCharacterRepository = brailleCharacterRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedDotOrientationLesson();
        seedBrailleAlphabetsLesson();
    }

    private void seedDotOrientationLesson() {
        final String title = "Dot Orientation";
        final int order = 1;
        Lesson lesson = lessonRepository.findByTitle(title)
                .or(() -> lessonRepository.findByLessonOrder(order))
                .orElse(null);

        if (lesson == null) {
            lesson = new Lesson(
                    title,
                    "Learn the fundamentals of the 6-dot Braille cell, dot numbering, and spatial positions.",
                    "DOT_ORIENTATION",
                    order,
                    "The Braille system is based on a rectangular cell consisting of six dot positions arranged in a 3x2 grid.\n\n"
                            + "The dot positions are numbered 1 through 6:\n"
                            + "- Left column: Dot 1 (top), Dot 2 (middle), Dot 3 (bottom)\n"
                            + "- Right column: Dot 4 (top), Dot 5 (middle), Dot 6 (bottom)\n\n"
                            + "Each character is formed by raising one or more of these six dots. "
                            + "Mastering the dot positions is essential for learning all Braille letters, numbers, and symbols."
            );
            lesson = lessonRepository.save(lesson);
            log.info("Seeded lesson: {}", title);
        } else {
            lesson.setTitle(title);
            lesson.setCategory("DOT_ORIENTATION");
            lesson.setLessonOrder(order);
            lesson = lessonRepository.save(lesson);
        }

        List<BrailleCharacter> characters = List.of(
                new BrailleCharacter("DOT-1", "1", "Dot 1: Top-left position"),
                new BrailleCharacter("DOT-2", "2", "Dot 2: Middle-left position"),
                new BrailleCharacter("DOT-3", "3", "Dot 3: Bottom-left position"),
                new BrailleCharacter("DOT-4", "4", "Dot 4: Top-right position"),
                new BrailleCharacter("DOT-5", "5", "Dot 5: Middle-right position"),
                new BrailleCharacter("DOT-6", "6", "Dot 6: Bottom-right position"),
                new BrailleCharacter("FULL-CELL", "1,2,3,4,5,6", "Full cell: All 6 dots raised")
        );

        seedCharactersIfAbsent(lesson, characters);
    }

    private void seedBrailleAlphabetsLesson() {
        final String title = "Braille Alphabets";
        final int order = 2;
        Lesson lesson = lessonRepository.findByTitle(title)
                .or(() -> lessonRepository.findByLessonOrder(order))
                .orElse(null);

        if (lesson == null) {
            lesson = new Lesson(
                    title,
                    "Master all 26 standard English Grade 1 Braille alphabet letters (A-Z) and their active dot patterns.",
                    "ALPHABET",
                    order,
                    "In English Grade 1 Braille, each letter of the alphabet from A through Z is represented by a unique combination of dots in the 6-dot cell:\n\n"
                            + "• Letters A-J use dots in the top two rows (dots 1, 2, 4, 5).\n"
                            + "• Letters K-T add dot 3 (bottom-left) to the patterns of A-J.\n"
                            + "• Letters U, V, X, Y, Z add dot 6 (bottom-right) to the patterns of K-O.\n"
                            + "• Letter W is unique (dots 2, 4, 5, 6) as it was incorporated into English Braille later.\n\n"
                            + "Explore each character below to learn its exact active dot configuration."
            );
            lesson = lessonRepository.save(lesson);
            log.info("Seeded lesson: {}", title);
        } else {
            lesson.setTitle(title);
            lesson.setCategory("ALPHABET");
            lesson.setLessonOrder(order);
            lesson = lessonRepository.save(lesson);
        }

        List<BrailleCharacter> alphabetCharacters = List.of(
                new BrailleCharacter("A", "1", "Letter A: Dot 1 (top-left)"),
                new BrailleCharacter("B", "1,2", "Letter B: Dots 1, 2 (left column top & middle)"),
                new BrailleCharacter("C", "1,4", "Letter C: Dots 1, 4 (top row)"),
                new BrailleCharacter("D", "1,4,5", "Letter D: Dots 1, 4, 5"),
                new BrailleCharacter("E", "1,5", "Letter E: Dots 1, 5 (diagonal top row)"),
                new BrailleCharacter("F", "1,2,4", "Letter F: Dots 1, 2, 4"),
                new BrailleCharacter("G", "1,2,4,5", "Letter G: Dots 1, 2, 4, 5 (full top two rows)"),
                new BrailleCharacter("H", "1,2,5", "Letter H: Dots 1, 2, 5"),
                new BrailleCharacter("I", "2,4", "Letter I: Dots 2, 4"),
                new BrailleCharacter("J", "2,4,5", "Letter J: Dots 2, 4, 5"),
                new BrailleCharacter("K", "1,3", "Letter K: Dots 1, 3 (left column top & bottom)"),
                new BrailleCharacter("L", "1,2,3", "Letter L: Dots 1, 2, 3 (full left column)"),
                new BrailleCharacter("M", "1,3,4", "Letter M: Dots 1, 3, 4"),
                new BrailleCharacter("N", "1,3,4,5", "Letter N: Dots 1, 3, 4, 5"),
                new BrailleCharacter("O", "1,3,5", "Letter O: Dots 1, 3, 5"),
                new BrailleCharacter("P", "1,2,3,4", "Letter P: Dots 1, 2, 3, 4"),
                new BrailleCharacter("Q", "1,2,3,4,5", "Letter Q: Dots 1, 2, 3, 4, 5"),
                new BrailleCharacter("R", "1,2,3,5", "Letter R: Dots 1, 2, 3, 5"),
                new BrailleCharacter("S", "2,3,4", "Letter S: Dots 2, 3, 4"),
                new BrailleCharacter("T", "2,3,4,5", "Letter T: Dots 2, 3, 4, 5"),
                new BrailleCharacter("U", "1,3,6", "Letter U: Dots 1, 3, 6"),
                new BrailleCharacter("V", "1,2,3,6", "Letter V: Dots 1, 2, 3, 6"),
                new BrailleCharacter("W", "2,4,5,6", "Letter W: Dots 2, 4, 5, 6"),
                new BrailleCharacter("X", "1,3,4,6", "Letter X: Dots 1, 3, 4, 6"),
                new BrailleCharacter("Y", "1,3,4,5,6", "Letter Y: Dots 1, 3, 4, 5, 6"),
                new BrailleCharacter("Z", "1,3,5,6", "Letter Z: Dots 1, 3, 5, 6")
        );

        seedCharactersIfAbsent(lesson, alphabetCharacters);
    }

    private void seedCharactersIfAbsent(Lesson lesson, List<BrailleCharacter> characters) {
        for (BrailleCharacter character : characters) {
            if (!brailleCharacterRepository.existsByLessonIdAndCharacter(lesson.getId(), character.getCharacter())) {
                character.setLesson(lesson);
                brailleCharacterRepository.save(character);
            }
        }
    }
}
