package com.braille.braille_learning.repository;

import com.braille.braille_learning.entity.QuizAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswer, Long> {

    Optional<QuizAttemptAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    List<QuizAttemptAnswer> findByAttemptIdOrderByAnsweredAtAsc(Long attemptId);

    long countByAttemptIdAndCorrectTrue(Long attemptId);
}
