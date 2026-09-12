package com.trackforge.auth.controller;

import com.trackforge.auth.dto.AuthDto;
import com.trackforge.auth.service.AuthService;
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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Auth endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthDto.AuthResponse> register(@Valid @RequestBody AuthDto.RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive tokens")
    public ResponseEntity<AuthDto.AuthResponse> login(@Valid @RequestBody AuthDto.LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<AuthDto.AuthResponse> refresh(@Valid @RequestBody AuthDto.RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req.getRefreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke tokens")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails user) {
        authService.logout(user.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<AuthDto.UserSummary> getProfile(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(authService.getProfile(user.getUsername()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update profile")
    public ResponseEntity<AuthDto.UserSummary> updateProfile(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody AuthDto.UpdateProfileRequest req) {
        return ResponseEntity.ok(authService.updateProfile(user.getUsername(), req));
    }
}