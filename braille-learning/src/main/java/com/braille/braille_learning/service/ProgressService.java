package com.braille.braille_learning.service;

import com.braille.braille_learning.dto.ProgressResponse;
import com.braille.braille_learning.dto.ProgressSummaryResponse;
import com.braille.braille_learning.dto.LessonProgressItemResponse;
import com.braille.braille_learning.dto.QuizProgressResponse;
import com.braille.braille_learning.entity.Lesson;
import com.braille.braille_learning.entity.LessonProgress;
import com.braille.braille_learning.entity.QuizAttempt;
import com.braille.braille_learning.entity.User;
import com.braille.braille_learning.exception.ResourceNotFoundException;
import com.braille.braille_learning.repository.LessonProgressRepository;
import com.braille.braille_learning.repository.LessonRepository;
import com.braille.braille_learning.repository.QuizAttemptRepository;
import com.braille.braille_learning.repository.QuizRepository;
import com.braille.braille_learning.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProgressService {

    private final LessonProgressRepository lessonProgressRepository;
    private final LessonRepository lessonRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;

    public ProgressService(
            LessonProgressRepository lessonProgressRepository,
            LessonRepository lessonRepository,
            QuizAttemptRepository quizAttemptRepository,
            QuizRepository quizRepository,
            UserRepository userRepository
    ) {
        this.lessonProgressRepository = lessonProgressRepository;
        this.lessonRepository = lessonRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ProgressResponse getProgress() {
        User user = getAuthenticatedUser();
        ProgressData data = calculateProgress(user);

        return ProgressResponse.builder()
                .totalLessons(data.totalLessons())
                .completedLessons(data.completedLessons())
                .remainingLessons(data.remainingLessons())
                .learningPercentage(data.learningPercentage())
                .totalQuizAttempts(data.completedAttempts().size())
                .averageQuizScore(data.averageQuizScore())
                .build();
    }

    @Transactional(readOnly = true)
    public ProgressSummaryResponse getProgressSummary() {
        ProgressData data = calculateProgress(getAuthenticatedUser());
        String learningStatus = data.totalLessons() == 0 || data.completedLessons() == 0
                ? "NOT_STARTED"
                : data.completedLessons() == data.totalLessons()
                ? "COMPLETED"
                : "IN_PROGRESS";
        double bestQuizScore = data.completedAttempts().stream()
                .mapToDouble(ProgressService::quizScorePercentage)
                .max()
                .orElse(0.0);

        return ProgressSummaryResponse.builder()
                .totalLessons(data.totalLessons())
                .completedLessons(data.completedLessons())
                .remainingLessons(data.remainingLessons())
                .learningPercentage(data.learningPercentage())
                .learningStatus(learningStatus)
                .totalCompletedQuizAttempts(data.completedAttempts().size())
                .averageQuizScore(data.averageQuizScore())
                .bestQuizScore(roundPercentage(bestQuizScore))
                .latestQuizCompletionDate(data.completedAttempts().isEmpty()
                        ? null
                        : data.completedAttempts().getFirst().getCompletedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<LessonProgressItemResponse> getLessonProgress() {
        User user = getAuthenticatedUser();
        Map<Long, LessonProgress> progressByLessonId = lessonProgressRepository.findByUserId(user.getId()).stream()
                .filter(progress -> progress.getLesson() != null && progress.getLesson().getId() != null)
                .collect(Collectors.toMap(
                        progress -> progress.getLesson().getId(),
                        Function.identity(),
                        (existing, replacement) -> replacement.isCompleted() ? replacement : existing
                ));

        return lessonRepository.findAllByOrderByLessonOrderAsc().stream()
                .map(lesson -> LessonProgressItemResponse.fromLesson(
                        lesson,
                        progressByLessonId.get(lesson.getId())
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QuizProgressResponse> getQuizProgress() {
        User user = getAuthenticatedUser();
        Map<Long, List<QuizAttempt>> attemptsByQuizId =
                quizAttemptRepository.findByUserIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(user.getId()).stream()
                        .filter(attempt -> attempt.getQuiz() != null && attempt.getQuiz().getId() != null)
                        .collect(Collectors.groupingBy(attempt -> attempt.getQuiz().getId()));

        return quizRepository.findAll().stream()
                .map(quiz -> QuizProgressResponse.fromQuiz(
                        quiz,
                        attemptsByQuizId.getOrDefault(quiz.getId(), List.of())
                ))
                .collect(Collectors.toList());
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("An authenticated student is required to calculate progress");
        }
        String email = authentication.getName().trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private ProgressData calculateProgress(User user) {
        long totalLessons = lessonRepository.count();
        List<LessonProgress> completedProgress =
                lessonProgressRepository.findByUserIdAndCompletedTrue(user.getId());
        long completedLessons = completedProgress.stream()
                .map(LessonProgress::getLesson)
                .filter(lesson -> lesson != null && lesson.getId() != null)
                .map(Lesson::getId)
                .distinct()
                .count();
        completedLessons = Math.min(completedLessons, totalLessons);

        List<QuizAttempt> completedAttempts =
                quizAttemptRepository.findByUserIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(user.getId());
        double averageQuizScore = completedAttempts.stream()
                .mapToDouble(ProgressService::quizScorePercentage)
                .average()
                .orElse(0.0);

        return new ProgressData(
                totalLessons,
                completedLessons,
                totalLessons - completedLessons,
                roundPercentage(percentage(completedLessons, totalLessons)),
                completedAttempts,
                roundPercentage(averageQuizScore)
        );
    }

    private static double quizScorePercentage(QuizAttempt attempt) {
        return percentage(attempt.getScore(), attempt.getTotalQuestions());
    }

    private static double percentage(long part, long total) {
        return total == 0 ? 0.0 : part * 100.0 / total;
    }

    private static double roundPercentage(double percentage) {
        return Math.round(percentage * 100.0) / 100.0;
    }

    private record ProgressData(
            long totalLessons,
            long completedLessons,
            long remainingLessons,
            double learningPercentage,
            List<QuizAttempt> completedAttempts,
            double averageQuizScore
    ) {
    }
}
