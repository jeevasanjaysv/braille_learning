package com.braille.braille_learning.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, unique = true, length = 150)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "lesson_order", nullable = false, unique = true)
    private Integer lessonOrder;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BrailleCharacter> characters = new ArrayList<>();

    public Lesson() {
        this.createdAt = LocalDateTime.now();
    }

    public Lesson(String title, String description, String category, Integer lessonOrder, String content) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.lessonOrder = lessonOrder;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public Lesson(Long id, String title, String description, String category, Integer lessonOrder, String content, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.lessonOrder = lessonOrder;
        this.content = content;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public void addCharacter(BrailleCharacter character) {
        characters.add(character);
        character.setLesson(this);
    }

    public void removeCharacter(BrailleCharacter character) {
        characters.remove(character);
        character.setLesson(null);
    }
}
