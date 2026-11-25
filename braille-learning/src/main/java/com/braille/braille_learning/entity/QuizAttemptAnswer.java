package com.braille.braille_learning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "quiz_attempt_answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attempt_question_answer",
                columnNames = {"attempt_id", "question_id"}
        )
)
public class QuizAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "submitted_answer", nullable = false, length = 500)
    private String submittedAnswer;

    @Column(name = "correct", nullable = false)
    private boolean correct;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;

    public QuizAttemptAnswer() {
    }

    public QuizAttemptAnswer(QuizAttempt attempt, Question question, String submittedAnswer, boolean correct) {
        this.attempt = attempt;
        this.question = question;
        this.submittedAnswer = submittedAnswer;
        this.correct = correct;
        this.answeredAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (answeredAt == null) {
            answeredAt = LocalDateTime.now();
        }
    }
}
