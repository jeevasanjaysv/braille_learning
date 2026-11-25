package com.braille.braille_learning.controller;

import com.braille.braille_learning.config.LessonDataInitializer;
import com.braille.braille_learning.dto.LoginRequest;
import com.braille.braille_learning.dto.RegisterRequest;
import com.braille.braille_learning.entity.Lesson;
import com.braille.braille_learning.repository.BrailleCharacterRepository;
import com.braille.braille_learning.repository.LessonRepository;
import com.braille.braille_learning.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class LessonIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private BrailleCharacterRepository brailleCharacterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonDataInitializer lessonDataInitializer;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        // Ensure seeded data is present
        lessonDataInitializer.run();

        // Register and login to obtain a valid JWT token
        RegisterRequest registerRequest = new RegisterRequest("Student Test", "student@example.com", "Password@123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("student@example.com", "Password@123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);
        jwtToken = (String) loginResponse.get("token");
        assertNotNull(jwtToken);
    }

    @Test
    @DisplayName("Protected lesson APIs should reject requests without a valid token (401)")
    void testUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/lessons"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        mockMvc.perform(get("/api/lessons/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/lessons/category/ALPHABET"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/lessons/1/characters"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should retrieve all lessons ordered by lesson order with valid JWT")
    void testGetAllLessons() throws Exception {
        mockMvc.perform(get("/api/lessons")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Dot Orientation"))
                .andExpect(jsonPath("$[0].category").value("DOT_ORIENTATION"))
                .andExpect(jsonPath("$[0].lessonOrder").value(1))
                .andExpect(jsonPath("$[1].title").value("Braille Alphabets"))
                .andExpect(jsonPath("$[1].category").value("ALPHABET"))
                .andExpect(jsonPath("$[1].lessonOrder").value(2));
    }

    @Test
    @DisplayName("Should retrieve single lesson by ID and return 404 for non-existent ID")
    void testGetLessonByIdAndNotFound() throws Exception {
        Lesson dotLesson = lessonRepository.findByTitle("Dot Orientation").orElseThrow();

        mockMvc.perform(get("/api/lessons/" + dotLesson.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dotLesson.getId()))
                .andExpect(jsonPath("$.title").value("Dot Orientation"))
                .andExpect(jsonPath("$.category").value("DOT_ORIENTATION"))
                .andExpect(jsonPath("$.content").isNotEmpty());

        mockMvc.perform(get("/api/lessons/999999")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Lesson not found with id: 999999"));
    }

    @Test
    @DisplayName("Should retrieve lessons by category")
    void testGetLessonsByCategory() throws Exception {
        mockMvc.perform(get("/api/lessons/category/DOT_ORIENTATION")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Dot Orientation"));

        mockMvc.perform(get("/api/lessons/category/ALPHABET")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Braille Alphabets"));
    }

    @Test
    @DisplayName("Should retrieve all 26 Braille alphabet characters (A-Z) with correct dot patterns")
    void testGetCharactersForAlphabetLesson() throws Exception {
        Lesson alphabetLesson = lessonRepository.findByTitle("Braille Alphabets").orElseThrow();

        mockMvc.perform(get("/api/lessons/" + alphabetLesson.getId() + "/characters")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(26)))
                .andExpect(jsonPath("$[0].character").value("A"))
                .andExpect(jsonPath("$[0].dotPattern").value("1"))
                .andExpect(jsonPath("$[1].character").value("B"))
                .andExpect(jsonPath("$[1].dotPattern").value("1,2"))
                .andExpect(jsonPath("$[2].character").value("C"))
                .andExpect(jsonPath("$[2].dotPattern").value("1,4"))
                .andExpect(jsonPath("$[25].character").value("Z"))
                .andExpect(jsonPath("$[25].dotPattern").value("1,3,5,6"));

        mockMvc.perform(get("/api/lessons/999999/characters")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("Re-running initializer must be idempotent and not create duplicate data")
    void testSeedingIdempotency() {
        long lessonCountBefore = lessonRepository.count();
        long characterCountBefore = brailleCharacterRepository.count();

        // Re-run initializer
        lessonDataInitializer.run();

        long lessonCountAfter = lessonRepository.count();
        long characterCountAfter = brailleCharacterRepository.count();

        assertEquals(lessonCountBefore, lessonCountAfter);
        assertEquals(characterCountBefore, characterCountAfter);
    }
}
