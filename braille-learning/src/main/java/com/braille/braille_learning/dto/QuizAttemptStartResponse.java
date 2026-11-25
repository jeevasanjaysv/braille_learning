package com.braille.braille_learning.dto;

import com.braille.braille_learning.entity.QuizAttempt;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QuizAttemptStartResponse {

    private Long attemptId;
    private Long quizId;
    private LocalDateTime startedAt;
    private String status;

    public static QuizAttemptStartResponse fromEntity(QuizAttempt attempt) {
        return QuizAttemptStartResponse.builder()
                .attemptId(attempt.getId())
                .quizId(attempt.getQuiz().getId())
                .startedAt(attempt.getStartedAt())
                .status(attempt.getCompletedAt() == null ? "IN_PROGRESS" : "COMPLETED")
                .build();
    }
}
