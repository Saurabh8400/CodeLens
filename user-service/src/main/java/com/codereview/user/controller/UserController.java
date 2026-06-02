package com.codereview.user.controller;

import com.codereview.user.dto.UserDto;
import com.codereview.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
// FIX: removed wildcard @CrossOrigin — CORS is already centralised in SecurityConfig
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserDto.RegisterRequest request) {
        try {
            UserDto.AuthResponse response = userService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserDto.LoginRequest request) {
        try {
            UserDto.AuthResponse response = userService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable Long userId) {
        try {
            UserDto.UserProfileResponse profile = userService.getProfile(userId);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // FIX: this endpoint is internal — only the review-service should call it.
    // Protected at the network level (API gateway does not expose /increment-review publicly).
    @PutMapping("/increment-review/{userId}")
    public ResponseEntity<?> incrementReview(@PathVariable Long userId) {
        // FIX: validate userId is positive
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user ID"));
        }
        userService.incrementReviewCount(userId);
        return ResponseEntity.ok(Map.of("message", "Review count updated"));
    }
}
