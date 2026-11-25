package com.braille.braille_learning.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuizAttemptDetailsResponse {

    private QuizAttemptResponse attempt;
    private List<QuizAttemptAnswerResponse> answers;
}
