package com.trackforge.auth.controller;

import com.trackforge.auth.dto.AuthDto;
import com.trackforge.auth.entity.User;
import com.trackforge.auth.repository.UserRepository;
import com.trackforge.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management")
public class UserController {

    private final UserRepository userRepository;
    private final AuthService authService;

    @GetMapping
    @Operation(summary = "Get all users")
    public ResponseEntity<List<AuthDto.UserSummary>> getAllUsers() {
        return ResponseEntity.ok(
                userRepository.findAll().stream()
                        .map(this::toSummary)
                        .collect(Collectors.toList())
        );
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user")
    public ResponseEntity<AuthDto.UserSummary> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getProfile(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<AuthDto.UserSummary> getById(@PathVariable UUID id) {
        return userRepository.findById(id)
                .map(u -> ResponseEntity.ok(toSummary(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Change user role.
     * NOTE: During initial setup, this is open to any authenticated user
     * so you can bootstrap the first ADMIN account.
     * After setup, add back: @PreAuthorize("hasRole('ADMIN')")
     */
    @PatchMapping("/{id}/role")
    @Operation(summary = "Update user role (any authenticated user during setup)")
    public ResponseEntity<AuthDto.UserSummary> updateRole(
            @PathVariable UUID id,
            @RequestParam String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(User.Role.valueOf(role.toUpperCase()));
        userRepository.save(user);
        return ResponseEntity.ok(toSummary(user));
    }

    private AuthDto.UserSummary toSummary(User u) {
        return AuthDto.UserSummary.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .avatarUrl(u.getAvatarUrl())
                .role(u.getRole())
                .createdAt(u.getCreatedAt())
                .build();
    }
}