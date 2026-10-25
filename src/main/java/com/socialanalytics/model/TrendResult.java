package com.socialanalytics.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated trend analysis result comparing topic frequency and engagement over periods.
 */
public class TrendResult {
    private List<Trend> allTrends = new ArrayList<>();
    private List<Trend> risingTopics = new ArrayList<>();
    private List<Trend> fallingTopics = new ArrayList<>();
    private List<Trend> emergingTopics = new ArrayList<>();
    private List<Hashtag> topHashtags = new ArrayList<>();
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    public TrendResult() {
    }

    public List<Trend> getAllTrends() {
        return allTrends;
    }

    public void setAllTrends(List<Trend> allTrends) {
        this.allTrends = allTrends != null ? allTrends : new ArrayList<>();
    }

    public List<Trend> getRisingTopics() {
        return risingTopics;
    }

    public void setRisingTopics(List<Trend> risingTopics) {
        this.risingTopics = risingTopics != null ? risingTopics : new ArrayList<>();
    }

    public List<Trend> getFallingTopics() {
        return fallingTopics;
    }

    public void setFallingTopics(List<Trend> fallingTopics) {
        this.fallingTopics = fallingTopics != null ? fallingTopics : new ArrayList<>();
    }

    public List<Trend> getEmergingTopics() {
        return emergingTopics;
    }

    public void setEmergingTopics(List<Trend> emergingTopics) {
        this.emergingTopics = emergingTopics != null ? emergingTopics : new ArrayList<>();
    }

    public List<Hashtag> getTopHashtags() {
        return topHashtags;
    }

    public void setTopHashtags(List<Hashtag> topHashtags) {
        this.topHashtags = topHashtags != null ? topHashtags : new ArrayList<>();
    }

    public LocalDateTime getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDateTime periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDateTime getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDateTime periodEnd) {
        this.periodEnd = periodEnd;
    }
}
