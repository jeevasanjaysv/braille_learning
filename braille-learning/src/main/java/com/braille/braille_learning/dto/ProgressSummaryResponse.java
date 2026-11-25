package com.braille.braille_learning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressSummaryResponse {

    private long totalLessons;
    private long completedLessons;
    private long remainingLessons;
    private double learningPercentage;
    private String learningStatus;
    private long totalCompletedQuizAttempts;
    private double averageQuizScore;
    private double bestQuizScore;
    private LocalDateTime latestQuizCompletionDate;
}
