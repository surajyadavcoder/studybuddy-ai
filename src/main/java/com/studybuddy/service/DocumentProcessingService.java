package com.studybuddy.service;

import com.studybuddy.entity.Document;
import com.studybuddy.repository.DocumentRepository;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final Tika tika = new Tika();

    public DocumentProcessingService(DocumentRepository documentRepository,
                                      ChunkingService chunkingService,
                                      EmbeddingService embeddingService,
                                      VectorStoreService vectorStoreService) {
        this.documentRepository = documentRepository;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * Handles the full pipeline for a newly uploaded document: extract text,
     * split into chunks, get embeddings, store in the vector table.
     * Runs asynchronously so the upload API can return immediately while
     * processing continues in the background.
     */
    @org.springframework.scheduling.annotation.Async
    public CompletableFuture<Void> processDocument(Long documentId, MultipartFile file) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        try {
            String extractedText = tika.parseToString(file.getInputStream());
            List<String> chunks = chunkingService.splitIntoChunks(extractedText);

            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                float[] embedding = embeddingService.embed(chunk);
                vectorStoreService.saveChunk(documentId, i, chunk, embedding);
            }

            document.setStatus("READY");
            document.setChunkCount(chunks.size());
            documentRepository.save(document);

        } catch (IOException | org.apache.tika.exception.TikaException e) {
            document.setStatus("FAILED");
            documentRepository.save(document);
            throw new RuntimeException("Failed to process document " + documentId, e);
        }

        return CompletableFuture.completedFuture(null);
    }
}
