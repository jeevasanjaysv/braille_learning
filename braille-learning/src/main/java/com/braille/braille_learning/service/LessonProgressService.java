package com.braille.braille_learning.service;

import com.braille.braille_learning.dto.LessonProgressResponse;
import com.braille.braille_learning.dto.ProgressCountResponse;
import com.braille.braille_learning.entity.Lesson;
import com.braille.braille_learning.entity.LessonProgress;
import com.braille.braille_learning.entity.User;
import com.braille.braille_learning.exception.ResourceNotFoundException;
import com.braille.braille_learning.repository.LessonProgressRepository;
import com.braille.braille_learning.repository.LessonRepository;
import com.braille.braille_learning.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonProgressService {

    private final LessonProgressRepository lessonProgressRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;



    @Transactional
    public LessonProgressResponse completeLesson(Long lessonId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));

        LessonProgress progress = lessonProgressRepository.findByUserIdAndLessonId(user.getId(), lesson.getId())
                .orElse(null);

        if (progress == null) {
            progress = new LessonProgress(user, lesson, true, LocalDateTime.now());
            progress = lessonProgressRepository.save(progress);
        } else if (!progress.isCompleted()) {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            progress = lessonProgressRepository.save(progress);
        }

        return LessonProgressResponse.fromEntity(progress);
    }

    @Transactional(readOnly = true)
    public List<LessonProgressResponse> getCompletedLessons(String userEmail) {
        User user = getUserByEmail(userEmail);
        return lessonProgressRepository.findByUserIdAndCompletedTrueOrderByCompletedAtDesc(user.getId()).stream()
                .map(LessonProgressResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProgressCountResponse getCompletedLessonCount(String userEmail) {
        User user = getUserByEmail(userEmail);
        long count = lessonProgressRepository.countByUserIdAndCompletedTrue(user.getId());
        return new ProgressCountResponse(count);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}
