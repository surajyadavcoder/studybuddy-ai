package com.studybuddy.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class VectorStoreService {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;

    public VectorStoreService(JdbcTemplate jdbcTemplate, EmbeddingService embeddingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
    }

    /**
     * Stores a single chunk of text along with its embedding vector.
     */
    public void saveChunk(Long documentId, int chunkIndex, String chunkText, float[] embedding) {
        String vectorLiteral = embeddingService.toPgVectorLiteral(embedding);
        String sql = "INSERT INTO document_chunks (document_id, chunk_index, chunk_text, embedding) " +
                "VALUES (?, ?, ?, ?::vector)";
        jdbcTemplate.update(sql, documentId, chunkIndex, chunkText, vectorLiteral);
    }

    /**
     * Finds the most relevant chunks for a given query embedding, restricted to
     * documents belonging to a specific set of document ids (usually all docs for a user,
     * or a single document if the user is asking about one specific file).
     */
    public List<String> findSimilarChunks(float[] queryEmbedding, List<Long> documentIds, int topK) {
        if (documentIds.isEmpty()) {
            return new ArrayList<>();
        }

        String vectorLiteral = embeddingService.toPgVectorLiteral(queryEmbedding);

        String inClause = String.join(",", documentIds.stream().map(String::valueOf).toArray(String[]::new));

        String sql = "SELECT chunk_text FROM document_chunks " +
                "WHERE document_id IN (" + inClause + ") " +
                "ORDER BY embedding <=> ?::vector " +
                "LIMIT ?";

        return jdbcTemplate.queryForList(sql, String.class, vectorLiteral, topK);
    }

    public void deleteChunksForDocument(Long documentId) {
        jdbcTemplate.update("DELETE FROM document_chunks WHERE document_id = ?", documentId);
    }
}
