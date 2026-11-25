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
        name = "questions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_quiz_question_order", columnNames = {"quiz_id", "question_order"})
        }
)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question_text", nullable = false, length = 500)
    private String questionText;

    @Column(name = "expected_answer", nullable = false, length = 255)
    private String expectedAnswer;

    @Column(name = "acceptable_answers", length = 500)
    private String acceptableAnswers;

    @Column(name = "explanation", length = 500)
    private String explanation;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    public Question() {
    }

    public Question(String questionText, String expectedAnswer, String acceptableAnswers, String explanation, Integer questionOrder) {
        this.questionText = questionText;
        this.expectedAnswer = expectedAnswer;
        this.acceptableAnswers = acceptableAnswers;
        this.explanation = explanation;
        this.questionOrder = questionOrder;
    }

    public Question(String questionText, String expectedAnswer, String acceptableAnswers, String explanation, Integer questionOrder, Quiz quiz) {
        this.questionText = questionText;
        this.expectedAnswer = expectedAnswer;
        this.acceptableAnswers = acceptableAnswers;
        this.explanation = explanation;
        this.questionOrder = questionOrder;
        this.quiz = quiz;
    }

    public Question(Long id, Quiz quiz, String questionText, String expectedAnswer, String acceptableAnswers, String explanation, Integer questionOrder) {
        this.id = id;
        this.quiz = quiz;
        this.questionText = questionText;
        this.expectedAnswer = expectedAnswer;
        this.acceptableAnswers = acceptableAnswers;
        this.explanation = explanation;
        this.questionOrder = questionOrder;
    }
}
