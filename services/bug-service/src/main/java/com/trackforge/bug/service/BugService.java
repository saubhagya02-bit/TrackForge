package com.trackforge.bug.service;

import com.trackforge.bug.dto.BugDto;
import com.trackforge.bug.dto.ProjectDto;
import com.trackforge.bug.entity.*;
import com.trackforge.bug.event.BugEvents;
import com.trackforge.bug.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BugService {

    private final BugRepository bugRepository;
    private final ProjectRepository projectRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final ActivityLogRepository activityLogRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final StorageService storageService;
    private final AiService aiService;
    private final EntityManager entityManager;

    // Bugs

    @Cacheable(value = "bugs", key = "#projectId + '-' + #page + '-' + #size + '-' + #status + '-' + #priority")
    public Page<BugDto.Summary> getBugsByProject(UUID projectId, String status, String priority,
                                                 UUID assigneeId, int page, int size, String sortBy) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + projectId));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        Bug.Status bugStatus = status != null ? Bug.Status.valueOf(status) : null;
        Bug.Priority bugPriority = priority != null ? Bug.Priority.valueOf(priority) : null;

        return bugRepository.findByProjectIdWithFilters(projectId, bugStatus, bugPriority, assigneeId, pageable)
                .map(this::toBugSummary);
    }

    public BugDto.Response getBugById(UUID id) {
        return toBugResponse(findBugOrThrow(id));
    }

    @Transactional
    @CacheEvict(value = "bugs", allEntries = true)
    public BugDto.Response createBug(UUID projectId, BugDto.CreateRequest req, UUID reporterId, String reporterUsername) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + projectId));

        // AI triage suggestion
        Map<String, Object> aiSuggestion = aiService.suggestTriageFields(req.getTitle(), req.getDescription());

        Bug bug = Bug.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .priority(Bug.Priority.valueOf(req.getPriority() != null ? req.getPriority() : "MEDIUM"))
                .severity(Bug.Severity.valueOf(req.getSeverity() != null ? req.getSeverity() : "MINOR"))
                .status(Bug.Status.OPEN)
                .project(project)
                .reporterId(reporterId)
                .assigneeId(req.getAssigneeId())
                .stepsToReproduce(req.getStepsToReproduce())
                .expectedBehavior(req.getExpectedBehavior())
                .actualBehavior(req.getActualBehavior())
                .environment(req.getEnvironment())
                .aiSuggestedPriority((String) aiSuggestion.get("priority"))
                .aiSuggestedSeverity((String) aiSuggestion.get("severity"))
                .aiConfidenceScore((Float) aiSuggestion.get("confidence"))
                .build();

        projectRepository.incrementBugCounter(projectId);
        entityManager.refresh(project);
        bug.setBugNumber(project.getKey() + "-" + project.getBugCounter());

        Bug saved = bugRepository.save(bug);

        // Log activity
        logActivity(saved, reporterId, reporterUsername,
                ActivityLog.ActivityType.BUG_CREATED, null, null, null,
                "Bug created: " + saved.getTitle());

        // Publish Kafka event
        BugEvents.BugCreatedEvent event = BugEvents.BugCreatedEvent.builder()
                .bugId(saved.getId())
                .bugNumber(saved.getBugNumber())
                .title(saved.getTitle())
                .projectId(projectId)
                .projectKey(project.getKey())
                .reporterId(reporterId)
                .reporterUsername(reporterUsername)
                .assigneeId(saved.getAssigneeId())
                .priority(saved.getPriority().name())
                .severity(saved.getSeverity().name())
                .createdAt(saved.getCreatedAt())
                .build();
        kafkaTemplate.send("bug-events", saved.getId().toString(), event);

        // WebSocket broadcast to project subscribers
        messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/bugs", toBugSummary(saved));

        // Async AI embedding for duplicate detection
        aiService.processBugEmbedding(saved.getId());

        return toBugResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "bugs", allEntries = true)
    public BugDto.Response updateBug(UUID id, BugDto.UpdateRequest req, UUID actorId, String actorUsername) {
        Bug bug = findBugOrThrow(id);

        if (req.getTitle() != null) bug.setTitle(req.getTitle());
        if (req.getDescription() != null) bug.setDescription(req.getDescription());
        if (req.getStepsToReproduce() != null) bug.setStepsToReproduce(req.getStepsToReproduce());
        if (req.getExpectedBehavior() != null) bug.setExpectedBehavior(req.getExpectedBehavior());
        if (req.getActualBehavior() != null) bug.setActualBehavior(req.getActualBehavior());
        if (req.getEnvironment() != null) bug.setEnvironment(req.getEnvironment());

        if (req.getStatus() != null) {
            String oldStatus = bug.getStatus().name();
            Bug.Status newStatus = Bug.Status.valueOf(req.getStatus());
            bug.setStatus(newStatus);
            if (newStatus == Bug.Status.RESOLVED || newStatus == Bug.Status.CLOSED) {
                bug.setResolvedAt(LocalDateTime.now());
            }
            logActivity(bug, actorId, actorUsername, ActivityLog.ActivityType.STATUS_CHANGED,
                    "status", oldStatus, newStatus.name(), null);

            // Publish status change event
            BugEvents.BugStatusChangedEvent event = BugEvents.BugStatusChangedEvent.builder()
                    .bugId(bug.getId()).bugNumber(bug.getBugNumber()).title(bug.getTitle())
                    .projectId(bug.getProject().getId()).projectKey(bug.getProject().getKey())
                    .assigneeId(bug.getAssigneeId()).actorId(actorId).actorUsername(actorUsername)
                    .oldStatus(oldStatus).newStatus(newStatus.name())
                    .changedAt(LocalDateTime.now()).build();
            kafkaTemplate.send("bug-events", bug.getId().toString(), event);
        }

        if (req.getPriority() != null) {
            String old = bug.getPriority().name();
            bug.setPriority(Bug.Priority.valueOf(req.getPriority()));
            logActivity(bug, actorId, actorUsername, ActivityLog.ActivityType.PRIORITY_CHANGED,
                    "priority", old, req.getPriority(), null);
        }
        if (req.getSeverity() != null) {
            String old = bug.getSeverity().name();
            bug.setSeverity(Bug.Severity.valueOf(req.getSeverity()));
            logActivity(bug, actorId, actorUsername, ActivityLog.ActivityType.SEVERITY_CHANGED,
                    "severity", old, req.getSeverity(), null);
        }

        // FIX: the old code only ever handled "assigneeId != null", so there was no
        // way to unassign a bug once it had an assignee — the frontend tried to send
        // an empty string, which failed UUID deserialization before it even got here.
        // clearAssignee is a new explicit flag on BugDto.UpdateRequest (defaults to
        // false) so "assign to X" and "clear assignee" are unambiguous, distinct
        // requests instead of both collapsing to a null/empty value.
        if (req.isClearAssignee()) {
            String old = bug.getAssigneeId() != null ? bug.getAssigneeId().toString() : "unassigned";
            bug.setAssigneeId(null);
            logActivity(bug, actorId, actorUsername, ActivityLog.ActivityType.ASSIGNEE_CHANGED,
                    "assignee", old, "unassigned", null);
        } else if (req.getAssigneeId() != null) {
            String old = bug.getAssigneeId() != null ? bug.getAssigneeId().toString() : "unassigned";
            bug.setAssigneeId(req.getAssigneeId());
            logActivity(bug, actorId, actorUsername, ActivityLog.ActivityType.ASSIGNEE_CHANGED,
                    "assignee", old, req.getAssigneeId().toString(), null);
        }

        Bug updated = bugRepository.save(bug);

        // WebSocket broadcast
        messagingTemplate.convertAndSend(
                "/topic/bugs/" + id, toBugSummary(updated));

        return toBugResponse(updated);
    }

    @Transactional
    @CacheEvict(value = "bugs", allEntries = true)
    public void deleteBug(UUID id) {
        Bug bug = findBugOrThrow(id);
        // Delete attachments from storage
        bug.getAttachments().forEach(a -> storageService.delete(a.getStoredFilename()));
        bugRepository.delete(bug);
    }

    // Attachments

    @Transactional
    public BugDto.AttachmentDto uploadAttachment(UUID bugId, MultipartFile file, UUID uploaderId) {
        Bug bug = findBugOrThrow(bugId);
        String stored = storageService.upload(file, "bugs/" + bugId);

        Attachment attachment = Attachment.builder()
                .bug(bug)
                .originalFilename(file.getOriginalFilename())
                .storedFilename(stored)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedBy(uploaderId)
                .build();
        attachment = attachmentRepository.save(attachment);

        String downloadUrl = storageService.generatePresignedUrl(stored);
        return toAttachmentDto(attachment, downloadUrl);
    }

    @Transactional
    public void deleteAttachment(UUID attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NoSuchElementException("Attachment not found"));
        storageService.delete(attachment.getStoredFilename());
        attachmentRepository.delete(attachment);
    }

    // Stats

    @Cacheable(value = "bugStats", key = "#projectId")
    public BugDto.StatsResponse getStats(UUID projectId) {
        Map<String, Long> byStatus = new HashMap<>();
        bugRepository.countGroupByStatus(projectId)
                .forEach(row -> byStatus.put(row[0].toString(), (Long) row[1]));

        Map<String, Long> byPriority = new HashMap<>();
        bugRepository.countGroupByPriority(projectId)
                .forEach(row -> byPriority.put(row[0].toString(), (Long) row[1]));

        long total = bugRepository.countByProjectId(projectId);

        return BugDto.StatsResponse.builder()
                .total(total)
                .byStatus(byStatus)
                .byPriority(byPriority)
                .resolvedThisWeek(0L)
                .avgResolutionHours(0.0)
                .build();
    }

    // Activity log
    public List<BugDto.ActivityDto> getActivityLog(UUID bugId) {
        return activityLogRepository.findByBugIdOrderByCreatedAtDesc(bugId)
                .stream().map(this::toActivityDto).collect(Collectors.toList());
    }

    // Search

    public List<BugDto.Summary> search(UUID projectId, String query) {
        return bugRepository.fullTextSearch(projectId, query)
                .stream().map(this::toBugSummary).collect(Collectors.toList());
    }

    // Duplicate check

    public BugDto.DuplicateCheckResponse checkDuplicates(UUID bugId) {
        Bug bug = findBugOrThrow(bugId);
        return aiService.findDuplicates(bugId, bug.getProject().getId());
    }

    // Helpers

    private Bug findBugOrThrow(UUID id) {
        return bugRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Bug not found: " + id));
    }

    private void logActivity(Bug bug, UUID actorId, String actorUsername,
                             ActivityLog.ActivityType type, String field,
                             String oldVal, String newVal, String description) {
        ActivityLog log = ActivityLog.builder()
                .bug(bug).actorId(actorId).actorUsername(actorUsername)
                .type(type).fieldChanged(field).oldValue(oldVal).newValue(newVal)
                .description(description).build();
        activityLogRepository.save(log);
    }

    private BugDto.Summary toBugSummary(Bug b) {
        return BugDto.Summary.builder()
                .id(b.getId()).bugNumber(b.getBugNumber()).title(b.getTitle())
                .status(b.getStatus().name()).priority(b.getPriority().name())
                .severity(b.getSeverity().name())
                .assignee(b.getAssigneeId() != null
                        ? BugDto.UserRef.builder().id(b.getAssigneeId()).build() : null)
                .createdAt(b.getCreatedAt()).build();
    }

    private BugDto.Response toBugResponse(Bug b) {
        List<BugDto.AttachmentDto> attachments = (b.getAttachments() != null)
                ? b.getAttachments().stream()
                .map(a -> toAttachmentDto(a, storageService.generatePresignedUrl(a.getStoredFilename())))
                .collect(Collectors.toList())
                : List.of();

        return BugDto.Response.builder()
                .id(b.getId()).bugNumber(b.getBugNumber()).title(b.getTitle())
                .description(b.getDescription()).status(b.getStatus().name())
                .priority(b.getPriority().name()).severity(b.getSeverity().name())
                .project(b.getProject() != null ? ProjectDto.Summary.builder()
                        .id(b.getProject().getId()).name(b.getProject().getName())
                        .key(b.getProject().getKey()).build() : null)
                .reporter(BugDto.UserRef.builder().id(b.getReporterId()).build())
                .assignee(b.getAssigneeId() != null
                        ? BugDto.UserRef.builder().id(b.getAssigneeId()).build() : null)
                .stepsToReproduce(b.getStepsToReproduce())
                .expectedBehavior(b.getExpectedBehavior())
                .actualBehavior(b.getActualBehavior())
                .environment(b.getEnvironment())
                .commentCount(b.getComments() != null ? b.getComments().size() : 0)
                .attachments(attachments)
                .aiSuggestedPriority(b.getAiSuggestedPriority())
                .aiSuggestedSeverity(b.getAiSuggestedSeverity())
                .aiConfidenceScore(b.getAiConfidenceScore())
                .createdAt(b.getCreatedAt()).updatedAt(b.getUpdatedAt())
                .resolvedAt(b.getResolvedAt()).build();
    }

    private BugDto.AttachmentDto toAttachmentDto(Attachment a, String url) {
        return BugDto.AttachmentDto.builder()
                .id(a.getId()).originalFilename(a.getOriginalFilename())
                .contentType(a.getContentType()).fileSize(a.getFileSize())
                .downloadUrl(url).createdAt(a.getCreatedAt()).build();
    }

    private BugDto.ActivityDto toActivityDto(ActivityLog l) {
        return BugDto.ActivityDto.builder()
                .id(l.getId()).actorUsername(l.getActorUsername()).type(l.getType())
                .fieldChanged(l.getFieldChanged()).oldValue(l.getOldValue())
                .newValue(l.getNewValue()).description(l.getDescription())
                .createdAt(l.getCreatedAt()).build();
    }
}