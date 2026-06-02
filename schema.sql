-- ============================================================
-- CodeScan AI — Database Schema Reference
-- NOTE: Tables are auto-created by Spring Boot (JPA)
-- This file is for reference only
-- ============================================================

-- Run this first to create the databases
CREATE DATABASE IF NOT EXISTS code_review_users;
CREATE DATABASE IF NOT EXISTS code_review_reviews;

-- ============================================================
-- Database: code_review_users
-- ============================================================
USE code_review_users;

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100)         NOT NULL,
    email         VARCHAR(255)         NOT NULL UNIQUE,
    password      VARCHAR(255)         NOT NULL,
    total_reviews INT DEFAULT 0,
    created_at    DATETIME             NOT NULL
);

-- ============================================================
-- Database: code_review_reviews
-- ============================================================
USE code_review_reviews;

CREATE TABLE IF NOT EXISTS reviews (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT               NOT NULL,
    user_email    VARCHAR(255),
    language      VARCHAR(50)          NOT NULL,
    code_snippet  TEXT                 NOT NULL,
    ai_feedback   LONGTEXT,
    overall_score INT,
    user_rating   INT,
    status        ENUM('PENDING','COMPLETED','FAILED') DEFAULT 'PENDING',
    created_at    DATETIME             NOT NULL,

    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_language (language)
);

-- ============================================================
-- Sample Queries (Analytics)
-- ============================================================

-- Average score per user
SELECT AVG(overall_score) FROM reviews WHERE user_id = 1;

-- Most reviewed language
SELECT language, COUNT(*) as cnt
FROM reviews
WHERE user_id = 1
GROUP BY language
ORDER BY cnt DESC
LIMIT 1;

-- Review history (paginated)
SELECT * FROM reviews
WHERE user_id = 1
ORDER BY created_at DESC
LIMIT 10 OFFSET 0;
