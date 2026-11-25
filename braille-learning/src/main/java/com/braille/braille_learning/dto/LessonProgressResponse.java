package com.braille.braille_learning.dto;

import com.braille.braille_learning.entity.LessonProgress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonProgressResponse {

    private Long id;
    private Long lessonId;
    private String lessonTitle;
    private String category;
    private Integer lessonOrder;
    private boolean completed;
    private LocalDateTime completedAt;

    public static LessonProgressResponse fromEntity(LessonProgress progress) {
        if (progress == null) {
            return null;
        }
        return LessonProgressResponse.builder()
                .id(progress.getId())
                .lessonId(progress.getLesson() != null ? progress.getLesson().getId() : null)
                .lessonTitle(progress.getLesson() != null ? progress.getLesson().getTitle() : null)
                .category(progress.getLesson() != null ? progress.getLesson().getCategory() : null)
                .lessonOrder(progress.getLesson() != null ? progress.getLesson().getLessonOrder() : null)
                .completed(progress.isCompleted())
                .completedAt(progress.getCompletedAt())
                .build();
    }
}
