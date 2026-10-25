package com.socialanalytics.model;

/**
 * Model representing the trend performance of a specific topic, keyword, or hashtag.
 */
public class Trend implements Comparable<Trend> {
    public enum TrendStatus {
        RISING("Rising", "↑"),
        FALLING("Falling", "↓"),
        STABLE("Stable", "→"),
        EMERGING("Emerging", "★");

        private final String label;
        private final String symbol;

        TrendStatus(String label, String symbol) {
            this.label = label;
            this.symbol = symbol;
        }

        public String getLabel() {
            return label;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    private String topic;
    private long previousMentions;
    private long currentMentions;
    private double growthPercentage;
    private long totalInteractions;
    private double averageInteractions;
    private TrendStatus status = TrendStatus.STABLE;
    private double trendScore; // 0 - 100
    private double mentionScore; // component 0-100
    private double engagementScore; // component 0-100
    private double growthScore; // component 0-100
    private double recencyScore; // component 0-100
    private String explanation; // Explanation showing why the topic received its score

    private boolean hasGrowthBaseline = true;

    public Trend() {
    }

    public Trend(String topic, long previousMentions, long currentMentions,
                 long totalInteractions) {
        this.topic = topic;
        this.previousMentions = previousMentions;
        this.currentMentions = currentMentions;
        this.totalInteractions = totalInteractions;
        this.averageInteractions = currentMentions > 0 ? (double) totalInteractions / currentMentions : 0.0;
        calculateGrowth();
    }

    public void calculateGrowth() {
        if (previousMentions == 0) {
            this.growthPercentage = 0.0;
            this.hasGrowthBaseline = false;
            this.status = (currentMentions > 0) ? TrendStatus.EMERGING : TrendStatus.STABLE;
        } else {
            this.hasGrowthBaseline = true;
            this.growthPercentage = ((double) (currentMentions - previousMentions) / previousMentions) * 100.0;
            if (growthPercentage > 15.0) {
                this.status = TrendStatus.RISING;
            } else if (growthPercentage < -15.0) {
                this.status = TrendStatus.FALLING;
            } else {
                this.status = TrendStatus.STABLE;
            }
        }
    }

    public boolean isHasGrowthBaseline() {
        return hasGrowthBaseline;
    }

    public void setHasGrowthBaseline(boolean hasGrowthBaseline) {
        this.hasGrowthBaseline = hasGrowthBaseline;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public long getPreviousMentions() {
        return previousMentions;
    }

    public void setPreviousMentions(long previousMentions) {
        this.previousMentions = previousMentions;
        calculateGrowth();
    }

    public long getCurrentMentions() {
        return currentMentions;
    }

    public void setCurrentMentions(long currentMentions) {
        this.currentMentions = currentMentions;
        this.averageInteractions = currentMentions > 0 ? (double) totalInteractions / currentMentions : 0.0;
        calculateGrowth();
    }

    public double getGrowthPercentage() {
        return growthPercentage;
    }

    public void setGrowthPercentage(double growthPercentage) {
        this.growthPercentage = growthPercentage;
    }

    public long getTotalInteractions() {
        return totalInteractions;
    }

    public void setTotalInteractions(long totalInteractions) {
        this.totalInteractions = totalInteractions;
        this.averageInteractions = currentMentions > 0 ? (double) totalInteractions / currentMentions : 0.0;
    }

    public double getAverageInteractions() {
        return averageInteractions;
    }

    public TrendStatus getStatus() {
        return status;
    }

    public void setStatus(TrendStatus status) {
        this.status = status;
    }

    public double getTrendScore() {
        return trendScore;
    }

    public void setTrendScore(double trendScore) {
        this.trendScore = trendScore;
    }

    public double getMentionScore() {
        return mentionScore;
    }

    public void setMentionScore(double mentionScore) {
        this.mentionScore = mentionScore;
    }

    public double getEngagementScore() {
        return engagementScore;
    }

    public void setEngagementScore(double engagementScore) {
        this.engagementScore = engagementScore;
    }

    public double getGrowthScore() {
        return growthScore;
    }

    public void setGrowthScore(double growthScore) {
        this.growthScore = growthScore;
    }

    public double getRecencyScore() {
        return recencyScore;
    }

    public void setRecencyScore(double recencyScore) {
        this.recencyScore = recencyScore;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    @Override
    public int compareTo(Trend o) {
        // Compare by overall trendScore descending
        return Double.compare(o.trendScore, this.trendScore);
    }

    @Override
    public String toString() {
        return String.format("%s: Score %.1f/100 (%s %+.1f%%, %d mentions)",
                topic, trendScore, status.getSymbol(), growthPercentage, currentMentions);
    }
}
