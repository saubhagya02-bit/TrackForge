package com.trackforge.auth.service;

import com.trackforge.auth.dto.AuthDto;
import com.trackforge.auth.entity.RefreshToken;
import com.trackforge.auth.entity.User;
import com.trackforge.auth.repository.RefreshTokenRepository;
import com.trackforge.auth.repository.UserRepository;
import com.trackforge.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new IllegalArgumentException("Username already taken: " + req.getUsername());
        if (userRepository.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("Email already in use: " + req.getEmail());

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .role(User.Role.DEVELOPER)
                .build();
        user = userRepository.save(user);

        // Publish user registered event to Kafka
        Map<String, Object> event = new HashMap<>();
        event.put("type", "USER_REGISTERED");
        event.put("userId", user.getId().toString());
        event.put("email", user.getEmail());
        event.put("username", user.getUsername());
        kafkaTemplate.send("user-events", user.getId().toString(), event);

        log.info("User registered: {}", user.getUsername());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthDto.AuthResponse login(AuthDto.LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );
        User user = userRepository.findByUsername(req.getUsername()).orElseThrow();

        // Revoke existing refresh tokens
        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("User logged in: {}", req.getUsername());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthDto.AuthResponse refresh(String refreshTokenStr) {
        RefreshToken rt = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now()))
            throw new IllegalArgumentException("Refresh token expired or revoked");

        rt.setRevoked(true);
        refreshTokenRepository.save(rt);

        return buildAuthResponse(rt.getUser());
    }

    @Transactional
    public void logout(String username) {
        userRepository.findByUsername(username)
                .ifPresent(user -> refreshTokenRepository.revokeAllByUserId(user.getId()));
        log.info("User logged out: {}", username);
    }

    public AuthDto.UserSummary getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return toSummary(user);
    }

    @Transactional
    public AuthDto.UserSummary updateProfile(String username, AuthDto.UpdateProfileRequest req) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());
        return toSummary(userRepository.save(user));
    }

    // Private helpers

    private AuthDto.AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("role", user.getRole().name());
        String accessToken = jwtUtil.generateToken(userDetails, claims);

        // Create new refresh token
        String refreshTokenStr = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenStr)
                .expiresAt(Instant.now().plusMillis(refreshExpiration))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthDto.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .expiresIn(jwtUtil.getExpiration())
                .user(toSummary(user))
                .build();
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

    public java.util.List<AuthDto.UserSummary> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toSummary)
                .collect(java.util.stream.Collectors.toList());
    }

    public AuthDto.UserSummary getUserById(java.util.UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        return toSummary(user);
    }

    @Transactional
    public AuthDto.UserSummary updateUserRole(java.util.UUID id, String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setRole(User.Role.valueOf(role));
        return toSummary(userRepository.save(user));
    }
}