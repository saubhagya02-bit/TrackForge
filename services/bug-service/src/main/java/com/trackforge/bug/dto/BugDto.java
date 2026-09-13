package com.trackforge.bug.dto;

import com.trackforge.bug.entity.ActivityLog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class BugDto {

    @Data
    public static class CreateRequest {
        @NotBlank @Size(max = 200) private String title;
        private String description;
        private String priority = "MEDIUM";
        private String severity = "MINOR";
        private UUID assigneeId;
        private String stepsToReproduce;
        private String expectedBehavior;
        private String actualBehavior;
        private String environment;
    }

    @Data
    public static class UpdateRequest {
        private String title;
        private String description;
        private String status;
        private String priority;
        private String severity;
        private UUID assigneeId;
        private String stepsToReproduce;
        private String expectedBehavior;
        private String actualBehavior;
        private String environment;
    }

    @Data @Builder
    public static class Response {
        private UUID id;
        private String bugNumber;
        private String title;
        private String description;
        private String status;
        private String priority;
        private String severity;
        private ProjectDto.Summary project;
        private UserRef reporter;
        private UserRef assignee;
        private String stepsToReproduce;
        private String expectedBehavior;
        private String actualBehavior;
        private String environment;
        private long commentCount;
        private List<AttachmentDto> attachments;
        private String aiSuggestedPriority;
        private String aiSuggestedSeverity;
        private Float aiConfidenceScore;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime resolvedAt;
    }

    @Data @Builder
    public static class Summary {
        private UUID id;
        private String bugNumber;
        private String title;
        private String status;
        private String priority;
        private String severity;
        private UserRef assignee;
        private LocalDateTime createdAt;
    }

    @Data @Builder
    public static class UserRef {
        private UUID id;
        private String username;
        private String fullName;
        private String avatarUrl;
    }

    @Data @Builder
    public static class AttachmentDto {
        private UUID id;
        private String originalFilename;
        private String contentType;
        private long fileSize;
        private String downloadUrl;
        private LocalDateTime createdAt;
    }

    @Data @Builder
    public static class ActivityDto {
        private UUID id;
        private String actorUsername;
        private ActivityLog.ActivityType type;
        private String fieldChanged;
        private String oldValue;
        private String newValue;
        private String description;
        private LocalDateTime createdAt;
    }

    @Data @Builder
    public static class StatsResponse {
        private long total;
        private java.util.Map<String, Long> byStatus;
        private java.util.Map<String, Long> byPriority;
        private long resolvedThisWeek;
        private double avgResolutionHours;
    }

    @Data @Builder
    public static class DuplicateCheckResponse {
        private boolean hasDuplicates;
        private List<Summary> similarBugs;
    }
}