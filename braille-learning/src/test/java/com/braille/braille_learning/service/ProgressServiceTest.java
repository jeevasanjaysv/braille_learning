package com.braille.braille_learning.service;

import com.braille.braille_learning.dto.ProgressResponse;
import com.braille.braille_learning.dto.ProgressSummaryResponse;
import com.braille.braille_learning.entity.Lesson;
import com.braille.braille_learning.entity.LessonProgress;
import com.braille.braille_learning.entity.QuizAttempt;
import com.braille.braille_learning.entity.User;
import com.braille.braille_learning.repository.LessonProgressRepository;
import com.braille.braille_learning.repository.LessonRepository;
import com.braille.braille_learning.repository.QuizAttemptRepository;
import com.braille.braille_learning.repository.QuizRepository;
import com.braille.braille_learning.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProgressServiceTest {

    private final LessonProgressRepository lessonProgressRepository = mock(LessonProgressRepository.class);
    private final LessonRepository lessonRepository = mock(LessonRepository.class);
    private final QuizAttemptRepository quizAttemptRepository = mock(QuizAttemptRepository.class);
    private final QuizRepository quizRepository = mock(QuizRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private final ProgressService progressService = new ProgressService(
            lessonProgressRepository,
            lessonRepository,
            quizAttemptRepository,
            quizRepository,
            userRepository
    );

    private final User user = new User(42L, "Progress Student", "student@example.com", "hash", LocalDateTime.now());

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("student@example.com", null, List.of())
        );
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(lessonProgressRepository.findByUserIdAndCompletedTrue(user.getId())).thenReturn(List.of());
        when(quizAttemptRepository.findByUserIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(user.getId()))
                .thenReturn(List.of());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reportsNoCompletedLessonsOrQuizAttempts() {
        when(lessonRepository.count()).thenReturn(4L);

        ProgressResponse progress = progressService.getProgress();

        assertEquals(4, progress.getTotalLessons());
        assertEquals(0, progress.getCompletedLessons());
        assertEquals(4, progress.getRemainingLessons());
        assertEquals(0.0, progress.getLearningPercentage());
        assertEquals(0, progress.getTotalQuizAttempts());
        assertEquals(0.0, progress.getAverageQuizScore());
    }

    @Test
    void summaryMarksStudentNotStartedWhenNoLessonsAreCompleted() {
        when(lessonRepository.count()).thenReturn(4L);

        ProgressSummaryResponse summary = progressService.getProgressSummary();

        assertEquals(4, summary.getTotalLessons());
        assertEquals(0, summary.getCompletedLessons());
        assertEquals(4, summary.getRemainingLessons());
        assertEquals(0.0, summary.getLearningPercentage());
        assertEquals("NOT_STARTED", summary.getLearningStatus());
        assertEquals(0, summary.getTotalCompletedQuizAttempts());
        assertEquals(0.0, summary.getAverageQuizScore());
        assertEquals(0.0, summary.getBestQuizScore());
        assertNull(summary.getLatestQuizCompletionDate());
    }

    @Test
    void calculatesPartialLessonProgressAndIgnoresDuplicateLessonRecords() {
        when(lessonRepository.count()).thenReturn(4L);
        Lesson firstLesson = lesson(1L);
        Lesson secondLesson = lesson(2L);
        when(lessonProgressRepository.findByUserIdAndCompletedTrue(user.getId())).thenReturn(List.of(
                completedProgress(firstLesson),
                completedProgress(firstLesson),
                completedProgress(secondLesson)
        ));

        ProgressResponse progress = progressService.getProgress();

        assertEquals(2, progress.getCompletedLessons());
        assertEquals(2, progress.getRemainingLessons());
        assertEquals(50.0, progress.getLearningPercentage());
    }

    @Test
    void summaryMarksStudentInProgressWhenSomeLessonsAreCompleted() {
        when(lessonRepository.count()).thenReturn(4L);
        when(lessonProgressRepository.findByUserIdAndCompletedTrue(user.getId()))
                .thenReturn(List.of(completedProgress(lesson(1L))));

        ProgressSummaryResponse summary = progressService.getProgressSummary();

        assertEquals(25.0, summary.getLearningPercentage());
        assertEquals("IN_PROGRESS", summary.getLearningStatus());
    }

    @Test
    void calculatesAllLessonsCompleted() {
        when(lessonRepository.count()).thenReturn(2L);
        when(lessonProgressRepository.findByUserIdAndCompletedTrue(user.getId()))
                .thenReturn(List.of(completedProgress(lesson(1L)), completedProgress(lesson(2L))));

        ProgressResponse progress = progressService.getProgress();

        assertEquals(2, progress.getCompletedLessons());
        assertEquals(0, progress.getRemainingLessons());
        assertEquals(100.0, progress.getLearningPercentage());
    }

    @Test
    void summaryMarksStudentCompletedWhenAllAvailableLessonsAreCompleted() {
        when(lessonRepository.count()).thenReturn(2L);
        when(lessonProgressRepository.findByUserIdAndCompletedTrue(user.getId()))
                .thenReturn(List.of(completedProgress(lesson(1L)), completedProgress(lesson(2L))));

        ProgressSummaryResponse summary = progressService.getProgressSummary();

        assertEquals("COMPLETED", summary.getLearningStatus());
        assertEquals(100.0, summary.getLearningPercentage());
        assertEquals(0, summary.getRemainingLessons());
    }

    @Test
    void returnsZeroLearningPercentageWhenThereAreNoLessons() {
        when(lessonRepository.count()).thenReturn(0L);
        when(lessonProgressRepository.findByUserIdAndCompletedTrue(user.getId()))
                .thenReturn(List.of(completedProgress(lesson(1L))));

        ProgressResponse progress = progressService.getProgress();

        assertEquals(0, progress.getTotalLessons());
        assertEquals(0, progress.getCompletedLessons());
        assertEquals(0, progress.getRemainingLessons());
        assertEquals(0.0, progress.getLearningPercentage());
    }

    @Test
    void summaryWithZeroLessonsIsNotStartedAndHasNoQuizScores() {
        when(lessonRepository.count()).thenReturn(0L);

        ProgressSummaryResponse summary = progressService.getProgressSummary();

        assertEquals(0, summary.getTotalLessons());
        assertEquals(0, summary.getCompletedLessons());
        assertEquals(0, summary.getRemainingLessons());
        assertEquals(0.0, summary.getLearningPercentage());
        assertEquals("NOT_STARTED", summary.getLearningStatus());
        assertEquals(0.0, summary.getAverageQuizScore());
        assertEquals(0.0, summary.getBestQuizScore());
    }

    @Test
    void averagesOnlyCompletedQuizAttemptPercentages() {
        when(lessonRepository.count()).thenReturn(0L);
        when(quizAttemptRepository.findByUserIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(user.getId()))
                .thenReturn(List.of(attempt(3, 4), attempt(1, 2)));

        ProgressResponse progress = progressService.getProgress();

        assertEquals(2, progress.getTotalQuizAttempts());
        assertEquals(62.5, progress.getAverageQuizScore());
    }

    @Test
    void summaryCalculatesAverageBestAndLatestCompletedQuizAttempt() {
        when(lessonRepository.count()).thenReturn(0L);
        LocalDateTime latestCompletion = LocalDateTime.now();
        QuizAttempt latest = attempt(4, 5);
        latest.setCompletedAt(latestCompletion);
        QuizAttempt best = attempt(9, 10);
        best.setCompletedAt(latestCompletion.minusDays(1));
        QuizAttempt lowest = attempt(1, 4);
        lowest.setCompletedAt(latestCompletion.minusDays(2));
        when(quizAttemptRepository.findByUserIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(user.getId()))
                .thenReturn(List.of(latest, best, lowest));

        ProgressSummaryResponse summary = progressService.getProgressSummary();

        assertEquals(3, summary.getTotalCompletedQuizAttempts());
        assertEquals(65.0, summary.getAverageQuizScore());
        assertEquals(90.0, summary.getBestQuizScore());
        assertEquals(latestCompletion, summary.getLatestQuizCompletionDate());
    }

    @Test
    void treatsCompletedAttemptsWithZeroQuestionsAsZeroPercent() {
        when(lessonRepository.count()).thenReturn(0L);
        when(quizAttemptRepository.findByUserIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(user.getId()))
                .thenReturn(List.of(attempt(0, 0)));

        ProgressResponse progress = progressService.getProgress();
        ProgressSummaryResponse summary = progressService.getProgressSummary();

        assertEquals(1, progress.getTotalQuizAttempts());
        assertEquals(0.0, progress.getAverageQuizScore());
        assertEquals(1, summary.getTotalCompletedQuizAttempts());
        assertEquals(0.0, summary.getAverageQuizScore());
        assertEquals(0.0, summary.getBestQuizScore());
    }

    private static Lesson lesson(Long id) {
        return new Lesson(id, "Lesson " + id, "Description", "TEST", id.intValue(), "", LocalDateTime.now());
    }

    private static LessonProgress completedProgress(Lesson lesson) {
        return new LessonProgress(1L, null, lesson, true, LocalDateTime.now());
    }

    private static QuizAttempt attempt(int score, int totalQuestions) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setScore(score);
        attempt.setTotalQuestions(totalQuestions);
        attempt.setCompletedAt(LocalDateTime.now());
        return attempt;
    }
}
