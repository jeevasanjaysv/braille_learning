package com.braille.braille_learning.controller;

import com.braille.braille_learning.dto.LessonProgressItemResponse;
import com.braille.braille_learning.dto.ProgressResponse;
import com.braille.braille_learning.dto.ProgressSummaryResponse;
import com.braille.braille_learning.dto.QuizProgressResponse;
import com.braille.braille_learning.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping
    public ResponseEntity<ProgressResponse> getProgress() {
        return ResponseEntity.ok(progressService.getProgress());
    }

    @GetMapping("/summary")
    public ResponseEntity<ProgressSummaryResponse> getProgressSummary() {
        return ResponseEntity.ok(progressService.getProgressSummary());
    }

    @GetMapping("/lessons")
    public ResponseEntity<List<LessonProgressItemResponse>> getLessonProgress() {
        return ResponseEntity.ok(progressService.getLessonProgress());
    }

    @GetMapping("/quizzes")
    public ResponseEntity<List<QuizProgressResponse>> getQuizProgress() {
        return ResponseEntity.ok(progressService.getQuizProgress());
    }
}
