package com.trackforge.bug.service;

import com.trackforge.bug.dto.BugDto;
import com.trackforge.bug.dto.CommentDto;
import com.trackforge.bug.entity.Bug;
import com.trackforge.bug.entity.Comment;
import com.trackforge.bug.event.BugEvents;
import com.trackforge.bug.repository.BugRepository;
import com.trackforge.bug.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final BugRepository bugRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    public List<CommentDto.Response> getAllForBug(UUID bugId) {
        return commentRepository.findByBugIdOrderByCreatedAtAsc(bugId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public CommentDto.Response add(UUID bugId, CommentDto.CreateRequest req, UUID authorId, String authorUsername) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new NoSuchElementException("Bug not found: " + bugId));

        Comment comment = Comment.builder()
                .bug(bug)
                .content(req.getContent())
                .authorId(authorId)
                .authorUsername(authorUsername)
                .edited(false)
                .build();
        Comment saved = commentRepository.save(comment);

        // WebSocket broadcast so anyone viewing the bug sees the new comment live
        messagingTemplate.convertAndSend("/topic/bugs/" + bugId + "/comments", toResponse(saved));

        BugEvents.CommentAddedEvent event = BugEvents.CommentAddedEvent.builder()
                .bugId(bugId)
                .bugNumber(bug.getBugNumber())
                .bugTitle(bug.getTitle())
                .projectId(bug.getProject().getId())
                .commentId(saved.getId())
                .authorId(authorId)
                .authorUsername(authorUsername)
                .content(req.getContent())
                .bugAssigneeId(bug.getAssigneeId())
                .bugReporterId(bug.getReporterId())
                .createdAt(LocalDateTime.now())
                .build();
        kafkaTemplate.send("comment-events", bugId.toString(), event);

        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID commentId, UUID requesterId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("Comment not found: " + commentId));

        if (!comment.getAuthorId().equals(requesterId)) {
            throw new AccessDeniedException("You can only delete your own comments");
        }

        commentRepository.delete(comment);
    }

    private CommentDto.Response toResponse(Comment c) {
        return CommentDto.Response.builder()
                .id(c.getId())
                .content(c.getContent())
                .author(BugDto.UserRef.builder()
                        .id(c.getAuthorId())
                        .username(c.getAuthorUsername())
                        .fullName(c.getAuthorFullname())
                        .build())
                .edited(c.isEdited())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}