package com.socialanalytics.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Normalized post domain model across all supported platforms.
 * Adheres to official API realities where some fields (like impressions) may be unavailable.
 */
public class Post {
    private String id;               // Platform or internal unique post ID
    private Long accountId;          // Reference to local SocialAccount ID
    private Platform platform;
    private String content;          // Caption / Tweet text / Post message
    private LocalDateTime timestamp;
    private String contentType;      // e.g. TEXT, IMAGE, VIDEO, REEL, CAROUSEL, LINK
    private long likes;
    private long commentsCount;
    private Long shares;             // Nullable: null means metric unavailable on this platform (e.g. Instagram API)
    private Long views;              // Nullable: official API may not provide impressions
    private String permalink;
    private List<String> hashtags = new ArrayList<>();
    private Double engagementRate;   // Calculated or platform-provided engagement rate

    public Post() {
    }

    public Post(String id, Long accountId, Platform platform, String content,
                LocalDateTime timestamp, String contentType, long likes,
                long commentsCount, Long shares, Long views, String permalink) {
        this.id = id;
        this.accountId = accountId;
        this.platform = platform;
        this.content = content;
        this.timestamp = timestamp;
        this.contentType = contentType;
        this.likes = likes;
        this.commentsCount = commentsCount;
        this.shares = (shares != null && shares >= 0) ? shares : null;
        this.views = views;
        this.permalink = permalink;
    }

    public Post(String id, Long accountId, Platform platform, String content,
                LocalDateTime timestamp, String contentType, long likes,
                long commentsCount, long shares, Long views, String permalink) {
        this(id, accountId, platform, content, timestamp, contentType, likes, commentsCount, (Long) shares, views, permalink);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getLikes() {
        return likes;
    }

    public void setLikes(long likes) {
        this.likes = Math.max(0, likes);
    }

    public long getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(long commentsCount) {
        this.commentsCount = Math.max(0, commentsCount);
    }

    public Long getShares() {
        return shares;
    }

    public void setShares(Long shares) {
        this.shares = (shares != null && shares >= 0) ? shares : null;
    }

    public void setShares(long shares) {
        this.shares = Math.max(0, shares);
    }

    public boolean isSharesAvailable() {
        return shares != null;
    }

    public Long getViews() {
        return views;
    }

    public void setViews(Long views) {
        this.views = (views != null && views >= 0) ? views : null;
    }

    public boolean isViewsAvailable() {
        return views != null;
    }

    public String getPermalink() {
        return permalink;
    }

    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    public List<String> getHashtags() {
        return Collections.unmodifiableList(hashtags);
    }

    public void setHashtags(List<String> hashtags) {
        this.hashtags = (hashtags != null) ? new ArrayList<>(hashtags) : new ArrayList<>();
    }

    public void addHashtag(String tag) {
        if (tag != null && !tag.isBlank()) {
            String clean = tag.startsWith("#") ? tag.substring(1) : tag;
            if (!this.hashtags.contains(clean.toLowerCase())) {
                this.hashtags.add(clean.toLowerCase());
            }
        }
    }

    public Double getEngagementRate() {
        return engagementRate;
    }

    public void setEngagementRate(Double engagementRate) {
        this.engagementRate = engagementRate;
    }

    /**
     * Calculates total direct interactions (likes + comments + (shares if supported)).
     */
    public long getTotalInteractions() {
        return likes + commentsCount + (shares != null ? shares : 0L);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return Objects.equals(id, post.id) && platform == post.platform;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, platform);
    }

    @Override
    public String toString() {
        return "Post{" +
                "id='" + id + '\'' +
                ", platform=" + platform +
                ", contentType='" + contentType + '\'' +
                ", likes=" + likes +
                ", comments=" + commentsCount +
                ", shares=" + shares +
                ", views=" + views +
                ", timestamp=" + timestamp +
                '}';
    }
}
