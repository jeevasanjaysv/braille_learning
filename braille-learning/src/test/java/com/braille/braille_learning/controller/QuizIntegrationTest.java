package com.braille.braille_learning.controller;

import com.braille.braille_learning.entity.Lesson;
import com.braille.braille_learning.entity.Question;
import com.braille.braille_learning.entity.Quiz;
import com.braille.braille_learning.repository.LessonRepository;
import com.braille.braille_learning.repository.QuestionRepository;
import com.braille.braille_learning.repository.QuizRepository;
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

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QuizIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private LessonRepository lessonRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Quiz quiz;
    private Quiz otherQuiz;
    private Question question;
    private Question secondQuestion;
    private Question otherQuizQuestion;

    @BeforeEach
    void setUp() {
        Lesson lesson = lessonRepository.findByTitle("Dot Orientation").orElseThrow();
        quiz = quizRepository.saveAndFlush(new Quiz("Quiz API integration test", "Test quiz", lesson));
        otherQuiz = quizRepository.saveAndFlush(new Quiz("Other quiz API integration test", "Other test quiz", lesson));

        question = questionRepository.saveAndFlush(new Question(
                "Which Braille dot represents the top-left position?",
                "Dot 1",
                "1;dot one",
                "Braille dot 1 is the top-left dot.",
                2,
                quiz
        ));
        secondQuestion = questionRepository.saveAndFlush(new Question(
                "Which Braille dot represents the middle-left position?",
                "Dot 2",
                "2;dot two",
                "Braille dot 2 is the middle-left dot.",
                1,
                quiz
        ));
        otherQuizQuestion = questionRepository.saveAndFlush(new Question(
                "Which column contains dots 4, 5, and 6?",
                "Right column",
                "Right;the right column",
                "Dots 4, 5, and 6 form the right column.",
                1,
                otherQuiz
        ));
    }

    @AfterEach
    void tearDown() {
        questionRepository.deleteAllByIdInBatch(
                java.util.List.of(question.getId(), secondQuestion.getId(), otherQuizQuestion.getId())
        );
        quizRepository.deleteAllByIdInBatch(java.util.List.of(quiz.getId(), otherQuiz.getId()));
    }

    @Test
    @WithMockUser
    @DisplayName("Quiz retrieval returns quiz details and ordered questions without answers")
    void retrievesQuizzesAndQuestionsWithoutExpectedAnswers() throws Exception {
        mockMvc.perform(get("/api/quizzes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/quizzes/" + quiz.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(quiz.getId()))
                .andExpect(jsonPath("$.questionCount").value(2));

        mockMvc.perform(get("/api/quizzes/" + quiz.getId() + "/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(secondQuestion.getId()))
                .andExpect(jsonPath("$[0].questionOrder").value(1))
                .andExpect(jsonPath("$[1].id").value(question.getId()))
                .andExpect(jsonPath("$[1].questionOrder").value(2))
                .andExpect(jsonPath("$[0].expectedAnswer").doesNotExist())
                .andExpect(jsonPath("$[1].expectedAnswer").doesNotExist());
    }

    @Test
    @WithMockUser
    @DisplayName("Correct and normalized answer earns one point")
    void acceptsCorrectAndNormalizedAnswers() throws Exception {
        submitAnswer("  DOT   one  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.correctAnswer").value("Dot 1"))
                .andExpect(jsonPath("$.explanation").value("Braille dot 1 is the top-left dot."));
    }

    @Test
    @WithMockUser
    @DisplayName("Incorrect answer returns correction and no points")
    void rejectsIncorrectAnswer() throws Exception {
        submitAnswer("Dot 2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.score").value(0))
                .andExpect(jsonPath("$.feedback").value("Incorrect. The correct answer is Dot 1."));
    }

    @Test
    @WithMockUser
    @DisplayName("Unknown quiz or question and questions from another quiz return 404")
    void rejectsUnknownOrMismatchedResources() throws Exception {
        mockMvc.perform(get("/api/quizzes/999999"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/quizzes/999999/questions"))
                .andExpect(status().isNotFound());

        submitAnswerFor(999999L, question.getId(), "Dot 1")
                .andExpect(status().isNotFound());

        submitAnswerFor(quiz.getId(), 999999L, "Dot 1")
                .andExpect(status().isNotFound());

        submitAnswerFor(quiz.getId(), otherQuizQuestion.getId(), "Right column")
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Quiz APIs require authentication")
    void protectsQuizApis() throws Exception {
        mockMvc.perform(get("/api/quizzes"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/quizzes/" + quiz.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/quizzes/" + quiz.getId() + "/questions"))
                .andExpect(status().isUnauthorized());
        submitAnswer("Dot 1")
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions submitAnswer(String answer) throws Exception {
        return submitAnswerFor(quiz.getId(), question.getId(), answer);
    }

    private org.springframework.test.web.servlet.ResultActions submitAnswerFor(
            Long quizId,
            Long questionId,
            String answer
    ) throws Exception {
        return mockMvc.perform(post("/api/quizzes/{quizId}/questions/{questionId}/answer", quizId, questionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("answer", answer))));
    }
}
