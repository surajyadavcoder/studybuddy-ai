package com.studybuddy.controller;

import com.studybuddy.dto.ChatDtos.*;
import com.studybuddy.entity.Document;
import com.studybuddy.repository.DocumentRepository;
import com.studybuddy.service.RagService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RagService ragService;
    private final DocumentRepository documentRepository;

    public ChatController(RagService ragService, DocumentRepository documentRepository) {
        this.ragService = ragService;
        this.documentRepository = documentRepository;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        List<Long> documentIds = request.getDocumentIds();

        if (documentIds == null || documentIds.isEmpty()) {
            documentIds = documentRepository.findByUserId(userId).stream()
                    .filter(doc -> "READY".equals(doc.getStatus()))
                    .map(Document::getId)
                    .toList();
        }

        String answer = ragService.answerQuestion(request.getQuestion(), documentIds);
        return new ChatResponse(answer);
    }
}
