package com.braille.braille_learning.security;

import com.braille.braille_learning.dto.LoginRequest;
import com.braille.braille_learning.dto.RegisterRequest;
import com.braille.braille_learning.entity.User;
import com.braille.braille_learning.repository.LessonProgressRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private LessonProgressRepository lessonProgressRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        if (lessonProgressRepository != null) {
            lessonProgressRepository.deleteAll();
        }
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete authentication and security workflow test")
    void testFullSecurityFlow() throws Exception {
        // Case 6: Access health API without a token - should succeed
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("Braille Learning System backend is running"));

        // Case 3: Access protected endpoint without a token - should return 401 Unauthorized
        mockMvc.perform(get("/api/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        // Case 4: Access protected endpoint with an invalid token - should return 401 Unauthorized
        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer invalid.jwt.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        // Case 1: Register a new user
        RegisterRequest registerRequest = new RegisterRequest("Jeeva Test", "jeeva@example.com", "Password@123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jeeva@example.com"))
                .andExpect(jsonPath("$.fullName").value("Jeeva Test"))
                .andExpect(jsonPath("$.id").isNotEmpty());

        // Case 7: Confirm password remains stored as a BCrypt hash in the database
        Optional<User> savedUserOpt = userRepository.findByEmail("jeeva@example.com");
        assertTrue(savedUserOpt.isPresent());
        User savedUser = savedUserOpt.get();
        assertNotNull(savedUser.getPasswordHash());
        assertTrue(savedUser.getPasswordHash().startsWith("$2a$") || savedUser.getPasswordHash().startsWith("$2b$"));
        assertFalse(savedUser.getPasswordHash().contains("Password@123"));

        // Case 2: Log in with correct credentials and receive a JWT
        LoginRequest loginRequest = new LoginRequest("jeeva@example.com", "Password@123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("jeeva@example.com"))
                .andExpect(jsonPath("$.user.name").value("Jeeva Test"))
                .andExpect(jsonPath("$.user.id").isNotEmpty())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        Map<String, Object> loginMap = objectMapper.readValue(responseBody, Map.class);
        String token = (String) loginMap.get("token");
        assertNotNull(token);

        // Case 5: Access protected endpoint with a valid token - should succeed
        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.user").value("jeeva@example.com"));
    }
}
