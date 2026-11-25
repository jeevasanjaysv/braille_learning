package com.braille.braille_learning.controller;

import com.braille.braille_learning.entity.Lesson;
import com.braille.braille_learning.entity.LessonProgress;
import com.braille.braille_learning.entity.Quiz;
import com.braille.braille_learning.entity.QuizAttempt;
import com.braille.braille_learning.entity.User;
import com.braille.braille_learning.repository.LessonProgressRepository;
import com.braille.braille_learning.repository.LessonRepository;
import com.braille.braille_learning.repository.QuizAttemptRepository;
import com.braille.braille_learning.repository.QuizRepository;
import com.braille.braille_learning.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProgressControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonProgressRepository lessonProgressRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    private User student;
    private User otherStudent;
    private Quiz quiz;
    private Lesson firstLesson;
    private Lesson secondLesson;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        student = userRepository.saveAndFlush(
                new User("Progress Student", "progress-" + suffix + "@example.com", "hash")
        );
        otherStudent = userRepository.saveAndFlush(
                new User("Other Progress Student", "other-progress-" + suffix + "@example.com", "hash")
        );
        firstLesson = lessonRepository.findByTitle("Dot Orientation").orElseThrow();
        secondLesson = lessonRepository.findByTitle("Braille Alphabets").orElseThrow();
        quiz = quizRepository.saveAndFlush(new Quiz("Progress API quiz " + suffix, "Progress test quiz", firstLesson));
    }

    @AfterEach
    void tearDown() {
        for (User user : List.of(student, otherStudent)) {
            List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdOrderByStartedAtDesc(user.getId());
            quizAttemptRepository.deleteAll(attempts);
            lessonProgressRepository.deleteAll(lessonProgressRepository.findByUserId(user.getId()));
        }
        quizRepository.deleteById(quiz.getId());
        userRepository.deleteAllByIdInBatch(List.of(student.getId(), otherStudent.getId()));
    }

    @Test
    @DisplayName("All progress endpoints require authentication")
    void protectsAllProgressEndpoints() throws Exception {
        mockMvc.perform(get("/api/progress")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/progress/summary")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/progress/lessons")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/progress/quizzes")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Student with no progress receives zero totals and incomplete lessons/quizzes")
    void returnsEmptyStudentProgress() throws Exception {
        String email = student.getEmail();
        mockMvc.perform(get("/api/progress").with(user(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLessons").value(lessonRepository.count()))
                .andExpect(jsonPath("$.completedLessons").value(0))
                .andExpect(jsonPath("$.remainingLessons").value(lessonRepository.count()))
                .andExpect(jsonPath("$.learningPercentage").value(0.0))
                .andExpect(jsonPath("$.totalQuizAttempts").value(0))
                .andExpect(jsonPath("$.averageQuizScore").value(0.0));

        mockMvc.perform(get("/api/progress/summary").with(user(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learningStatus").value("NOT_STARTED"))
                .andExpect(jsonPath("$.totalCompletedQuizAttempts").value(0))
                .andExpect(jsonPath("$.averageQuizScore").value(0.0))
                .andExpect(jsonPath("$.bestQuizScore").value(0.0))
                .andExpect(jsonPath("$.latestQuizCompletionDate").doesNotExist());

        mockMvc.perform(get("/api/progress/lessons").with(user(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(lessonRepository.count()))
                .andExpect(jsonPath("$[0].lessonId").isNumber())
                .andExpect(jsonPath("$[0].lessonTitle").isNotEmpty())
                .andExpect(jsonPath("$[0].completed").value(false));

        String quizzesJson = mockMvc.perform(get("/api/progress/quizzes").with(user(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode quizProgress = findById(objectMapper.readTree(quizzesJson), "quizId", quiz.getId());
        assertEquals(0, quizProgress.get("completedAttempts").asInt());
        assertFalse(quizProgress.hasNonNull("latestScore"));
    }

    @Test
    @DisplayName("Lesson and quiz progress is calculated only for the authenticated student")
    void returnsOnlyAuthenticatedStudentsProgress() throws Exception {
        LocalDateTime studentCompletedAt = LocalDateTime.now().minusDays(1);
        lessonProgressRepository.saveAndFlush(
                new LessonProgress(student, firstLesson, true, studentCompletedAt)
        );
        lessonProgressRepository.saveAndFlush(
                new LessonProgress(otherStudent, secondLesson, true, LocalDateTime.now())
        );

        quizAttemptRepository.saveAndFlush(completedAttempt(student, quiz, 1, 2, LocalDateTime.now().minusHours(2)));
        quizAttemptRepository.saveAndFlush(completedAttempt(student, quiz, 2, 2, LocalDateTime.now().minusHours(1)));
        quizAttemptRepository.saveAndFlush(completedAttempt(otherStudent, quiz, 0, 2, LocalDateTime.now()));

        mockMvc.perform(get("/api/progress").with(user(student.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedLessons").value(1))
                .andExpect(jsonPath("$.totalQuizAttempts").value(2))
                .andExpect(jsonPath("$.averageQuizScore").value(75.0));

        mockMvc.perform(get("/api/progress/summary").with(user(student.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedLessons").value(1))
                .andExpect(jsonPath("$.learningStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.totalCompletedQuizAttempts").value(2))
                .andExpect(jsonPath("$.averageQuizScore").value(75.0))
                .andExpect(jsonPath("$.bestQuizScore").value(100.0))
                .andExpect(jsonPath("$.latestQuizCompletionDate").isNotEmpty());

        String lessonJson = mockMvc.perform(get("/api/progress/lessons").with(user(student.getEmail())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode lessons = objectMapper.readTree(lessonJson);
        JsonNode completedLesson = findById(lessons, "lessonId", firstLesson.getId());
        JsonNode otherStudentsLesson = findById(lessons, "lessonId", secondLesson.getId());
        assertTrue(completedLesson.get("completed").asBoolean());
        assertTrue(completedLesson.hasNonNull("completedAt"));
        assertFalse(otherStudentsLesson.get("completed").asBoolean());

        String quizJson = mockMvc.perform(get("/api/progress/quizzes").with(user(student.getEmail())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode quizzes = objectMapper.readTree(quizJson);
        JsonNode quizProgress = findById(quizzes, "quizId", quiz.getId());
        assertEquals(2, quizProgress.get("completedAttempts").asInt());
        assertEquals(2, quizProgress.get("latestScore").asInt());
        assertEquals(2, quizProgress.get("bestScore").asInt());
        assertEquals(2, quizProgress.get("latestTotalQuestions").asInt());
        assertTrue(quizProgress.hasNonNull("latestCompletionDate"));
    }

    private static QuizAttempt completedAttempt(
            User user,
            Quiz quiz,
            int score,
            int totalQuestions,
            LocalDateTime completedAt
    ) {
        QuizAttempt attempt = new QuizAttempt(user, quiz);
        attempt.setScore(score);
        attempt.setTotalQuestions(totalQuestions);
        attempt.setCompletedAt(completedAt);
        return attempt;
    }

    private static JsonNode findById(JsonNode items, String idProperty, Long id) {
        for (JsonNode item : items) {
            if (item.path(idProperty).asLong() == id) {
                return item;
            }
        }
        throw new AssertionError("No item found with " + idProperty + " " + id);
    }
}
