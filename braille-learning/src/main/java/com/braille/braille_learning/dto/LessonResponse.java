package com.braille.braille_learning.dto;

import com.braille.braille_learning.entity.Lesson;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private Integer lessonOrder;
    private String content;
    private LocalDateTime createdAt;
    private Integer characterCount;
    private List<BrailleCharacterResponse> characters;

    public static LessonResponse fromEntity(Lesson lesson) {
        if (lesson == null) {
            return null;
        }
        List<BrailleCharacterResponse> characterResponses = lesson.getCharacters() != null
                ? lesson.getCharacters().stream()
                        .map(BrailleCharacterResponse::fromEntity)
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return LessonResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .category(lesson.getCategory())
                .lessonOrder(lesson.getLessonOrder())
                .content(lesson.getContent())
                .createdAt(lesson.getCreatedAt())
                .characterCount(characterResponses.size())
                .characters(characterResponses)
                .build();
    }
}
