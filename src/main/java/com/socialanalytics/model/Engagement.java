package com.socialanalytics.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates aggregated engagement metrics across posts and platforms.
 */
public class Engagement {
    private long totalPosts;
    private long totalLikes;
    private long totalComments;
    private Long totalShares;          // Nullable if shares are unsupported on platform (e.g. Instagram API)
    private Long totalViews;           // Nullable if views metric not permitted
    private double averageLikes;
    private double averageComments;
    private Double averageShares;      // Nullable
    private Double engagementRate;     // Nullable: null when valid denominator (followers or impressions) is unavailable
    private boolean engagementRateAvailable;
    private String engagementRateBasis = "UNAVAILABLE"; // "IMPRESSIONS", "FOLLOWERS", "UNAVAILABLE"
    private boolean sharesAvailable;
    private boolean viewsAvailable;
    private List<Post> topPosts = new ArrayList<>();
    private List<Post> lowestPosts = new ArrayList<>();
    private Map<Platform, PlatformBreakdown> platformBreakdowns = new HashMap<>();

    public static class PlatformBreakdown {
        private final Platform platform;
        private long postsCount;
        private long likes;
        private long comments;
        private Long shares;
        private boolean sharesAvailable;
        private Long views;
        private double averageEngagement;

        public PlatformBreakdown(Platform platform) {
            this.platform = platform;
        }

        public Platform getPlatform() {
            return platform;
        }

        public long getPostsCount() {
            return postsCount;
        }

        public void setPostsCount(long postsCount) {
            this.postsCount = postsCount;
        }

        public long getLikes() {
            return likes;
        }

        public void setLikes(long likes) {
            this.likes = likes;
        }

        public long getComments() {
            return comments;
        }

        public void setComments(long comments) {
            this.comments = comments;
        }

        public Long getShares() {
            return shares;
        }

        public void setShares(Long shares) {
            this.shares = shares;
            this.sharesAvailable = (shares != null);
        }

        public void setShares(long shares) {
            this.shares = shares;
            this.sharesAvailable = true;
        }

        public boolean isSharesAvailable() {
            return sharesAvailable;
        }

        public void setSharesAvailable(boolean sharesAvailable) {
            this.sharesAvailable = sharesAvailable;
        }

        public Long getViews() {
            return views;
        }

        public void setViews(Long views) {
            this.views = views;
        }

        public double getAverageEngagement() {
            return averageEngagement;
        }

        public void setAverageEngagement(double averageEngagement) {
            this.averageEngagement = averageEngagement;
        }

        public long getTotalInteractions() {
            return likes + comments + (shares != null ? shares : 0L);
        }
    }

    public Engagement() {
    }

    public long getTotalPosts() {
        return totalPosts;
    }

    public void setTotalPosts(long totalPosts) {
        this.totalPosts = totalPosts;
    }

    public long getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(long totalLikes) {
        this.totalLikes = totalLikes;
    }

    public long getTotalComments() {
        return totalComments;
    }

    public void setTotalComments(long totalComments) {
        this.totalComments = totalComments;
    }

    public Long getTotalShares() {
        return totalShares;
    }

    public void setTotalShares(Long totalShares) {
        this.totalShares = totalShares;
        this.sharesAvailable = (totalShares != null);
    }

    public void setTotalShares(long totalShares) {
        this.totalShares = totalShares;
        this.sharesAvailable = true;
    }

    public Long getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(Long totalViews) {
        this.totalViews = totalViews;
    }

    public double getAverageLikes() {
        return averageLikes;
    }

    public void setAverageLikes(double averageLikes) {
        this.averageLikes = averageLikes;
    }

    public double getAverageComments() {
        return averageComments;
    }

    public void setAverageComments(double averageComments) {
        this.averageComments = averageComments;
    }

    public Double getAverageShares() {
        return averageShares;
    }

    public void setAverageShares(Double averageShares) {
        this.averageShares = averageShares;
    }

    public Double getEngagementRate() {
        return engagementRate;
    }

    public void setEngagementRate(Double engagementRate) {
        this.engagementRate = engagementRate;
        this.engagementRateAvailable = (engagementRate != null && !Double.isNaN(engagementRate) && !Double.isInfinite(engagementRate));
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

    public boolean isSharesAvailable() {
        return sharesAvailable;
    }

    public void setSharesAvailable(boolean sharesAvailable) {
        this.sharesAvailable = sharesAvailable;
    }

    public boolean isViewsAvailable() {
        return viewsAvailable;
    }

    public void setViewsAvailable(boolean viewsAvailable) {
        this.viewsAvailable = viewsAvailable;
    }

    public List<Post> getTopPosts() {
        return topPosts;
    }

    public void setTopPosts(List<Post> topPosts) {
        this.topPosts = topPosts != null ? topPosts : new ArrayList<>();
    }

    public List<Post> getLowestPosts() {
        return lowestPosts;
    }

    public void setLowestPosts(List<Post> lowestPosts) {
        this.lowestPosts = lowestPosts != null ? lowestPosts : new ArrayList<>();
    }

    public Map<Platform, PlatformBreakdown> getPlatformBreakdowns() {
        return platformBreakdowns;
    }

    public void setPlatformBreakdowns(Map<Platform, PlatformBreakdown> platformBreakdowns) {
        this.platformBreakdowns = platformBreakdowns != null ? platformBreakdowns : new HashMap<>();
    }

    public long getTotalInteractions() {
        return totalLikes + totalComments + (totalShares != null ? totalShares : 0L);
    }
}
