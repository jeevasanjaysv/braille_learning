package com.braille.braille_learning.dto;

import com.braille.braille_learning.entity.QuizAttemptAnswer;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QuizAttemptAnswerResponse {

    private Long questionId;
    private String submittedAnswer;
    private boolean correct;
    private LocalDateTime answeredAt;

    public static QuizAttemptAnswerResponse fromEntity(QuizAttemptAnswer answer) {
        return QuizAttemptAnswerResponse.builder()
                .questionId(answer.getQuestion().getId())
                .submittedAnswer(answer.getSubmittedAnswer())
                .correct(answer.isCorrect())
                .answeredAt(answer.getAnsweredAt())
                .build();
    }
}
