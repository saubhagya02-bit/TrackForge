package com.trackforge.bug.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

public class CommentDto {

    @Data
    public static class CreateRequest {
        @NotBlank private String content;
    }

    @Data @Builder
    public static class Response {
        private UUID id;
        private String content;
        private BugDto.UserRef author;
        private boolean edited;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}