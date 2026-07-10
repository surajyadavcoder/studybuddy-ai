package com.studybuddy.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 150;

    /**
     * Splits raw text into overlapping chunks. Overlap keeps context from being
     * cut off mid-sentence between two chunks, which helps retrieval quality.
     */
    public List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        String cleaned = text.replaceAll("\\s+", " ").trim();
        int start = 0;

        while (start < cleaned.length()) {
            int end = Math.min(start + CHUNK_SIZE, cleaned.length());

            // try to break at a sentence boundary near the end instead of mid-word
            if (end < cleaned.length()) {
                int lastPeriod = cleaned.lastIndexOf('.', end);
                if (lastPeriod > start + (CHUNK_SIZE / 2)) {
                    end = lastPeriod + 1;
                }
            }

            String chunk = cleaned.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (end == cleaned.length()) {
                break;
            }

            start = end - CHUNK_OVERLAP;
            if (start < 0) {
                start = end;
            }
        }

        return chunks;
    }
}
