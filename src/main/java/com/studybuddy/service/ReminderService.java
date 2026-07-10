package com.studybuddy.service;

import com.studybuddy.entity.QuizAttempt;
import com.studybuddy.entity.User;
import com.studybuddy.repository.QuizAttemptRepository;
import com.studybuddy.repository.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReminderService {

    private final UserRepository userRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final JavaMailSender mailSender;

    public ReminderService(UserRepository userRepository,
                            QuizAttemptRepository quizAttemptRepository,
                            JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.mailSender = mailSender;
    }

    /**
     * Runs once a day at 9 AM server time. Looks at each user's recent quiz
     * attempts, finds topics with a low accuracy rate, and emails a reminder
     * to revise those specific topics.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyRevisionReminders() {
        List<User> users = userRepository.findAll();

        for (User user : users) {
            List<QuizAttempt> attempts = quizAttemptRepository.findByUserId(user.getId());

            if (attempts.isEmpty()) {
                continue;
            }

            List<String> weakTopics = findWeakTopics(attempts);

            if (!weakTopics.isEmpty()) {
                sendReminderEmail(user, weakTopics);
            }
        }
    }

    private List<String> findWeakTopics(List<QuizAttempt> attempts) {
        // Groups attempts and flags anything under 60 percent accuracy as weak.
        // Note: topic name lookup would normally join against the quiz table;
        // simplified here to keep the scheduler self contained.
        Map<Long, List<QuizAttempt>> byQuiz = attempts.stream()
                .collect(Collectors.groupingBy(QuizAttempt::getQuizId));

        return byQuiz.entrySet().stream()
                .filter(entry -> {
                    long correctCount = entry.getValue().stream().filter(QuizAttempt::isCorrect).count();
                    double accuracy = (double) correctCount / entry.getValue().size();
                    return accuracy < 0.6;
                })
                .map(entry -> "Quiz #" + entry.getKey())
                .collect(Collectors.toList());
    }

    private void sendReminderEmail(User user, List<String> weakTopics) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Time to revise: a few topics need another look");
        message.setText("Hi " + user.getName() + ",\n\n"
                + "Based on your recent quiz attempts, these topics could use another revision pass:\n\n"
                + String.join("\n", weakTopics)
                + "\n\nA quick 10 minute review now will save you time later.\n\n- StudyBuddy");
        mailSender.send(message);
    }
}
