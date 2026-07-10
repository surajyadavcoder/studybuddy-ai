package com.studybuddy.controller;

import com.studybuddy.entity.QuizAttempt;
import com.studybuddy.repository.QuizAttemptRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final QuizAttemptRepository quizAttemptRepository;

    public DashboardController(QuizAttemptRepository quizAttemptRepository) {
        this.quizAttemptRepository = quizAttemptRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        List<QuizAttempt> attempts = quizAttemptRepository.findByUserId(userId);
        long totalAttempts = attempts.size();
        long correctAttempts = attempts.stream().filter(QuizAttempt::isCorrect).count();

        double accuracy = totalAttempts == 0 ? 0.0 : (correctAttempts * 100.0) / totalAttempts;

        return Map.of(
                "totalQuizzesAttempted", totalAttempts,
                "correctAnswers", correctAttempts,
                "accuracyPercentage", Math.round(accuracy * 10.0) / 10.0
        );
    }
}
