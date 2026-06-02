package com.codereview.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class UserDto {

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Name is required")
        private String name;

        @Email(message = "Valid email is required")
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;
    }

    @Data
    public static class LoginRequest {
        @Email(message = "Valid email is required")
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String name;
        private String email;
        private Long userId;

        public AuthResponse(String token, String name, String email, Long userId) {
            this.token = token;
            this.name = name;
            this.email = email;
            this.userId = userId;
        }
    }

    @Data
    public static class UserProfileResponse {
        private Long id;
        private String name;
        private String email;
        private Integer totalReviews;
        private String createdAt;
    }
}
