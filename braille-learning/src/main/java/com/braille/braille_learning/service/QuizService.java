package com.braille.braille_learning.service;

import com.braille.braille_learning.dto.AnswerSubmissionResponse;
import com.braille.braille_learning.dto.QuestionResponse;
import com.braille.braille_learning.dto.QuizResponse;
import com.braille.braille_learning.entity.Question;
import com.braille.braille_learning.entity.Quiz;
import com.braille.braille_learning.exception.ResourceNotFoundException;
import com.braille.braille_learning.repository.QuestionRepository;
import com.braille.braille_learning.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;

    public QuizService(QuizRepository quizRepository, QuestionRepository questionRepository) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> getAllQuizzes() {
        return quizRepository.findAll().stream()
                .map(QuizResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuizById(Long quizId) {
        return QuizResponse.fromEntity(findQuiz(quizId));
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestions(Long quizId) {
        findQuiz(quizId);
        return questionRepository.findByQuizIdOrderByQuestionOrderAsc(quizId).stream()
                .map(QuestionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AnswerSubmissionResponse submitAnswer(Long quizId, Long questionId, String submittedAnswer) {
        return checkAnswer(quizId, questionId, submittedAnswer);
    }

    @Transactional(readOnly = true)
    public AnswerSubmissionResponse checkAnswer(Long quizId, Long questionId, String submittedAnswer) {
        findQuiz(quizId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        if (question.getQuiz() == null || !quizId.equals(question.getQuiz().getId())) {
            throw new ResourceNotFoundException(
                    "Question not found with id: " + questionId + " in quiz with id: " + quizId
            );
        }

        boolean correct = Stream.concat(
                        Stream.of(question.getExpectedAnswer()),
                        question.getAcceptableAnswers() == null
                                ? Stream.empty()
                                : Arrays.stream(question.getAcceptableAnswers().split(";"))
                )
                .map(QuizService::normalizeAnswer)
                .anyMatch(acceptedAnswer -> acceptedAnswer.equals(normalizeAnswer(submittedAnswer)));

        String feedback = correct
                ? "Correct! The answer is " + question.getExpectedAnswer() + "."
                : "Incorrect. The correct answer is " + question.getExpectedAnswer() + ".";

        return AnswerSubmissionResponse.builder()
                .correct(correct)
                .feedback(feedback)
                .explanation(question.getExplanation())
                .correctAnswer(question.getExpectedAnswer())
                .score(correct ? 1 : 0)
                .build();
    }

    private Quiz findQuiz(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));
    }

    private static String normalizeAnswer(String answer) {
        return answer.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\bone\\b", "1")
                .replaceAll("\\btwo\\b", "2")
                .replaceAll("\\bthree\\b", "3")
                .replaceAll("\\bfour\\b", "4")
                .replaceAll("\\bfive\\b", "5")
                .replaceAll("\\bsix\\b", "6");
    }
}
