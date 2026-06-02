package com.codereview.review.repository;

import com.codereview.review.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Review> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(r.overallScore) FROM Review r WHERE r.userId = :userId AND r.overallScore IS NOT NULL")
    Double avgScoreByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(r.userRating) FROM Review r WHERE r.userId = :userId AND r.userRating IS NOT NULL")
    Double avgRatingByUserId(@Param("userId") Long userId);

    @Query("SELECT r.language FROM Review r WHERE r.userId = :userId GROUP BY r.language ORDER BY COUNT(r) DESC LIMIT 1")
    String topLanguageByUserId(@Param("userId") Long userId);
}
