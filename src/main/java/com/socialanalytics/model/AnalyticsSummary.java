package com.socialanalytics.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated analytics snapshot bundling engagement, sentiment, trends,
 * best posting hours, content type performance, and recommendations.
 */
public class AnalyticsSummary {
    private long totalPosts;
    private long totalInteractions;
    private Double averageEngagementRate;
    private boolean engagementRateAvailable = false;
    private String engagementRateBasis = "UNAVAILABLE";
    private Post topPost;
    private String bestPostingTimeWindow = "Insufficient Data";
    private int bestPostingHour = -1;
    private boolean demoData = false;
    private String dataSourceDescription = "";
    private String connectedAccountName = "";
    private boolean liveDataAvailable = false;
    private LocalDateTime generatedAt = LocalDateTime.now();

    private Engagement engagement = new Engagement();
    private SentimentResult sentiment = new SentimentResult();
    private TrendResult trends = new TrendResult();
    private List<Recommendation> recommendations = new ArrayList<>();
    private Map<Integer, Double> hourlyEngagement = new HashMap<>();
    private Map<String, Double> contentTypePerformance = new HashMap<>();

    public AnalyticsSummary() {
    }

    public long getTotalPosts() {
        return totalPosts;
    }

    public void setTotalPosts(long totalPosts) {
        this.totalPosts = totalPosts;
    }

    public long getTotalInteractions() {
        return totalInteractions;
    }

    public void setTotalInteractions(long totalInteractions) {
        this.totalInteractions = totalInteractions;
    }

    public Double getAverageEngagementRate() {
        return averageEngagementRate;
    }

    public void setAverageEngagementRate(Double averageEngagementRate) {
        this.averageEngagementRate = averageEngagementRate;
        this.engagementRateAvailable = (averageEngagementRate != null && !Double.isNaN(averageEngagementRate) && !Double.isInfinite(averageEngagementRate));
    }

    public boolean isEngagementRateAvailable() {
        return engagementRateAvailable;
    }

    public void setEngagementRateAvailable(boolean engagementRateAvailable) {
        this.engagementRateAvailable = engagementRateAvailable;
    }

    public String getEngagementRateBasis() {
        return engagementRateBasis;
    }

    public void setEngagementRateBasis(String engagementRateBasis) {
        this.engagementRateBasis = engagementRateBasis;
    }

    public Post getTopPost() {
        return topPost;
    }

    public void setTopPost(Post topPost) {
        this.topPost = topPost;
    }

    public String getBestPostingTimeWindow() {
        return bestPostingTimeWindow;
    }

    public void setBestPostingTimeWindow(String bestPostingTimeWindow) {
        this.bestPostingTimeWindow = bestPostingTimeWindow;
    }

    public int getBestPostingHour() {
        return bestPostingHour;
    }

    public void setBestPostingHour(int bestPostingHour) {
        this.bestPostingHour = bestPostingHour;
    }

    public boolean isDemoData() {
        return demoData;
    }

    public void setDemoData(boolean demoData) {
        this.demoData = demoData;
    }

    public String getDataSourceDescription() {
        return dataSourceDescription;
    }

    public void setDataSourceDescription(String dataSourceDescription) {
        this.dataSourceDescription = dataSourceDescription;
    }

    public String getConnectedAccountName() {
        return connectedAccountName;
    }

    public void setConnectedAccountName(String connectedAccountName) {
        this.connectedAccountName = connectedAccountName;
    }

    public boolean isLiveDataAvailable() {
        return liveDataAvailable;
    }

    public void setLiveDataAvailable(boolean liveDataAvailable) {
        this.liveDataAvailable = liveDataAvailable;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Engagement getEngagement() {
        return engagement;
    }

    public void setEngagement(Engagement engagement) {
        this.engagement = engagement;
    }

    public SentimentResult getSentiment() {
        return sentiment;
    }

    public void setSentiment(SentimentResult sentiment) {
        this.sentiment = sentiment;
    }

    public TrendResult getTrends() {
        return trends;
    }

    public void setTrends(TrendResult trends) {
        this.trends = trends;
    }

    public List<Recommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<Recommendation> recommendations) {
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }

    public Map<Integer, Double> getHourlyEngagement() {
        return hourlyEngagement;
    }

    public void setHourlyEngagement(Map<Integer, Double> hourlyEngagement) {
        this.hourlyEngagement = hourlyEngagement != null ? hourlyEngagement : new HashMap<>();
    }

    public Map<String, Double> getContentTypePerformance() {
        return contentTypePerformance;
    }

    public void setContentTypePerformance(Map<String, Double> contentTypePerformance) {
        this.contentTypePerformance = contentTypePerformance != null ? contentTypePerformance : new HashMap<>();
    }
}
