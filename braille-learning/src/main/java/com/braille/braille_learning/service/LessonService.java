package com.braille.braille_learning.service;

import com.braille.braille_learning.dto.BrailleCharacterResponse;
import com.braille.braille_learning.dto.LessonResponse;
import com.braille.braille_learning.entity.Lesson;
import com.braille.braille_learning.exception.ResourceNotFoundException;
import com.braille.braille_learning.repository.BrailleCharacterRepository;
import com.braille.braille_learning.repository.LessonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final BrailleCharacterRepository brailleCharacterRepository;

    public LessonService(LessonRepository lessonRepository, BrailleCharacterRepository brailleCharacterRepository) {
        this.lessonRepository = lessonRepository;
        this.brailleCharacterRepository = brailleCharacterRepository;
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> getAllLessons() {
        return lessonRepository.findAllByOrderByLessonOrderAsc().stream()
                .map(LessonResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LessonResponse getLessonById(Long id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + id));
        return LessonResponse.fromEntity(lesson);
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> getLessonsByCategory(String category) {
        String cleanCategory = category != null ? category.trim() : "";
        List<Lesson> lessons = lessonRepository.findByCategoryOrderByLessonOrderAsc(cleanCategory.toUpperCase());
        if (lessons.isEmpty()) {
            lessons = lessonRepository.findByCategoryOrderByLessonOrderAsc(cleanCategory);
        }
        return lessons.stream()
                .map(LessonResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BrailleCharacterResponse> getCharactersByLessonId(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new ResourceNotFoundException("Lesson not found with id: " + lessonId);
        }
        return brailleCharacterRepository.findByLessonIdOrderByCharacterAsc(lessonId).stream()
                .map(BrailleCharacterResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
