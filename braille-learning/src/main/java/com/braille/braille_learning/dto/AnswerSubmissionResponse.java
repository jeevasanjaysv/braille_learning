package com.braille.braille_learning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerSubmissionResponse {

    private boolean correct;
    private String feedback;
    private String explanation;
    private String correctAnswer;
    private int score;
}
