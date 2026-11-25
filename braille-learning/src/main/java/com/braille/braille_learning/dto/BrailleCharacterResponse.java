package com.braille.braille_learning.dto;

import com.braille.braille_learning.entity.BrailleCharacter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrailleCharacterResponse {

    private Long id;
    private String character;
    private String dotPattern;
    private String description;
    private Long lessonId;

    public static BrailleCharacterResponse fromEntity(BrailleCharacter entity) {
        if (entity == null) {
            return null;
        }
        return BrailleCharacterResponse.builder()
                .id(entity.getId())
                .character(entity.getCharacter())
                .dotPattern(entity.getDotPattern())
                .description(entity.getDescription())
                .lessonId(entity.getLesson() != null ? entity.getLesson().getId() : null)
                .build();
    }
}
