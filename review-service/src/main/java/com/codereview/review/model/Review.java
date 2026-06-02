package com.codereview.review.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(nullable = false)
    private String language;

    @Column(name = "code_snippet", columnDefinition = "TEXT", nullable = false)
    private String codeSnippet;

    @Column(name = "ai_feedback", columnDefinition = "LONGTEXT")
    private String aiFeedback;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "user_rating")
    private Integer userRating;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ReviewStatus status = ReviewStatus.PENDING;

    public enum ReviewStatus {
        PENDING, COMPLETED, FAILED
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
