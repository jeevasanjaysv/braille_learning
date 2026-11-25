package com.braille.braille_learning.dto;

import com.braille.braille_learning.entity.QuizAttempt;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QuizAttemptResponse {

    private Long attemptId;
    private Long quizId;
    private String quizTitle;
    private int score;
    private int totalQuestions;
    private double percentage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String status;

    public static QuizAttemptResponse fromEntity(QuizAttempt attempt) {
        double percentage = attempt.getTotalQuestions() == 0
                ? 0
                : Math.round(attempt.getScore() * 10000.0 / attempt.getTotalQuestions()) / 100.0;

        return QuizAttemptResponse.builder()
                .attemptId(attempt.getId())
                .quizId(attempt.getQuiz().getId())
                .quizTitle(attempt.getQuiz().getTitle())
                .score(attempt.getScore())
                .totalQuestions(attempt.getTotalQuestions())
                .percentage(percentage)
                .startedAt(attempt.getStartedAt())
                .completedAt(attempt.getCompletedAt())
                .status(attempt.getCompletedAt() == null ? "IN_PROGRESS" : "COMPLETED")
                .build();
    }
}
