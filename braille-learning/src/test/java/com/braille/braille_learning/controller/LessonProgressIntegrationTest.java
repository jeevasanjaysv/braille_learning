package com.braille.braille_learning.controller;

import com.braille.braille_learning.config.LessonDataInitializer;
import com.braille.braille_learning.dto.LoginRequest;
import com.braille.braille_learning.dto.RegisterRequest;
import com.braille.braille_learning.entity.Lesson;
import com.braille.braille_learning.repository.LessonProgressRepository;
import com.braille.braille_learning.repository.LessonRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LessonProgressIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonProgressRepository lessonProgressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonDataInitializer lessonDataInitializer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        lessonProgressRepository.deleteAll();
        userRepository.deleteAll();
        lessonDataInitializer.run();
    }

    @AfterEach
    void tearDown() {
        lessonProgressRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerAndLogin(String name, String email, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(name, email, password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);
        return (String) loginResponse.get("token");
    }

    @Test
    @DisplayName("Progress endpoints must reject unauthenticated requests with 401")
    void testUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(post("/api/lessons/1/complete"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/lessons/progress"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/lessons/progress/count"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("User can complete lessons, completion timestamp is saved, count increments, and idempotency prevents duplicates")
    void testCompleteLessonAndCountAndIdempotency() throws Exception {
        String token = registerAndLogin("Alice Progress", "alice.progress@example.com", "Password@123");

        Lesson dotLesson = lessonRepository.findByTitle("Dot Orientation").orElseThrow();
        Lesson alphabetLesson = lessonRepository.findByTitle("Braille Alphabets").orElseThrow();

        // 1. Initial count should be 0
        mockMvc.perform(get("/api/lessons/progress/count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedCount").value(0));

        // 2. Complete Lesson 1
        mockMvc.perform(post("/api/lessons/" + dotLesson.getId() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(dotLesson.getId()))
                .andExpect(jsonPath("$.lessonTitle").value("Dot Orientation"))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        // 3. Complete Lesson 1 again - should be idempotent and not create duplicate
        long progressCountBefore = lessonProgressRepository.count();
        mockMvc.perform(post("/api/lessons/" + dotLesson.getId() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(dotLesson.getId()))
                .andExpect(jsonPath("$.completed").value(true));
        long progressCountAfter = lessonProgressRepository.count();
        assertEquals(progressCountBefore, progressCountAfter);

        // 4. Complete Lesson 2
        mockMvc.perform(post("/api/lessons/" + alphabetLesson.getId() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(alphabetLesson.getId()))
                .andExpect(jsonPath("$.lessonTitle").value("Braille Alphabets"))
                .andExpect(jsonPath("$.completed").value(true));

        // 5. Check count is now 2
        mockMvc.perform(get("/api/lessons/progress/count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedCount").value(2));

        // 6. Retrieve list of completed lessons
        mockMvc.perform(get("/api/lessons/progress")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].completed").value(true))
                .andExpect(jsonPath("$[1].completed").value(true));
    }

    @Test
    @DisplayName("Users cannot see another user's progress")
    void testUserProgressIsolation() throws Exception {
        String tokenUser1 = registerAndLogin("User One", "user1@example.com", "Password@123");
        String tokenUser2 = registerAndLogin("User Two", "user2@example.com", "Password@123");

        Lesson dotLesson = lessonRepository.findByTitle("Dot Orientation").orElseThrow();

        // User 1 completes the lesson
        mockMvc.perform(post("/api/lessons/" + dotLesson.getId() + "/complete")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk());

        // User 2 checks progress - must be empty
        mockMvc.perform(get("/api/lessons/progress")
                        .header("Authorization", "Bearer " + tokenUser2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/lessons/progress/count")
                        .header("Authorization", "Bearer " + tokenUser2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedCount").value(0));
    }

    @Test
    @DisplayName("Completing a non-existent lesson ID returns 404 Not Found")
    void testCompleteInvalidLessonId() throws Exception {
        String token = registerAndLogin("Bob Test", "bob.test@example.com", "Password@123");

        mockMvc.perform(post("/api/lessons/999999/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Lesson not found with id: 999999"));
    }
}
