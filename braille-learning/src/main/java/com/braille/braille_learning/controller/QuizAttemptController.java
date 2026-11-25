package com.braille.braille_learning.controller;

import com.braille.braille_learning.dto.AnswerSubmissionRequest;
import com.braille.braille_learning.dto.AnswerSubmissionResponse;
import com.braille.braille_learning.dto.QuizAttemptDetailsResponse;
import com.braille.braille_learning.dto.QuizAttemptResponse;
import com.braille.braille_learning.service.QuizAttemptService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/attempts")
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    public QuizAttemptController(QuizAttemptService quizAttemptService) {
        this.quizAttemptService = quizAttemptService;
    }

    @PostMapping("/{attemptId}/questions/{questionId}/answer")
    public ResponseEntity<AnswerSubmissionResponse> submitAnswer(
            @PathVariable Long attemptId,
            @PathVariable Long questionId,
            @Valid @RequestBody AnswerSubmissionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quizAttemptService.submitAnswer(
                attemptId,
                questionId,
                request.getAnswer(),
                authentication.getName()
        ));
    }

    @PostMapping("/{attemptId}/complete")
    public ResponseEntity<QuizAttemptResponse> completeAttempt(
            @PathVariable Long attemptId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quizAttemptService.completeAttempt(attemptId, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<QuizAttemptResponse>> getHistory(Authentication authentication) {
        return ResponseEntity.ok(quizAttemptService.getHistory(authentication.getName()));
    }

    @GetMapping("/{attemptId}")
    public ResponseEntity<QuizAttemptDetailsResponse> getAttemptDetails(
            @PathVariable Long attemptId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quizAttemptService.getAttemptDetails(attemptId, authentication.getName()));
    }
}
