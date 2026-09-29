package com.trackforge.bug.controller;

import com.trackforge.bug.dto.CommentDto;
import com.trackforge.bug.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bugs/{bugId}/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Bug comment endpoints")
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    @Operation(summary = "List comments for a bug")
    public ResponseEntity<List<CommentDto.Response>> getAll(@PathVariable UUID bugId) {
        return ResponseEntity.ok(commentService.getAllForBug(bugId));
    }

    @PostMapping
    @Operation(summary = "Add a comment to a bug")
    public ResponseEntity<CommentDto.Response> add(
            @PathVariable UUID bugId,
            @Valid @RequestBody CommentDto.CreateRequest req,
            @AuthenticationPrincipal UserDetails user,
            @RequestHeader(value = "X-Username", required = false) String username) {

        UUID authorId = UUID.fromString(user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.add(bugId, req, authorId, username));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a comment (author only)")
    public ResponseEntity<Void> delete(
            @PathVariable UUID bugId,
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID requesterId = UUID.fromString(user.getUsername());
        commentService.delete(id, requesterId);
        return ResponseEntity.noContent().build();
    }
}