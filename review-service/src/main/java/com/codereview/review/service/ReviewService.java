package com.codereview.review.service;

import com.codereview.review.dto.ReviewDto;
import com.codereview.review.model.Review;
import com.codereview.review.repository.ReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AiReviewService aiReviewService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    // FIX: wrap entire method in @Transactional so DB state stays consistent on failure
    @Transactional
    public ReviewDto.ReviewResponse createReview(Long userId, String userEmail, ReviewDto.ReviewRequest request) {
        Review review = new Review();
        review.setUserId(userId);
        review.setUserEmail(userEmail);
        review.setLanguage(request.getLanguage());
        review.setCodeSnippet(request.getCodeSnippet());
        review.setStatus(Review.ReviewStatus.PENDING);
        review = reviewRepository.save(review);

        try {
            AiReviewService.AiReviewResult result = aiReviewService.analyzeCode(
                    request.getCodeSnippet(), request.getLanguage());

            review.setAiFeedback(result.getRawFeedback());
            review.setOverallScore(result.getOverallScore());
            review.setStatus(Review.ReviewStatus.COMPLETED);
            review = reviewRepository.save(review);

            // Fire-and-forget user count increment — failure must not roll back the review
            try {
                restTemplate.put("http://localhost:8081/api/users/increment-review/" + userId, null);
            } catch (Exception e) {
                log.warn("Could not update user review count: {}", e.getMessage());
            }

            return mapToResponse(review, result);

        } catch (Exception e) {
            review.setStatus(Review.ReviewStatus.FAILED);
            reviewRepository.save(review);
            // FIX: re-throw as RuntimeException to trigger rollback on the outer tx
            throw new RuntimeException("Review processing failed: " + e.getMessage(), e);
        }
    }

    public Page<ReviewDto.ReviewResponse> getUserReviews(Long userId, int page, int size) {
        Page<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size));
        return reviews.map(this::mapToResponseSimple);
    }

    public ReviewDto.ReviewResponse getReviewById(Long reviewId, Long userId) {
        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        return mapToResponseWithParsedFeedback(review);
    }

    public ReviewDto.ReviewResponse rateReview(Long reviewId, Long userId, int rating) {
        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        review.setUserRating(rating);
        reviewRepository.save(review);
        return mapToResponseWithParsedFeedback(review);
    }

    public ReviewDto.AnalyticsResponse getAnalytics(Long userId) {
        ReviewDto.AnalyticsResponse analytics = new ReviewDto.AnalyticsResponse();
        analytics.setTotalReviews(reviewRepository.countByUserId(userId));
        // FIX: handle null averages gracefully (user with no scored reviews)
        Double avgScore = reviewRepository.avgScoreByUserId(userId);
        analytics.setAverageScore(avgScore != null ? avgScore : 0.0);
        Double avgRating = reviewRepository.avgRatingByUserId(userId);
        analytics.setAverageRating(avgRating != null ? avgRating : 0.0);
        analytics.setMostReviewedLanguage(reviewRepository.topLanguageByUserId(userId));
        return analytics;
    }

    private ReviewDto.ReviewResponse mapToResponse(Review review, AiReviewService.AiReviewResult result) {
        ReviewDto.ReviewResponse response = new ReviewDto.ReviewResponse();
        response.setId(review.getId());
        response.setLanguage(review.getLanguage());
        response.setCodeSnippet(review.getCodeSnippet());
        response.setOverallScore(review.getOverallScore());
        response.setStatus(review.getStatus().name());
        response.setCreatedAt(review.getCreatedAt().toString());
        response.setAiFeedback(review.getAiFeedback());
        response.setSummary(result.getSummary());
        response.setIssues(result.getIssues());
        response.setSuggestions(result.getSuggestions());
        return response;
    }

    private ReviewDto.ReviewResponse mapToResponseSimple(Review review) {
        ReviewDto.ReviewResponse response = new ReviewDto.ReviewResponse();
        response.setId(review.getId());
        response.setLanguage(review.getLanguage());
        // FIX: null-safe snippet truncation
        String snippet = review.getCodeSnippet();
        response.setCodeSnippet(snippet != null && snippet.length() > 100
                ? snippet.substring(0, 100) + "..."
                : snippet);
        response.setOverallScore(review.getOverallScore());
        response.setUserRating(review.getUserRating());
        response.setStatus(review.getStatus().name());
        response.setCreatedAt(review.getCreatedAt().toString());
        return response;
    }

    private ReviewDto.ReviewResponse mapToResponseWithParsedFeedback(Review review) {
        ReviewDto.ReviewResponse response = mapToResponseSimple(review);
        response.setCodeSnippet(review.getCodeSnippet());

        if (review.getAiFeedback() != null && !review.getAiFeedback().isBlank()) {
            try {
                var parsed = objectMapper.readTree(review.getAiFeedback());
                response.setSummary(parsed.path("summary").asText());

                List<ReviewDto.Issue> issues = new java.util.ArrayList<>();
                var issuesNode = parsed.path("issues");
                if (issuesNode.isArray()) {
                    for (var node : issuesNode) {
                        ReviewDto.Issue issue = new ReviewDto.Issue();
                        issue.setSeverity(node.path("severity").asText());
                        issue.setType(node.path("type").asText());
                        issue.setDescription(node.path("description").asText());
                        issue.setLineHint(node.path("lineHint").asText());
                        issues.add(issue);
                    }
                }
                response.setIssues(issues);

                List<String> suggestions = new java.util.ArrayList<>();
                var suggestionsNode = parsed.path("suggestions");
                if (suggestionsNode.isArray()) {
                    for (var s : suggestionsNode) suggestions.add(s.asText());
                }
                response.setSuggestions(suggestions);
            } catch (Exception e) {
                log.warn("Could not parse stored AI feedback: {}", e.getMessage());
            }
        }
        return response;
    }
}
