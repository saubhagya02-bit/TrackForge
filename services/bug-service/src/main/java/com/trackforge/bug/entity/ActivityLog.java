package com.trackforge.bug.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "activity_logs", indexes = {
        @Index(name = "idx_activity_bug", columnList = "bug_id"),
        @Index(name = "idx_activity_created", columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bug_id", nullable = false)
    private Bug bug;

    @Column(nullable = false)
    private UUID actorId;

    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type;

    private String fieldChanged;
    private String oldValue;
    private String newValue;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum ActivityType {
        BUG_CREATED, STATUS_CHANGED, PRIORITY_CHANGED, SEVERITY_CHANGED,
        ASSIGNEE_CHANGED, ATTACHMENT_DELETED, BUG_UPDATED
    }
}
