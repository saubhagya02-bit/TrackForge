package com.trackforge.bug.controller;

import com.trackforge.bug.dto.BugDto;
import com.trackforge.bug.service.BugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/bugs")
@RequiredArgsConstructor
@Tag(name = "Bugs", description = "Bug management endpoints")
public class BugController {

    private final BugService bugService;

    @GetMapping
    @Operation(summary = "List bugs with filters and pagination")
    public ResponseEntity<Page<BugDto.Summary>> getBugs(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        return ResponseEntity.ok(bugService.getBugsByProject(projectId, status, priority, assigneeId, page, size, sortBy));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get bug by ID")
    public ResponseEntity<BugDto.Response> getBug(@PathVariable UUID projectId, @PathVariable UUID id) {
        return ResponseEntity.ok(bugService.getBugById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new bug")
    public ResponseEntity<BugDto.Response> createBug(
            @PathVariable UUID projectId,
            @Valid @RequestBody BugDto.CreateRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = extractUserId(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bugService.createBug(projectId, req, userId, user.getUsername()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a bug")
    public ResponseEntity<BugDto.Response> updateBug(
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @RequestBody BugDto.UpdateRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = extractUserId(user);
        return ResponseEntity.ok(bugService.updateBug(id, req, userId, user.getUsername()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a bug")
    public ResponseEntity<Void> deleteBug(@PathVariable UUID projectId, @PathVariable UUID id) {
        bugService.deleteBug(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Get bug stats for project")
    public ResponseEntity<BugDto.StatsResponse> getStats(@PathVariable UUID projectId) {
        return ResponseEntity.ok(bugService.getStats(projectId));
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text search bugs in project")
    public ResponseEntity<List<BugDto.Summary>> search(
            @PathVariable UUID projectId,
            @RequestParam String q) {
        return ResponseEntity.ok(bugService.search(projectId, q));
    }

    @GetMapping("/{id}/activity")
    @Operation(summary = "Get activity log for a bug")
    public ResponseEntity<List<BugDto.ActivityDto>> getActivity(@PathVariable UUID projectId,
                                                                @PathVariable UUID id) {
        return ResponseEntity.ok(bugService.getActivityLog(id));
    }

    @GetMapping("/{id}/duplicates")
    @Operation(summary = "Check for duplicate bugs using AI")
    public ResponseEntity<BugDto.DuplicateCheckResponse> checkDuplicates(
            @PathVariable UUID projectId, @PathVariable UUID id) {
        return ResponseEntity.ok(bugService.checkDuplicates(id));
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file attachment to a bug")
    public ResponseEntity<BugDto.AttachmentDto> uploadAttachment(
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = extractUserId(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bugService.uploadAttachment(id, file, userId));
    }

    @DeleteMapping("/{bugId}/attachments/{attachmentId}")
    @Operation(summary = "Delete an attachment")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable UUID projectId,
            @PathVariable UUID bugId,
            @PathVariable UUID attachmentId) {
        bugService.deleteAttachment(attachmentId);
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(UserDetails user) {
        try { return UUID.fromString(user.getUsername()); }
        catch (Exception e) { return UUID.randomUUID(); }
    }
}