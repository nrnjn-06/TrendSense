-- Social Analytics MySQL Production Schema
-- Designed for MySQL 8.x with utf8mb4 and InnoDB engine

CREATE DATABASE IF NOT EXISTS social_analytics CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE social_analytics;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS social_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    platform VARCHAR(30) NOT NULL,
    platform_account_id VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    display_name VARCHAR(150),
    profile_url VARCHAR(500),
    follower_count BIGINT DEFAULT 0,
    following_count BIGINT DEFAULT 0,
    is_connected BOOLEAN DEFAULT TRUE,
    is_demo BOOLEAN DEFAULT FALSE,
    access_token TEXT,
    token_expires_at TIMESTAMP NULL,
    last_synced_at TIMESTAMP NULL,
    INDEX idx_user_platform (user_id, platform),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS posts (
    id VARCHAR(100) NOT NULL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    platform VARCHAR(30) NOT NULL,
    content TEXT,
    timestamp TIMESTAMP NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    likes BIGINT DEFAULT 0,
    comments_count BIGINT DEFAULT 0,
    shares BIGINT DEFAULT 0,
    views BIGINT NULL,
    permalink VARCHAR(500),
    engagement_rate DOUBLE DEFAULT 0.0,
    INDEX idx_account_time (account_id, timestamp),
    INDEX idx_platform (platform),
    FOREIGN KEY (account_id) REFERENCES social_accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS comments (
    comment_id VARCHAR(100) NOT NULL PRIMARY KEY,
    post_id VARCHAR(100) NOT NULL,
    platform VARCHAR(30) NOT NULL,
    author_name VARCHAR(100),
    text TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    sentiment VARCHAR(20) DEFAULT 'NEUTRAL',
    sentiment_score DOUBLE DEFAULT 0.0,
    INDEX idx_post_sentiment (post_id, sentiment),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hashtags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tag VARCHAR(100) NOT NULL UNIQUE,
    mention_count BIGINT DEFAULT 0,
    total_interactions BIGINT DEFAULT 0,
    INDEX idx_tag (tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS post_hashtags (
    post_id VARCHAR(100) NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (post_id, tag),
    INDEX idx_hashtag_post (tag, post_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS engagement_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_posts BIGINT DEFAULT 0,
    total_likes BIGINT DEFAULT 0,
    total_comments BIGINT DEFAULT 0,
    total_shares BIGINT DEFAULT 0,
    total_views BIGINT NULL,
    average_likes DOUBLE DEFAULT 0.0,
    average_comments DOUBLE DEFAULT 0.0,
    average_shares DOUBLE DEFAULT 0.0,
    engagement_rate DOUBLE DEFAULT 0.0,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sentiment_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_analyzed BIGINT DEFAULT 0,
    positive_count BIGINT DEFAULT 0,
    neutral_count BIGINT DEFAULT 0,
    negative_count BIGINT DEFAULT 0,
    positive_pct DOUBLE DEFAULT 0.0,
    neutral_pct DOUBLE DEFAULT 0.0,
    negative_pct DOUBLE DEFAULT 0.0,
    average_score DOUBLE DEFAULT 0.0,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS trend_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    topic VARCHAR(100) NOT NULL,
    previous_mentions BIGINT DEFAULT 0,
    current_mentions BIGINT DEFAULT 0,
    growth_percentage DOUBLE DEFAULT 0.0,
    trend_score DOUBLE DEFAULT 0.0,
    status VARCHAR(30) NOT NULL,
    explanation TEXT,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS recommendations (
    id VARCHAR(100) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    rule_triggered VARCHAR(255) NOT NULL,
    confidence_score DOUBLE DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
