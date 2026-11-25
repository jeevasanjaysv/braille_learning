package com.braille.braille_learning.service;

import com.braille.braille_learning.dto.AnswerSubmissionResponse;
import com.braille.braille_learning.dto.QuizAttemptAnswerResponse;
import com.braille.braille_learning.dto.QuizAttemptDetailsResponse;
import com.braille.braille_learning.dto.QuizAttemptResponse;
import com.braille.braille_learning.dto.QuizAttemptStartResponse;
import com.braille.braille_learning.entity.Question;
import com.braille.braille_learning.entity.Quiz;
import com.braille.braille_learning.entity.QuizAttempt;
import com.braille.braille_learning.entity.QuizAttemptAnswer;
import com.braille.braille_learning.entity.User;
import com.braille.braille_learning.exception.InvalidQuizAttemptStateException;
import com.braille.braille_learning.exception.ResourceNotFoundException;
import com.braille.braille_learning.repository.QuestionRepository;
import com.braille.braille_learning.repository.QuizAttemptAnswerRepository;
import com.braille.braille_learning.repository.QuizAttemptRepository;
import com.braille.braille_learning.repository.QuizRepository;
import com.braille.braille_learning.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class QuizAttemptService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAttemptAnswerRepository answerRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final QuizService quizService;

    public QuizAttemptService(
            QuizAttemptRepository quizAttemptRepository,
            QuizAttemptAnswerRepository answerRepository,
            QuizRepository quizRepository,
            QuestionRepository questionRepository,
            UserRepository userRepository,
            QuizService quizService
    ) {
        this.quizAttemptRepository = quizAttemptRepository;
        this.answerRepository = answerRepository;
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.quizService = quizService;
    }

    @Transactional
    public QuizAttemptStartResponse startAttempt(Long quizId, String userEmail) {
        User user = findUser(userEmail);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));
        QuizAttempt attempt = quizAttemptRepository.save(new QuizAttempt(user, quiz));
        return QuizAttemptStartResponse.fromEntity(attempt);
    }

    @Transactional
    public AnswerSubmissionResponse submitAnswer(
            Long attemptId,
            Long questionId,
            String submittedAnswer,
            String userEmail
    ) {
        QuizAttempt attempt = findOwnedAttemptForUpdate(attemptId, userEmail);
        ensureInProgress(attempt);

        AnswerSubmissionResponse result = quizService.checkAnswer(
                attempt.getQuiz().getId(),
                questionId,
                submittedAnswer
        );

        Question question = questionRepository.getReferenceById(questionId);
        QuizAttemptAnswer savedAnswer = answerRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseGet(() -> new QuizAttemptAnswer(attempt, question, submittedAnswer, result.isCorrect()));
        savedAnswer.setSubmittedAnswer(submittedAnswer);
        savedAnswer.setCorrect(result.isCorrect());
        savedAnswer.setAnsweredAt(LocalDateTime.now());
        answerRepository.saveAndFlush(savedAnswer);

        attempt.setScore(Math.toIntExact(answerRepository.countByAttemptIdAndCorrectTrue(attemptId)));
        result.setScore(attempt.getScore());
        return result;
    }

    @Transactional
    public QuizAttemptResponse completeAttempt(Long attemptId, String userEmail) {
        QuizAttempt attempt = findOwnedAttemptForUpdate(attemptId, userEmail);
        ensureInProgress(attempt);

        attempt.setScore(Math.toIntExact(answerRepository.countByAttemptIdAndCorrectTrue(attemptId)));
        attempt.setTotalQuestions(Math.toIntExact(questionRepository.countByQuizId(attempt.getQuiz().getId())));
        attempt.setCompletedAt(LocalDateTime.now());
        return QuizAttemptResponse.fromEntity(quizAttemptRepository.save(attempt));
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptResponse> getHistory(String userEmail) {
        User user = findUser(userEmail);
        return quizAttemptRepository.findByUserIdOrderByStartedAtDesc(user.getId()).stream()
                .map(QuizAttemptResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuizAttemptDetailsResponse getAttemptDetails(Long attemptId, String userEmail) {
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserEmailIgnoreCase(attemptId, normalizeEmail(userEmail))
                .orElseThrow(() -> attemptNotFound(attemptId));
        List<QuizAttemptAnswerResponse> answers = answerRepository
                .findByAttemptIdOrderByAnsweredAtAsc(attemptId).stream()
                .map(QuizAttemptAnswerResponse::fromEntity)
                .collect(Collectors.toList());
        return QuizAttemptDetailsResponse.builder()
                .attempt(QuizAttemptResponse.fromEntity(attempt))
                .answers(answers)
                .build();
    }

    private QuizAttempt findOwnedAttemptForUpdate(Long attemptId, String userEmail) {
        return quizAttemptRepository.findOwnedByIdForUpdate(attemptId, normalizeEmail(userEmail))
                .orElseThrow(() -> attemptNotFound(attemptId));
    }

    private void ensureInProgress(QuizAttempt attempt) {
        if (attempt.getCompletedAt() != null) {
            throw new InvalidQuizAttemptStateException(
                    "Quiz attempt with id " + attempt.getId() + " has already been completed"
            );
        }
    }

    private User findUser(String email) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + normalizedEmail));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static ResourceNotFoundException attemptNotFound(Long attemptId) {
        return new ResourceNotFoundException("Quiz attempt not found with id: " + attemptId);
    }
}
