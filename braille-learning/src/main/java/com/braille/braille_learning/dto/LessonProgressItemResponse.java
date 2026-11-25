package com.braille.braille_learning.dto;

import com.braille.braille_learning.entity.Lesson;
import com.braille.braille_learning.entity.LessonProgress;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LessonProgressItemResponse {

    private Long lessonId;
    private String lessonTitle;
    private String category;
    private boolean completed;
    private LocalDateTime completedAt;

    public static LessonProgressItemResponse fromLesson(Lesson lesson, LessonProgress progress) {
        boolean completed = progress != null && progress.isCompleted();
        return LessonProgressItemResponse.builder()
                .lessonId(lesson.getId())
                .lessonTitle(lesson.getTitle())
                .category(lesson.getCategory())
                .completed(completed)
                .completedAt(completed ? progress.getCompletedAt() : null)
                .build();
    }
}
