package com.braille.braille_learning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSubmissionRequest {

    @NotBlank(message = "Answer must not be blank")
    @Size(max = 500, message = "Answer must not exceed 500 characters")
    private String answer;
}
