package com.trackforge.bug.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bugs", indexes = {
        @Index(name = "idx_bugs_project", columnList = "project_id"),
        @Index(name = "idx_bugs_assignee", columnList = "assignee_id"),
        @Index(name = "idx_bugs_status", columnList = "status"),
        @Index(name = "idx_bugs_number", columnList = "bug_number")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bug {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity = Severity.MINOR;

    @Column(length = 30)
    private String bugNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private UUID reporterId;

    private UUID assigneeId;

    @OneToMany(mappedBy = "bug", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Comment> comments;

    @OneToMany(mappedBy = "bug", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Attachment> attachments;

    @OneToMany(mappedBy = "bug", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<ActivityLog> activityLogs;

    @Column(columnDefinition = "TEXT")
    private String stepsToReproduce;

    @Column(columnDefinition = "TEXT")
    private String expectedBehavior;

    @Column(columnDefinition = "TEXT")
    private String actualBehavior;

    @Column(length = 200)
    private String environment;

    // AI-suggested fields
    private String aiSuggestedPriority;
    private String aiSuggestedSeverity;
    private Float aiConfidenceScore;

    // pgvector embedding for duplicate detection
    @Column(columnDefinition = "vector(1536)")
    private String embedding;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime resolvedAt;

    public enum Status {
        OPEN, IN_PROGRESS, IN_REVIEW, RESOLVED, CLOSED, REOPENED
    }

    public enum Priority {
        CRITICAL, HIGH, MEDIUM, LOW
    }

    public enum Severity {
        BLOCKER, CRITICAL, MAJOR, MINOR, TRIVIAL
    }
}