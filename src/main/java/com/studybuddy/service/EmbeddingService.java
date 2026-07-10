package com.studybuddy.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private final WebClient webClient;

    @Value("${app.ollama.embedding-model}")
    private String embeddingModel;

    public EmbeddingService(@Value("${app.ollama.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Converts a piece of text into a vector embedding using a local Ollama model.
     * nomic-embed-text produces 768-dimension vectors, free and runs fully offline.
     */
    @SuppressWarnings("unchecked")
    public float[] embed(String text) {
        Map<String, Object> requestBody = Map.of(
                "model", embeddingModel,
                "prompt", text
        );

        Map<String, Object> response = webClient.post()
                .uri("/api/embeddings")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("embedding")) {
            throw new RuntimeException("Ollama embeddings API returned an unexpected response. "
                    + "Make sure Ollama is running and the model has been pulled.");
        }

        List<Double> embeddingList = (List<Double>) response.get("embedding");

        float[] embedding = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            embedding[i] = embeddingList.get(i).floatValue();
        }
        return embedding;
    }

    /**
     * Formats a float array into the string representation pgvector expects,
     * e.g. "[0.001,0.234,...]"
     */
    public String toPgVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
