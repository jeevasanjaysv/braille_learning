package com.braille.braille_learning.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSubmissionRequest {

    @NotBlank(message = "Answer must not be blank")
    private String answer;
}
