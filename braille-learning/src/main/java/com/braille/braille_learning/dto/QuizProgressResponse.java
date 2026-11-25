package com.braille.braille_learning.dto;

import com.braille.braille_learning.entity.Quiz;
import com.braille.braille_learning.entity.QuizAttempt;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuizProgressResponse {

    private Long quizId;
    private String quizTitle;
    private long completedAttempts;
    private Integer latestScore;
    private Integer latestTotalQuestions;
    private LocalDateTime latestCompletionDate;
    private Integer bestScore;

    public static QuizProgressResponse fromQuiz(Quiz quiz, List<QuizAttempt> attempts) {
        QuizAttempt latestAttempt = attempts.isEmpty() ? null : attempts.get(0);
        Integer bestScore = attempts.stream()
                .map(QuizAttempt::getScore)
                .max(Integer::compareTo)
                .orElse(null);

        return QuizProgressResponse.builder()
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .completedAttempts(attempts.size())
                .latestScore(latestAttempt != null ? latestAttempt.getScore() : null)
                .latestTotalQuestions(latestAttempt != null ? latestAttempt.getTotalQuestions() : null)
                .latestCompletionDate(latestAttempt != null ? latestAttempt.getCompletedAt() : null)
                .bestScore(bestScore)
                .build();
    }
}
