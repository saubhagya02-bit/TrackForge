package com.trackforge.bug.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

public class ProjectDto {

    // Create Project

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {

        @NotBlank(message = "Project name is required")
        @Size(
                max = 100,
                message = "Project name must not exceed 100 characters"
        )
        private String name;

        @Size(
                max = 1000,
                message = "Project description must not exceed 1000 characters"
        )
        private String description;

        @NotBlank(message = "Project key is required")
        @Size(
                min = 2,
                max = 20,
                message = "Project key must be between 2 and 20 characters"
        )
        private String key;
    }

    // Update Project

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {

        @Size(
                max = 100,
                message = "Project name must not exceed 100 characters"
        )
        private String name;

        @Size(
                max = 1000,
                message = "Project description must not exceed 1000 characters"
        )
        private String description;

        private String status;
    }

    // Project Response

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {

        private UUID id;

        private String name;

        private String description;

        private String key;

        private String status;

        private BugDto.UserRef owner;

        private long totalBugs;

        private long openBugs;

        private long inProgressBugs;

        private long resolvedBugs;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;
    }

    // Project Summary

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {

        private UUID id;

        private String name;

        private String key;
    }
}