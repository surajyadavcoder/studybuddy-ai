package com.studybuddy.service;

import com.studybuddy.entity.Quiz;
import com.studybuddy.repository.QuizRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuizGenerationService {

    private final JdbcTemplate jdbcTemplate;
    private final QuizRepository quizRepository;
    private final WebClient webClient;
    private final String apiKey;

    @Value("${app.groq.chat-model}")
    private String chatModel;

    public QuizGenerationService(JdbcTemplate jdbcTemplate, QuizRepository quizRepository,
                                 @Value("${app.groq.base-url}") String baseUrl,
                                 @Value("${app.groq.api-key}") String apiKey) {
        this.jdbcTemplate = jdbcTemplate;
        this.quizRepository = quizRepository;
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @SuppressWarnings("unchecked")
    public List<Quiz> generateQuizForDocument(Long documentId, int numberOfQuestions) {
        List<String> chunks = jdbcTemplate.queryForList(
                "SELECT chunk_text FROM document_chunks WHERE document_id = ? ORDER BY chunk_index LIMIT 8",
                String.class, documentId
        );

        if (chunks.isEmpty()) {
            throw new RuntimeException("No content found for document " + documentId + ", cannot generate quiz");
        }

        String combinedText = String.join("\n\n", chunks);

        String prompt = "Based on the following study material, create " + numberOfQuestions + " multiple "
                + "choice questions to test understanding. For each question give exactly 4 options and "
                + "clearly mark the correct one. Use this exact format for every question, with no extra "
                + "commentary before or after:\n\n"
                + "Q: <question text>\n"
                + "A) <option 1>\n"
                + "B) <option 2>\n"
                + "C) <option 3>\n"
                + "D) <option 4>\n"
                + "CORRECT: <letter>\n"
                + "TOPIC: <short topic name>\n\n"
                + "Study material:\n" + combinedText;

        Map<String, Object> requestBody = Map.of(
                "model", chatModel,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "stream", false
        );

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("choices")) {
            throw new RuntimeException("Groq chat API returned an unexpected response. "
                    + "Check that your Groq API key is set correctly.");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String rawText = (String) message.get("content");

        List<Quiz> quizzes = parseQuizzesFromText(rawText, documentId);
        return quizRepository.saveAll(quizzes);
    }

    private List<Quiz> parseQuizzesFromText(String rawText, Long documentId) {
        List<Quiz> quizzes = new ArrayList<>();

        Pattern pattern = Pattern.compile(
                "Q:\\s*(.+?)\\s*" +
                        "A\\)\\s*(.+?)\\s*" +
                        "B\\)\\s*(.+?)\\s*" +
                        "C\\)\\s*(.+?)\\s*" +
                        "D\\)\\s*(.+?)\\s*" +
                        "CORRECT:\\s*([A-D])\\s*" +
                        "TOPIC:\\s*(.+?)(?=Q:|$)",
                Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(rawText);

        while (matcher.find()) {
            Quiz quiz = new Quiz();
            quiz.setDocumentId(documentId);
            quiz.setQuestion(matcher.group(1).trim());
            String options = matcher.group(2).trim() + "|" + matcher.group(3).trim() + "|"
                    + matcher.group(4).trim() + "|" + matcher.group(5).trim();
            quiz.setOptions(options);
            quiz.setCorrectAnswer(matcher.group(6).trim());
            quiz.setTopic(matcher.group(7).trim());
            quizzes.add(quiz);
        }

        return quizzes;
    }
}