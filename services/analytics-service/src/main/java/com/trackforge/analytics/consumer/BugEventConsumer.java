package com.trackforge.analytics.consumer;

import com.trackforge.analytics.entity.BugEventRecord;
import com.trackforge.analytics.repository.BugEventRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BugEventConsumer {

    private final BugEventRecordRepository repository;

    @KafkaListener(topics = "bug-events", groupId = "analytics-service")
    public void handleBugEvent(Map<String, Object> event) {
        String type = (String) event.get("type");
        try {
            switch (type) {
                case "BUG_CREATED" -> recordBugCreated(event);
                case "BUG_STATUS_CHANGED" -> recordStatusChanged(event);
                default -> log.debug("Ignoring bug-events type for analytics: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to record analytics event {}: {}", type, e.getMessage());
        }
    }

    @KafkaListener(topics = "comment-events", groupId = "analytics-service")
    public void handleCommentEvent(Map<String, Object> event) {
        try {
            BugEventRecord record = BugEventRecord.builder()
                    .bugId(uuid(event.get("bugId")))
                    .projectId(uuid(event.get("projectId")))
                    .eventType("COMMENT_ADDED")
                    .actorId(uuid(event.get("authorId")))
                    .occurredAt(dateTime(event.get("createdAt")))
                    .build();
            repository.save(record);
        } catch (Exception e) {
            log.error("Failed to record comment analytics event: {}", e.getMessage());
        }
    }

    private void recordBugCreated(Map<String, Object> event) {
        BugEventRecord record = BugEventRecord.builder()
                .bugId(uuid(event.get("bugId")))
                .projectId(uuid(event.get("projectId")))
                .projectKey((String) event.get("projectKey"))
                .eventType("BUG_CREATED")
                .priority((String) event.get("priority"))
                .severity((String) event.get("severity"))
                .actorId(uuid(event.get("reporterId")))
                .occurredAt(dateTime(event.get("createdAt")))
                .build();
        repository.save(record);
    }

    private void recordStatusChanged(Map<String, Object> event) {
        BugEventRecord record = BugEventRecord.builder()
                .bugId(uuid(event.get("bugId")))
                .projectId(uuid(event.get("projectId")))
                .projectKey((String) event.get("projectKey"))
                .eventType("BUG_STATUS_CHANGED")
                .oldStatus((String) event.get("oldStatus"))
                .newStatus((String) event.get("newStatus"))
                .actorId(uuid(event.get("actorId")))
                .occurredAt(dateTime(event.get("changedAt")))
                .build();
        repository.save(record);
    }

    private UUID uuid(Object value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private LocalDateTime dateTime(Object value) {
        if (value == null) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(value.toString());
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}