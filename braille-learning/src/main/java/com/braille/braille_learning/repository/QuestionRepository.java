package com.braille.braille_learning.repository;

import com.braille.braille_learning.entity.Question;
import com.braille.braille_learning.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuizIdOrderByQuestionOrderAsc(Long quizId);

    List<Question> findByQuizOrderByQuestionOrderAsc(Quiz quiz);

    Optional<Question> findByQuizIdAndQuestionOrder(Long quizId, Integer questionOrder);

    long countByQuizId(Long quizId);
}
