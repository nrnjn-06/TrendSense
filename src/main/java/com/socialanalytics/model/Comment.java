package com.socialanalytics.model;

import java.time.LocalDateTime;

/**
 * Normalized comment domain model across supported platforms.
 */
public class Comment {
    public enum SentimentType {
        POSITIVE, NEUTRAL, NEGATIVE
    }

    private String commentId;
    private String postId;
    private Platform platform;
    private String authorName;
    private String text;
    private LocalDateTime timestamp;
    private SentimentType sentiment = SentimentType.NEUTRAL;
    private double sentimentScore = 0.0;

    public Comment() {
    }

    public Comment(String commentId, String postId, Platform platform, String authorName,
                   String text, LocalDateTime timestamp) {
        this.commentId = commentId;
        this.postId = postId;
        this.platform = platform;
        this.authorName = authorName;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public SentimentType getSentiment() {
        return sentiment;
    }

    public void setSentiment(SentimentType sentiment) {
        this.sentiment = sentiment;
    }

    public double getSentimentScore() {
        return sentimentScore;
    }

    public void setSentimentScore(double sentimentScore) {
        this.sentimentScore = sentimentScore;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "commentId='" + commentId + '\'' +
                ", postId='" + postId + '\'' +
                ", sentiment=" + sentiment +
                ", sentimentScore=" + sentimentScore +
                ", text='" + (text != null && text.length() > 30 ? text.substring(0, 30) + "..." : text) + '\'' +
                '}';
    }
}
