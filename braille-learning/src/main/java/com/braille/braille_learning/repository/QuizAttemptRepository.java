package com.braille.braille_learning.repository;

import com.braille.braille_learning.entity.QuizAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByUserIdOrderByStartedAtDesc(Long userId);

    List<QuizAttempt> findByUserIdAndQuizIdOrderByStartedAtDesc(Long userId, Long quizId);

    List<QuizAttempt> findByUserIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(Long userId);

    long countByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select attempt from QuizAttempt attempt
            where attempt.id = :attemptId and lower(attempt.user.email) = lower(:email)
            """)
    Optional<QuizAttempt> findOwnedByIdForUpdate(
            @Param("attemptId") Long attemptId,
            @Param("email") String email
    );

    Optional<QuizAttempt> findByIdAndUserEmailIgnoreCase(Long attemptId, String email);
}
