package com.trackforge.bug.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

public class BugEvents {

    @Data @Builder
    public static class BugCreatedEvent {
        private String type = "BUG_CREATED";
        private UUID bugId;
        private String bugNumber;
        private String title;
        private UUID projectId;
        private String projectKey;
        private UUID reporterId;
        private String reporterUsername;
        private UUID assigneeId;
        private String assigneeUsername;
        private String priority;
        private String severity;
        private LocalDateTime createdAt;
    }

    @Data @Builder
    public static class BugUpdatedEvent {
        private String type = "BUG_UPDATED";
        private UUID bugId;
        private String bugNumber;
        private String title;
        private UUID projectId;
        private UUID actorId;
        private String actorUsername;
        private String fieldChanged;
        private String oldValue;
        private String newValue;
        private LocalDateTime updatedAt;
    }

    @Data @Builder
    public static class BugStatusChangedEvent {
        private String type = "BUG_STATUS_CHANGED";
        private UUID bugId;
        private String bugNumber;
        private String title;
        private UUID projectId;
        private String projectKey;
        private UUID assigneeId;
        private UUID actorId;
        private String actorUsername;
        private String oldStatus;
        private String newStatus;
        private LocalDateTime changedAt;
    }

    @Data @Builder
    public static class CommentAddedEvent {
        private String type = "COMMENT_ADDED";
        private UUID bugId;
        private String bugNumber;
        private String bugTitle;
        private UUID projectId;
        private UUID commentId;
        private UUID authorId;
        private String authorUsername;
        private String content;
        private UUID bugAssigneeId;
        private UUID bugReporterId;
        private LocalDateTime createdAt;
    }
}
