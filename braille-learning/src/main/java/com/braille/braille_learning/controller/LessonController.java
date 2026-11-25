package com.braille.braille_learning.controller;

import com.braille.braille_learning.dto.BrailleCharacterResponse;
import com.braille.braille_learning.dto.LessonProgressResponse;
import com.braille.braille_learning.dto.LessonResponse;
import com.braille.braille_learning.dto.ProgressCountResponse;
import com.braille.braille_learning.service.LessonProgressService;
import com.braille.braille_learning.service.LessonService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    private final LessonService lessonService;
    private final LessonProgressService lessonProgressService;

    public LessonController(LessonService lessonService, LessonProgressService lessonProgressService) {
        this.lessonService = lessonService;
        this.lessonProgressService = lessonProgressService;
    }

    @GetMapping
    public ResponseEntity<List<LessonResponse>> getAllLessons() {
        List<LessonResponse> lessons = lessonService.getAllLessons();
        return ResponseEntity.ok(lessons);
    }

    @GetMapping("/progress")
    public ResponseEntity<List<LessonProgressResponse>> getCompletedLessons(Authentication authentication) {
        List<LessonProgressResponse> progress = lessonProgressService.getCompletedLessons(authentication.getName());
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/progress/count")
    public ResponseEntity<ProgressCountResponse> getCompletedLessonCount(Authentication authentication) {
        ProgressCountResponse count = lessonProgressService.getCompletedLessonCount(authentication.getName());
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LessonResponse> getLessonById(@PathVariable Long id) {
        LessonResponse lesson = lessonService.getLessonById(id);
        return ResponseEntity.ok(lesson);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<LessonResponse>> getLessonsByCategory(@PathVariable String category) {
        List<LessonResponse> lessons = lessonService.getLessonsByCategory(category);
        return ResponseEntity.ok(lessons);
    }

    @GetMapping("/{id}/characters")
    public ResponseEntity<List<BrailleCharacterResponse>> getCharactersByLessonId(@PathVariable Long id) {
        List<BrailleCharacterResponse> characters = lessonService.getCharactersByLessonId(id);
        return ResponseEntity.ok(characters);
    }

    @PostMapping("/{lessonId}/complete")
    public ResponseEntity<LessonProgressResponse> completeLesson(
            @PathVariable Long lessonId,
            Authentication authentication
    ) {
        LessonProgressResponse response = lessonProgressService.completeLesson(lessonId, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
