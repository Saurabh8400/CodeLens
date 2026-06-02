package com.codereview.review.controller;

import com.codereview.review.dto.ReviewDto;
import com.codereview.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
// FIX: removed wildcard @CrossOrigin — CORS is already handled in ReviewServiceConfig
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<?> createReview(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Email", defaultValue = "") String userEmail,
            @Valid @RequestBody ReviewDto.ReviewRequest request) {
        try {
            ReviewDto.ReviewResponse response = reviewService.createReview(userId, userEmail, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserReviews(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            // FIX: cap page size to prevent accidental large fetches
            @RequestParam(defaultValue = "10") int size) {
        int cappedSize = Math.min(size, 50);
        Page<ReviewDto.ReviewResponse> reviews = reviewService.getUserReviews(userId, page, cappedSize);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<?> getReviewById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long reviewId) {
        try {
            ReviewDto.ReviewResponse review = reviewService.getReviewById(reviewId, userId);
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{reviewId}/rate")
    public ResponseEntity<?> rateReview(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewDto.RatingRequest ratingRequest) {
        try {
            if (ratingRequest.getRating() < 1 || ratingRequest.getRating() > 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "Rating must be between 1 and 5"));
            }
            ReviewDto.ReviewResponse review = reviewService.rateReview(reviewId, userId, ratingRequest.getRating());
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics(@RequestHeader("X-User-Id") Long userId) {
        ReviewDto.AnalyticsResponse analytics = reviewService.getAnalytics(userId);
        return ResponseEntity.ok(analytics);
    }
}
