package com.braille.braille_learning.controller;

import com.braille.braille_learning.dto.LoginRequest;
import com.braille.braille_learning.dto.LoginResponse;
import com.braille.braille_learning.dto.RegisterRequest;
import com.braille.braille_learning.dto.UserResponse;
import com.braille.braille_learning.entity.User;
import com.braille.braille_learning.exception.DuplicateEmailException;
import com.braille.braille_learning.exception.GlobalExceptionHandler;
import com.braille.braille_learning.exception.InvalidCredentialsException;
import com.braille.braille_learning.repository.LessonProgressRepository;
import com.braille.braille_learning.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AuthControllerTest {

    @Autowired
    private AuthController authController;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private LessonProgressRepository lessonProgressRepository;

    @Autowired
    private Validator validator;

    @BeforeEach
    void setUp() {
        if (lessonProgressRepository != null) {
            lessonProgressRepository.deleteAll();
        }
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Registration succeeds with 201 Created and password is saved as BCrypt hash in MySQL")
    void testRegisterSuccessAndPasswordHashed() {
        RegisterRequest request = new RegisterRequest("Alice Test", "alice@example.com", "TestingPassword123");

        ResponseEntity<UserResponse> response = authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Alice Test", response.getBody().getFullName());
        assertEquals("alice@example.com", response.getBody().getEmail());
        assertNotNull(response.getBody().getId());
        assertNotNull(response.getBody().getCreatedAt());

        Optional<User> savedUserOpt = userRepository.findByEmail("alice@example.com");
        assertTrue(savedUserOpt.isPresent());
        User savedUser = savedUserOpt.get();
        assertNotNull(savedUser.getPasswordHash());
        assertTrue(savedUser.getPasswordHash().startsWith("$2a$") || savedUser.getPasswordHash().startsWith("$2b$"));
        assertFalse(savedUser.getPasswordHash().contains("TestingPassword123"));
    }

    @Test
    @DisplayName("Duplicate email registration throws DuplicateEmailException and maps to 409 Conflict")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Bob Test", "bob@example.com", "TestingPassword123");
        authController.register(request);

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class, () -> {
            authController.register(request);
        });

        ResponseEntity<Map<String, Object>> errorResponse = globalExceptionHandler.handleDuplicateEmail(ex);
        assertEquals(HttpStatus.CONFLICT, errorResponse.getStatusCode());
        assertEquals(409, errorResponse.getBody().get("status"));
        assertTrue(errorResponse.getBody().get("message").toString().contains("bob@example.com"));
    }

    @Test
    @DisplayName("Invalid registration input triggers Bean Validation errors")
    void testRegisterValidationFailure() {
        RegisterRequest invalidRequest = new RegisterRequest("", "invalid-email", "123");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(invalidRequest);

        assertFalse(violations.isEmpty());
        boolean hasNameError = violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
        boolean hasEmailError = violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        boolean hasPasswordError = violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password"));

        assertTrue(hasNameError);
        assertTrue(hasEmailError);
        assertTrue(hasPasswordError);
    }

    @Test
    @DisplayName("Login succeeds with 200 OK and returns JWT token and user info without password")
    void testLoginSuccess() {
        RegisterRequest registerRequest = new RegisterRequest("Charlie Test", "charlie@example.com", "TestingPassword123");
        authController.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest("charlie@example.com", "TestingPassword123");
        ResponseEntity<LoginResponse> loginResponse = authController.login(loginRequest);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertNotNull(loginResponse.getBody().getToken());
        assertFalse(loginResponse.getBody().getToken().isBlank());
        assertEquals("Bearer", loginResponse.getBody().getTokenType());

        assertNotNull(loginResponse.getBody().getUser());
        assertEquals("Charlie Test", loginResponse.getBody().getUser().getName());
        assertEquals("charlie@example.com", loginResponse.getBody().getUser().getEmail());
        assertNotNull(loginResponse.getBody().getUser().getId());
    }

    @Test
    @DisplayName("Login with wrong password throws InvalidCredentialsException and maps to 401 Unauthorized")
    void testLoginWrongPassword() {
        RegisterRequest registerRequest = new RegisterRequest("Dana Test", "dana@example.com", "TestingPassword123");
        authController.register(registerRequest);

        LoginRequest wrongLoginRequest = new LoginRequest("dana@example.com", "WrongPassword");
        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> {
            authController.login(wrongLoginRequest);
        });

        ResponseEntity<Map<String, Object>> errorResponse = globalExceptionHandler.handleInvalidCredentials(ex);
        assertEquals(HttpStatus.UNAUTHORIZED, errorResponse.getStatusCode());
        assertEquals(401, errorResponse.getBody().get("status"));
    }

    @Test
    @DisplayName("Login with non-existent email throws InvalidCredentialsException and maps to 401 Unauthorized")
    void testLoginNonExistentEmail() {
        LoginRequest nonExistentLoginRequest = new LoginRequest("nobody@example.com", "TestingPassword123");
        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> {
            authController.login(nonExistentLoginRequest);
        });

        ResponseEntity<Map<String, Object>> errorResponse = globalExceptionHandler.handleInvalidCredentials(ex);
        assertEquals(HttpStatus.UNAUTHORIZED, errorResponse.getStatusCode());
        assertEquals(401, errorResponse.getBody().get("status"));
    }

    @Test
    @DisplayName("Invalid login input triggers Bean Validation errors")
    void testLoginValidationFailure() {
        LoginRequest invalidRequest = new LoginRequest("", "");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(invalidRequest);

        assertFalse(violations.isEmpty());
        boolean hasEmailError = violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        boolean hasPasswordError = violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password"));

        assertTrue(hasEmailError);
        assertTrue(hasPasswordError);
    }
}
