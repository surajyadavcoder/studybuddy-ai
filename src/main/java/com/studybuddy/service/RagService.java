package com.studybuddy.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class RagService {

    private final VectorStoreService vectorStoreService;
    private final EmbeddingService embeddingService;
    private final WebClient webClient;
    private final String apiKey;

    @Value("${app.groq.chat-model}")
    private String chatModel;

    public RagService(VectorStoreService vectorStoreService,
                      EmbeddingService embeddingService,
                      @Value("${app.groq.base-url}") String baseUrl,
                      @Value("${app.groq.api-key}") String apiKey) {
        this.vectorStoreService = vectorStoreService;
        this.embeddingService = embeddingService;
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public String answerQuestion(String question, List<Long> documentIds) {
        float[] queryEmbedding = embeddingService.embed(question);
        List<String> relevantChunks = vectorStoreService.findSimilarChunks(queryEmbedding, documentIds, 5);

        if (relevantChunks.isEmpty()) {
            return "I could not find anything relevant in your uploaded documents to answer that. "
                    + "Try uploading the material this question relates to.";
        }

        String context = String.join("\n\n---\n\n", relevantChunks);

        String systemPrompt = "You are a study assistant. Answer the student's question using only the "
                + "context provided below from their own notes and materials. If the answer is not "
                + "contained in the context, say so honestly instead of making something up. "
                + "Keep the explanation simple and clear.\n\nContext:\n" + context;

        return callChatModel(systemPrompt, question);
    }

    @SuppressWarnings("unchecked")
    private String callChatModel(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "model", chatModel,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
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
        return (String) message.get("content");
    }
}