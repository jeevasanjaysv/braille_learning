package com.braille.braille_learning.dto;

import com.braille.braille_learning.entity.Quiz;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResponse {

    private Long id;
    private String title;
    private String description;
    private Long lessonId;
    private String lessonTitle;
    private int questionCount;
    private LocalDateTime createdAt;

    public static QuizResponse fromEntity(Quiz quiz) {
        if (quiz == null) {
            return null;
        }
        return QuizResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .lessonId(quiz.getLesson() != null ? quiz.getLesson().getId() : null)
                .lessonTitle(quiz.getLesson() != null ? quiz.getLesson().getTitle() : null)
                .questionCount(quiz.getQuestions() != null ? quiz.getQuestions().size() : 0)
                .createdAt(quiz.getCreatedAt())
                .build();
    }
}
