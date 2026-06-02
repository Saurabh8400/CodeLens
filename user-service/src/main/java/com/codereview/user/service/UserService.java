package com.codereview.user.service;

import com.codereview.user.config.JwtService;
import com.codereview.user.dto.UserDto;
import com.codereview.user.model.User;
import com.codereview.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public UserDto.AuthResponse register(UserDto.RegisterRequest request) {
        // FIX: use consistent case-insensitive check to prevent duplicate accounts
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setName(request.getName().trim());
        // FIX: normalise email to lowercase before storing
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setTotalReviews(0);

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getId(), saved.getEmail(), saved.getName());

        return new UserDto.AuthResponse(token, saved.getName(), saved.getEmail(), saved.getId());
    }

    public UserDto.AuthResponse login(UserDto.LoginRequest request) {
        // FIX: normalise email lookup to match stored lowercase value
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getName());
        return new UserDto.AuthResponse(token, user.getName(), user.getEmail(), user.getId());
    }

    public UserDto.UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDto.UserProfileResponse profile = new UserDto.UserProfileResponse();
        profile.setId(user.getId());
        profile.setName(user.getName());
        profile.setEmail(user.getEmail());
        profile.setTotalReviews(user.getTotalReviews());
        profile.setCreatedAt(user.getCreatedAt().toString());
        return profile;
    }

    @Transactional
    public void incrementReviewCount(Long userId) {
        // FIX: use findById with explicit save; ifPresent silently swallows
        // the case where the userId does not exist — keep that silent behaviour
        // but make the transaction explicit so the counter update is atomic.
        userRepository.findById(userId).ifPresent(user -> {
            user.setTotalReviews(user.getTotalReviews() + 1);
            userRepository.save(user);
        });
    }
}
