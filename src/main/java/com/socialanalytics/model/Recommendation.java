package com.socialanalytics.model;

/**
 * Encapsulates an actionable, rule-based recommendation derived from analytics metrics.
 */
public class Recommendation {
    public enum Priority {
        HIGH, MEDIUM, LOW
    }

    public enum RecommendationType {
        CONTENT_TOPIC,
        POSTING_TIME,
        CONTENT_TYPE,
        HASHTAG_STRATEGY,
        SENTIMENT_ACTION
    }

    private String id;
    private RecommendationType type;
    private Priority priority;
    private String title;
    private String description;
    private String ruleTriggered;
    private double confidenceScore; // 0 - 100

    public Recommendation() {
    }

    public Recommendation(String id, RecommendationType type, Priority priority,
                          String title, String description, String ruleTriggered,
                          double confidenceScore) {
        this.id = id;
        this.type = type;
        this.priority = priority;
        this.title = title;
        this.description = description;
        this.ruleTriggered = ruleTriggered;
        this.confidenceScore = confidenceScore;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RecommendationType getType() {
        return type;
    }

    public void setType(RecommendationType type) {
        this.type = type;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRuleTriggered() {
        return ruleTriggered;
    }

    public void setRuleTriggered(String ruleTriggered) {
        this.ruleTriggered = ruleTriggered;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    @Override
    public String toString() {
        return String.format("[%s - %s] %s: %s (Confidence: %.0f%%)",
                priority, type, title, description, confidenceScore);
    }
}
