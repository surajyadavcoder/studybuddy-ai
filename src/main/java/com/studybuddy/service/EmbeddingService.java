package com.studybuddy.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private final WebClient webClient;
    private final String apiKey;

    @Value("${app.gemini.embedding-model}")
    private String embeddingModel;

    public EmbeddingService(@Value("${app.gemini.base-url}") String baseUrl,
                            @Value("${app.gemini.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Converts a piece of text into a vector embedding using Google Gemini's
     * embedding API (free tier). Output is fixed at 768 dimensions to match
     * the existing pgvector schema.
     */
    @SuppressWarnings("unchecked")
    public float[] embed(String text) {
        Map<String, Object> requestBody = Map.of(
                "content", Map.of("parts", List.of(Map.of("text", text))),
                "outputDimensionality", 768
        );

        Map<String, Object> response = webClient.post()
                .uri("/v1beta/models/" + embeddingModel + ":embedContent?key=" + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("embedding")) {
            throw new RuntimeException("Gemini embeddings API returned an unexpected response. "
                    + "Check that your Gemini API key is set correctly.");
        }

        Map<String, Object> embeddingObj = (Map<String, Object>) response.get("embedding");
        List<Double> embeddingList = (List<Double>) embeddingObj.get("values");

        float[] embedding = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            embedding[i] = embeddingList.get(i).floatValue();
        }
        return embedding;
    }

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