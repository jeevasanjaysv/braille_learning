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

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "lesson_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_lesson_progress", columnNames = {"user_id", "lesson_id"})
        }
)
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public LessonProgress() {
    }

    public LessonProgress(User user, Lesson lesson, boolean completed, LocalDateTime completedAt) {
        this.user = user;
        this.lesson = lesson;
        this.completed = completed;
        this.completedAt = completedAt;
    }

    public LessonProgress(Long id, User user, Lesson lesson, boolean completed, LocalDateTime completedAt) {
        this.id = id;
        this.user = user;
        this.lesson = lesson;
        this.completed = completed;
        this.completedAt = completedAt;
    }
}
