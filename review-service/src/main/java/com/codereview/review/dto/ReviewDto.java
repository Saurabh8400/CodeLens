package com.codereview.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

public class ReviewDto {

    @Data
    public static class ReviewRequest {
        @NotBlank(message = "Code snippet is required")
        private String codeSnippet;

        @NotBlank(message = "Language is required")
        private String language;
    }

    @Data
    public static class ReviewResponse {
        private Long id;
        private String language;
        private String codeSnippet;
        private String aiFeedback;
        private Integer overallScore;
        private Integer userRating;
        private String status;
        private String createdAt;
        private List<Issue> issues;
        private List<String> suggestions;
        private String summary;
    }

    @Data
    public static class Issue {
        private String severity;   // HIGH, MEDIUM, LOW
        private String type;       // BUG, PERFORMANCE, SECURITY, STYLE
        private String description;
        private String lineHint;
    }

    @Data
    public static class RatingRequest {
        @NotNull(message = "Rating is required")
        private Integer rating;  // 1-5
    }

    @Data
    public static class AnalyticsResponse {
        private Long totalReviews;
        private Double averageScore;
        private Double averageRating;
        private String mostReviewedLanguage;
    }
}
