package com.studybuddy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public static class SignupRequest {
        @NotBlank
        private String name;

        @Email
        @NotBlank
        private String email;

        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class LoginRequest {
        @Email
        @NotBlank
        private String email;

        @NotBlank
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class AuthResponse {
        private String token;
        private String name;
        private String email;

        public AuthResponse(String token, String name, String email) {
            this.token = token;
            this.name = name;
            this.email = email;
        }

        public String getToken() {
            return token;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
    }
}
