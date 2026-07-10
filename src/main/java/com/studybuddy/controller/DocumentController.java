package com.studybuddy.controller;

import com.studybuddy.entity.Document;
import com.studybuddy.repository.DocumentRepository;
import com.studybuddy.service.DocumentProcessingService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final DocumentProcessingService documentProcessingService;

    public DocumentController(DocumentRepository documentRepository,
                               DocumentProcessingService documentProcessingService) {
        this.documentRepository = documentRepository;
        this.documentProcessingService = documentProcessingService;
    }

    @PostMapping("/upload")
    public Document upload(@RequestParam("file") MultipartFile file, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        Document document = new Document(userId, file.getOriginalFilename());
        document = documentRepository.save(document);

        documentProcessingService.processDocument(document.getId(), file);

        return document;
    }

    @GetMapping
    public List<Document> listMyDocuments(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return documentRepository.findByUserId(userId);
    }

    @GetMapping("/{id}")
    public Document getDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }
}
