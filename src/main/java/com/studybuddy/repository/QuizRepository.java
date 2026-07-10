package com.studybuddy.repository;

import com.studybuddy.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByDocumentId(Long documentId);
}
