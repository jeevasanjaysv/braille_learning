package com.braille.braille_learning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressResponse {

    private long totalLessons;
    private long completedLessons;
    private long remainingLessons;
    private double learningPercentage;
    private long totalQuizAttempts;
    private double averageQuizScore;
}
