package com.trackforge.notification.consumer;

import com.trackforge.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BugEventConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "bug-events", groupId = "notification-service")
    public void handleBugEvent(Map<String, Object> event) {
        String type = (String) event.get("type");
        log.info("Received bug event: {}", type);

        try {
            switch (type) {
                case "BUG_CREATED"        -> handleBugCreated(event);
                case "BUG_STATUS_CHANGED" -> handleStatusChanged(event);
                case "COMMENT_ADDED"      -> handleCommentAdded(event);
                default -> log.debug("Unhandled event type: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to process event {}: {}", type, e.getMessage());
        }
    }

    @KafkaListener(topics = "user-events", groupId = "notification-service")
    public void handleUserEvent(Map<String, Object> event) {
        String type = (String) event.get("type");
        if ("USER_REGISTERED".equals(type)) {
            String email = (String) event.get("email");
            String username = (String) event.get("username");
            emailService.sendWelcomeEmail(email, username);
        }
    }

    private void handleBugCreated(Map<String, Object> event) {
        String assigneeId = (String) event.get("assigneeId");
        if (assigneeId == null) return;
        log.info("BUG_CREATED: {} assigned to {}", event.get("bugNumber"), assigneeId);
    }

    private void handleStatusChanged(Map<String, Object> event) {
        log.info("STATUS_CHANGED: {} → {} → {}",
                event.get("bugNumber"), event.get("oldStatus"), event.get("newStatus"));
    }

    private void handleCommentAdded(Map<String, Object> event) {
        log.info("COMMENT_ADDED: on {} by {}", event.get("bugNumber"), event.get("authorUsername"));
    }
}