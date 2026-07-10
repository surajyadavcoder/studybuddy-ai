package com.studybuddy.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class ChatDtos {

    public static class ChatRequest {
        @NotBlank
        private String question;

        // optional: restrict the question to specific documents, otherwise all of the user's ready documents are used
        private List<Long> documentIds;

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public List<Long> getDocumentIds() {
            return documentIds;
        }

        public void setDocumentIds(List<Long> documentIds) {
            this.documentIds = documentIds;
        }
    }

    public static class ChatResponse {
        private String answer;

        public ChatResponse(String answer) {
            this.answer = answer;
        }

        public String getAnswer() {
            return answer;
        }
    }
}
