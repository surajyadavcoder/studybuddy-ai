package com.studybuddy.controller;

import com.studybuddy.entity.Quiz;
import com.studybuddy.entity.QuizAttempt;
import com.studybuddy.repository.QuizAttemptRepository;
import com.studybuddy.repository.QuizRepository;
import com.studybuddy.service.QuizGenerationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizGenerationService quizGenerationService;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public QuizController(QuizGenerationService quizGenerationService,
                           QuizRepository quizRepository,
                           QuizAttemptRepository quizAttemptRepository) {
        this.quizGenerationService = quizGenerationService;
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    @PostMapping("/generate/{documentId}")
    public List<Quiz> generate(@PathVariable Long documentId,
                                @RequestParam(defaultValue = "5") int count) {
        return quizGenerationService.generateQuizForDocument(documentId, count);
    }

    @GetMapping("/document/{documentId}")
    public List<Quiz> getQuizzesForDocument(@PathVariable Long documentId) {
        return quizRepository.findByDocumentId(documentId);
    }

    @PostMapping("/{quizId}/attempt")
    public QuizAttempt submitAttempt(@PathVariable Long quizId,
                                      @RequestBody Map<String, String> body,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String selectedAnswer = body.get("selectedAnswer");

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserId(userId);
        attempt.setQuizId(quizId);
        attempt.setSelectedAnswer(selectedAnswer);
        attempt.setCorrect(quiz.getCorrectAnswer().equalsIgnoreCase(selectedAnswer));

        return quizAttemptRepository.save(attempt);
    }
}
