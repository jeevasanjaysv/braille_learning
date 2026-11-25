package com.braille.braille_learning.repository;

import com.braille.braille_learning.config.LessonDataInitializer;
import com.braille.braille_learning.entity.Lesson;
import com.braille.braille_learning.entity.Question;
import com.braille.braille_learning.entity.Quiz;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class QuizRepositoryTest {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonDataInitializer lessonDataInitializer;

    @BeforeEach
    void setUp() {
        cleanTestQuizzes();
        lessonDataInitializer.run();
    }

    @AfterEach
    void tearDown() {
        cleanTestQuizzes();
    }

    private void cleanTestQuizzes() {
        questionRepository.deleteAll();
        quizRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save a quiz with questions and fetch them ordered by questionOrder")
    void testSaveQuizWithDirectAnswerQuestions() {
        Lesson dotLesson = lessonRepository.findByTitle("Dot Orientation").orElseThrow();

        Quiz quiz = new Quiz("Dot Orientation Spoken Quiz", "Direct answer quiz on Braille dot positions", dotLesson);

        Question q1 = new Question(
                "Which Braille dot represents the top-left position?",
                "Dot 1",
                "1;Dot 1;dot one",
                "Dot 1 is located at the top-left of the standard 6-dot cell.",
                1
        );

        Question q2 = new Question(
                "Which column contains dots 4, 5, and 6?",
                "Right column",
                "Right;Right column;the right column",
                "Dots 4, 5, and 6 form the right vertical column of the cell.",
                2
        );

        quiz.addQuestion(q1);
        quiz.addQuestion(q2);

        Quiz savedQuiz = quizRepository.save(quiz);

        assertNotNull(savedQuiz.getId());
        assertNotNull(savedQuiz.getCreatedAt());

        List<Question> orderedQuestions = questionRepository.findByQuizIdOrderByQuestionOrderAsc(savedQuiz.getId());
        assertEquals(2, orderedQuestions.size());
        assertEquals(1, orderedQuestions.get(0).getQuestionOrder());
        assertEquals("Which Braille dot represents the top-left position?", orderedQuestions.get(0).getQuestionText());
        assertEquals("Dot 1", orderedQuestions.get(0).getExpectedAnswer());
        assertEquals("1;Dot 1;dot one", orderedQuestions.get(0).getAcceptableAnswers());

        assertEquals(2, orderedQuestions.get(1).getQuestionOrder());
        assertEquals("Right column", orderedQuestions.get(1).getExpectedAnswer());
    }

    @Test
    @DisplayName("Should find quizzes by lesson")
    void testFindQuizzesByLesson() {
        Lesson dotLesson = lessonRepository.findByTitle("Dot Orientation").orElseThrow();

        Quiz quiz1 = new Quiz("Quiz A", "Description A", dotLesson);
        quizRepository.save(quiz1);

        List<Quiz> quizzes = quizRepository.findByLessonId(dotLesson.getId());
        assertFalse(quizzes.isEmpty());
        assertTrue(quizzes.stream().anyMatch(q -> q.getTitle().equals("Quiz A")));
    }

    @Test
    @DisplayName("Should enforce unique constraint on question order within the same quiz")
    void testUniqueQuestionOrderConstraint() {
        Quiz quiz = new Quiz("Quiz Unique Order", "Description", null);
        Quiz savedQuiz = quizRepository.save(quiz);

        Question q1 = new Question("Question 1", "Answer 1", null, null, 1, savedQuiz);
        questionRepository.saveAndFlush(q1);

        Question q2DuplicateOrder = new Question("Question 2", "Answer 2", null, null, 1, savedQuiz);
        assertThrows(DataIntegrityViolationException.class, () -> {
            questionRepository.saveAndFlush(q2DuplicateOrder);
        });
    }
}
