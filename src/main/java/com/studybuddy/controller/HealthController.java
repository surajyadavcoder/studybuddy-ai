package com.studybuddy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public String home() {
        return "StudyBuddy AI backend is running. See /api/auth/signup, /api/auth/login, /api/chat and other endpoints.";
    }
}