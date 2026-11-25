package com.braille.braille_learning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(
        name = "braille_characters",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lesson_character", columnNames = {"lesson_id", "character_value"})
        }
)
public class BrailleCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_value", nullable = false, length = 10)
    private String character;

    @Column(name = "dot_pattern", nullable = false, length = 50)
    private String dotPattern;

    @Column(name = "description", length = 255)
    private String description;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    public BrailleCharacter() {
    }

    public BrailleCharacter(String character, String dotPattern, String description) {
        this.character = character;
        this.dotPattern = dotPattern;
        this.description = description;
    }

    public BrailleCharacter(String character, String dotPattern, String description, Lesson lesson) {
        this.character = character;
        this.dotPattern = dotPattern;
        this.description = description;
        this.lesson = lesson;
    }

    public BrailleCharacter(Long id, String character, String dotPattern, String description, Lesson lesson) {
        this.id = id;
        this.character = character;
        this.dotPattern = dotPattern;
        this.description = description;
        this.lesson = lesson;
    }

    public String getCharacterValue() {
        return character;
    }

    public void setCharacterValue(String characterValue) {
        this.character = characterValue;
    }
}
