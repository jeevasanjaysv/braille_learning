package com.braille.braille_learning.dto;

import com.braille.braille_learning.entity.Question;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionResponse {

    private Long id;
    private Long quizId;
    private String questionText;
    private Integer questionOrder;

    public static QuestionResponse fromEntity(Question question) {
        if (question == null) {
            return null;
        }
        return QuestionResponse.builder()
                .id(question.getId())
                .quizId(question.getQuiz() != null ? question.getQuiz().getId() : null)
                .questionText(question.getQuestionText())
                .questionOrder(question.getQuestionOrder())
                .build();
    }
}
