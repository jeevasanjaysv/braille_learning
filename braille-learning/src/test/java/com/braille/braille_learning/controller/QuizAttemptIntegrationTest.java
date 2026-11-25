package com.braille.braille_learning.controller;

import com.braille.braille_learning.entity.Lesson;
import com.braille.braille_learning.entity.Question;
import com.braille.braille_learning.entity.Quiz;
import com.braille.braille_learning.entity.QuizAttempt;
import com.braille.braille_learning.entity.User;
import com.braille.braille_learning.repository.LessonRepository;
import com.braille.braille_learning.repository.QuestionRepository;
import com.braille.braille_learning.repository.QuizAttemptAnswerRepository;
import com.braille.braille_learning.repository.QuizAttemptRepository;
import com.braille.braille_learning.repository.QuizRepository;
import com.braille.braille_learning.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QuizAttemptIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuizAttemptRepository attemptRepository;

    @Autowired
    private QuizAttemptAnswerRepository answerRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String studentEmail;
    private String otherStudentEmail;
    private User student;
    private User otherStudent;
    private Quiz quiz;
    private Quiz otherQuiz;
    private Question firstQuestion;
    private Question secondQuestion;
    private Question otherQuizQuestion;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        studentEmail = "student-" + suffix + "@example.com";
        otherStudentEmail = "other-" + suffix + "@example.com";
        student = userRepository.saveAndFlush(new User("Attempt Student", studentEmail, "not-a-real-password"));
        otherStudent = userRepository.saveAndFlush(new User("Other Student", otherStudentEmail, "not-a-real-password"));
        Lesson lesson = lessonRepository.findByTitle("Dot Orientation").orElseThrow();
        quiz = quizRepository.saveAndFlush(new Quiz("Attempt quiz " + suffix, "Attempt integration test", lesson));
        otherQuiz = quizRepository.saveAndFlush(new Quiz("Other attempt quiz " + suffix, "Other test", lesson));
        firstQuestion = questionRepository.saveAndFlush(new Question(
                "Which dot is top-left?",
                "Dot 1",
                "1;dot one",
                "Dot 1 is top-left.",
                1,
                quiz
        ));
        secondQuestion = questionRepository.saveAndFlush(new Question(
                "Which dot is middle-left?",
                "Dot 2",
                "2;dot two",
                "Dot 2 is middle-left.",
                2,
                quiz
        ));
        otherQuizQuestion = questionRepository.saveAndFlush(new Question(
                "Which is the right column?",
                "Right column",
                "right",
                "Dots 4, 5, and 6 form the right column.",
                1,
                otherQuiz
        ));
    }

    @AfterEach
    void tearDown() {
        for (User user : List.of(student, otherStudent)) {
            List<QuizAttempt> attempts = attemptRepository.findByUserIdOrderByStartedAtDesc(user.getId());
            for (QuizAttempt attempt : attempts) {
                answerRepository.deleteAll(answerRepository.findByAttemptIdOrderByAnsweredAtAsc(attempt.getId()));
            }
            attemptRepository.deleteAll(attempts);
        }
        questionRepository.deleteAllByIdInBatch(
                List.of(firstQuestion.getId(), secondQuestion.getId(), otherQuizQuestion.getId())
        );
        quizRepository.deleteAllByIdInBatch(List.of(quiz.getId(), otherQuiz.getId()));
        userRepository.deleteAllByIdInBatch(List.of(student.getId(), otherStudent.getId()));
    }

    @Test
    @WithMockUser
    @DisplayName("Starting a quiz creates a user-owned in-progress attempt")
    void startsAttempt() throws Exception {
        mockMvc.perform(post("/api/quizzes/{quizId}/attempts", quiz.getId()).with(user(studentEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").isNumber())
                .andExpect(jsonPath("$.quizId").value(quiz.getId()))
                .andExpect(jsonPath("$.startedAt").isNotEmpty())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        assertEquals(1, attemptRepository.countByUserId(student.getId()));
    }

    @Test
    @WithMockUser
    @DisplayName("Each attempt question contributes at most one point")
    void correctAnswerIncreasesScoreOnceAndWrongAnswerDoesNotScore() throws Exception {
        long attemptId = startAttemptAs(studentEmail);

        submitAnswerAs(attemptId, firstQuestion.getId(), "dot one", studentEmail)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.score").value(1));
        submitAnswerAs(attemptId, firstQuestion.getId(), "Dot 1", studentEmail)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.score").value(1));
        assertEquals(1, answerRepository.findByAttemptIdOrderByAnsweredAtAsc(attemptId).size());

        submitAnswerAs(attemptId, secondQuestion.getId(), "Dot 3", studentEmail)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.score").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("Completing an attempt saves score and disallows later changes")
    void completesAttemptOnce() throws Exception {
        long attemptId = startAttemptAs(studentEmail);
        submitAnswerAs(attemptId, firstQuestion.getId(), "Dot 1", studentEmail);
        submitAnswerAs(attemptId, secondQuestion.getId(), "Dot 3", studentEmail);

        mockMvc.perform(post("/api/attempts/{attemptId}/complete", attemptId).with(user(studentEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(attemptId))
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(2))
                .andExpect(jsonPath("$.percentage").value(50.0))
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        QuizAttempt completed = attemptRepository.findById(attemptId).orElseThrow();
        assertNotNull(completed.getCompletedAt());
        assertEquals(1, completed.getScore());

        mockMvc.perform(post("/api/attempts/{attemptId}/complete", attemptId).with(user(studentEmail)))
                .andExpect(status().isConflict());
        submitAnswerAs(attemptId, firstQuestion.getId(), "Dot 1", studentEmail)
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("Invalid attempts and questions from another quiz are rejected")
    void rejectsInvalidAttemptAndQuestionRelationship() throws Exception {
        long attemptId = startAttemptAs(studentEmail);

        mockMvc.perform(post("/api/attempts/{attemptId}/complete", 999999L).with(user(studentEmail)))
                .andExpect(status().isNotFound());
        submitAnswerAs(999999L, firstQuestion.getId(), "Dot 1", studentEmail)
                .andExpect(status().isNotFound());
        submitAnswerAs(attemptId, 999999L, "Dot 1", studentEmail)
                .andExpect(status().isNotFound());
        submitAnswerAs(attemptId, otherQuizQuestion.getId(), "Right column", studentEmail)
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("Attempt history and details are isolated to the logged-in student")
    void limitsHistoryAndDetailsToOwner() throws Exception {
        long studentAttempt = startAttemptAs(studentEmail);
        long otherAttempt = startAttemptAs(otherStudentEmail);
        submitAnswerAs(studentAttempt, firstQuestion.getId(), "Dot 1", studentEmail);

        mockMvc.perform(get("/api/attempts").with(user(studentEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].attemptId").value(studentAttempt));
        mockMvc.perform(get("/api/attempts").with(user(otherStudentEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].attemptId").value(otherAttempt));

        mockMvc.perform(get("/api/attempts/{attemptId}", studentAttempt).with(user(studentEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attempt.attemptId").value(studentAttempt))
                .andExpect(jsonPath("$.answers", hasSize(1)))
                .andExpect(jsonPath("$.answers[0].correct").value(true))
                .andExpect(jsonPath("$.answers[0].submittedAnswer").value("Dot 1"));
        mockMvc.perform(get("/api/attempts/{attemptId}", studentAttempt).with(user(otherStudentEmail)))
                .andExpect(status().isNotFound());
        assertFalse(attemptRepository.findByIdAndUserEmailIgnoreCase(studentAttempt, otherStudentEmail).isPresent());
    }

    @Test
    @DisplayName("Attempt APIs require authentication")
    void protectsAttemptApis() throws Exception {
        mockMvc.perform(post("/api/quizzes/{quizId}/attempts", quiz.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/attempts/{attemptId}/questions/{questionId}/answer", 1, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"Dot 1\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/attempts/{attemptId}/complete", 1))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/attempts"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/attempts/{attemptId}", 1))
                .andExpect(status().isUnauthorized());
    }

    private long startAttemptAs(String email) throws Exception {
        String response = mockMvc.perform(post("/api/quizzes/{quizId}/attempts", quiz.getId()).with(user(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("attemptId").asLong();
    }

    private ResultActions submitAnswerAs(Long attemptId, Long questionId, String answer, String email)
            throws Exception {
        return mockMvc.perform(post("/api/attempts/{attemptId}/questions/{questionId}/answer", attemptId, questionId)
                .with(user(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("answer", answer))));
    }
}
