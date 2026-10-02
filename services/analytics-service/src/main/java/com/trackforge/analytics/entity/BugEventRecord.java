package com.trackforge.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bug_event_records", indexes = {
        @Index(name = "idx_bug_event_project", columnList = "project_id"),
        @Index(name = "idx_bug_event_bug", columnList = "bug_id"),
        @Index(name = "idx_bug_event_type", columnList = "event_type"),
        @Index(name = "idx_bug_event_occurred", columnList = "occurred_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BugEventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID bugId;

    @Column(nullable = false)
    private UUID projectId;

    private String projectKey;

    @Column(nullable = false, length = 40)
    private String eventType;

    private String priority;
    private String severity;
    private String oldStatus;
    private String newStatus;
    private UUID actorId;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @CreationTimestamp
    private LocalDateTime recordedAt;
}