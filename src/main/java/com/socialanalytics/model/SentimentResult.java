package com.socialanalytics.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the results of sentiment analysis on post captions and comments.
 */
public class SentimentResult {
    private long totalAnalyzed;
    private long postsAnalyzed;
    private long commentsAnalyzed;
    private String corpusDescription = "";
    private long positiveCount;
    private long neutralCount;
    private long negativeCount;
    private double positivePercent;
    private double neutralPercent;
    private double negativePercent;
    private double averageScore; // Range: -1.0 (most negative) to +1.0 (most positive)
    private Map<String, Integer> topPositiveKeywords = new HashMap<>();
    private Map<String, Integer> topNegativeKeywords = new HashMap<>();

    public SentimentResult() {
    }

    public void calculatePercentages() {
        if (totalAnalyzed <= 0) {
            this.positivePercent = 0.0;
            this.neutralPercent = 0.0;
            this.negativePercent = 0.0;
            return;
        }
        this.positivePercent = Math.round(((double) positiveCount * 100.0 / totalAnalyzed) * 10.0) / 10.0;
        this.negativePercent = Math.round(((double) negativeCount * 100.0 / totalAnalyzed) * 10.0) / 10.0;
        // Guarantee exact 100.0% sum within 1 decimal place
        this.neutralPercent = Math.max(0.0, Math.round((100.0 - this.positivePercent - this.negativePercent) * 10.0) / 10.0);
    }

    public long getPostsAnalyzed() {
        return postsAnalyzed;
    }

    public void setPostsAnalyzed(long postsAnalyzed) {
        this.postsAnalyzed = postsAnalyzed;
    }

    public long getCommentsAnalyzed() {
        return commentsAnalyzed;
    }

    public void setCommentsAnalyzed(long commentsAnalyzed) {
        this.commentsAnalyzed = commentsAnalyzed;
    }

    public String getCorpusDescription() {
        return corpusDescription;
    }

    public void setCorpusDescription(String corpusDescription) {
        this.corpusDescription = corpusDescription;
    }

    public long getTotalAnalyzed() {
        return totalAnalyzed;
    }

    public void setTotalAnalyzed(long totalAnalyzed) {
        this.totalAnalyzed = totalAnalyzed;
    }

    public long getPositiveCount() {
        return positiveCount;
    }

    public void setPositiveCount(long positiveCount) {
        this.positiveCount = positiveCount;
    }

    public long getNeutralCount() {
        return neutralCount;
    }

    public void setNeutralCount(long neutralCount) {
        this.neutralCount = neutralCount;
    }

    public long getNegativeCount() {
        return negativeCount;
    }

    public void setNegativeCount(long negativeCount) {
        this.negativeCount = negativeCount;
    }

    public double getPositivePercent() {
        return positivePercent;
    }

    public void setPositivePercent(double positivePercent) {
        this.positivePercent = positivePercent;
    }

    public double getNeutralPercent() {
        return neutralPercent;
    }

    public void setNeutralPercent(double neutralPercent) {
        this.neutralPercent = neutralPercent;
    }

    public double getNegativePercent() {
        return negativePercent;
    }

    public void setNegativePercent(double negativePercent) {
        this.negativePercent = negativePercent;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }

    public Map<String, Integer> getTopPositiveKeywords() {
        return Collections.unmodifiableMap(topPositiveKeywords);
    }

    public void setTopPositiveKeywords(Map<String, Integer> topPositiveKeywords) {
        this.topPositiveKeywords = topPositiveKeywords != null ? new HashMap<>(topPositiveKeywords) : new HashMap<>();
    }

    public Map<String, Integer> getTopNegativeKeywords() {
        return Collections.unmodifiableMap(topNegativeKeywords);
    }

    public void setTopNegativeKeywords(Map<String, Integer> topNegativeKeywords) {
        this.topNegativeKeywords = topNegativeKeywords != null ? new HashMap<>(topNegativeKeywords) : new HashMap<>();
    }

    public String getDominantSentiment() {
        if (positiveCount >= neutralCount && positiveCount >= negativeCount) {
            return "Positive";
        } else if (negativeCount >= positiveCount && negativeCount >= neutralCount) {
            return "Negative";
        } else {
            return "Neutral";
        }
    }

    @Override
    public String toString() {
        return String.format("Sentiment [Total: %d, Positive: %.1f%%, Neutral: %.1f%%, Negative: %.1f%%]",
                totalAnalyzed, positivePercent, neutralPercent, negativePercent);
    }
}
