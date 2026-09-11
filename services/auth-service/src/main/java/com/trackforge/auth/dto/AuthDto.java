package com.trackforge.auth.dto;

import com.trackforge.auth.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

public class AuthDto {

    @Data
    public static class RegisterRequest {
        @NotBlank @Size(min = 3, max = 50)
        private String username;
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 6, max = 100)
        private String password;
        private String fullName;
    }

    @Data
    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
    }

    @Data
    public static class RefreshRequest {
        @NotBlank private String refreshToken;
    }

    @Data @Builder
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private final String tokenType = "Bearer";
        private long expiresIn;
        private UserSummary user;
    }

    @Data @Builder
    public static class UserSummary {
        private UUID id;
        private String username;
        private String email;
        private String fullName;
        private String avatarUrl;
        private User.Role role;
        private LocalDateTime createdAt;
    }

    @Data
    public static class UpdateProfileRequest {
        private String fullName;
        private String avatarUrl;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank private String currentPassword;
        @NotBlank @Size(min = 6) private String newPassword;
    }
}