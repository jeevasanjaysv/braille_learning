package com.braille.braille_learning.controller;

import com.braille.braille_learning.dto.AnswerSubmissionRequest;
import com.braille.braille_learning.dto.AnswerSubmissionResponse;
import com.braille.braille_learning.dto.QuestionResponse;
import com.braille.braille_learning.dto.QuizResponse;
import com.braille.braille_learning.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping
    public ResponseEntity<List<QuizResponse>> getAllQuizzes() {
        return ResponseEntity.ok(quizService.getAllQuizzes());
    }

    @GetMapping("/{quizId}")
    public ResponseEntity<QuizResponse> getQuizById(@PathVariable Long quizId) {
        return ResponseEntity.ok(quizService.getQuizById(quizId));
    }

    @GetMapping("/{quizId}/questions")
    public ResponseEntity<List<QuestionResponse>> getQuestions(@PathVariable Long quizId) {
        return ResponseEntity.ok(quizService.getQuestions(quizId));
    }

    @PostMapping("/{quizId}/questions/{questionId}/answer")
    public ResponseEntity<AnswerSubmissionResponse> submitAnswer(
            @PathVariable Long quizId,
            @PathVariable Long questionId,
            @Valid @RequestBody AnswerSubmissionRequest request
    ) {
        return ResponseEntity.ok(quizService.submitAnswer(quizId, questionId, request.getAnswer()));
    }
}
